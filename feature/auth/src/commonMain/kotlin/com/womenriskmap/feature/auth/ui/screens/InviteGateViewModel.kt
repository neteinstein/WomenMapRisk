package com.womenriskmap.feature.auth.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.repository.InviteRepository
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.core.domain.rules.InviteCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InviteGateUiState(val code: String = "", val busy: Boolean = false, val error: Throwable? = null) {
    val canSubmit: Boolean get() = InviteCode.isValid(code) && !busy
}

/** Accounts created without a code (e.g. Google) stay `pending_invite` until a valid invite is redeemed. */
class InviteGateViewModel(private val invites: InviteRepository, private val sessions: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow(InviteGateUiState())
    val state: StateFlow<InviteGateUiState> = _state.asStateFlow()

    fun onCodeChange(value: String) = _state.update { it.copy(code = value.uppercase().take(9), error = null) }

    fun submit() {
        val code = _state.value.code
        if (!InviteCode.isValid(code)) {
            _state.update { it.copy(error = DomainException.InvalidInvite()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            val result = invites.redeem(code).onSuccess { sessions.refresh() }
            _state.update { it.copy(busy = false, error = result.exceptionOrNull()) }
        }
    }

    fun exploreWithoutAccount() {
        viewModelScope.launch { sessions.signOut() }
    }
}
