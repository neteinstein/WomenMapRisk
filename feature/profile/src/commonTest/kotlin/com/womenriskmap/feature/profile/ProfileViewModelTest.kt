package com.womenriskmap.feature.profile

import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.testing.FakeLocationProvider
import com.womenriskmap.core.testing.FakePreferencesRepository
import com.womenriskmap.core.testing.FakeReportRepository
import com.womenriskmap.core.testing.FakeSessionRepository
import com.womenriskmap.core.testing.TestClock
import com.womenriskmap.core.testing.runViewModelTest
import com.womenriskmap.core.testing.testReport
import com.womenriskmap.core.testing.testUser
import com.womenriskmap.feature.profile.domain.AccountRepository
import com.womenriskmap.feature.profile.ui.screens.ProfileEffect
import com.womenriskmap.feature.profile.ui.screens.ProfileViewModel
import com.womenriskmap.feature.profile.ui.screens.SettingsEffect
import com.womenriskmap.feature.profile.ui.screens.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class ProfileViewModelTest {
    private class FakeAccount(var fail: Boolean = false) : AccountRepository {
        var deleted = false
        override suspend fun exportMyData() = if (fail) Result.failure(DomainException.Network()) else Result.success("{\"profile\":{}}")
        override suspend fun deleteMyAccount(): Result<Unit> {
            deleted = true
            return Result.success(Unit)
        }
    }

    @Test
    fun shows_profile_and_my_reports_with_24h_edit() = runViewModelTest {
        val reports = FakeReportRepository().apply {
            mine = listOf(testReport("new", isMine = true, age = 1.hours), testReport("old", isMine = true, age = 30.hours))
        }
        val vm = ProfileViewModel(FakeSessionRepository(SessionState.SignedIn(testUser())), reports, TestClock())
        assertEquals("ana@example.com", vm.state.value.profile?.email)
        assertEquals(2, vm.state.value.myReports.size)
        assertTrue(vm.canEdit(vm.state.value.myReports.first { it.id == "new" }))
        assertFalse(vm.canEdit(vm.state.value.myReports.first { it.id == "old" }))
    }

    @Test
    fun edit_pseudonym_and_country() = runViewModelTest {
        val sessions = FakeSessionRepository(SessionState.SignedIn(testUser()))
        val vm = ProfileViewModel(sessions, FakeReportRepository(), TestClock())
        vm.onEdit()
        vm.onPseudonymChange("Lua")
        vm.onCountryChange("ES")
        vm.save()
        assertEquals(ProfileEffect.Saved, vm.effects.first())
        assertEquals("update:Lua:ES", sessions.calls.last())
        assertFalse(vm.state.value.editing)
    }

    @Test
    fun visitor_state_and_sign_out() = runViewModelTest {
        val sessions = FakeSessionRepository(SessionState.Visitor)
        assertTrue(ProfileViewModel(sessions, FakeReportRepository(), TestClock()).state.value.isVisitor)
        val signedIn = FakeSessionRepository(SessionState.SignedIn(testUser()))
        ProfileViewModel(signedIn, FakeReportRepository(), TestClock()).signOut()
        assertEquals(SessionState.Visitor, signedIn.state.value)
    }

    @Test
    fun settings_history_off_by_default_and_toggle() = runViewModelTest {
        val prefs = FakePreferencesRepository()
        val vm = SettingsViewModel(FakeLocationProvider(), prefs, FakeSessionRepository(SessionState.SignedIn(testUser())), FakeAccount())
        assertFalse(vm.state.value.historyEnabled)
        assertFalse(vm.state.value.locationGranted)
        vm.onHistoryChange(true)
        assertTrue(vm.state.value.historyEnabled)
    }

    @Test
    fun export_shares_json_and_errors_are_reported() = runViewModelTest {
        val vm = SettingsViewModel(FakeLocationProvider(), FakePreferencesRepository(), FakeSessionRepository(), FakeAccount())
        vm.export()
        assertIs<SettingsEffect.ShareExport>(vm.effects.first())
        val failing =
            SettingsViewModel(FakeLocationProvider(), FakePreferencesRepository(), FakeSessionRepository(), FakeAccount(fail = true))
        failing.export()
        assertIs<SettingsEffect.Error>(failing.effects.first())
    }

    @Test
    fun delete_account_requires_confirmation_and_wipes_local_history() = runViewModelTest {
        val account = FakeAccount()
        val prefs = FakePreferencesRepository().apply { setLocationHistoryEnabled(true) }
        val vm = SettingsViewModel(FakeLocationProvider(), prefs, FakeSessionRepository(SessionState.SignedIn(testUser())), account)
        vm.onDeleteTapped()
        assertTrue(vm.state.value.confirmingDelete)
        vm.confirmDelete()
        assertEquals(SettingsEffect.AccountDeleted, vm.effects.first())
        assertTrue(account.deleted)
        assertFalse(prefs.locationHistoryEnabled.value)
    }
}
