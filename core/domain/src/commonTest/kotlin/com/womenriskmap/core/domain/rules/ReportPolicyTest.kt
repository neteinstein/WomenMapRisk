package com.womenriskmap.core.domain.rules

import com.womenriskmap.core.domain.NOW
import com.womenriskmap.core.domain.RIBEIRA
import com.womenriskmap.core.domain.model.AccountStatus
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.report
import com.womenriskmap.core.domain.rules.ReportPolicy.SubmitCheck
import com.womenriskmap.core.domain.user
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class ReportPolicyTest {
    private val other = GeoPoint(41.1580, -8.6291)

    @Test
    fun visitors_unconfirmed_pending_and_blocked_users_cannot_report() {
        assertEquals(SubmitCheck.NotEnabled, ReportPolicy.checkSubmit(null, emptyList(), RIBEIRA, null, NOW))
        assertEquals(SubmitCheck.NotEnabled, ReportPolicy.checkSubmit(user(emailConfirmed = false), emptyList(), RIBEIRA, null, NOW))
        assertEquals(
            SubmitCheck.NotEnabled,
            ReportPolicy.checkSubmit(user(status = AccountStatus.BLOCKED), emptyList(), RIBEIRA, null, NOW),
        )
        assertEquals(
            SubmitCheck.NotEnabled,
            ReportPolicy.checkSubmit(user(status = AccountStatus.PENDING_INVITE), emptyList(), RIBEIRA, null, NOW),
        )
    }

    @Test
    fun max_five_reports_per_day() {
        val five = (1..5).map { i -> report("r$i", at = GeoPoint(41.0 + i * 0.01, -8.6), isMine = true, age = 1.hours) }
        assertEquals(SubmitCheck.DailyLimitReached, ReportPolicy.checkSubmit(user(), five, other, null, NOW))
        assertEquals(SubmitCheck.Allowed, ReportPolicy.checkSubmit(user(), five.take(4), other, null, NOW))
    }

    @Test
    fun yesterdays_reports_do_not_count_towards_today() {
        val old = (1..5).map { i -> report("r$i", isMine = true, age = 1.days + 1.hours) }
        assertEquals(SubmitCheck.Allowed, ReportPolicy.checkSubmit(user(), old, RIBEIRA, null, NOW))
    }

    @Test
    fun same_place_same_day_is_blocked() {
        val mine = listOf(report(at = RIBEIRA, isMine = true, age = 2.hours))
        assertEquals(SubmitCheck.DuplicateSamePlaceSameDay, ReportPolicy.checkSubmit(user(), mine, RIBEIRA, null, NOW))
        assertEquals(SubmitCheck.Allowed, ReportPolicy.checkSubmit(user(), mine, other, null, NOW))
    }

    @Test
    fun description_max_300() {
        assertEquals(SubmitCheck.DescriptionTooLong, ReportPolicy.checkSubmit(user(), emptyList(), RIBEIRA, "x".repeat(301), NOW))
        assertEquals(SubmitCheck.Allowed, ReportPolicy.checkSubmit(user(), emptyList(), RIBEIRA, "x".repeat(300), NOW))
    }

    @Test
    fun edit_window_is_24h_and_own_reports_only() {
        assertTrue(ReportPolicy.canEditOrDelete(report(isMine = true, age = 23.hours), NOW))
        assertFalse(ReportPolicy.canEditOrDelete(report(isMine = true, age = 25.hours), NOW))
        assertFalse(ReportPolicy.canEditOrDelete(report(isMine = false, age = 1.hours), NOW))
    }

    @Test
    fun confirm_once_and_never_own() {
        assertTrue(ReportPolicy.canConfirm(report(), user()))
        assertFalse(ReportPolicy.canConfirm(report(isMine = true), user()))
        assertFalse(ReportPolicy.canConfirm(report(confirmedByMe = true), user()))
        assertFalse(ReportPolicy.canConfirm(report(), null))
        assertFalse(ReportPolicy.canConfirm(report(), user(emailConfirmed = false)))
    }

    @Test
    fun cannot_flag_own_report() {
        assertTrue(ReportPolicy.canFlag(report(), user()))
        assertFalse(ReportPolicy.canFlag(report(isMine = true), user()))
    }
}
