package com.womenriskmap.core.domain.repository

import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.SavedZone
import kotlinx.coroutines.flow.StateFlow

interface SavedZoneRepository {
    val savedZones: StateFlow<List<SavedZone>>

    suspend fun refresh(): Result<Unit>

    suspend fun save(zoneId: String, center: GeoPoint, name: String): Result<SavedZone>

    suspend fun delete(id: String): Result<Unit>
}
