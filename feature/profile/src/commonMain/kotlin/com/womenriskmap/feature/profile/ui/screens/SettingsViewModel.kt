package com.womenriskmap.feature.profile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womenriskmap.core.domain.model.AppUpdate
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.domain.model.UpdateCheckResult
import com.womenriskmap.core.domain.repository.AppUpdater
import com.womenriskmap.core.domain.repository.LocationProvider
import com.womenriskmap.core.domain.repository.PreferencesRepository
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.feature.profile.domain.AccountRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val locationGranted: Boolean = false,
    val historyEnabled: Boolean = false,
    val signedIn: Boolean = false,
    val exporting: Boolean = false,
    val confirmingDelete: Boolean = false,
    val deleting: Boolean = false,
    /** Only the self-updating Android "github" build shows the Updates section. */
    val updatesSupported: Boolean = false,
    val update: UpdateStatus = UpdateStatus.Idle,
)

sealed interface UpdateStatus {
    data object Idle : UpdateStatus

    data object Checking : UpdateStatus

    data class UpToDate(val versionName: String) : UpdateStatus

    /** [needsPermission]: the user was sent to "install unknown apps" and must allow it, then tap Update again. */
    data class Available(val update: AppUpdate, val needsPermission: Boolean = false) : UpdateStatus

    data class Installing(val update: AppUpdate) : UpdateStatus
}

sealed interface SettingsEffect {
    data class ShareExport(val json: String) : SettingsEffect
    data object AccountDeleted : SettingsEffect
    data class Error(val error: Throwable) : SettingsEffect
}

/** Spec §4 Ecrã 9: location permission (with why), history (off by default), export, delete account. */
class SettingsViewModel(
    private val location: LocationProvider,
    private val preferences: PreferencesRepository,
    private val sessions: SessionRepository,
    private val account: AccountRepository,
    private val updater: AppUpdater,
) : ViewModel() {
    private val _state = MutableStateFlow(
        SettingsUiState(locationGranted = location.hasPermission(), updatesSupported = updater.isSupported),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()
    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch { preferences.locationHistoryEnabled.collect { v -> _state.update { it.copy(historyEnabled = v) } } }
        viewModelScope.launch { sessions.session.collect { s -> _state.update { it.copy(signedIn = s is SessionState.SignedIn) } } }
    }

    fun refreshPermission() = _state.update { it.copy(locationGranted = location.hasPermission()) }
    fun onPermissionResult(granted: Boolean) = _state.update { it.copy(locationGranted = granted || location.hasPermission()) }

    fun onHistoryChange(enabled: Boolean) {
        viewModelScope.launch { preferences.setLocationHistoryEnabled(enabled) }
    }

    fun export() {
        viewModelScope.launch {
            _state.update { it.copy(exporting = true) }
            account.exportMyData()
                .onSuccess { _effects.send(SettingsEffect.ShareExport(it)) }
                .onFailure { _effects.send(SettingsEffect.Error(it)) }
            _state.update { it.copy(exporting = false) }
        }
    }

    fun checkForUpdates() {
        if (_state.value.update is UpdateStatus.Checking) return
        viewModelScope.launch {
            _state.update { it.copy(update = UpdateStatus.Checking) }
            updater.checkForUpdate()
                .onSuccess { result ->
                    val status = when (result) {
                        is UpdateCheckResult.UpToDate -> UpdateStatus.UpToDate(result.currentVersionName)
                        is UpdateCheckResult.Available -> UpdateStatus.Available(result.update)
                    }
                    _state.update { it.copy(update = status) }
                }
                .onFailure { error ->
                    _state.update { it.copy(update = UpdateStatus.Idle) }
                    _effects.send(SettingsEffect.Error(error))
                }
        }
    }

    fun installUpdate() {
        val available = _state.value.update as? UpdateStatus.Available ?: return
        if (!updater.canInstallPackages()) {
            _state.update { it.copy(update = available.copy(needsPermission = true)) }
            updater.openInstallPermissionSettings()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(update = UpdateStatus.Installing(available.update)) }
            updater.downloadAndInstall(available.update)
                .onFailure { _effects.send(SettingsEffect.Error(it)) }
            // The system installer takes over from here; if the user cancels, they can tap Update again.
            _state.update { it.copy(update = UpdateStatus.Available(available.update)) }
        }
    }

    fun onDeleteTapped() = _state.update { it.copy(confirmingDelete = true) }
    fun onDismissDelete() = _state.update { it.copy(confirmingDelete = false) }

    fun confirmDelete() {
        viewModelScope.launch {
            _state.update { it.copy(confirmingDelete = false, deleting = true) }
            account.deleteMyAccount()
                .onSuccess {
                    preferences.setLocationHistoryEnabled(false) // also wipes the stored zone
                    _effects.send(SettingsEffect.AccountDeleted)
                }
                .onFailure { _effects.send(SettingsEffect.Error(it)) }
            _state.update { it.copy(deleting = false) }
        }
    }
}
