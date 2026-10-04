package com.womenriskmap.core.domain.usecase

import com.womenriskmap.core.domain.NOW
import com.womenriskmap.core.domain.model.BoundingBox
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.ReportDraft
import com.womenriskmap.core.domain.model.ReportFilter
import com.womenriskmap.core.domain.model.ReportType
import com.womenriskmap.core.domain.model.RiskLevel
import com.womenriskmap.core.domain.report
import com.womenriskmap.core.domain.repository.AreaSnapshot
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.rules.ZoneRiskCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class LoadZonesUseCaseTest {
    private val clock =
        object : Clock {
            override fun now(): Instant = NOW
        }
    private val area = BoundingBox(41.0, -8.8, 41.3, -8.5)

    private class Repo(private val result: Result<AreaSnapshot>) : ReportRepository {
        override val changes: Flow<Unit> = emptyFlow()

        override suspend fun reportsIn(area: BoundingBox) = result

        override suspend fun reportsInZone(zoneId: String): Result<List<Report>> = TODO()

        override suspend fun submit(draft: ReportDraft): Result<Report> = TODO()

        override suspend fun update(reportId: String, draft: ReportDraft): Result<Report> = TODO()

        override suspend fun delete(reportId: String): Result<Unit> = TODO()

        override suspend fun confirm(reportId: String): Result<Unit> = TODO()

        override suspend fun flag(reportId: String, reason: FlagReason): Result<Unit> = TODO()

        override suspend fun myReports(): Result<List<Report>> = TODO()
    }

    @Test
    fun applies_filter_then_aggregates() =
        runTest {
            val reports =
                listOf(
                    report("a", type = ReportType.ROBBERY),
                    report("b", type = ReportType.ROBBERY),
                    report("c", type = ReportType.ROBBERY),
                    report("d", type = ReportType.POORLY_LIT),
                )
            val useCase = LoadZonesUseCase(Repo(Result.success(AreaSnapshot(reports, NOW, false))), ZoneRiskCalculator(), clock)

            val all = useCase(area, ReportFilter()).getOrThrow()
            assertEquals(4, all.zones.single().reportCount)
            assertEquals(RiskLevel.RED, all.zones.single().risk)

            val lit = useCase(area, ReportFilter(types = setOf(ReportType.POORLY_LIT))).getOrThrow()
            assertEquals(RiskLevel.YELLOW, lit.zones.single().risk)
        }

    @Test
    fun propagates_stale_flag_and_failures() =
        runTest {
            val stale = LoadZonesUseCase(Repo(Result.success(AreaSnapshot(emptyList(), NOW, true))), ZoneRiskCalculator(), clock)
            assertTrue(stale(area, ReportFilter()).getOrThrow().isStale)

            val failing = LoadZonesUseCase(Repo(Result.failure(IllegalStateException())), ZoneRiskCalculator(), clock)
            assertTrue(failing(area, ReportFilter()).isFailure)
        }
}
