package com.womenriskmap.core.domain.repository

import com.womenriskmap.core.domain.model.Invite
import kotlinx.datetime.LocalDate

data class InviteData(val usageDays: List<LocalDate>, val invites: List<Invite>)

interface InviteRepository {
    /** Records today as a usage day (idempotent). Called on app start/resume when signed in. */
    suspend fun recordUsage(): Result<Unit>

    suspend fun load(): Result<InviteData>

    suspend fun create(): Result<Invite>

    suspend fun revoke(inviteId: String): Result<Unit>

    /** True when [code] exists, is unused, unrevoked and unexpired. */
    suspend fun validate(code: String): Result<Boolean>

    /** Binds the signed-in account to [code] and activates it. */
    suspend fun redeem(code: String): Result<Unit>
}
