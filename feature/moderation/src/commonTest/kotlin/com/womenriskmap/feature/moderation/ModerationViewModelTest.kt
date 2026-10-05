package com.womenriskmap.feature.moderation

import com.womenriskmap.core.domain.model.AccountStatus
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.core.domain.model.ReportStatus
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.domain.model.UserRole
import com.womenriskmap.core.testing.FakeSessionRepository
import com.womenriskmap.core.testing.runViewModelTest
import com.womenriskmap.core.testing.testReport
import com.womenriskmap.core.testing.testUser
import com.womenriskmap.feature.moderation.domain.ChainLink
import com.womenriskmap.feature.moderation.domain.ModerationAction
import com.womenriskmap.feature.moderation.domain.ModerationCounters
import com.womenriskmap.feature.moderation.domain.ModerationItem
import com.womenriskmap.feature.moderation.domain.ModerationRepository
import com.womenriskmap.feature.moderation.domain.QueueKind
import com.womenriskmap.feature.moderation.ui.screens.ModerationEffect
import com.womenriskmap.feature.moderation.ui.screens.ModerationViewModel
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModerationViewModelTest {
    private class FakeModeration : ModerationRepository {
        val actions = mutableListOf<Triple<String, ModerationAction, String>>()
        var items = listOf(
            ModerationItem(
                testReport("f1", status = ReportStatus.HIDDEN),
                QueueKind.FLAGGED,
                3,
                listOf(FlagReason.SPAM),
                AccountStatus.ACTIVE,
            ),
            ModerationItem(testReport("e1", status = ReportStatus.PENDING), QueueKind.ESTABLISHMENT, 0, emptyList(), AccountStatus.ACTIVE),
        )
        override suspend fun counters() = Result.success(ModerationCounters(4, 1, 1))
        override suspend fun queue() = Result.success(items)
        override suspend fun act(reportId: String, action: ModerationAction, reason: String): Result<Unit> {
            actions += Triple(reportId, action, reason)
            items = items.filterNot { it.report.id == reportId }
            return Result.success(Unit)
        }
        override suspend fun inviterChain(
            reportId: String,
        ) = Result.success(listOf(ChainLink(0, "user-abc123", AccountStatus.ACTIVE, 2, 0)))
    }

    private fun moderator() = FakeSessionRepository(SessionState.SignedIn(testUser(role = UserRole.MODERATOR)))

    @Test
    fun non_moderators_see_nothing() = runViewModelTest {
        val vm = ModerationViewModel(FakeModeration(), FakeSessionRepository(SessionState.SignedIn(testUser())))
        assertFalse(vm.state.value.isModerator)
        assertTrue(vm.state.value.items.isEmpty())
    }

    @Test
    fun loads_counters_and_tabs() = runViewModelTest {
        val vm = ModerationViewModel(FakeModeration(), moderator())
        assertEquals(ModerationCounters(4, 1, 1), vm.state.value.counters)
        assertEquals(listOf("f1"), vm.state.value.visibleItems.map { it.report.id })
        vm.onTab(QueueKind.ESTABLISHMENT)
        assertEquals(listOf("e1"), vm.state.value.visibleItems.map { it.report.id })
    }

    @Test
    fun actions_require_a_reason_and_are_recorded() = runViewModelTest {
        val repo = FakeModeration()
        val vm = ModerationViewModel(repo, moderator())
        val item = vm.state.value.visibleItems.first()
        vm.onAction(item, ModerationAction.REMOVE)
        vm.confirmAction() // blank reason: ignored
        assertTrue(repo.actions.isEmpty())
        vm.onReasonChange("Contém dados pessoais")
        vm.confirmAction()
        assertEquals(ModerationEffect.Done, vm.effects.first())
        assertEquals(Triple("f1", ModerationAction.REMOVE, "Contém dados pessoais"), repo.actions.single())
        assertNull(vm.state.value.pending)
        assertTrue(vm.state.value.visibleItems.isEmpty())
    }

    @Test
    fun inviter_chain_is_shown() = runViewModelTest {
        val vm = ModerationViewModel(FakeModeration(), moderator())
        vm.showChain(vm.state.value.visibleItems.first())
        assertEquals("user-abc123", vm.state.value.chain?.links?.single()?.alias)
        vm.hideChain()
        assertNull(vm.state.value.chain)
    }
}
