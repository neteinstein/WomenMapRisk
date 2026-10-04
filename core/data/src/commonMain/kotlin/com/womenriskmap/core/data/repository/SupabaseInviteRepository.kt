package com.womenriskmap.core.data.repository

import com.womenriskmap.core.data.remote.dto.InviteDataDto
import com.womenriskmap.core.data.remote.dto.InviteDto
import com.womenriskmap.core.data.remote.remote
import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.Invite
import com.womenriskmap.core.domain.repository.InviteData
import com.womenriskmap.core.domain.repository.InviteRepository
import com.womenriskmap.core.domain.rules.InviteCode
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseInviteRepository(private val client: SupabaseClient) : InviteRepository {
    override suspend fun recordUsage(): Result<Unit> = remote {
        client.postgrest.rpc("touch_usage")
        Unit
    }

    override suspend fun load(): Result<InviteData> = remote {
        val dto = client.postgrest.rpc("my_invite_data").decodeAs<InviteDataDto>()
        InviteData(usageDays = dto.usageDays.map(LocalDate::parse), invites = dto.invites.map { it.toDomain() })
    }

    override suspend fun create(): Result<Invite> = remote {
        client.postgrest.rpc("create_invite").decodeAs<InviteDto>().toDomain()
    }

    override suspend fun revoke(inviteId: String): Result<Unit> = remote {
        client.postgrest.rpc("revoke_invite", buildJsonObject { put("p_id", inviteId) })
        Unit
    }

    override suspend fun validate(code: String): Result<Boolean> = remote {
        val normalized = InviteCode.normalize(code) ?: return@remote false
        client.postgrest.rpc("validate_invite", buildJsonObject { put("p_code", normalized) }).decodeAs<Boolean>()
    }

    override suspend fun redeem(code: String): Result<Unit> = remote {
        val normalized = InviteCode.normalize(code) ?: throw DomainException.InvalidInvite()
        client.postgrest.rpc("redeem_invite", buildJsonObject { put("p_code", normalized) })
        Unit
    }
}
