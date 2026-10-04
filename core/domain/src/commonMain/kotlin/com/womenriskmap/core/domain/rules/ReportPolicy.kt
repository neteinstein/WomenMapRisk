package com.womenriskmap.core.domain.rules

import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.UserProfile
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Spec §6 Prevenção de abusos / §5 Confirmar / §4 Ecrã 5. Client-side mirror for instant UX feedback;
 * the database enforces the same rules (triggers + RLS) and is the source of truth.
 */
object ReportPolicy {
    const val MAX_REPORTS_PER_DAY = 5
    const val MAX_DESCRIPTION_LENGTH = 300
    val EDIT_WINDOW = 24.hours
    const val FLAGS_TO_HIDE = 3

    /** "Day" boundaries follow the pilot city's timezone. */
    val policyTimeZone: TimeZone = TimeZone.of("Europe/Lisbon")

    sealed interface SubmitCheck {
        data object Allowed : SubmitCheck

        data object NotEnabled : SubmitCheck

        data object DailyLimitReached : SubmitCheck

        data object DuplicateSamePlaceSameDay : SubmitCheck

        data object DescriptionTooLong : SubmitCheck
    }

    fun checkSubmit(
        user: UserProfile?,
        myReports: List<Report>,
        location: GeoPoint,
        description: String?,
        now: Instant,
    ): SubmitCheck {
        if (user == null || !user.isEnabled) return SubmitCheck.NotEnabled
        if ((description?.length ?: 0) > MAX_DESCRIPTION_LENGTH) return SubmitCheck.DescriptionTooLong
        val today = now.toLocalDateTime(policyTimeZone).date
        val todays = myReports.filter { it.createdAt.toLocalDateTime(policyTimeZone).date == today }
        if (todays.size >= MAX_REPORTS_PER_DAY) return SubmitCheck.DailyLimitReached
        val zone = LocationAnonymizer.zoneId(location)
        if (todays.any { it.zoneId == zone }) return SubmitCheck.DuplicateSamePlaceSameDay
        return SubmitCheck.Allowed
    }

    /** Spec §4 Ecrã 5: "Pode editar ou apagar o reporte durante 24 horas." */
    fun canEditOrDelete(report: Report, now: Instant): Boolean = report.isMine && now - report.createdAt < EDIT_WINDOW

    /** Spec §5: só pode confirmar uma vez, e não pode confirmar o seu. */
    fun canConfirm(report: Report, user: UserProfile?): Boolean =
        user != null && user.isEnabled && !report.isMine && !report.confirmedByMe

    fun canFlag(report: Report, user: UserProfile?): Boolean = user != null && user.isEnabled && !report.isMine
}
