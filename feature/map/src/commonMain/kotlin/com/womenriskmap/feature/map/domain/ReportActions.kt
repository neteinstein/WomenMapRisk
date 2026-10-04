package com.womenriskmap.feature.map.domain

import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.profileOrNull
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.core.domain.rules.ReportPolicy

/** Spec §5 "Confirmar um reporte": +1, once per user, never your own. */
class ConfirmReportUseCase(private val reports: ReportRepository, private val sessions: SessionRepository) {
    suspend operator fun invoke(report: Report): Result<Unit> {
        val user = sessions.session.value.profileOrNull
        return when {
            report.isMine -> Result.failure(DomainException.CannotConfirmOwn())
            report.confirmedByMe -> Result.failure(DomainException.AlreadyConfirmed())
            !ReportPolicy.canConfirm(report, user) -> Result.failure(DomainException.NotAllowed())
            else -> reports.confirm(report.id)
        }
    }
}

/** Spec §4 Ecrã 4 "Denunciar reporte". */
class FlagReportUseCase(private val reports: ReportRepository, private val sessions: SessionRepository) {
    suspend operator fun invoke(report: Report, reason: FlagReason): Result<Unit> =
        if (!ReportPolicy.canFlag(report, sessions.session.value.profileOrNull)) {
            Result.failure(DomainException.NotAllowed())
        } else {
            reports.flag(report.id, reason)
        }
}
