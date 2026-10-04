package com.womenriskmap.core.domain.repository

import com.womenriskmap.core.domain.model.GeoPoint
import kotlinx.coroutines.flow.StateFlow

interface LocationProvider {
    fun hasPermission(): Boolean

    /** Current position, or null if permission is denied or location is unavailable. Never persisted unless history is enabled. */
    suspend fun currentLocation(): GeoPoint?
}

interface ConnectivityMonitor {
    val isOnline: StateFlow<Boolean>
}

interface PreferencesRepository {
    /** Spec §4 Ecrã 9: off by default. When off, the app never stores the user's location. */
    val locationHistoryEnabled: StateFlow<Boolean>
    val lastKnownLocation: StateFlow<GeoPoint?>

    suspend fun setLocationHistoryEnabled(enabled: Boolean)

    /** Persisted only when location history is enabled; otherwise ignored. */
    suspend fun rememberLocation(point: GeoPoint)
}
