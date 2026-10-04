package com.womenriskmap.feature.auth

import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.testing.FakeSessionRepository
import com.womenriskmap.core.testing.runViewModelTest
import com.womenriskmap.feature.auth.domain.SignUpError
import com.womenriskmap.feature.auth.ui.screens.AuthEffect
import com.womenriskmap.feature.auth.ui.screens.AuthMode
import com.womenriskmap.feature.auth.ui.screens.AuthViewModel
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthViewModelTest {
    private fun AuthViewModel.fillValidSignUp() {
        onEmailChange("ana@example.com")
        onPasswordChange("segura123")
        onInviteCodeChange("abcd-efgh")
        onAcceptTermsChange(true)
        onDeclareWomanChange(true)
    }

    @Test
    fun invalid_sign_up_shows_field_errors_and_does_not_call_repository() = runViewModelTest {
        val sessions = FakeSessionRepository()
        val vm = AuthViewModel(sessions)
        vm.submit()
        assertTrue(SignUpError.INVALID_EMAIL in vm.state.value.fieldErrors)
        assertTrue(SignUpError.DECLARATION_REQUIRED in vm.state.value.fieldErrors)
        assertEquals(emptyList(), sessions.calls)
    }

    @Test
    fun valid_sign_up_goes_to_confirm_email() = runViewModelTest {
        val sessions = FakeSessionRepository()
        val vm = AuthViewModel(sessions)
        vm.fillValidSignUp()
        vm.submit()
        assertEquals(AuthEffect.ConfirmEmail("ana@example.com"), vm.effects.first())
        assertEquals("signUp:ana@example.com:PT:ABCD-EFGH", sessions.calls.single())
    }

    @Test
    fun server_errors_are_exposed_for_clear_messages() = runViewModelTest {
        val sessions = FakeSessionRepository().apply { nextError = DomainException.EmailAlreadyRegistered() }
        val vm = AuthViewModel(sessions)
        vm.fillValidSignUp()
        vm.submit()
        assertIs<DomainException.EmailAlreadyRegistered>(vm.state.value.error)
        assertEquals(false, vm.state.value.submitting)
    }

    @Test
    fun login_with_unconfirmed_email_offers_resend() = runViewModelTest {
        val sessions = FakeSessionRepository().apply { nextError = DomainException.EmailNotConfirmed() }
        val vm = AuthViewModel(sessions, initialMode = AuthMode.LOGIN)
        vm.onEmailChange("ana@example.com")
        vm.onPasswordChange("x")
        vm.submit()
        assertTrue(vm.state.value.canResendConfirmation)
        vm.resendConfirmation()
        assertEquals("resend:ana@example.com", sessions.calls.last())
    }

    @Test
    fun successful_login_emits_signed_in() = runViewModelTest {
        val vm = AuthViewModel(FakeSessionRepository(), initialMode = AuthMode.LOGIN)
        vm.onEmailChange("ana@example.com")
        vm.onPasswordChange("segura123")
        vm.submit()
        assertEquals(AuthEffect.SignedIn, vm.effects.first())
    }

    @Test
    fun invite_from_link_is_prefilled_and_formatted() = runViewModelTest {
        val vm = AuthViewModel(FakeSessionRepository(), initialInvite = "abcdefgh")
        assertEquals("ABCD-EFGH", vm.state.value.inviteCode)
    }
}
