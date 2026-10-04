package com.womenriskmap.feature.saved

import com.womenriskmap.core.domain.model.RiskLevel
import com.womenriskmap.core.domain.model.SavedZone
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.domain.rules.LocationAnonymizer
import com.womenriskmap.core.domain.rules.ZoneRiskCalculator
import com.womenriskmap.core.testing.FakeReportRepository
import com.womenriskmap.core.testing.FakeSavedZoneRepository
import com.womenriskmap.core.testing.FakeSessionRepository
import com.womenriskmap.core.testing.PORTO_RIBEIRA
import com.womenriskmap.core.testing.TestClock
import com.womenriskmap.core.testing.runViewModelTest
import com.womenriskmap.core.testing.testReport
import com.womenriskmap.core.testing.testUser
import com.womenriskmap.feature.saved.domain.SavedZonesWithRiskUseCase
import com.womenriskmap.feature.saved.ui.screens.SavedEffect
import com.womenriskmap.feature.saved.ui.screens.SavedViewModel
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SavedViewModelTest {
    private val zone = SavedZone("s1", LocationAnonymizer.zoneId(PORTO_RIBEIRA), LocationAnonymizer.snap(PORTO_RIBEIRA), "Ribeira")

    private fun vm(saved: FakeSavedZoneRepository, session: SessionState = SessionState.SignedIn(testUser())): SavedViewModel {
        val reports = FakeReportRepository((1..3).map { testReport("r$it") })
        return SavedViewModel(saved, FakeSessionRepository(session), SavedZonesWithRiskUseCase(reports, ZoneRiskCalculator(), TestClock()))
    }

    @Test
    fun shows_saved_zones_with_current_colour() = runViewModelTest {
        val saved = FakeSavedZoneRepository().apply { set(listOf(zone)) }
        val vm = vm(saved)
        val item = vm.state.value.items.single()
        assertEquals("Ribeira", item.zone.name)
        assertEquals(RiskLevel.RED, item.risk)
    }

    @Test
    fun swipe_delete_then_undo() = runViewModelTest {
        val saved = FakeSavedZoneRepository().apply { set(listOf(zone)) }
        val vm = vm(saved)
        vm.delete(zone)
        assertEquals(SavedEffect.Deleted(zone), vm.effects.first())
        assertTrue(vm.state.value.items.isEmpty())
        vm.undoDelete(zone)
        assertEquals(zone.zoneId, vm.state.value.items.single().zone.zoneId)
    }

    @Test
    fun visitors_see_sign_up_hint() = runViewModelTest {
        val vm = vm(FakeSavedZoneRepository(), SessionState.Visitor)
        assertTrue(vm.state.value.isVisitor)
    }
}
