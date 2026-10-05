package com.womenriskmap.core.domain

import com.womenriskmap.core.domain.model.AccountStatus
import com.womenriskmap.core.domain.model.DayPeriod
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.OccurredWhen
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.ReportStatus
import com.womenriskmap.core.domain.model.ReportType
import com.womenriskmap.core.domain.model.UserProfile
import com.womenriskmap.core.domain.model.UserRole
import com.womenriskmap.core.domain.rules.LocationAnonymizer
import kotlin.time.Duration
import kotlin.time.Instant

val NOW: Instant = Instant.parse("2026-10-04T12:00:00Z")
val RIBEIRA = GeoPoint(41.1405, -8.6131)

fun report(
    id: String = "r1",
    at: GeoPoint = RIBEIRA,
    age: Duration = Duration.ZERO,
    confirmations: Int = 0,
    status: ReportStatus = ReportStatus.PUBLISHED,
    type: ReportType = ReportType.VERBAL_HARASSMENT,
    dayPeriod: DayPeriod = DayPeriod.NIGHT,
    isMine: Boolean = false,
    confirmedByMe: Boolean = false,
) = Report(
    id = id,
    zoneId = LocationAnonymizer.zoneId(at),
    location = LocationAnonymizer.snap(at),
    type = type,
    occurredWhen = OccurredWhen.TODAY,
    dayPeriod = dayPeriod,
    description = null,
    confirmations = confirmations,
    status = status,
    createdAt = NOW - age,
    isMine = isMine,
    confirmedByMe = confirmedByMe,
)

fun user(
    status: AccountStatus = AccountStatus.ACTIVE,
    emailConfirmed: Boolean = true,
    role: UserRole = UserRole.USER,
) = UserProfile(
    id = "u1",
    email = "ana@example.com",
    pseudonym = null,
    country = "PT",
    createdAt = NOW,
    status = status,
    role = role,
    emailConfirmed = emailConfirmed,
)
