package com.womenriskmap.core.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.womenriskmap.core.domain.model.GeoPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/**
 * Spec §4 Ecrã 5: "pode mover o pino no mapa". The pin stays centred; the user moves the map under it.
 * Reports the centre whenever the camera rests. The value is snapped to a zone before storage (privacy).
 */
@OptIn(FlowPreview::class)
@Composable
fun PinPickerMap(
    initialCenter: GeoPoint,
    cameraCommands: Flow<CameraCommand>,
    onCenterChanged: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val onCenter = rememberUpdatedState(onCenterChanged)
    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(if (dark) DARK_STYLE else LIGHT_STYLE),
        initialCameraPosition = CameraPosition(target = Position(initialCenter.longitude, initialCenter.latitude), zoom = 16.0),
    )
    LaunchedEffect(mapState) {
        cameraCommands.collect { cmd ->
            mapState.animateCamera(CameraUpdate(target = Position(cmd.point.longitude, cmd.point.latitude), zoom = cmd.zoom))
        }
    }
    LaunchedEffect(mapState) {
        snapshotFlow { mapState.isCameraMoving to mapState.cameraPosition }
            .filter { !it.first }
            .debounce(150)
            .collect { (_, position) -> onCenter.value(GeoPoint(position.target.latitude, position.target.longitude)) }
    }
    Box(modifier) {
        MaplibreMap(state = mapState)
        Icon(
            Icons.Filled.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            // Lift by half the icon so the pin's tip (not its centre) marks the point.
            modifier = Modifier.align(Alignment.Center).padding(bottom = 40.dp).size(44.dp),
        )
    }
}
