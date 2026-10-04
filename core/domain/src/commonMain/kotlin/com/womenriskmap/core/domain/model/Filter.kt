package com.womenriskmap.core.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Spec §4 Ecrã 6: Período. */
@Serializable
enum class FilterPeriod(val window: Duration) {
    LAST_WEEK(7.days),
    LAST_MONTH(30.days),
    LAST_6_MONTHS(182.days),
    LAST_12_MONTHS(365.days),
}

/** Spec §4 Ecrã 6: Período do dia. */
@Serializable
enum class DayPeriodFilter { DAY, NIGHT, BOTH }

/** Spec §4 Ecrã 6. Empty [types] = all types. Default = everything from the last 12 months. */
@Serializable
data class ReportFilter(
    val types: Set<ReportType> = emptySet(),
    val period: FilterPeriod = FilterPeriod.LAST_12_MONTHS,
    val dayPeriod: DayPeriodFilter = DayPeriodFilter.BOTH,
) {
    val isDefault: Boolean get() = this == ReportFilter()

    fun matches(report: Report, now: Instant): Boolean {
        if (types.isNotEmpty() && report.type !in types) return false
        if (now - report.createdAt > period.window) return false
        return when (dayPeriod) {
            DayPeriodFilter.BOTH -> true
            DayPeriodFilter.DAY -> report.dayPeriod == DayPeriod.DAY
            DayPeriodFilter.NIGHT -> report.dayPeriod == DayPeriod.NIGHT
        }
    }
}
