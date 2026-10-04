package com.womenriskmap.core.data.repository

import com.womenriskmap.core.data.local.LocalStores
import com.womenriskmap.core.data.local.StoredPreferences
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.repository.PreferencesRepository
import com.womenriskmap.core.domain.rules.LocationAnonymizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class KStorePreferencesRepository(private val stores: LocalStores, scope: CoroutineScope) : PreferencesRepository {
    private val prefs = stores.preferences.updates.map { it ?: StoredPreferences() }

    override val locationHistoryEnabled: StateFlow<Boolean> =
        prefs.map { it.locationHistoryEnabled }.stateIn(scope, SharingStarted.Eagerly, false)
    override val lastKnownLocation: StateFlow<GeoPoint?> =
        prefs.map { it.lastKnownLocation }.stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun setLocationHistoryEnabled(enabled: Boolean) = stores.preferences.update {
        val current = it ?: StoredPreferences()
        // Turning history off deletes what was stored (spec §6: only stored if the user enables it).
        current.copy(locationHistoryEnabled = enabled, lastKnownLocation = if (enabled) current.lastKnownLocation else null)
    }

    override suspend fun rememberLocation(point: GeoPoint) = stores.preferences.update {
        val current = it ?: StoredPreferences()
        // Even when enabled, only the anonymised zone centre is kept on-device.
        if (current.locationHistoryEnabled) current.copy(lastKnownLocation = LocationAnonymizer.snap(point)) else current
    }
}
