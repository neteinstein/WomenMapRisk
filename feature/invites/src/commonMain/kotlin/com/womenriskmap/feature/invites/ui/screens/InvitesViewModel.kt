package com.womenriskmap.feature.invites.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womenriskmap.core.domain.model.Invite
import com.womenriskmap.core.domain.repository.InviteRepository
import com.womenriskmap.core.domain.rules.InviteCode
import com.womenriskmap.core.domain.rules.InviteEligibility
import com.womenriskmap.feature.invites.domain.GetInviteOverviewUseCase
import com.womenriskmap.feature.invites.domain.inviteShareLink
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvitesUiState(
    val loading: Boolean = true,
    val status: InviteEligibility.Status? = null,
    val invites: List<Invite> = emptyList(),
    /** Women-only reminder dialog (addendum: users must be reminded when inviting). */
    val confirmingCreate: Boolean = false,
    val womenOnlyConfirmed: Boolean = false,
    val creating: Boolean = false,
    val error: Throwable? = null,
) {
    val canCreate: Boolean get() = (status as? InviteEligibility.Status.Unlocked)?.let { it.unlimited || it.remaining > 0 } == true &&
        !creating
}

sealed interface InvitesEffect {
    data class Share(val code: String, val link: String) : InvitesEffect
}

class InvitesViewModel(
    private val overview: GetInviteOverviewUseCase,
    private val invites: InviteRepository,
    private val linkBaseUrl: String,
) : ViewModel() {
    private val _state = MutableStateFlow(InvitesUiState())
    val state: StateFlow<InvitesUiState> = _state.asStateFlow()
    private val _effects = Channel<InvitesEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            overview()
                .onSuccess { o -> _state.update { it.copy(loading = false, status = o.status, invites = o.invites, error = null) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e) } }
        }
    }

    fun onCreateTapped() = _state.update { it.copy(confirmingCreate = true, womenOnlyConfirmed = false) }
    fun onWomenOnlyConfirmedChange(value: Boolean) = _state.update { it.copy(womenOnlyConfirmed = value) }
    fun onDismissCreate() = _state.update { it.copy(confirmingCreate = false) }

    fun confirmCreate() {
        if (!_state.value.womenOnlyConfirmed) return
        _state.update { it.copy(confirmingCreate = false, creating = true) }
        viewModelScope.launch {
            invites.create()
                .onSuccess { invite ->
                    _effects.send(InvitesEffect.Share(InviteCode.format(invite.code), inviteShareLink(linkBaseUrl, invite.code)))
                    _state.update { it.copy(creating = false) }
                    refresh()
                }
                .onFailure { e -> _state.update { it.copy(creating = false, error = e) } }
        }
    }

    fun share(invite: Invite) {
        _effects.trySend(InvitesEffect.Share(InviteCode.format(invite.code), inviteShareLink(linkBaseUrl, invite.code)))
    }

    fun revoke(invite: Invite) {
        viewModelScope.launch {
            invites.revoke(invite.id).onSuccess { refresh() }.onFailure { e -> _state.update { it.copy(error = e) } }
        }
    }
}
