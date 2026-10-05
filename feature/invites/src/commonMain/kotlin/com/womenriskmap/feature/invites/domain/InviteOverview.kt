package com.womenriskmap.feature.invites.domain

import com.womenriskmap.core.domain.model.Invite
import com.womenriskmap.core.domain.model.profileOrNull
import com.womenriskmap.core.domain.repository.InviteRepository
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.core.domain.rules.InviteCode
import com.womenriskmap.core.domain.rules.InviteEligibility
import com.womenriskmap.core.domain.rules.ReportPolicy
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class InviteOverview(val status: InviteEligibility.Status, val invites: List<Invite>)

/** docs/spec/addendum-invites.md: unlock after 3 consecutive / 5 distinct days; max 5; moderators exempt. */
class GetInviteOverviewUseCase(
    private val invites: InviteRepository,
    private val sessions: SessionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): Result<InviteOverview> {
        val profile = sessions.session.value.profileOrNull
        return invites.load().map { data ->
            val today = clock.now().toLocalDateTime(ReportPolicy.policyTimeZone).date
            val active = data.invites.count {
                it.state == com.womenriskmap.core.domain.model.InviteState.PENDING ||
                    it.state == com.womenriskmap.core.domain.model.InviteState.USED
            }
            val status = InviteEligibility.evaluate(
                usageDays = data.usageDays,
                today = today,
                activeInvites = active,
                isEnabled = profile?.isEnabled == true,
                isModerator = profile?.isModerator == true,
            )
            InviteOverview(status, data.invites.sortedByDescending { it.createdAt })
        }
    }
}

/** The text shared with the invitee: code + a link that pre-fills sign-up on the web app. */
fun inviteShareLink(baseUrl: String, code: String): String = "${baseUrl.trimEnd('/')}/?invite=${InviteCode.normalize(code) ?: code}"
