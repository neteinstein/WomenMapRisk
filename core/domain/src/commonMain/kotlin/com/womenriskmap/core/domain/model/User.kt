package com.womenriskmap.core.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
enum class AccountStatus {
    /** Signed in (e.g. via Google) but no invite redeemed yet: behaves as a visitor. */
    PENDING_INVITE,
    ACTIVE,
    BLOCKED,
}

@Serializable
enum class UserRole { USER, MODERATOR }

/** Spec §8 Utilizadora. */
@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val pseudonym: String?,
    val country: String,
    val createdAt: Instant,
    val status: AccountStatus,
    val role: UserRole,
    val emailConfirmed: Boolean,
) {
    val isModerator: Boolean get() = role == UserRole.MODERATOR

    /** Spec §6 Contas + invite addendum: may report, confirm, flag, save and invite. */
    val isEnabled: Boolean get() = emailConfirmed && status == AccountStatus.ACTIVE
}

sealed interface SessionState {
    data object Loading : SessionState

    /** "Explorar sem conta" (spec §3 Visitante). */
    data object Visitor : SessionState

    data class SignedIn(val profile: UserProfile) : SessionState
}

val SessionState.profileOrNull: UserProfile? get() = (this as? SessionState.SignedIn)?.profile
val SessionState.canContribute: Boolean get() = profileOrNull?.isEnabled == true
