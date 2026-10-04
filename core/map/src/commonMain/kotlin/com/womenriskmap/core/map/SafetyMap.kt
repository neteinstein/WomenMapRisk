package com.womenriskmap.core.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.womenriskmap.core.designsystem.theme.LocalRiskColors
import com.womenriskmap.core.domain.model.BoundingBox
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.RiskLevel
import com.womenriskmap.core.domain.model.Zone
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.exponential
import org.maplibre.compose.expressions.dsl.interpolate
import org.maplibre.compose.expressions.dsl.zoom
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/** One-shot camera command; [id] lets the same target be re-issued. */
data class CameraCommand(val point: GeoPoint, val zoom: Double, val id: Long)

/**
 * The ONLY place that touches MapLibre. Everything above it works with domain types, so the map
 * engine can be swapped (MapLibre on web is alpha) without touching screens or ViewModels.
 *
 * Zones render as translucent circles (~zone size, scaling with zoom) plus a symbol per risk level:
 * "!" for yellow, "×" for red. That makes colour non-essential (spec §4 Ecrã 3, daltonismo).
 * Green = no reports, so green areas are simply the plain map (see legend).
 */
@OptIn(FlowPreview::class)
@Composable
fun SafetyMap(
    zones: List<Zone>,
    initialCenter: GeoPoint,
    initialZoom: Double,
    cameraCommands: Flow<CameraCommand>,
    onZoneClick: (zoneId: String) -> Unit,
    onViewportChanged: (BoundingBox, GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val dark = isSystemInDarkTheme()
    val risk = LocalRiskColors.current
    val onZoneClickState = rememberUpdatedState(onZoneClick)
    val yellowJson = remember(zones) { zonesGeoJson(zones.filter { it.risk == RiskLevel.YELLOW }) }
    val redJson = remember(zones) { zonesGeoJson(zones.filter { it.risk == RiskLevel.RED }) }

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(if (dark) DARK_STYLE else LIGHT_STYLE),
        initialCameraPosition = CameraPosition(target = Position(initialCenter.longitude, initialCenter.latitude), zoom = initialZoom),
    ) {
        // ~60 m radius: at zoom 15 one dp is ~3 m at Porto's latitude, so 20dp ≈ 60 m. Exponential base 2 keeps it geographic.
        val radius = interpolate(exponential(2f), zoom(), 10 to const(4.dp), 13 to const(10.dp), 15 to const(22.dp), 20 to const(700.dp))
        val clickHandler: org.maplibre.compose.layers.FeaturesClickHandler = { features ->
            features.firstNotNullOfOrNull { (it.properties?.get("zone_id") as? JsonPrimitive)?.content }
                ?.let {
                    onZoneClickState.value(it)
                    ClickResult.Consume
                } ?: ClickResult.Pass
        }
        listOf(
            Triple("yellow", yellowJson, risk.yellow to risk.onYellow),
            Triple("red", redJson, risk.red to risk.onRed),
        ).forEach { (name, json, colors) ->
            // Declared GeoJSON is immutable: a new key recreates the source (and its layers) when zones change.
            key(json) {
                val source = rememberGeoJsonSource(GeoJsonData.JsonString(json))
                CircleLayer(
                    id = "zones-$name",
                    source = source,
                    color = const(colors.first),
                    opacity = const(0.32f),
                    radius = radius,
                    strokeColor = const(colors.first),
                    strokeWidth = const(2.dp),
                    onClick = clickHandler,
                )
                SymbolLayer(
                    id = "zones-$name-symbol",
                    source = source,
                    textField = const(if (name == "red") "×" else "!"),
                    textFont = const(listOf("Noto Sans Regular")),
                    textSize = const(1.4.em),
                    textColor = const(colors.second),
                    textHaloColor = const(colors.first),
                    textHaloWidth = const(6.dp),
                    textAllowOverlap = const(true),
                    minZoom = 12f,
                    onClick = clickHandler,
                )
            }
        }
    }

    LaunchedEffect(mapState) {
        cameraCommands.collect { cmd ->
            mapState.animateCamera(CameraUpdate(target = Position(cmd.point.longitude, cmd.point.latitude), zoom = cmd.zoom))
        }
    }
    // Report the visible area whenever the camera comes to rest (initial load, gestures, animations).
    val onViewportChangedState = rememberUpdatedState(onViewportChanged)
    LaunchedEffect(mapState) {
        snapshotFlow { mapState.isCameraMoving to mapState.cameraPosition }
            .filter { (moving, _) -> !moving }
            .debounce(200)
            .collect { (_, position) ->
                val bounds = mapState.getVisibleBounds() ?: return@collect
                onViewportChangedState.value(
                    BoundingBox(south = bounds.south, west = bounds.west, north = bounds.north, east = bounds.east),
                    GeoPoint(position.target.latitude, position.target.longitude),
                )
            }
    }

    MaplibreMap(modifier = modifier, state = mapState, viewportInsets = contentPadding)
}

/** Zones -> GeoJSON FeatureCollection (points at zone centres, `zone_id` property for clicks). */
fun zonesGeoJson(zones: List<Zone>): String = buildJsonObject {
    put("type", "FeatureCollection")
    put(
        "features",
        buildJsonArray {
            zones.forEach { z ->
                add(
                    buildJsonObject {
                        put("type", "Feature")
                        put(
                            "geometry",
                            buildJsonObject {
                                put("type", "Point")
                                put(
                                    "coordinates",
                                    buildJsonArray {
                                        add(JsonPrimitive(z.center.longitude))
                                        add(JsonPrimitive(z.center.latitude))
                                    },
                                )
                            },
                        )
                        put(
                            "properties",
                            buildJsonObject {
                                put("zone_id", z.id)
                                put("count", z.reportCount)
                            },
                        )
                    },
                )
            }
        },
    )
}.toString()

internal const val LIGHT_STYLE = "https://tiles.openfreemap.org/styles/positron"
internal const val DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"
