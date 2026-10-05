package com.womenriskmap.feature.moderation.domain

import com.womenriskmap.core.domain.model.AccountStatus
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.core.domain.model.Report

/** Spec §4 Ecrã 10: contadores. */
data class ModerationCounters(val newReports: Int, val pendingFlags: Int, val pendingEstablishments: Int)

enum class QueueKind { FLAGGED, ESTABLISHMENT }

/** A queue entry. The author's identity is never exposed, only her account status. */
data class ModerationItem(
    val report: Report,
    val kind: QueueKind,
    val openFlags: Int,
    val flagReasons: List<FlagReason>,
    val authorStatus: AccountStatus?,
)

enum class ModerationAction { APPROVE, REMOVE, BLOCK_USER, REVOKE_INVITES }

/** One step up the invite tree, pseudonymous ("user-3fa2c1"). */
data class ChainLink(val depth: Int, val alias: String, val status: AccountStatus, val invitedCount: Int, val blockedInvitees: Int)

interface ModerationRepository {
    suspend fun counters(): Result<ModerationCounters>
    suspend fun queue(): Result<List<ModerationItem>>

    /** Every action requires a recorded reason (spec §4 Ecrã 10: "com motivo registado"). */
    suspend fun act(reportId: String, action: ModerationAction, reason: String): Result<Unit>
    suspend fun inviterChain(reportId: String): Result<List<ChainLink>>
}
