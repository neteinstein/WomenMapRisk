package com.womenriskmap.feature.auth.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.core.domain.rules.Countries
import com.womenriskmap.core.domain.rules.InviteCode
import com.womenriskmap.feature.auth.domain.SignUpError
import com.womenriskmap.feature.auth.domain.SignUpForm
import com.womenriskmap.feature.auth.domain.SignUpValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_UP, LOGIN }

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_UP,
    val email: String = "",
    val password: String = "",
    val country: String = Countries.DEFAULT,
    val inviteCode: String = "",
    val acceptedTerms: Boolean = false,
    val declaredWoman: Boolean = false,
    val passwordVisible: Boolean = false,
    val submitting: Boolean = false,
    /** Field errors are shown only after the first submit attempt. */
    val fieldErrors: Set<SignUpError> = emptySet(),
    val error: DomainException? = null,
) {
    val canResendConfirmation: Boolean get() = error is DomainException.EmailNotConfirmed
}

sealed interface AuthEffect {
    data class ConfirmEmail(val email: String) : AuthEffect
    data object SignedIn : AuthEffect
}

class AuthViewModel(
    private val sessions: SessionRepository,
    initialMode: AuthMode = AuthMode.SIGN_UP,
    initialInvite: String? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(
        AuthUiState(mode = initialMode, inviteCode = initialInvite?.let { InviteCode.format(it) }.orEmpty()),
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onModeChange(mode: AuthMode) = _state.update { it.copy(mode = mode, fieldErrors = emptySet(), error = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onCountryChange(value: String) = _state.update { it.copy(country = value) }
    fun onInviteCodeChange(value: String) = _state.update { it.copy(inviteCode = value.uppercase().take(9), error = null) }
    fun onAcceptTermsChange(value: Boolean) = _state.update { it.copy(acceptedTerms = value) }
    fun onDeclareWomanChange(value: Boolean) = _state.update { it.copy(declaredWoman = value) }
    fun onTogglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun onErrorShown() = _state.update { it.copy(error = null) }

    fun submit() {
        val s = _state.value
        if (s.submitting) return
        when (s.mode) {
            AuthMode.SIGN_UP -> signUp(s)
            AuthMode.LOGIN -> login(s)
        }
    }

    private fun signUp(s: AuthUiState) {
        val errors = SignUpValidator.validate(SignUpForm(s.email, s.password, s.country, s.inviteCode, s.acceptedTerms, s.declaredWoman))
        _state.update { it.copy(fieldErrors = errors) }
        if (errors.isNotEmpty()) return
        launchSubmit {
            sessions.signUp(s.email.trim(), s.password, s.country, s.inviteCode)
                .onSuccess { _effects.send(AuthEffect.ConfirmEmail(s.email.trim())) }
        }
    }

    private fun login(s: AuthUiState) {
        val errors = buildSet {
            if (!SignUpValidator.isValidEmail(s.email)) add(SignUpError.INVALID_EMAIL)
            if (s.password.isEmpty()) add(SignUpError.WEAK_PASSWORD)
        }
        _state.update { it.copy(fieldErrors = errors) }
        if (errors.isNotEmpty()) return
        launchSubmit { sessions.signIn(s.email.trim(), s.password).onSuccess { _effects.send(AuthEffect.SignedIn) } }
    }

    fun signInWithGoogle() = launchSubmit { sessions.signInWithGoogle() }

    fun resendConfirmation() {
        val email = _state.value.email.trim()
        launchSubmit { sessions.resendConfirmation(email).onSuccess { _effects.send(AuthEffect.ConfirmEmail(email)) } }
    }

    private fun launchSubmit(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            val result = block()
            _state.update {
                it.copy(
                    submitting = false,
                    error =
                    result.exceptionOrNull() as? DomainException ?: result.exceptionOrNull()?.let { e ->
                        DomainException.Unknown(e.message, e)
                    },
                )
            }
        }
    }
}
