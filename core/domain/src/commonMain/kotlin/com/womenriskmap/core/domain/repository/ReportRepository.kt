package com.womenriskmap.core.domain.repository

import com.womenriskmap.core.domain.model.BoundingBox
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.ReportDraft
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** Reports in an area. [isStale] = served from the offline cache (spec §7 Sem internet). */
data class AreaSnapshot(val reports: List<Report>, val fetchedAt: Instant, val isStale: Boolean)

interface ReportRepository {
    /** Emits whenever published reports change server-side (realtime), so the map refreshes in < 1 min. */
    val changes: Flow<Unit>

    suspend fun reportsIn(area: BoundingBox): Result<AreaSnapshot>

    suspend fun reportsInZone(zoneId: String): Result<List<Report>>

    suspend fun submit(draft: ReportDraft): Result<Report>

    suspend fun update(reportId: String, draft: ReportDraft): Result<Report>

    suspend fun delete(reportId: String): Result<Unit>

    suspend fun confirm(reportId: String): Result<Unit>

    suspend fun flag(reportId: String, reason: FlagReason): Result<Unit>

    /** The current user's own reports, any status (spec §4 Ecrã 8 "Os meus reportes"). */
    suspend fun myReports(): Result<List<Report>>
}
