package com.womenriskmap.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(val latitude: Double, val longitude: Double)

@Serializable
data class BoundingBox(val south: Double, val west: Double, val north: Double, val east: Double) {
    operator fun contains(point: GeoPoint): Boolean =
        point.latitude in south..north && point.longitude in west..east
}

/** Pilot city (decision log: Porto area). The map opens here when location is unavailable. */
object PilotCity {
    const val NAME = "Porto"
    val center = GeoPoint(latitude = 41.1496, longitude = -8.6110)
    const val DEFAULT_ZOOM = 13.0
}
