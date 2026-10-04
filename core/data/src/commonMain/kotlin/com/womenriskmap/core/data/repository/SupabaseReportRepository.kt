package com.womenriskmap.core.data.repository

import com.womenriskmap.core.data.local.AreaCache
import com.womenriskmap.core.data.local.LocalStores
import com.womenriskmap.core.data.remote.dto.ReportDto
import com.womenriskmap.core.data.remote.dto.wire
import com.womenriskmap.core.data.remote.remote
import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.BoundingBox
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.ReportDraft
import com.womenriskmap.core.domain.repository.AreaSnapshot
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.rules.LocationAnonymizer
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

class SupabaseReportRepository(
    private val client: SupabaseClient,
    private val stores: LocalStores,
    private val clock: Clock,
) : ReportRepository {

    /** Server broadcasts `changed` on topic `reports-feed` (no personal data) when published reports change. */
    override val changes: Flow<Unit> = flow {
        val channel = client.channel("reports-feed")
        val events = channel.broadcastFlow<JsonObject>("changed")
        channel.subscribe()
        emitAll(events.map { })
    }.onCompletion { runCatching { client.channel("reports-feed").unsubscribe() } }
        .catch { /* realtime unavailable: the map falls back to polling */ }

    override suspend fun reportsIn(area: BoundingBox): Result<AreaSnapshot> {
        val fresh = remote {
            client.postgrest.rpc(
                "reports_in_bbox",
                buildJsonObject {
                    put("p_south", area.south)
                    put("p_west", area.west)
                    put("p_north", area.north)
                    put("p_east", area.east)
                },
            ).decodeAs<List<ReportDto>>().map { it.toDomain() }
        }
        fresh.onSuccess { reports ->
            val now = clock.now()
            stores.area.set(AreaCache(reports, now))
            return Result.success(AreaSnapshot(reports, now, isStale = false))
        }
        val error = fresh.exceptionOrNull()
        if (error is DomainException.Network) {
            val cache = stores.area.get()
            if (cache?.fetchedAt != null) {
                return Result.success(AreaSnapshot(cache.reports.filter { it.location in area }, cache.fetchedAt, isStale = true))
            }
        }
        return Result.failure(error ?: DomainException.Unknown())
    }

    override suspend fun reportsInZone(zoneId: String): Result<List<Report>> = remote {
        client.postgrest.rpc("reports_in_zone", buildJsonObject { put("p_zone_id", zoneId) })
            .decodeAs<List<ReportDto>>().map { it.toDomain() }
    }.recoverCatching { error ->
        if (error !is DomainException.Network) throw error
        stores.area.get()?.reports?.filter { it.zoneId == zoneId } ?: throw error
    }

    override suspend fun submit(draft: ReportDraft): Result<Report> = remote {
        client.postgrest.rpc("submit_report", draftParams(draft)).decodeAs<ReportDto>().toDomain()
    }

    override suspend fun update(reportId: String, draft: ReportDraft): Result<Report> = remote {
        client.postgrest.rpc("update_report", draftParams(draft) { put("p_id", reportId) }).decodeAs<ReportDto>().toDomain()
    }

    override suspend fun delete(reportId: String): Result<Unit> = remote {
        client.postgrest.rpc("delete_report", buildJsonObject { put("p_id", reportId) })
        Unit
    }

    override suspend fun confirm(reportId: String): Result<Unit> = remote {
        client.postgrest.rpc("confirm_report", buildJsonObject { put("p_id", reportId) })
        Unit
    }

    override suspend fun flag(reportId: String, reason: FlagReason): Result<Unit> = remote {
        client.postgrest.rpc(
            "flag_report",
            buildJsonObject {
                put("p_id", reportId)
                put("p_reason", reason.wire)
            },
        )
        Unit
    }

    override suspend fun myReports(): Result<List<Report>> = remote {
        client.postgrest.rpc("my_reports").decodeAs<List<ReportDto>>().map { it.toDomain() }
    }

    /** The client already sends the snapped point; the server snaps again regardless (defence in depth). */
    private fun draftParams(draft: ReportDraft, extra: JsonObjectBuilder.() -> Unit = {}) = buildJsonObject {
        val snapped = LocationAnonymizer.snap(draft.location)
        put("p_lat", snapped.latitude)
        put("p_lng", snapped.longitude)
        put("p_type", draft.type.wire)
        put("p_occurred_when", draft.occurredWhen.wire)
        put("p_day_period", draft.dayPeriod.wire)
        put("p_description", draft.description?.trim()?.takeIf { it.isNotEmpty() })
        put("p_is_establishment", draft.isEstablishment)
        extra()
    }
}
