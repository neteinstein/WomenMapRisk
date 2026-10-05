package com.womenriskmap.feature.profile.data

import com.womenriskmap.core.data.remote.remote
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.feature.profile.domain.AccountRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class SupabaseAccountRepository(private val client: SupabaseClient, private val sessions: SessionRepository) : AccountRepository {
    private val pretty = Json { prettyPrint = true }

    override suspend fun exportMyData(): Result<String> = remote {
        val json = client.postgrest.rpc("export_my_data").decodeAs<JsonElement>()
        pretty.encodeToString(JsonElement.serializer(), json)
    }

    override suspend fun deleteMyAccount(): Result<Unit> = remote {
        client.postgrest.rpc("delete_my_account")
        Unit
    }.onSuccess { sessions.signOut() }
}
