package com.womenriskmap.feature.saved.domain

import com.womenriskmap.core.domain.model.RiskLevel
import com.womenriskmap.core.domain.model.SavedZone
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.rules.ZoneRiskCalculator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock

data class SavedZoneItem(val zone: SavedZone, val risk: RiskLevel?)

/** Spec §4 Ecrã 7: saved zones "com nome e cor atual". Risk is null when it can't be computed (offline). */
class SavedZonesWithRiskUseCase(
    private val reports: ReportRepository,
    private val calculator: ZoneRiskCalculator,
    private val clock: Clock,
) {
    suspend operator fun invoke(zones: List<SavedZone>): List<SavedZoneItem> = coroutineScope {
        val now = clock.now()
        zones.map { zone ->
            async {
                val risk = reports.reportsInZone(zone.zoneId).getOrNull()?.let { calculator.riskOf(it, now).second }
                SavedZoneItem(zone, risk)
            }
        }.awaitAll()
    }
}
