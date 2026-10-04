package com.womenriskmap.core.data.repository

import com.womenriskmap.core.data.remote.dto.SavedZoneDto
import com.womenriskmap.core.data.remote.remote
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.SavedZone
import com.womenriskmap.core.domain.repository.SavedZoneRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseSavedZoneRepository(private val client: SupabaseClient) : SavedZoneRepository {
    private val state = MutableStateFlow<List<SavedZone>>(emptyList())
    override val savedZones: StateFlow<List<SavedZone>> = state.asStateFlow()

    override suspend fun refresh(): Result<Unit> = remote {
        state.value = client.postgrest.rpc("my_saved_zones").decodeAs<List<SavedZoneDto>>().map { it.toDomain() }
    }

    override suspend fun save(zoneId: String, center: GeoPoint, name: String): Result<SavedZone> = remote {
        val saved = client.postgrest.rpc(
            "save_zone",
            buildJsonObject {
                put("p_zone_id", zoneId)
                put("p_lat", center.latitude)
                put("p_lng", center.longitude)
                put("p_name", name)
            },
        ).decodeAs<SavedZoneDto>().toDomain()
        state.update { list -> list.filterNot { it.zoneId == saved.zoneId } + saved }
        saved
    }

    override suspend fun delete(id: String): Result<Unit> {
        val before = state.value
        state.update { list -> list.filterNot { it.id == id } } // optimistic: swipe-to-delete feels instant
        return remote {
            client.postgrest.rpc("delete_saved_zone", buildJsonObject { put("p_id", id) })
            Unit
        }.onFailure { state.value = before }
    }
}
