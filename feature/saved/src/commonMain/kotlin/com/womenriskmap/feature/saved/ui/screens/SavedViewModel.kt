package com.womenriskmap.feature.saved.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womenriskmap.core.domain.model.SavedZone
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.domain.repository.SavedZoneRepository
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.feature.saved.domain.SavedZoneItem
import com.womenriskmap.feature.saved.domain.SavedZonesWithRiskUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedUiState(
    val items: List<SavedZoneItem> = emptyList(),
    val loading: Boolean = true,
    val isVisitor: Boolean = false,
)

sealed interface SavedEffect {
    data class Deleted(val zone: SavedZone) : SavedEffect
    data class Error(val error: Throwable) : SavedEffect
}

class SavedViewModel(
    private val savedZones: SavedZoneRepository,
    private val sessions: SessionRepository,
    private val withRisk: SavedZonesWithRiskUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SavedUiState())
    val state: StateFlow<SavedUiState> = _state.asStateFlow()
    private val _effects = Channel<SavedEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessions.session.collect { session ->
                val visitor = session !is SessionState.SignedIn
                _state.update { it.copy(isVisitor = visitor, loading = !visitor) }
                if (!visitor) savedZones.refresh()
            }
        }
        viewModelScope.launch {
            savedZones.savedZones.collect { zones ->
                // Show names immediately (keeping known colours), then fill in the current colours.
                val known = _state.value.items.associateBy { it.zone.id }
                _state.update { s -> s.copy(items = zones.map { z -> known[z.id] ?: SavedZoneItem(z, null) }) }
                val items = withRisk(zones)
                _state.update { it.copy(items = items, loading = false) }
            }
        }
    }

    /** Spec §4 Ecrã 7: "Deslizar para apagar". Undo re-saves the zone. */
    fun delete(zone: SavedZone) {
        viewModelScope.launch {
            savedZones.delete(zone.id)
                .onSuccess { _effects.send(SavedEffect.Deleted(zone)) }
                .onFailure { _effects.send(SavedEffect.Error(it)) }
        }
    }

    fun undoDelete(zone: SavedZone) {
        viewModelScope.launch { savedZones.save(zone.zoneId, zone.center, zone.name) }
    }
}
