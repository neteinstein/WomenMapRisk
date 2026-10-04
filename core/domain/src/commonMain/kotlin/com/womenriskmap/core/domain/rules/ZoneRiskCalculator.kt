package com.womenriskmap.core.domain.rules

import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.ReportStatus
import com.womenriskmap.core.domain.model.RiskLevel
import com.womenriskmap.core.domain.model.Zone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Spec §6 Cor das zonas:
 *  - Verde: 0 reportes ativos. Amarelo: 1 a 2. Vermelho: 3 ou mais (a definir com dados reais).
 *  - Só contam reportes dos últimos 12 meses.
 *  - Reportes mais recentes ou com mais confirmações pesam mais no cálculo.
 *
 * Each active report gets weight = recencyFactor × (1 + confirmationWeight × confirmations).
 * GREEN when no active reports; RED when the weighted score ≥ [RiskThresholds.red]; otherwise YELLOW.
 * With default weights, 3 fresh unconfirmed reports = RED, matching the spec's 3+ rule.
 */
data class RiskThresholds(
    val red: Double = 3.0,
    val confirmationWeight: Double = 0.25,
    val maxAge: Duration = 365.days,
)

class ZoneRiskCalculator(private val thresholds: RiskThresholds = RiskThresholds()) {
    fun isActive(report: Report, now: Instant): Boolean =
        report.status == ReportStatus.PUBLISHED && now - report.createdAt in Duration.ZERO..thresholds.maxAge

    fun weight(report: Report, now: Instant): Double {
        if (!isActive(report, now)) return 0.0
        val age = now - report.createdAt
        val recency =
            when {
                age <= 30.days -> 1.0
                age <= 90.days -> 0.85
                age <= 180.days -> 0.7
                else -> 0.5
            }
        return recency * (1.0 + thresholds.confirmationWeight * report.confirmations)
    }

    fun riskOf(reports: List<Report>, now: Instant): Pair<Double, RiskLevel> {
        val active = reports.filter { isActive(it, now) }
        if (active.isEmpty()) return 0.0 to RiskLevel.GREEN
        val score = active.sumOf { weight(it, now) }
        return score to if (score >= thresholds.red) RiskLevel.RED else RiskLevel.YELLOW
    }

    /** Groups reports by zone and computes each zone's risk. Zones with no active reports are dropped. */
    fun aggregate(reports: List<Report>, now: Instant): List<Zone> =
        reports.filter { isActive(it, now) }
            .groupBy { it.zoneId }
            .map { (zoneId, zoneReports) ->
                val (score, risk) = riskOf(zoneReports, now)
                Zone(
                    id = zoneId,
                    center = zoneReports.first().location,
                    reportCount = zoneReports.size,
                    confirmationCount = zoneReports.sumOf { it.confirmations },
                    score = score,
                    risk = risk,
                )
            }
            .sortedByDescending { it.score }
}
