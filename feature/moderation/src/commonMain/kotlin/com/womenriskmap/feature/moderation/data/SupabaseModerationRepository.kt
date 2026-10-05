package com.womenriskmap.feature.moderation.data

import com.womenriskmap.core.data.remote.dto.ReportDto
import com.womenriskmap.core.data.remote.dto.enumOf
import com.womenriskmap.core.data.remote.remote
import com.womenriskmap.core.domain.model.AccountStatus
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.feature.moderation.domain.ChainLink
import com.womenriskmap.feature.moderation.domain.ModerationAction
import com.womenriskmap.feature.moderation.domain.ModerationCounters
import com.womenriskmap.feature.moderation.domain.ModerationItem
import com.womenriskmap.feature.moderation.domain.ModerationRepository
import com.womenriskmap.feature.moderation.domain.QueueKind
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseModerationRepository(private val client: SupabaseClient) : ModerationRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun counters(): Result<ModerationCounters> = remote {
        val dto = json.decodeFromJsonElement(CountersDto.serializer(), client.postgrest.rpc("moderation_counters").decodeAs<JsonElement>())
        ModerationCounters(dto.newReports, dto.pendingFlags, dto.pendingEstablishments)
    }

    override suspend fun queue(): Result<List<ModerationItem>> = remote {
        client.postgrest.rpc("moderation_queue").decodeAs<List<JsonObject>>().map { obj ->
            val report = json.decodeFromJsonElement(ReportDto.serializer(), obj).toDomain()
            val extra = json.decodeFromJsonElement(QueueExtraDto.serializer(), obj)
            ModerationItem(
                report = report,
                kind = if (extra.kind == "establishment") QueueKind.ESTABLISHMENT else QueueKind.FLAGGED,
                openFlags = extra.openFlags,
                flagReasons = extra.flagReasons.map { enumOf(it, FlagReason.OTHER) },
                authorStatus = extra.authorStatus?.let { enumOf(it, AccountStatus.ACTIVE) },
            )
        }
    }

    override suspend fun act(reportId: String, action: ModerationAction, reason: String): Result<Unit> = remote {
        val params = buildJsonObject {
            put(if (action == ModerationAction.APPROVE || action == ModerationAction.REMOVE) "p_id" else "p_report_id", reportId)
            put("p_reason", reason.trim())
            if (action == ModerationAction.APPROVE) put("p_action", "approve")
            if (action == ModerationAction.REMOVE) put("p_action", "remove")
        }
        val function = when (action) {
            ModerationAction.APPROVE, ModerationAction.REMOVE -> "moderate_report"
            ModerationAction.BLOCK_USER -> "block_author"
            ModerationAction.REVOKE_INVITES -> "revoke_author_invites"
        }
        client.postgrest.rpc(function, params)
        Unit
    }

    override suspend fun inviterChain(reportId: String): Result<List<ChainLink>> = remote {
        client.postgrest.rpc("inviter_chain", buildJsonObject { put("p_report_id", reportId) })
            .decodeAs<List<JsonObject>>()
            .map { json.decodeFromJsonElement(ChainDto.serializer(), it) }
            .map { ChainLink(it.depth, it.alias, enumOf(it.status, AccountStatus.ACTIVE), it.invitedCount, it.blockedInvitees) }
    }
}

@Serializable
private data class CountersDto(
    @SerialName("new_reports") val newReports: Int = 0,
    @SerialName("pending_flags") val pendingFlags: Int = 0,
    @SerialName("pending_establishments") val pendingEstablishments: Int = 0,
)

@Serializable
private data class QueueExtraDto(
    val kind: String = "flagged",
    @SerialName("open_flags") val openFlags: Int = 0,
    @SerialName("flag_reasons") val flagReasons: List<String> = emptyList(),
    @SerialName("author_status") val authorStatus: String? = null,
)

@Serializable
private data class ChainDto(
    val depth: Int,
    val alias: String,
    val status: String,
    @SerialName("invited_count") val invitedCount: Int = 0,
    @SerialName("blocked_invitees") val blockedInvitees: Int = 0,
)
