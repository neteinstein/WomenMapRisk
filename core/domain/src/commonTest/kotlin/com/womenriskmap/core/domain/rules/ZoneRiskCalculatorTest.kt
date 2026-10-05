package com.womenriskmap.core.domain.rules

import com.womenriskmap.core.domain.NOW
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.ReportStatus
import com.womenriskmap.core.domain.model.RiskLevel
import com.womenriskmap.core.domain.report
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class ZoneRiskCalculatorTest {
    private val calc = ZoneRiskCalculator()

    @Test
    fun no_active_reports_is_green() {
        assertEquals(RiskLevel.GREEN, calc.riskOf(emptyList(), NOW).second)
        assertEquals(RiskLevel.GREEN, calc.riskOf(listOf(report(age = 400.days)), NOW).second)
    }

    @Test
    fun one_or_two_fresh_reports_are_yellow() {
        assertEquals(RiskLevel.YELLOW, calc.riskOf(listOf(report()), NOW).second)
        assertEquals(RiskLevel.YELLOW, calc.riskOf(listOf(report("a"), report("b")), NOW).second)
    }

    @Test
    fun three_fresh_reports_are_red() {
        assertEquals(RiskLevel.RED, calc.riskOf(listOf(report("a"), report("b"), report("c")), NOW).second)
    }

    @Test
    fun only_last_12_months_count() {
        val old = (1..5).map { report("o$it", age = 366.days) }
        assertEquals(RiskLevel.GREEN, calc.riskOf(old, NOW).second)
    }

    @Test
    fun pending_hidden_and_removed_reports_do_not_count() {
        val reports =
            listOf(ReportStatus.PENDING, ReportStatus.HIDDEN, ReportStatus.REMOVED).mapIndexed {
                    i,
                    s,
                ->
                report("r$i", status = s)
            }
        assertEquals(RiskLevel.GREEN, calc.riskOf(reports, NOW).second)
    }

    @Test
    fun recent_and_confirmed_reports_weigh_more() {
        assertTrue(calc.weight(report(age = 1.days), NOW) > calc.weight(report(age = 200.days), NOW))
        assertTrue(calc.weight(report(confirmations = 4), NOW) > calc.weight(report(confirmations = 0), NOW))
    }

    @Test
    fun three_old_reports_stay_yellow_until_confirmed() {
        val old = (1..3).map { report("o$it", age = 300.days) }
        assertEquals(RiskLevel.YELLOW, calc.riskOf(old, NOW).second)
        val confirmed = (1..3).map { report("c$it", age = 300.days, confirmations = 4) }
        assertEquals(RiskLevel.RED, calc.riskOf(confirmed, NOW).second)
    }

    @Test
    fun aggregate_groups_by_zone_and_sums_counts() {
        val elsewhere = GeoPoint(41.1580, -8.6291)
        val zones =
            calc.aggregate(
                listOf(report("a", confirmations = 2), report("b", confirmations = 1), report("c", at = elsewhere)),
                NOW,
            )
        assertEquals(2, zones.size)
        val top = zones.first()
        assertEquals(2, top.reportCount)
        assertEquals(3, top.confirmationCount)
    }

    @Test
    fun simultaneous_reports_in_same_place_both_count() {
        // Spec §7: two users reporting the same place at the same time: both accepted and summed.
        val zones = calc.aggregate(listOf(report("a"), report("b")), NOW)
        assertEquals(2, zones.single().reportCount)
    }
}
