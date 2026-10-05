package com.womenriskmap.feature.profile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.domain.model.UserProfile
import com.womenriskmap.core.domain.model.profileOrNull
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.core.domain.rules.ReportPolicy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class ProfileUiState(
    val profile: UserProfile? = null,
    val isVisitor: Boolean = true,
    val myReports: List<Report> = emptyList(),
    val loadingReports: Boolean = false,
    val editing: Boolean = false,
    val pseudonymDraft: String = "",
    val countryDraft: String = "PT",
    val saving: Boolean = false,
)

sealed interface ProfileEffect {
    data object Saved : ProfileEffect
    data class Error(val error: Throwable) : ProfileEffect
}

/** Spec §4 Ecrã 8. */
class ProfileViewModel(
    private val sessions: SessionRepository,
    private val reports: ReportRepository,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()
    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessions.session.collect { session ->
                val profile = session.profileOrNull
                _state.update {
                    it.copy(
                        profile = profile,
                        isVisitor = session !is SessionState.SignedIn,
                        pseudonymDraft = profile?.pseudonym.orEmpty(),
                        countryDraft = profile?.country ?: "PT",
                    )
                }
                if (profile != null) loadReports()
            }
        }
    }

    fun loadReports() {
        viewModelScope.launch {
            _state.update { it.copy(loadingReports = true) }
            val list = reports.myReports().getOrDefault(_state.value.myReports)
            _state.update { it.copy(myReports = list, loadingReports = false) }
        }
    }

    fun canEdit(report: Report): Boolean = ReportPolicy.canEditOrDelete(report, clock.now())

    fun onEdit() = _state.update { it.copy(editing = true) }
    fun onCancelEdit() = _state.update { s ->
        s.copy(
            editing = false,
            pseudonymDraft = s.profile?.pseudonym.orEmpty(),
            countryDraft =
            s.profile?.country ?: "PT",
        )
    }
    fun onPseudonymChange(value: String) = _state.update { it.copy(pseudonymDraft = value.take(40)) }
    fun onCountryChange(value: String) = _state.update { it.copy(countryDraft = value) }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            sessions.updateProfile(s.pseudonymDraft, s.countryDraft)
                .onSuccess {
                    _state.update { it.copy(saving = false, editing = false) }
                    _effects.send(ProfileEffect.Saved)
                }
                .onFailure { e ->
                    _state.update { it.copy(saving = false) }
                    _effects.send(ProfileEffect.Error(e))
                }
        }
    }

    fun signOut() {
        viewModelScope.launch { sessions.signOut() }
    }
}
