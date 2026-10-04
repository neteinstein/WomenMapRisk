package com.womenriskmap.core.domain.repository

import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.Place

interface GeocodingRepository {
    suspend fun search(query: String, near: GeoPoint?): Result<List<Place>>

    /** Street or area name for an approximate point (spec §4 Ecrã 4: "Nome da rua ou zona aproximada"). */
    suspend fun nameOf(point: GeoPoint): Result<String?>
}
