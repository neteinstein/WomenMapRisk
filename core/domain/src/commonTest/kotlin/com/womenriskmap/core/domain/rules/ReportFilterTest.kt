package com.womenriskmap.core.domain.rules

import com.womenriskmap.core.domain.NOW
import com.womenriskmap.core.domain.model.DayPeriod
import com.womenriskmap.core.domain.model.DayPeriodFilter
import com.womenriskmap.core.domain.model.FilterPeriod
import com.womenriskmap.core.domain.model.ReportFilter
import com.womenriskmap.core.domain.model.ReportType
import com.womenriskmap.core.domain.report
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class ReportFilterTest {
    @Test
    fun default_filter_matches_everything_in_12_months() {
        assertTrue(ReportFilter().isDefault)
        assertTrue(ReportFilter().matches(report(age = 300.days), NOW))
        assertFalse(ReportFilter().matches(report(age = 400.days), NOW))
    }

    @Test
    fun type_filter_is_multi_select() {
        val f = ReportFilter(types = setOf(ReportType.ROBBERY, ReportType.FOLLOWED))
        assertTrue(f.matches(report(type = ReportType.ROBBERY), NOW))
        assertTrue(f.matches(report(type = ReportType.FOLLOWED), NOW))
        assertFalse(f.matches(report(type = ReportType.POORLY_LIT), NOW))
    }

    @Test
    fun period_filter_windows() {
        assertTrue(ReportFilter(period = FilterPeriod.LAST_WEEK).matches(report(age = 6.days), NOW))
        assertFalse(ReportFilter(period = FilterPeriod.LAST_WEEK).matches(report(age = 8.days), NOW))
        assertFalse(ReportFilter(period = FilterPeriod.LAST_MONTH).matches(report(age = 31.days), NOW))
        assertTrue(ReportFilter(period = FilterPeriod.LAST_6_MONTHS).matches(report(age = 150.days), NOW))
    }

    @Test
    fun day_period_filter() {
        val day = report(dayPeriod = DayPeriod.DAY)
        val night = report(dayPeriod = DayPeriod.NIGHT)
        assertTrue(ReportFilter(dayPeriod = DayPeriodFilter.DAY).matches(day, NOW))
        assertFalse(ReportFilter(dayPeriod = DayPeriodFilter.DAY).matches(night, NOW))
        assertTrue(ReportFilter(dayPeriod = DayPeriodFilter.NIGHT).matches(night, NOW))
        assertTrue(ReportFilter(dayPeriod = DayPeriodFilter.BOTH).matches(day, NOW))
    }
}
