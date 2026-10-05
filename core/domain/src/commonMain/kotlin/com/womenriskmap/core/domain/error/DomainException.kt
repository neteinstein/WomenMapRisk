package com.womenriskmap.core.domain.error

/**
 * Every failure crossing the data -> domain boundary is mapped to one of these, so ViewModels can show
 * clear messages (spec §4 Ecrã 2 errors) without knowing about Supabase/Ktor.
 */
sealed class DomainException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    class Network(cause: Throwable? = null) : DomainException("network", cause)

    class EmailAlreadyRegistered : DomainException("email_already_registered")

    class WeakPassword : DomainException("weak_password")

    class EmailNotConfirmed : DomainException("email_not_confirmed")

    class InvalidCredentials : DomainException("invalid_credentials")

    class InvalidInvite : DomainException("invalid_invite")

    class DailyReportLimit : DomainException("daily_report_limit")

    class DuplicateReport : DomainException("duplicate_report")

    class EditWindowExpired : DomainException("edit_window_expired")

    class AlreadyConfirmed : DomainException("already_confirmed")

    class CannotConfirmOwn : DomainException("cannot_confirm_own")

    class InvitesLocked : DomainException("invites_locked")

    class InviteLimitReached : DomainException("invite_limit_reached")

    class NotAllowed : DomainException("not_allowed")

    class AccountBlocked : DomainException("account_blocked")

    class Unknown(message: String? = null, cause: Throwable? = null) : DomainException(message, cause)
}

/** Like [runCatching] but never swallows coroutine cancellation and always yields a [DomainException]. */
inline fun <T> domainCatching(
    mapper: (Throwable) -> DomainException = { DomainException.Unknown(it.message, it) },
    block: () -> T,
): Result<T> =
    try {
        Result.success(block())
    } catch (e: kotlin.coroutines.cancellation.CancellationException) {
        throw e
    } catch (e: DomainException) {
        Result.failure(e)
    } catch (e: Throwable) {
        Result.failure(mapper(e))
    }
