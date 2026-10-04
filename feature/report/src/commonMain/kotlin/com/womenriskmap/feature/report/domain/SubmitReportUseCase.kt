package com.womenriskmap.feature.report.domain

import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.DayPeriod
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.ReportDraft
import com.womenriskmap.core.domain.model.profileOrNull
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.core.domain.rules.ReportPolicy
import com.womenriskmap.core.domain.rules.ReportPolicy.SubmitCheck
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Spec §5 "Reportar" + §6 abuse rules. Checks the policy locally for instant feedback (the server
 * re-checks everything), then submits.
 */
class SubmitReportUseCase(
    private val reports: ReportRepository,
    private val sessions: SessionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(draft: ReportDraft): Result<Report> {
        val user = sessions.session.value.profileOrNull
        // If "my reports" can't be fetched (flaky network), let the server be the judge.
        val mine = reports.myReports().getOrDefault(emptyList())
        when (ReportPolicy.checkSubmit(user, mine, draft.location, draft.description, clock.now())) {
            SubmitCheck.Allowed -> Unit
            SubmitCheck.NotEnabled -> return Result.failure(
                if (user?.emailConfirmed ==
                    false
                ) {
                    DomainException.EmailNotConfirmed()
                } else {
                    DomainException.NotAllowed()
                },
            )
            SubmitCheck.DailyLimitReached -> return Result.failure(DomainException.DailyReportLimit())
            SubmitCheck.DuplicateSamePlaceSameDay -> return Result.failure(DomainException.DuplicateReport())
            SubmitCheck.DescriptionTooLong -> return Result.failure(DomainException.Unknown("description_too_long"))
        }
        return reports.submit(draft)
    }
}

/** Pre-selects the time of day from the local clock (07:00–19:59 = day), saving a tap. */
fun defaultDayPeriod(now: Instant, zone: TimeZone = TimeZone.currentSystemDefault()): DayPeriod =
    if (now.toLocalDateTime(zone).hour in 7..19) DayPeriod.DAY else DayPeriod.NIGHT
