package com.womenriskmap.core.domain.rules

import com.womenriskmap.core.domain.model.GeoPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor

/**
 * Spec §6 Privacidade: "Nunca se mostra o ponto exato: a localização é arredondada para uma zona
 * aproximada (raio de cerca de 50 m)."
 *
 * The world is split into ~100 m × ~100 m cells. A report is stored and shown at the CENTRE of its
 * cell, so the published point is at most ~70 m (half the diagonal) from where it happened, and
 * typically ~50 m away.
 *
 * MUST stay identical to `public.snap_to_zone()` in supabase/migrations. The server re-snaps every
 * insert, so a modified client can never store an exact point. Shared test vectors live in
 * LocationAnonymizerTest and supabase/tests/zone_grid_test.sql.
 */
object LocationAnonymizer {
    /** ~100 m of latitude, in degrees. */
    const val LAT_STEP_DEG = 0.0009

    data class Cell(val latIndex: Long, val lngIndex: Long) {
        val zoneId: String get() = "z${latIndex}_$lngIndex"
    }

    fun cellOf(point: GeoPoint): Cell {
        val latIndex = floor(point.latitude / LAT_STEP_DEG).toLong()
        val lngIndex = floor(point.longitude / lngStepFor(latIndex)).toLong()
        return Cell(latIndex, lngIndex)
    }

    fun zoneId(point: GeoPoint): String = cellOf(point).zoneId

    /** The anonymised location to store/display for [point]. */
    fun snap(point: GeoPoint): GeoPoint = centerOf(cellOf(point))

    fun centerOf(cell: Cell): GeoPoint {
        val lngStep = lngStepFor(cell.latIndex)
        return GeoPoint(
            latitude = (cell.latIndex + 0.5) * LAT_STEP_DEG,
            longitude = (cell.lngIndex + 0.5) * lngStep,
        )
    }

    fun parseZoneId(zoneId: String): Cell? {
        val parts = zoneId.removePrefix("z").split('_')
        if (!zoneId.startsWith("z") || parts.size != 2) return null
        val lat = parts[0].toLongOrNull() ?: return null
        val lng = parts[1].toLongOrNull() ?: return null
        return Cell(lat, lng)
    }

    /** Longitude step so cells stay ~100 m wide; depends only on the row, so it is deterministic. */
    private fun lngStepFor(latIndex: Long): Double {
        val rowCenterLat = (latIndex + 0.5) * LAT_STEP_DEG
        val c = cos(rowCenterLat * PI / 180.0).coerceAtLeast(0.01)
        return LAT_STEP_DEG / c
    }
}
