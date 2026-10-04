package com.womenriskmap.core.domain.model

import kotlinx.serialization.Serializable

/** Spec §6 Cor das zonas. Each level also has a distinct symbol in the UI (colour-blind support). */
@Serializable
enum class RiskLevel { GREEN, YELLOW, RED }

/** An approximate area (~100 m cell) aggregating the active reports inside it. */
@Serializable
data class Zone(
    val id: String,
    val center: GeoPoint,
    val reportCount: Int,
    val confirmationCount: Int,
    val score: Double,
    val risk: RiskLevel,
)

/** A saved zone (spec §4 Ecrã 7, §8 Zona guardada). */
@Serializable
data class SavedZone(
    val id: String,
    val zoneId: String,
    val center: GeoPoint,
    val name: String,
)

/** A geocoding search result (spec §4 Ecrã 3: rua, morada ou local). */
@Serializable
data class Place(val name: String, val detail: String?, val location: GeoPoint)
