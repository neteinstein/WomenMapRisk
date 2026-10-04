package com.womenriskmap.core.domain.usecase

import com.womenriskmap.core.domain.model.BoundingBox
import com.womenriskmap.core.domain.model.ReportFilter
import com.womenriskmap.core.domain.model.Zone
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.rules.ZoneRiskCalculator
import kotlin.time.Clock
import kotlin.time.Instant

data class ZonesResult(val zones: List<Zone>, val isStale: Boolean, val fetchedAt: Instant)

/** Reports in [area] → filtered (spec §4 Ecrã 6) → aggregated into coloured zones (spec §6). */
class LoadZonesUseCase(
    private val reports: ReportRepository,
    private val calculator: ZoneRiskCalculator,
    private val clock: Clock,
) {
    suspend operator fun invoke(area: BoundingBox, filter: ReportFilter): Result<ZonesResult> =
        reports.reportsIn(area).map { snapshot ->
            val now = clock.now()
            val filtered = snapshot.reports.filter { filter.matches(it, now) }
            ZonesResult(calculator.aggregate(filtered, now), snapshot.isStale, snapshot.fetchedAt)
        }
}
