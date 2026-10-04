package com.womenriskmap.feature.report

import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.DayPeriod
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.PilotCity
import com.womenriskmap.core.domain.model.ReportStatus
import com.womenriskmap.core.domain.model.ReportType
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.domain.rules.DescriptionGuard
import com.womenriskmap.core.testing.FakeGeocodingRepository
import com.womenriskmap.core.testing.FakeLocationProvider
import com.womenriskmap.core.testing.FakeReportRepository
import com.womenriskmap.core.testing.FakeSessionRepository
import com.womenriskmap.core.testing.PORTO_RIBEIRA
import com.womenriskmap.core.testing.TestClock
import com.womenriskmap.core.testing.runViewModelTest
import com.womenriskmap.core.testing.testReport
import com.womenriskmap.core.testing.testUser
import com.womenriskmap.feature.report.domain.SubmitReportUseCase
import com.womenriskmap.feature.report.domain.defaultDayPeriod
import com.womenriskmap.feature.report.ui.screens.ReportEffect
import com.womenriskmap.feature.report.ui.screens.ReportStep
import com.womenriskmap.feature.report.ui.screens.ReportViewModel
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ReportViewModelTest {
    private val clock = TestClock()

    private fun vm(
        reports: FakeReportRepository = FakeReportRepository(),
        sessions: FakeSessionRepository = FakeSessionRepository(SessionState.SignedIn(testUser())),
        location: FakeLocationProvider = FakeLocationProvider(PORTO_RIBEIRA),
        near: GeoPoint? = null,
        editingId: String? = null,
    ) = ReportViewModel(
        SubmitReportUseCase(reports, sessions, clock),
        reports,
        sessions,
        location,
        FakeGeocodingRepository(),
        clock,
        near,
        editingId,
    )

    @Test
    fun defaults_to_current_position_and_now() = runViewModelTest {
        val vm = vm()
        assertEquals(ReportEffect.MoveCamera(PORTO_RIBEIRA), vm.effects.first())
        assertEquals(PORTO_RIBEIRA, vm.state.value.location)
        assertTrue(vm.state.value.locationReady)
    }

    @Test
    fun falls_back_to_map_centre_then_pilot_city() = runViewModelTest {
        val near = GeoPoint(41.16, -8.60)
        assertEquals(near, vm(location = FakeLocationProvider(), near = near).state.value.location)
        assertEquals(PilotCity.center, vm(location = FakeLocationProvider()).state.value.location)
    }

    @Test
    fun three_taps_path_type_then_send() = runViewModelTest {
        val reports = FakeReportRepository()
        val vm = vm(reports = reports)
        assertFalse(vm.state.value.canSubmit)
        vm.onTypeSelected(ReportType.POORLY_LIT)
        assertTrue(vm.state.value.canSubmit)
        vm.submit()
        assertEquals(ReportStep.DONE, vm.state.value.step)
        assertEquals(ReportType.POORLY_LIT, reports.submitted.single().type)
        assertFalse(vm.state.value.showSupport)
    }

    @Test
    fun assault_shows_support_with_country_contacts() = runViewModelTest {
        val vm = vm()
        vm.onTypeSelected(ReportType.ASSAULT)
        vm.submit()
        assertTrue(vm.state.value.showSupport)
        assertTrue(vm.state.value.emergencyContacts.any { it.number == "112" })
    }

    @Test
    fun establishment_reports_are_pending_review() = runViewModelTest {
        val vm = vm()
        vm.onTypeSelected(ReportType.OTHER)
        vm.onEstablishmentChange(true)
        vm.submit()
        assertEquals(ReportStatus.PENDING, vm.state.value.submitted?.status)
        assertTrue(vm.state.value.pendingReview)
    }

    @Test
    fun description_is_capped_and_inspected() = runViewModelTest {
        val vm = vm()
        vm.onDescriptionChange("x".repeat(400))
        assertEquals(300, vm.state.value.description.length)
        vm.onDescriptionChange("carro AA-12-34")
        assertTrue(DescriptionGuard.Finding.LICENSE_PLATE in vm.state.value.findings)
    }

    @Test
    fun daily_limit_is_enforced_before_submitting() = runViewModelTest {
        val reports = FakeReportRepository().apply {
            mine = (1..5).map { testReport("m$it", at = GeoPoint(41.0 + it * 0.01, -8.6), isMine = true, age = 1.hours) }
        }
        val vm = vm(reports = reports)
        vm.onTypeSelected(ReportType.ROBBERY)
        vm.submit()
        assertIs<DomainException.DailyReportLimit>(vm.state.value.error)
        assertTrue(reports.submitted.isEmpty())
    }

    @Test
    fun unconfirmed_email_cannot_submit() = runViewModelTest {
        val vm = vm(sessions = FakeSessionRepository(SessionState.SignedIn(testUser(emailConfirmed = false))))
        vm.onTypeSelected(ReportType.ROBBERY)
        vm.submit()
        assertIs<DomainException.EmailNotConfirmed>(vm.state.value.error)
    }

    @Test
    fun edit_within_24h_loads_and_can_delete() = runViewModelTest {
        val mine = testReport("m1", isMine = true, age = 2.hours, type = ReportType.FOLLOWED)
        val reports = FakeReportRepository().apply { this.mine = listOf(mine) }
        val vm = vm(reports = reports, editingId = "m1")
        assertEquals(ReportType.FOLLOWED, vm.state.value.type)
        assertTrue(vm.state.value.isEdit)
        vm.delete()
        assertEquals(listOf("m1"), reports.deleted)
    }

    @Test
    fun edit_after_24h_is_refused() = runViewModelTest {
        val reports = FakeReportRepository().apply { mine = listOf(testReport("old", isMine = true, age = 25.hours)) }
        val vm = vm(reports = reports, editingId = "old")
        assertIs<DomainException.EditWindowExpired>(vm.state.value.error)
    }

    @Test
    fun day_period_default_follows_clock() {
        assertEquals(DayPeriod.DAY, defaultDayPeriod(Instant.parse("2026-10-04T12:00:00Z"), TimeZone.UTC))
        assertEquals(DayPeriod.NIGHT, defaultDayPeriod(Instant.parse("2026-10-04T22:30:00Z"), TimeZone.UTC))
    }
}
