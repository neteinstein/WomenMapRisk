package com.womenriskmap.core.domain.rules

import com.womenriskmap.core.domain.model.GeoPoint
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocationAnonymizerTest {
    private fun metersBetween(a: GeoPoint, b: GeoPoint): Double {
        val r = 6_371_000.0
        val dLat = (b.latitude - a.latitude) * PI / 180
        val dLng = (b.longitude - a.longitude) * PI / 180
        val h = sin(dLat / 2).pow(2) + cos(a.latitude * PI / 180) * cos(b.latitude * PI / 180) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(h))
    }

    @Test
    fun snapped_point_is_never_the_exact_point_and_stays_within_about_70m() {
        val samples = (0 until 200).map { i -> GeoPoint(41.10 + i * 0.000437, -8.65 + i * 0.000611) }
        samples.forEach { p ->
            val snapped = LocationAnonymizer.snap(p)
            val d = metersBetween(p, snapped)
            assertTrue(d <= 75.0, "distance $d m for $p")
        }
        assertTrue(samples.any { LocationAnonymizer.snap(it) != it })
    }

    @Test
    fun snapping_is_idempotent_and_all_points_in_a_cell_share_one_zone() {
        val p = GeoPoint(41.1496, -8.6110)
        val snapped = LocationAnonymizer.snap(p)
        assertEquals(snapped, LocationAnonymizer.snap(snapped))
        assertEquals(LocationAnonymizer.zoneId(p), LocationAnonymizer.zoneId(snapped))
    }

    @Test
    fun points_far_apart_get_different_zones() {
        assertNotEquals(
            LocationAnonymizer.zoneId(GeoPoint(41.1496, -8.6110)),
            LocationAnonymizer.zoneId(GeoPoint(41.1580, -8.6291)),
        )
    }

    /** Shared vector with supabase/tests/zone_grid_test.sql: keep both in sync. */
    @Test
    fun known_vector_matches_sql_implementation() {
        assertEquals("z45721_-7205", LocationAnonymizer.zoneId(GeoPoint(41.1496, -8.6110)))
    }

    @Test
    fun zone_id_round_trips() {
        val cell = LocationAnonymizer.cellOf(GeoPoint(41.1496, -8.6110))
        assertEquals(cell, LocationAnonymizer.parseZoneId(cell.zoneId))
        assertNull(LocationAnonymizer.parseZoneId("nope"))
        assertNull(LocationAnonymizer.parseZoneId("z1_x"))
    }
}
