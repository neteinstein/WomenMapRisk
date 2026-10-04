package com.womenriskmap.core.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
enum class InviteState { PENDING, USED, EXPIRED, REVOKED }

@Serializable
data class Invite(
    val id: String,
    val code: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val state: InviteState,
)
