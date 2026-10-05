package com.womenriskmap.feature.profile

import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.AppUpdate
import com.womenriskmap.core.domain.model.UpdateCheckResult
import com.womenriskmap.core.testing.FakeAppUpdater
import com.womenriskmap.core.testing.FakeLocationProvider
import com.womenriskmap.core.testing.FakePreferencesRepository
import com.womenriskmap.core.testing.FakeSessionRepository
import com.womenriskmap.core.testing.runViewModelTest
import com.womenriskmap.feature.profile.domain.AccountRepository
import com.womenriskmap.feature.profile.ui.screens.SettingsEffect
import com.womenriskmap.feature.profile.ui.screens.SettingsViewModel
import com.womenriskmap.feature.profile.ui.screens.UpdateStatus
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SettingsUpdateTest {
    private val update = AppUpdate("1.0.7", "https://x/7-github.apk")

    private fun vm(updater: FakeAppUpdater) =
        SettingsViewModel(FakeLocationProvider(), FakePreferencesRepository(), FakeSessionRepository(), NoAccount, updater)

    private object NoAccount : AccountRepository {
        override suspend fun exportMyData() = Result.success("{}")
        override suspend fun deleteMyAccount() = Result.success(Unit)
    }

    @Test
    fun section_hidden_when_build_cannot_self_update() = runViewModelTest {
        assertFalse(vm(FakeAppUpdater(isSupported = false)).state.value.updatesSupported)
        assertTrue(vm(FakeAppUpdater(isSupported = true)).state.value.updatesSupported)
    }

    @Test
    fun check_reports_up_to_date_or_available() = runViewModelTest {
        val upToDate = vm(FakeAppUpdater(isSupported = true, result = Result.success(UpdateCheckResult.UpToDate("1.0.7"))))
        upToDate.checkForUpdates()
        assertEquals(UpdateStatus.UpToDate("1.0.7"), upToDate.state.value.update)

        val available = vm(FakeAppUpdater(isSupported = true, result = Result.success(UpdateCheckResult.Available(update))))
        available.checkForUpdates()
        assertEquals(UpdateStatus.Available(update), available.state.value.update)
    }

    @Test
    fun check_failure_shows_error_and_resets() = runViewModelTest {
        val vm = vm(FakeAppUpdater(isSupported = true, result = Result.failure(DomainException.Network())))
        vm.checkForUpdates()
        assertIs<SettingsEffect.Error>(vm.effects.first())
        assertEquals(UpdateStatus.Idle, vm.state.value.update)
    }

    @Test
    fun install_without_permission_opens_system_settings_first() = runViewModelTest {
        val updater = FakeAppUpdater(isSupported = true, result = Result.success(UpdateCheckResult.Available(update)), canInstall = false)
        val vm = vm(updater)
        vm.checkForUpdates()
        vm.installUpdate()
        assertEquals(listOf("check", "openPermission"), updater.calls)
        assertEquals(UpdateStatus.Available(update, needsPermission = true), vm.state.value.update)

        updater.canInstall = true
        vm.installUpdate()
        assertEquals("install:1.0.7", updater.calls.last())
        assertEquals(UpdateStatus.Available(update), vm.state.value.update)
    }

    @Test
    fun install_failure_shows_error() = runViewModelTest {
        val updater = FakeAppUpdater(
            isSupported = true,
            result = Result.success(UpdateCheckResult.Available(update)),
            installResult = Result.failure(DomainException.Network()),
        )
        val vm = vm(updater)
        vm.checkForUpdates()
        vm.installUpdate()
        assertIs<SettingsEffect.Error>(vm.effects.first())
    }
}
