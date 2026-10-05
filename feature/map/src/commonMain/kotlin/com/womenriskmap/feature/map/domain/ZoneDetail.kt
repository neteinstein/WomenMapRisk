package com.womenriskmap.feature.map.domain

import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.RiskLevel
import com.womenriskmap.core.domain.repository.GeocodingRepository
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.rules.LocationAnonymizer
import com.womenriskmap.core.domain.rules.ZoneRiskCalculator
import kotlin.time.Clock

/** Spec §4 Ecrã 4: name, totals, reports newest first. */
data class ZoneDetail(
    val zoneId: String,
    val center: GeoPoint,
    val name: String?,
    val reports: List<Report>,
    val risk: RiskLevel,
) {
    val totalReports: Int get() = reports.size
    val totalConfirmations: Int get() = reports.sumOf { it.confirmations }
}

class LoadZoneDetailUseCase(
    private val reports: ReportRepository,
    private val geocoding: GeocodingRepository,
    private val calculator: ZoneRiskCalculator,
    private val clock: Clock,
) {
    suspend operator fun invoke(zoneId: String): Result<ZoneDetail> {
        val cell = LocationAnonymizer.parseZoneId(zoneId) ?: return Result.failure(IllegalArgumentException("bad zone id"))
        val center = LocationAnonymizer.centerOf(cell)
        return reports.reportsInZone(zoneId).map { list ->
            val now = clock.now()
            val active = list.filter { calculator.isActive(it, now) }.sortedByDescending { it.createdAt }
            ZoneDetail(
                zoneId = zoneId,
                center = center,
                // The street name is a nice-to-have: offline or geocoder failures fall back to "Zona aproximada".
                name = geocoding.nameOf(center).getOrNull(),
                reports = active,
                risk = calculator.riskOf(active, now).second,
            )
        }
    }
}
