package com.womenriskmap.core.domain.rules

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * docs/spec/addendum-invites.md:
 *  - Only enabled accounts can invite; up to 5 invites each.
 *  - Unlocked after 3 consecutive days of usage OR 5 distinct (intermittent) days.
 *  - Moderators: no usage requirement, no cap.
 * Mirror of `public.invite_status()` in SQL (the server enforces).
 */
object InviteEligibility {
    const val MAX_INVITES = 5
    const val REQUIRED_STREAK = 3
    const val REQUIRED_DISTINCT_DAYS = 5

    sealed interface Status {
        data object NotEnabled : Status

        data class Locked(val currentStreak: Int, val distinctDays: Int) : Status

        data class Unlocked(val used: Int, val remaining: Int, val unlimited: Boolean = false) : Status
    }

    fun evaluate(
        usageDays: Collection<LocalDate>,
        today: LocalDate,
        activeInvites: Int,
        isEnabled: Boolean,
        isModerator: Boolean,
    ): Status {
        if (!isEnabled) return Status.NotEnabled
        if (isModerator) return Status.Unlocked(used = activeInvites, remaining = Int.MAX_VALUE, unlimited = true)
        val days = usageDays.filter { it <= today }.toSet()
        val unlocked = longestStreak(days) >= REQUIRED_STREAK || days.size >= REQUIRED_DISTINCT_DAYS
        if (!unlocked) return Status.Locked(currentStreak = currentStreak(days, today), distinctDays = days.size)
        return Status.Unlocked(used = activeInvites, remaining = (MAX_INVITES - activeInvites).coerceAtLeast(0))
    }

    /** Longest run of consecutive calendar days in [days]. */
    fun longestStreak(days: Set<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in days.sorted()) {
            run = if (previous != null && previous.plusOneDay() == day) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }

    /** Consecutive days ending today (or yesterday, if not opened yet today), for progress display. */
    fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in days) today else today.minus(DatePeriod(days = 1))
        var streak = 0
        while (cursor in days) {
            streak++
            cursor = cursor.minus(DatePeriod(days = 1))
        }
        return streak
    }

    private fun LocalDate.plusOneDay(): LocalDate = LocalDate.fromEpochDays(toEpochDays() + 1)
}
