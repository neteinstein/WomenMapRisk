package com.womenriskmap.feature.auth.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckEmailUiState(val email: String, val busy: Boolean = false, val resent: Boolean = false, val error: Throwable? = null)

/** Spec §4 Ecrã 2: "Confirmação por email antes de poder reportar." */
class CheckEmailViewModel(email: String, private val sessions: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow(CheckEmailUiState(email))
    val state: StateFlow<CheckEmailUiState> = _state.asStateFlow()

    fun resend() = run { sessions.resendConfirmation(_state.value.email).onSuccess { _state.update { it.copy(resent = true) } } }

    /** After tapping the link, the user returns; re-reading the profile picks up the confirmed state. */
    fun refresh() = run { sessions.refresh() }

    private fun run(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, resent = false) }
            val result = block()
            _state.update {
                it.copy(
                    busy = false,
                    error = result.exceptionOrNull()?.let { e ->
                        e as? DomainException
                            ?: DomainException.Unknown(cause = e)
                    },
                )
            }
        }
    }
}
