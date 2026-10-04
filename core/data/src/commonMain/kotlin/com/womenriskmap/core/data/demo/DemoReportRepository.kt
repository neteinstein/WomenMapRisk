package com.womenriskmap.core.data.demo

import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.BoundingBox
import com.womenriskmap.core.domain.model.DayPeriod
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.OccurredWhen
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.ReportDraft
import com.womenriskmap.core.domain.model.ReportStatus
import com.womenriskmap.core.domain.model.ReportType
import com.womenriskmap.core.domain.repository.AreaSnapshot
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.rules.LocationAnonymizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * DEMO MODE ONLY: used when no Supabase project is configured (AppConfig.isConfigured == false), so the app
 * can be explored (web preview, reviewers, offline dev) with sample Porto data. Read-only for visitors;
 * nothing leaves the device. Mirrors supabase/seed.sql.
 */
class DemoReportRepository(private val clock: Clock) : ReportRepository {
    private val updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val changes: Flow<Unit> = updates

    private val reports: MutableList<Report> = sample().toMutableList()

    private fun sample(): List<Report> {
        val now = clock.now()
        fun r(
            id: String,
            lat: Double,
            lng: Double,
            type: ReportType,
            age: Duration,
            confirmations: Int,
            period: DayPeriod,
            description: String?,
        ) =
            Report(
                id = id,
                zoneId = LocationAnonymizer.zoneId(GeoPoint(lat, lng)),
                location = LocationAnonymizer.snap(GeoPoint(lat, lng)),
                type = type,
                occurredWhen = if (age < 1.days) {
                    OccurredWhen.TODAY
                } else if (age < 7.days) {
                    OccurredWhen.THIS_WEEK
                } else {
                    OccurredWhen.EARLIER
                },
                dayPeriod = period,
                description = description,
                confirmations = confirmations,
                status = ReportStatus.PUBLISHED,
                createdAt = now - age,
            )
        return listOf(
            r("d1", 41.1456, -8.6109, ReportType.POORLY_LIT, 3.days, 4, DayPeriod.NIGHT, "Rua muito escura depois das 21h."),
            r("d2", 41.1457, -8.6111, ReportType.VERBAL_HARASSMENT, 10.hours, 2, DayPeriod.NIGHT, null),
            r("d3", 41.1455, -8.6108, ReportType.FOLLOWED, 40.days, 1, DayPeriod.NIGHT, "Fui seguida até à estação."),
            r("d4", 41.1497, -8.6062, ReportType.DESERTED, 20.days, 0, DayPeriod.NIGHT, "Zona sem movimento à noite."),
            r("d5", 41.1409, -8.6136, ReportType.ROBBERY, 100.days, 3, DayPeriod.DAY, null),
            r("d6", 41.1580, -8.6291, ReportType.VERBAL_HARASSMENT, 5.days, 0, DayPeriod.DAY, "Comentários na paragem de autocarro."),
            r("d7", 41.1621, -8.5835, ReportType.POORLY_LIT, 200.days, 0, DayPeriod.NIGHT, null),
            r("d8", 41.1412, -8.6139, ReportType.FOLLOWED, 2.days, 1, DayPeriod.NIGHT, null),
            r("d9", 41.1410, -8.6133, ReportType.VERBAL_HARASSMENT, 12.days, 2, DayPeriod.NIGHT, "Grupo a fazer comentários."),
        )
    }

    override suspend fun reportsIn(area: BoundingBox) = Result.success(
        AreaSnapshot(
            reports.filter {
                it.location in area
            },
            clock.now(),
            isStale = false,
        ),
    )

    override suspend fun reportsInZone(zoneId: String) = Result.success(reports.filter { it.zoneId == zoneId })

    override suspend fun submit(draft: ReportDraft): Result<Report> = Result.failure(DomainException.NotAllowed())

    override suspend fun update(reportId: String, draft: ReportDraft): Result<Report> = Result.failure(DomainException.NotAllowed())

    override suspend fun delete(reportId: String): Result<Unit> = Result.failure(DomainException.NotAllowed())

    override suspend fun confirm(reportId: String): Result<Unit> = Result.failure(DomainException.NotAllowed())

    override suspend fun flag(reportId: String, reason: FlagReason): Result<Unit> = Result.failure(DomainException.NotAllowed())

    override suspend fun myReports(): Result<List<Report>> = Result.success(emptyList())
}
