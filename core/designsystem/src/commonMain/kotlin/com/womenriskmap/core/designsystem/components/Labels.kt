package com.womenriskmap.core.designsystem.components

import androidx.compose.runtime.Composable
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.resources.Res
import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.DayPeriod
import com.womenriskmap.core.domain.model.DayPeriodFilter
import com.womenriskmap.core.domain.model.FilterPeriod
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.core.domain.model.InviteState
import com.womenriskmap.core.domain.model.OccurredWhen
import com.womenriskmap.core.domain.model.ReportStatus
import com.womenriskmap.core.domain.model.ReportType
import com.womenriskmap.core.domain.rules.DescriptionGuard
import com.womenriskmap.core.domain.rules.EmergencyContacts
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

// Domain enum -> string resource. Kept here so every feature labels things identically.

val ReportType.label: StringResource
    get() = when (this) {
        ReportType.VERBAL_HARASSMENT -> Res.string.type_verbal_harassment
        ReportType.FOLLOWED -> Res.string.type_followed
        ReportType.POORLY_LIT -> Res.string.type_poorly_lit
        ReportType.DESERTED -> Res.string.type_deserted
        ReportType.ROBBERY -> Res.string.type_robbery
        ReportType.ASSAULT -> Res.string.type_assault
        ReportType.OTHER -> Res.string.type_other
    }

val OccurredWhen.label: StringResource
    get() = when (this) {
        OccurredWhen.NOW -> Res.string.when_now
        OccurredWhen.TODAY -> Res.string.when_today
        OccurredWhen.THIS_WEEK -> Res.string.when_this_week
        OccurredWhen.EARLIER -> Res.string.when_earlier
    }

val DayPeriod.label: StringResource
    get() = when (this) {
        DayPeriod.DAY -> Res.string.period_day
        DayPeriod.NIGHT -> Res.string.period_night
    }

val DayPeriodFilter.label: StringResource
    get() = when (this) {
        DayPeriodFilter.DAY -> Res.string.period_day
        DayPeriodFilter.NIGHT -> Res.string.period_night
        DayPeriodFilter.BOTH -> Res.string.period_both
    }

val FilterPeriod.label: StringResource
    get() = when (this) {
        FilterPeriod.LAST_WEEK -> Res.string.filter_last_week
        FilterPeriod.LAST_MONTH -> Res.string.filter_last_month
        FilterPeriod.LAST_6_MONTHS -> Res.string.filter_last_6_months
        FilterPeriod.LAST_12_MONTHS -> Res.string.filter_last_12_months
    }

val ReportStatus.label: StringResource
    get() = when (this) {
        ReportStatus.PENDING -> Res.string.status_pending
        ReportStatus.PUBLISHED -> Res.string.status_published
        ReportStatus.HIDDEN -> Res.string.status_hidden
        ReportStatus.REMOVED -> Res.string.status_removed
    }

val FlagReason.label: StringResource
    get() = when (this) {
        FlagReason.FALSE_INFORMATION -> Res.string.flag_reason_false_information
        FlagReason.PERSONAL_DATA -> Res.string.flag_reason_personal_data
        FlagReason.OFFENSIVE -> Res.string.flag_reason_offensive
        FlagReason.SPAM -> Res.string.flag_reason_spam
        FlagReason.OTHER -> Res.string.flag_reason_other
    }

val InviteState.label: StringResource
    get() = when (this) {
        InviteState.PENDING -> Res.string.invite_state_pending
        InviteState.USED -> Res.string.invite_state_used
        InviteState.EXPIRED -> Res.string.invite_state_expired
        InviteState.REVOKED -> Res.string.invite_state_revoked
    }

val EmergencyContacts.Service.label: StringResource
    get() = when (this) {
        EmergencyContacts.Service.EMERGENCY -> Res.string.emergency_emergency
        EmergencyContacts.Service.VICTIM_SUPPORT -> Res.string.emergency_victim_support
        EmergencyContacts.Service.DOMESTIC_VIOLENCE -> Res.string.emergency_domestic_violence
    }

val DescriptionGuard.Finding.label: StringResource
    get() = when (this) {
        DescriptionGuard.Finding.LICENSE_PLATE -> Res.string.report_guard_license_plate
        DescriptionGuard.Finding.PHONE_NUMBER -> Res.string.report_guard_phone_number
        DescriptionGuard.Finding.EMAIL -> Res.string.report_guard_email
        DescriptionGuard.Finding.URL -> Res.string.report_guard_url
    }

fun countryLabel(iso: String): StringResource = when (iso.uppercase()) {
    "PT" -> Res.string.country_PT
    "ES" -> Res.string.country_ES
    "FR" -> Res.string.country_FR
    "GB" -> Res.string.country_GB
    "DE" -> Res.string.country_DE
    "IT" -> Res.string.country_IT
    "NL" -> Res.string.country_NL
    "BR" -> Res.string.country_BR
    "US" -> Res.string.country_US
    else -> Res.string.country_XX
}

/** Spec §4 Ecrã 2: clear error messages (PT/EN) for every domain failure. */
fun Throwable.messageRes(): StringResource = when (this) {
    is DomainException.Network -> Res.string.error_network
    is DomainException.EmailAlreadyRegistered -> Res.string.error_email_registered
    is DomainException.WeakPassword -> Res.string.error_weak_password
    is DomainException.EmailNotConfirmed -> Res.string.error_email_not_confirmed
    is DomainException.InvalidCredentials -> Res.string.error_invalid_credentials
    is DomainException.InvalidInvite -> Res.string.error_invalid_invite
    is DomainException.DailyReportLimit -> Res.string.error_daily_limit
    is DomainException.DuplicateReport -> Res.string.error_duplicate
    is DomainException.EditWindowExpired -> Res.string.error_edit_window
    is DomainException.AlreadyConfirmed -> Res.string.error_already_confirmed
    is DomainException.CannotConfirmOwn -> Res.string.error_cannot_confirm_own
    is DomainException.InvitesLocked -> Res.string.error_invites_locked
    is DomainException.InviteLimitReached -> Res.string.error_invite_limit
    is DomainException.NotAllowed -> Res.string.error_not_allowed
    is DomainException.AccountBlocked -> Res.string.error_account_blocked
    else -> Res.string.error_generic
}

/** "agora mesmo", "há 5 min", "há 3 horas", "há 2 dias". */
@Composable
fun relativeTime(instant: Instant, now: Instant = Clock.System.now()): String {
    val elapsed = now - instant
    val minutes = elapsed.inWholeMinutes.toInt()
    val hours = elapsed.inWholeHours.toInt()
    val days = elapsed.inWholeDays.toInt()
    return when {
        minutes < 1 -> stringResource(Res.string.just_now)
        hours < 1 -> pluralStringResource(Res.plurals.time_minutes_ago, minutes, minutes)
        days < 1 -> pluralStringResource(Res.plurals.time_hours_ago, hours, hours)
        else -> pluralStringResource(Res.plurals.time_days_ago, days, days)
    }
}
