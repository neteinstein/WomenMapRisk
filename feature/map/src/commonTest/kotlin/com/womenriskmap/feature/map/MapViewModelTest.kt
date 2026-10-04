package com.womenriskmap.feature.map

import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.BoundingBox
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.PilotCity
import com.womenriskmap.core.domain.model.ReportFilter
import com.womenriskmap.core.domain.model.ReportType
import com.womenriskmap.core.domain.model.RiskLevel
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.domain.rules.LocationAnonymizer
import com.womenriskmap.core.domain.rules.ZoneRiskCalculator
import com.womenriskmap.core.domain.usecase.LoadZonesUseCase
import com.womenriskmap.core.testing.FakeConnectivityMonitor
import com.womenriskmap.core.testing.FakeGeocodingRepository
import com.womenriskmap.core.testing.FakeLocationProvider
import com.womenriskmap.core.testing.FakePreferencesRepository
import com.womenriskmap.core.testing.FakeReportRepository
import com.womenriskmap.core.testing.FakeSavedZoneRepository
import com.womenriskmap.core.testing.FakeSessionRepository
import com.womenriskmap.core.testing.PORTO_RIBEIRA
import com.womenriskmap.core.testing.TestClock
import com.womenriskmap.core.testing.runViewModelTest
import com.womenriskmap.core.testing.testReport
import com.womenriskmap.core.testing.testUser
import com.womenriskmap.feature.map.domain.ConfirmReportUseCase
import com.womenriskmap.feature.map.domain.FlagReportUseCase
import com.womenriskmap.feature.map.domain.LoadZoneDetailUseCase
import com.womenriskmap.feature.map.ui.screens.MapEffect
import com.womenriskmap.feature.map.ui.screens.MapFocus
import com.womenriskmap.feature.map.ui.screens.MapMessage
import com.womenriskmap.feature.map.ui.screens.MapViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    private val porto = BoundingBox(41.0, -8.8, 41.3, -8.5)

    private class Env(
        val reports: FakeReportRepository = FakeReportRepository(),
        val sessions: FakeSessionRepository = FakeSessionRepository(SessionState.SignedIn(testUser())),
        val location: FakeLocationProvider = FakeLocationProvider(),
        val connectivity: FakeConnectivityMonitor = FakeConnectivityMonitor(),
        val saved: FakeSavedZoneRepository = FakeSavedZoneRepository(),
        val focus: MapFocus? = null,
    ) {
        val clock = TestClock()
        val calculator = ZoneRiskCalculator()
        fun vm() = MapViewModel(
            loadZones = LoadZonesUseCase(reports, calculator, clock),
            loadZoneDetail = LoadZoneDetailUseCase(reports, FakeGeocodingRepository(), calculator, clock),
            confirmReport = ConfirmReportUseCase(reports, sessions),
            flagReport = FlagReportUseCase(reports, sessions),
            sessions = sessions,
            reports = reports,
            savedZones = saved,
            geocoding = FakeGeocodingRepository(),
            location = location,
            connectivity = connectivity,
            preferences = FakePreferencesRepository(),
            clock = clock,
            focus = focus,
            pollInterval = null,
        )
    }

    private fun TestScope.viewport(vm: MapViewModel) {
        vm.onViewportChanged(porto, PilotCity.center)
        advanceTimeBy(300)
        runCurrent()
    }

    @Test
    fun location_refused_opens_pilot_city() = runViewModelTest {
        val vm = Env().vm()
        val first = vm.effects.first()
        assertEquals(MapEffect.MoveCamera(PilotCity.center, PilotCity.DEFAULT_ZOOM), first)
        assertTrue(vm.state.value.locationDenied)
    }

    @Test
    fun user_location_centres_map_when_permitted() = runViewModelTest {
        val here = GeoPoint(41.15, -8.62)
        val vm = Env(location = FakeLocationProvider(here)).vm()
        assertEquals(MapEffect.MoveCamera(here, MapViewModel.USER_ZOOM), vm.effects.first())
    }

    @Test
    fun loads_coloured_zones_for_viewport() = runViewModelTest {
        val reports = FakeReportRepository((1..3).map { testReport("r$it") })
        val vm = Env(reports = reports).vm()
        viewport(vm)
        assertEquals(RiskLevel.RED, vm.state.value.zones.single().risk)
        assertTrue(!vm.state.value.showEmptyState)
    }

    @Test
    fun empty_area_shows_invitation_to_contribute() = runViewModelTest {
        val vm = Env().vm()
        viewport(vm)
        assertTrue(vm.state.value.showEmptyState)
    }

    @Test
    fun offline_shows_stale_data_and_disables_reporting() = runViewModelTest {
        val reports = FakeReportRepository(listOf(testReport())).apply { stale = true }
        val env = Env(reports = reports, connectivity = FakeConnectivityMonitor(online = false))
        val vm = env.vm()
        viewport(vm)
        assertTrue(vm.state.value.isStale)
        assertTrue(!vm.state.value.reportEnabled)
        assertIs<MapEffect.MoveCamera>(vm.effects.first())
        vm.onReportTapped()
        assertEquals(MapEffect.Message(MapMessage.OFFLINE_NO_REPORT), vm.effects.first())
    }

    @Test
    fun filters_apply_and_clear() = runViewModelTest {
        val reports =
            FakeReportRepository(
                listOf(
                    testReport("a", type = ReportType.ROBBERY),
                    testReport("b", type = ReportType.POORLY_LIT, at = GeoPoint(41.16, -8.60)),
                ),
            )
        val vm = Env(reports = reports).vm()
        viewport(vm)
        assertEquals(2, vm.state.value.zones.size)
        vm.onOpenFilters()
        vm.onFilterDraftChange(ReportFilter(types = setOf(ReportType.ROBBERY)))
        vm.onApplyFilters()
        runCurrent()
        assertEquals(1, vm.state.value.zones.size)
        vm.onClearFilters()
        runCurrent()
        assertEquals(2, vm.state.value.zones.size)
    }

    @Test
    fun only_last_12_months_appear() = runViewModelTest {
        val reports = FakeReportRepository(listOf(testReport(age = 400.days)))
        val vm = Env(reports = reports).vm()
        viewport(vm)
        assertTrue(vm.state.value.zones.isEmpty())
    }

    @Test
    fun visitor_trying_to_report_gets_sign_up_prompt() = runViewModelTest {
        val vm = Env(sessions = FakeSessionRepository(SessionState.Visitor)).vm()
        vm.effects.first() // initial camera
        vm.onReportTapped()
        assertEquals(MapEffect.VisitorPrompt, vm.effects.first())
    }

    @Test
    fun zone_sheet_confirm_increments_once() = runViewModelTest {
        val report = testReport("r1", confirmations = 2)
        val env = Env(reports = FakeReportRepository(listOf(report)))
        val vm = env.vm()
        vm.onZoneSelected(report.zoneId)
        runCurrent()
        val detail = assertNotNull(vm.state.value.sheet?.detail)
        assertEquals("Rua das Flores", detail.name)
        vm.onConfirm(detail.reports.single())
        runCurrent()
        val updated = vm.state.value.sheet!!.detail!!.reports.single()
        assertEquals(3, updated.confirmations)
        assertTrue(updated.confirmedByMe)
        assertEquals(listOf("r1"), env.reports.confirmed)
        assertTrue(!vm.canConfirm(updated))
    }

    @Test
    fun cannot_confirm_own_report() = runViewModelTest {
        val mine = testReport("mine", isMine = true)
        val env = Env(reports = FakeReportRepository(listOf(mine)))
        val vm = env.vm()
        vm.onZoneSelected(mine.zoneId)
        runCurrent()
        assertTrue(!vm.canConfirm(mine))
        vm.onConfirm(mine)
        runCurrent()
        assertEquals(emptyList(), env.reports.confirmed)
    }

    @Test
    fun flag_and_save_zone() = runViewModelTest {
        val report = testReport("r1")
        val env = Env(reports = FakeReportRepository(listOf(report)))
        val vm = env.vm()
        vm.onZoneSelected(report.zoneId)
        runCurrent()
        vm.onFlag(report, FlagReason.OFFENSIVE)
        vm.onSaveZone()
        runCurrent()
        assertEquals(listOf("r1" to FlagReason.OFFENSIVE), env.reports.flagged)
        assertEquals(report.zoneId, env.saved.savedZones.value.single().zoneId)
        assertTrue(vm.state.value.sheet!!.isSaved)
    }

    @Test
    fun focus_from_saved_centres_and_opens_zone() = runViewModelTest {
        val zoneId = LocationAnonymizer.zoneId(PORTO_RIBEIRA)
        val vm = Env(reports = FakeReportRepository(listOf(testReport())), focus = MapFocus(PORTO_RIBEIRA, zoneId)).vm()
        assertIs<MapEffect.MoveCamera>(vm.effects.first())
        assertEquals(zoneId, vm.state.value.sheet?.zoneId)
    }

    @Test
    fun realtime_change_triggers_reload() = runViewModelTest {
        val env = Env()
        val vm = env.vm()
        viewport(vm)
        assertTrue(vm.state.value.zones.isEmpty())
        env.reports.reports = listOf(testReport())
        env.reports.changesFlow.tryEmit(Unit)
        advanceTimeBy(300)
        runCurrent()
        assertEquals(1, vm.state.value.zones.size)
    }

    @Test
    fun zone_detail_failure_is_reported() = runViewModelTest {
        val env = Env(reports = FakeReportRepository(listOf(testReport())))
        val vm = env.vm()
        vm.effects.first()
        env.reports.nextError = DomainException.Network()
        vm.onZoneSelected(testReport().zoneId)
        assertIs<MapEffect.Error>(vm.effects.first())
    }
}
