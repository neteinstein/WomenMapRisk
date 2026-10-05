package com.womenriskmap.core.data.remote

import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.error.domainCatching
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.io.IOException

/**
 * Maps Supabase/Ktor failures to [DomainException]. SQL functions raise stable message codes
 * (e.g. `raise exception 'daily_report_limit'`); see supabase/migrations. Keep both lists in sync.
 */
fun Throwable.toDomainException(): DomainException = when (this) {
    is DomainException -> this
    is AuthRestException -> when (errorCode) {
        AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> DomainException.EmailAlreadyRegistered()
        AuthErrorCode.WeakPassword -> DomainException.WeakPassword()
        AuthErrorCode.EmailNotConfirmed -> DomainException.EmailNotConfirmed()
        AuthErrorCode.InvalidCredentials -> DomainException.InvalidCredentials()
        else -> DomainException.Unknown(message, this)
    }
    is RestException -> mapSqlCode("$error ${description.orEmpty()} $message") ?: DomainException.Unknown(message, this)
    is HttpRequestException, is HttpRequestTimeoutException, is ConnectTimeoutException, is IOException -> DomainException.Network(this)
    else -> DomainException.Unknown(message, this)
}

internal fun mapSqlCode(text: String): DomainException? = when {
    "daily_report_limit" in text -> DomainException.DailyReportLimit()
    "duplicate_report" in text -> DomainException.DuplicateReport()
    "edit_window_expired" in text -> DomainException.EditWindowExpired()
    "already_confirmed" in text -> DomainException.AlreadyConfirmed()
    "cannot_confirm_own" in text -> DomainException.CannotConfirmOwn()
    "invites_locked" in text -> DomainException.InvitesLocked()
    "invite_limit_reached" in text -> DomainException.InviteLimitReached()
    "invalid_invite" in text -> DomainException.InvalidInvite()
    "account_blocked" in text -> DomainException.AccountBlocked()
    "email_not_confirmed" in text -> DomainException.EmailNotConfirmed()
    "not_allowed" in text || "permission denied" in text -> DomainException.NotAllowed()
    else -> null
}

/** Runs a remote call, mapping every failure to a [DomainException]. */
inline fun <T> remote(block: () -> T): Result<T> = domainCatching(mapper = { it.toDomainException() }, block = block)
