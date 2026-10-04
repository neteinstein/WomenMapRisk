package com.womenriskmap.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womenriskmap.core.domain.model.AccountStatus
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.domain.model.profileOrNull
import com.womenriskmap.core.domain.repository.InviteRepository
import com.womenriskmap.core.domain.repository.PreferencesRepository
import com.womenriskmap.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Where the app should be, derived from session + first-launch state. */
enum class AppGate { LOADING, WELCOME, INVITE_GATE, MAIN }

data class AppUiState(val gate: AppGate = AppGate.LOADING, val session: SessionState = SessionState.Loading)

class AppViewModel(
    private val sessions: SessionRepository,
    private val preferences: PreferencesRepository,
    private val invites: InviteRepository,
) : ViewModel() {

    val state: StateFlow<AppUiState> = combine(sessions.session, preferences.welcomeSeen) { session, welcomeSeen ->
        AppUiState(gate = gateFor(session, welcomeSeen), session = session)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    init {
        // Invite addendum: each day a signed-in, enabled user opens the app counts as a usage day.
        viewModelScope.launch {
            sessions.session
                .map { it.profileOrNull?.takeIf { p -> p.isEnabled }?.id }
                .distinctUntilChanged()
                .filter { it != null }
                .collect { invites.recordUsage() }
        }
    }

    /** Call on app resume (a new calendar day may have started). Idempotent server-side. */
    fun onResume() {
        if (sessions.session.value.profileOrNull?.isEnabled == true) viewModelScope.launch { invites.recordUsage() }
    }

    fun onWelcomeDone() {
        viewModelScope.launch { preferences.setWelcomeSeen() }
    }

    companion object {
        fun gateFor(session: SessionState, welcomeSeen: Boolean): AppGate = when (session) {
            SessionState.Loading -> AppGate.LOADING
            is SessionState.SignedIn ->
                if (session.profile.status == AccountStatus.PENDING_INVITE) AppGate.INVITE_GATE else AppGate.MAIN
            SessionState.Visitor -> if (welcomeSeen) AppGate.MAIN else AppGate.WELCOME
        }
    }
}
