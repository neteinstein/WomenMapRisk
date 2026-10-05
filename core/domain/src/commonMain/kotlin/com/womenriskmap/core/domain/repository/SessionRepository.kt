package com.womenriskmap.core.domain.repository

import com.womenriskmap.core.domain.model.SessionState
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val session: StateFlow<SessionState>

    /** Creates the account; the user must confirm the email before contributing (spec §4 Ecrã 2). */
    suspend fun signUp(email: String, password: String, country: String, inviteCode: String): Result<Unit>

    suspend fun signIn(email: String, password: String): Result<Unit>

    /** Starts the Google OAuth flow (browser / system sheet). The session updates when it completes. */
    suspend fun signInWithGoogle(): Result<Unit>

    suspend fun resendConfirmation(email: String): Result<Unit>

    suspend fun signOut(): Result<Unit>

    /** Re-reads the profile (e.g. after email confirmation, or after redeeming an invite). */
    suspend fun refresh(): Result<Unit>

    suspend fun updateProfile(pseudonym: String?, country: String): Result<Unit>
}
