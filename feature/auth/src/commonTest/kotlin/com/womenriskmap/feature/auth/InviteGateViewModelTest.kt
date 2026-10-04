package com.womenriskmap.feature.auth

import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.testing.FakeInviteRepository
import com.womenriskmap.core.testing.FakeSessionRepository
import com.womenriskmap.core.testing.runViewModelTest
import com.womenriskmap.feature.auth.ui.screens.CheckEmailViewModel
import com.womenriskmap.feature.auth.ui.screens.InviteGateViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InviteGateViewModelTest {
    @Test
    fun valid_code_is_redeemed_and_profile_refreshed() = runViewModelTest {
        val invites = FakeInviteRepository()
        val sessions = FakeSessionRepository()
        val vm = InviteGateViewModel(invites, sessions)
        vm.onCodeChange("abcd-efgh")
        assertTrue(vm.state.value.canSubmit)
        vm.submit()
        assertEquals(listOf("ABCD-EFGH"), invites.redeemed)
        assertTrue("refresh" in sessions.calls)
    }

    @Test
    fun malformed_or_unknown_codes_show_invalid_invite() = runViewModelTest {
        val vm = InviteGateViewModel(FakeInviteRepository(), FakeSessionRepository())
        vm.onCodeChange("123")
        assertFalse(vm.state.value.canSubmit)
        vm.submit()
        assertIs<DomainException.InvalidInvite>(vm.state.value.error)

        vm.onCodeChange("ZZZZ-ZZZZ")
        vm.submit()
        assertIs<DomainException.InvalidInvite>(vm.state.value.error)
    }

    @Test
    fun explore_without_account_signs_out() = runViewModelTest {
        val sessions = FakeSessionRepository()
        InviteGateViewModel(FakeInviteRepository(), sessions).exploreWithoutAccount()
        assertEquals(listOf("signOut"), sessions.calls)
    }

    @Test
    fun check_email_resend_and_refresh() = runViewModelTest {
        val sessions = FakeSessionRepository()
        val vm = CheckEmailViewModel("ana@example.com", sessions)
        vm.resend()
        assertTrue(vm.state.value.resent)
        vm.refresh()
        assertEquals(listOf("resend:ana@example.com", "refresh"), sessions.calls)
    }
}
