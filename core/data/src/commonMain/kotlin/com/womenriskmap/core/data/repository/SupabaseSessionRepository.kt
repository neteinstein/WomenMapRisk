package com.womenriskmap.core.data.repository

import com.womenriskmap.core.data.local.CachedProfile
import com.womenriskmap.core.data.local.LocalStores
import com.womenriskmap.core.data.remote.dto.ProfileDto
import com.womenriskmap.core.data.remote.remote
import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.core.domain.rules.InviteCode
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseSessionRepository(
    private val client: SupabaseClient,
    private val stores: LocalStores,
    scope: CoroutineScope,
) : SessionRepository {
    private val state = MutableStateFlow<SessionState>(SessionState.Loading)
    override val session: StateFlow<SessionState> = state.asStateFlow()

    init {
        scope.launch {
            client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Initializing -> state.value = SessionState.Loading
                    is SessionStatus.NotAuthenticated -> {
                        stores.clearPersonal()
                        state.value = SessionState.Visitor
                    }
                    is SessionStatus.Authenticated -> loadProfile()
                    // Offline token refresh: keep the cached profile so the user isn't logged out on a train.
                    is SessionStatus.RefreshFailure -> if (state.value is SessionState.Loading) showCachedOrVisitor()
                }
            }
        }
    }

    private suspend fun loadProfile(): Result<Unit> = remote {
        val dto = client.postgrest.rpc("my_profile").decodeAs<ProfileDto>()
        stores.profile.set(CachedProfile(dto))
        state.value = SessionState.SignedIn(dto.toDomain())
    }.onFailure { if (it is DomainException.Network) showCachedOrVisitor() }

    private suspend fun showCachedOrVisitor() {
        state.value = stores.profile.get()?.profile?.let { SessionState.SignedIn(it.toDomain()) } ?: SessionState.Visitor
    }

    override suspend fun signUp(email: String, password: String, country: String, inviteCode: String): Result<Unit> = remote {
        val code = InviteCode.normalize(inviteCode) ?: throw DomainException.InvalidInvite()
        val valid = client.postgrest.rpc("validate_invite", buildJsonObject { put("p_code", code) }).decodeAs<Boolean>()
        if (!valid) throw DomainException.InvalidInvite()
        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            // Read by the `handle_new_user` trigger, which creates the profile and redeems the invite.
            data = buildJsonObject {
                put("country", country)
                put("invite_code", code)
                put("declared_woman", true)
            }
        }
        Unit
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = remote {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    override suspend fun signInWithGoogle(): Result<Unit> = remote { client.auth.signInWith(Google) }

    override suspend fun resendConfirmation(email: String): Result<Unit> = remote {
        client.auth.resendEmail(OtpType.Email.SIGNUP, email.trim())
    }

    override suspend fun signOut(): Result<Unit> = remote {
        client.auth.signOut()
        stores.clearPersonal()
        state.value = SessionState.Visitor
    }

    override suspend fun refresh(): Result<Unit> =
        if (client.auth.currentSessionOrNull() == null) Result.success(Unit) else loadProfile()

    override suspend fun updateProfile(pseudonym: String?, country: String): Result<Unit> = remote {
        client.postgrest.rpc(
            "update_my_profile",
            buildJsonObject {
                put("p_pseudonym", pseudonym?.trim()?.takeIf { it.isNotEmpty() })
                put("p_country", country)
            },
        )
        Unit
    }.onSuccess { refresh() }
}
