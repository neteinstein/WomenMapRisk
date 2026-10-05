package com.womenriskmap.feature.invites

import com.womenriskmap.core.domain.model.Invite
import com.womenriskmap.core.domain.model.InviteState
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.domain.model.UserRole
import com.womenriskmap.core.domain.rules.InviteEligibility.Status
import com.womenriskmap.core.testing.FakeInviteRepository
import com.womenriskmap.core.testing.FakeSessionRepository
import com.womenriskmap.core.testing.TEST_NOW
import com.womenriskmap.core.testing.TestClock
import com.womenriskmap.core.testing.runViewModelTest
import com.womenriskmap.core.testing.testUser
import com.womenriskmap.feature.invites.domain.GetInviteOverviewUseCase
import com.womenriskmap.feature.invites.domain.inviteShareLink
import com.womenriskmap.feature.invites.ui.screens.InvitesEffect
import com.womenriskmap.feature.invites.ui.screens.InvitesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class InvitesViewModelTest {
    private val today = LocalDate(2026, 10, 4)

    private fun vm(repo: FakeInviteRepository, role: UserRole = UserRole.USER, emailConfirmed: Boolean = true) = InvitesViewModel(
        GetInviteOverviewUseCase(
            repo,
            FakeSessionRepository(SessionState.SignedIn(testUser(role = role, emailConfirmed = emailConfirmed))),
            TestClock(),
        ),
        repo,
        "https://womenriskmap.example",
    )

    private fun streak(n: Int) = (0 until n).map { LocalDate.fromEpochDays(today.toEpochDays() - it) }

    @Test
    fun new_user_sees_locked_progress() = runViewModelTest {
        val vm = vm(FakeInviteRepository(usageDays = streak(2)))
        assertEquals(Status.Locked(currentStreak = 2, distinctDays = 2), vm.state.value.status)
        assertFalse(vm.state.value.canCreate)
    }

    @Test
    fun create_requires_women_only_confirmation_then_shares() = runViewModelTest {
        val repo = FakeInviteRepository(usageDays = streak(3))
        val vm = vm(repo)
        assertIs<Status.Unlocked>(vm.state.value.status)
        vm.onCreateTapped()
        assertTrue(vm.state.value.confirmingCreate)
        vm.confirmCreate() // not confirmed yet: no-op
        assertTrue(repo.invites.isEmpty())
        vm.onWomenOnlyConfirmedChange(true)
        vm.confirmCreate()
        val share = vm.effects.first()
        assertIs<InvitesEffect.Share>(share)
        assertTrue(share.link.startsWith("https://womenriskmap.example/?invite="))
        assertEquals(Status.Unlocked(used = 1, remaining = 4), vm.state.value.status)
    }

    @Test
    fun five_invites_cap() = runViewModelTest {
        val invites = (1..5).map { Invite("i$it", "ABCDEFG$it".replace("1", "H"), TEST_NOW, TEST_NOW + 30.days, InviteState.USED) }
        val vm = vm(FakeInviteRepository(usageDays = streak(3), invites = invites))
        assertEquals(Status.Unlocked(used = 5, remaining = 0), vm.state.value.status)
        assertFalse(vm.state.value.canCreate)
    }

    @Test
    fun revoked_and_expired_invites_free_the_slot() = runViewModelTest {
        val invites = listOf(
            Invite("a", "AAAAAAAA", TEST_NOW, TEST_NOW, InviteState.REVOKED),
            Invite("b", "BBBBBBBB", TEST_NOW, TEST_NOW, InviteState.EXPIRED),
            Invite("c", "CCCCCCCC", TEST_NOW, TEST_NOW + 30.days, InviteState.PENDING),
        )
        val vm = vm(FakeInviteRepository(usageDays = streak(3), invites = invites))
        assertEquals(Status.Unlocked(used = 1, remaining = 4), vm.state.value.status)
    }

    @Test
    fun revoke_pending_invite() = runViewModelTest {
        val pending = Invite("p", "PPPPPPPP", TEST_NOW, TEST_NOW + 30.days, InviteState.PENDING)
        val repo = FakeInviteRepository(usageDays = streak(3), invites = listOf(pending))
        val vm = vm(repo)
        vm.revoke(pending)
        assertEquals(InviteState.REVOKED, vm.state.value.invites.single().state)
    }

    @Test
    fun moderators_are_unlimited_and_unconfirmed_users_cannot_invite() = runViewModelTest {
        assertTrue((vm(FakeInviteRepository(), role = UserRole.MODERATOR).state.value.status as Status.Unlocked).unlimited)
        assertEquals(Status.NotEnabled, vm(FakeInviteRepository(usageDays = streak(5)), emailConfirmed = false).state.value.status)
    }

    @Test
    fun share_link_normalizes_code() {
        assertEquals("https://x.pt/?invite=ABCDEFGH", inviteShareLink("https://x.pt/", "abcd-efgh"))
    }
}
