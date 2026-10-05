package com.womenriskmap.app

import com.womenriskmap.core.domain.model.AccountStatus
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.testing.FakeInviteRepository
import com.womenriskmap.core.testing.FakePreferencesRepository
import com.womenriskmap.core.testing.FakeSessionRepository
import com.womenriskmap.core.testing.runViewModelTest
import com.womenriskmap.core.testing.testUser
import kotlin.test.Test
import kotlin.test.assertEquals

class AppViewModelTest {
    @Test
    fun gate_follows_session_and_first_launch() {
        assertEquals(AppGate.LOADING, AppViewModel.gateFor(SessionState.Loading, false))
        assertEquals(AppGate.WELCOME, AppViewModel.gateFor(SessionState.Visitor, false))
        assertEquals(AppGate.MAIN, AppViewModel.gateFor(SessionState.Visitor, true))
        assertEquals(AppGate.MAIN, AppViewModel.gateFor(SessionState.SignedIn(testUser()), false))
        assertEquals(
            AppGate.INVITE_GATE,
            AppViewModel.gateFor(SessionState.SignedIn(testUser(status = AccountStatus.PENDING_INVITE)), true),
        )
    }

    @Test
    fun records_usage_day_once_per_enabled_sign_in() = runViewModelTest {
        val sessions = FakeSessionRepository(SessionState.Visitor)
        val invites = FakeInviteRepository()
        val vm = AppViewModel(sessions, FakePreferencesRepository(), invites)
        assertEquals(0, invites.usageRecorded)

        sessions.state.value = SessionState.SignedIn(testUser(emailConfirmed = false))
        assertEquals(0, invites.usageRecorded)

        sessions.state.value = SessionState.SignedIn(testUser())
        assertEquals(1, invites.usageRecorded)

        vm.onResume()
        assertEquals(2, invites.usageRecorded)
    }

    @Test
    fun welcome_done_moves_visitor_to_main() = runViewModelTest {
        val prefs = FakePreferencesRepository()
        val vm = AppViewModel(FakeSessionRepository(SessionState.Visitor), prefs, FakeInviteRepository())
        assertEquals(AppGate.WELCOME, vm.state.value.gate)
        vm.onWelcomeDone()
        assertEquals(AppGate.MAIN, vm.state.value.gate)
    }
}
