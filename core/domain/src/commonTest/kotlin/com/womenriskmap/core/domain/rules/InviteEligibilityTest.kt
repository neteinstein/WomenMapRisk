package com.womenriskmap.core.domain.rules

import com.womenriskmap.core.domain.rules.InviteEligibility.Status
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class InviteEligibilityTest {
    private val today = LocalDate(2026, 10, 4)

    private fun d(s: String) = LocalDate.parse(s)

    private fun eval(days: List<LocalDate>, active: Int = 0, enabled: Boolean = true, moderator: Boolean = false) =
        InviteEligibility.evaluate(days, today, active, enabled, moderator)

    @Test
    fun disabled_accounts_cannot_invite() {
        assertEquals(Status.NotEnabled, eval(listOf(d("2026-10-02"), d("2026-10-03"), d("2026-10-04")), enabled = false))
    }

    @Test
    fun new_user_is_locked_with_progress() {
        assertEquals(Status.Locked(currentStreak = 1, distinctDays = 1), eval(listOf(today)))
        assertEquals(Status.Locked(currentStreak = 2, distinctDays = 2), eval(listOf(d("2026-10-03"), today)))
    }

    @Test
    fun three_consecutive_days_unlock() {
        assertEquals(Status.Unlocked(used = 0, remaining = 5), eval(listOf(d("2026-10-02"), d("2026-10-03"), today)))
    }

    @Test
    fun streak_in_the_past_still_counts() {
        assertEquals(Status.Unlocked(0, 5), eval(listOf(d("2026-09-01"), d("2026-09-02"), d("2026-09-03"))))
    }

    @Test
    fun streak_across_month_and_year_boundaries() {
        assertEquals(3, InviteEligibility.longestStreak(setOf(d("2025-12-31"), d("2026-01-01"), d("2026-01-02"))))
        assertEquals(3, InviteEligibility.longestStreak(setOf(d("2026-02-27"), d("2026-02-28"), d("2026-03-01"))))
    }

    @Test
    fun five_intermittent_days_unlock() {
        val days = listOf("2026-09-01", "2026-09-05", "2026-09-10", "2026-09-20", "2026-10-01").map(::d)
        assertEquals(Status.Unlocked(0, 5), eval(days))
    }

    @Test
    fun four_intermittent_days_stay_locked() {
        val days = listOf("2026-09-01", "2026-09-05", "2026-09-10", "2026-09-20").map(::d)
        assertEquals(Status.Locked(currentStreak = 0, distinctDays = 4), eval(days))
    }

    @Test
    fun duplicate_and_future_days_are_ignored() {
        val days = listOf(d("2026-10-03"), d("2026-10-03"), today, d("2026-10-05"))
        assertEquals(Status.Locked(currentStreak = 2, distinctDays = 2), eval(days))
    }

    @Test
    fun cap_of_five_invites() {
        val days = listOf(d("2026-10-02"), d("2026-10-03"), today)
        assertEquals(Status.Unlocked(used = 3, remaining = 2), eval(days, active = 3))
        assertEquals(Status.Unlocked(used = 5, remaining = 0), eval(days, active = 5))
    }

    @Test
    fun moderators_are_exempt() {
        val status = eval(emptyList(), active = 40, moderator = true)
        assertEquals(true, (status as Status.Unlocked).unlimited)
    }

    @Test
    fun current_streak_counts_from_yesterday_if_not_opened_today() {
        assertEquals(2, InviteEligibility.currentStreak(setOf(d("2026-10-02"), d("2026-10-03")), today))
    }
}
