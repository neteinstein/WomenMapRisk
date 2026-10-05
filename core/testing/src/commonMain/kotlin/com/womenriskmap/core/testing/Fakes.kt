package com.womenriskmap.core.testing

import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.AccountStatus
import com.womenriskmap.core.domain.model.BoundingBox
import com.womenriskmap.core.domain.model.DayPeriod
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.Invite
import com.womenriskmap.core.domain.model.InviteState
import com.womenriskmap.core.domain.model.OccurredWhen
import com.womenriskmap.core.domain.model.Place
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.ReportDraft
import com.womenriskmap.core.domain.model.ReportStatus
import com.womenriskmap.core.domain.model.ReportType
import com.womenriskmap.core.domain.model.SavedZone
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.domain.model.UserProfile
import com.womenriskmap.core.domain.model.UserRole
import com.womenriskmap.core.domain.repository.AreaSnapshot
import com.womenriskmap.core.domain.repository.ConnectivityMonitor
import com.womenriskmap.core.domain.repository.GeocodingRepository
import com.womenriskmap.core.domain.repository.InviteData
import com.womenriskmap.core.domain.repository.InviteRepository
import com.womenriskmap.core.domain.repository.LocationProvider
import com.womenriskmap.core.domain.repository.PreferencesRepository
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.repository.SavedZoneRepository
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.core.domain.rules.LocationAnonymizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

val TEST_NOW: Instant = Instant.parse("2026-10-04T12:00:00Z")
val PORTO_RIBEIRA = GeoPoint(41.1405, -8.6131)

class TestClock(var now: Instant = TEST_NOW) : Clock {
    override fun now(): Instant = now
}

fun testUser(
    status: AccountStatus = AccountStatus.ACTIVE,
    emailConfirmed: Boolean = true,
    role: UserRole = UserRole.USER,
    country: String = "PT",
) = UserProfile("u1", "ana@example.com", null, country, TEST_NOW - 30.days, status, role, emailConfirmed)

fun testReport(
    id: String = "r1",
    at: GeoPoint = PORTO_RIBEIRA,
    age: Duration = Duration.ZERO,
    type: ReportType = ReportType.VERBAL_HARASSMENT,
    confirmations: Int = 0,
    status: ReportStatus = ReportStatus.PUBLISHED,
    isMine: Boolean = false,
    confirmedByMe: Boolean = false,
    description: String? = null,
) = Report(
    id = id,
    zoneId = LocationAnonymizer.zoneId(at),
    location = LocationAnonymizer.snap(at),
    type = type,
    occurredWhen = OccurredWhen.TODAY,
    dayPeriod = DayPeriod.NIGHT,
    description = description,
    confirmations = confirmations,
    status = status,
    createdAt = TEST_NOW - age,
    isMine = isMine,
    confirmedByMe = confirmedByMe,
)

class FakeSessionRepository(initial: SessionState = SessionState.Visitor) : SessionRepository {
    val state = MutableStateFlow(initial)
    override val session: StateFlow<SessionState> = state
    var nextError: Throwable? = null
    val calls = mutableListOf<String>()

    private fun result(call: String): Result<Unit> {
        calls += call
        return nextError?.let {
            nextError = null
            Result.failure(it)
        } ?: Result.success(Unit)
    }

    override suspend fun signUp(email: String, password: String, country: String, inviteCode: String) = result(
        "signUp:$email:$country:$inviteCode",
    )
    override suspend fun signIn(email: String, password: String) = result("signIn:$email").onSuccess {
        state.value =
            SessionState.SignedIn(testUser())
    }
    override suspend fun signInWithGoogle() = result("google")
    override suspend fun resendConfirmation(email: String) = result("resend:$email")
    override suspend fun signOut() = result("signOut").onSuccess { state.value = SessionState.Visitor }
    override suspend fun refresh() = result("refresh")
    override suspend fun updateProfile(pseudonym: String?, country: String) = result("update:$pseudonym:$country")
}

class FakeReportRepository(var reports: List<Report> = emptyList()) : ReportRepository {
    val changesFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val changes: Flow<Unit> = changesFlow
    var stale = false
    var nextError: Throwable? = null
    var mine: List<Report> = emptyList()
    val submitted = mutableListOf<ReportDraft>()
    val confirmed = mutableListOf<String>()
    val flagged = mutableListOf<Pair<String, FlagReason>>()
    val deleted = mutableListOf<String>()

    private fun <T> next(value: () -> T): Result<T> = nextError?.let {
        nextError = null
        Result.failure(it)
    } ?: Result.success(value())

    override suspend fun reportsIn(area: BoundingBox) = next { AreaSnapshot(reports.filter { it.location in area }, TEST_NOW, stale) }
    override suspend fun reportsInZone(zoneId: String) = next { reports.filter { it.zoneId == zoneId } }
    override suspend fun submit(draft: ReportDraft) = next {
        submitted += draft
        testReport(
            id = "new${submitted.size}",
            at = draft.location,
            type = draft.type,
            isMine = true,
            status = if (draft.isEstablishment) ReportStatus.PENDING else ReportStatus.PUBLISHED,
        )
            .also { mine = mine + it }
    }
    override suspend fun update(
        reportId: String,
        draft: ReportDraft,
    ) = next { testReport(id = reportId, at = draft.location, type = draft.type, isMine = true) }
    override suspend fun delete(reportId: String) = next {
        deleted += reportId
        mine = mine.filterNot { it.id == reportId }
    }
    override suspend fun confirm(reportId: String) = next {
        confirmed += reportId
        Unit
    }
    override suspend fun flag(reportId: String, reason: FlagReason) = next {
        flagged += reportId to reason
        Unit
    }
    override suspend fun myReports() = next { mine }
}

class FakeSavedZoneRepository : SavedZoneRepository {
    private val state = MutableStateFlow<List<SavedZone>>(emptyList())
    override val savedZones: StateFlow<List<SavedZone>> = state
    var nextError: Throwable? = null

    fun set(zones: List<SavedZone>) {
        state.value = zones
    }

    override suspend fun refresh(): Result<Unit> = nextError?.let {
        nextError = null
        Result.failure(it)
    } ?: Result.success(Unit)
    override suspend fun save(zoneId: String, center: GeoPoint, name: String): Result<SavedZone> {
        val zone = SavedZone("s${state.value.size + 1}", zoneId, center, name)
        state.update { it + zone }
        return Result.success(zone)
    }
    override suspend fun delete(id: String): Result<Unit> {
        nextError?.let {
            nextError = null
            return Result.failure(it)
        }
        state.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }
}

class FakeInviteRepository(
    var usageDays: List<LocalDate> = emptyList(),
    var invites: List<Invite> = emptyList(),
) : InviteRepository {
    var validCodes = setOf("ABCDEFGH")
    var usageRecorded = 0
    var nextError: Throwable? = null
    val redeemed = mutableListOf<String>()

    private fun <T> next(value: () -> T): Result<T> = nextError?.let {
        nextError = null
        Result.failure(it)
    } ?: Result.success(value())

    override suspend fun recordUsage() = next {
        usageRecorded++
        Unit
    }
    override suspend fun load() = next { InviteData(usageDays, invites) }
    override suspend fun create() = next {
        Invite("i${invites.size + 1}", "NEWCODE${invites.size + 2}".take(8), TEST_NOW, TEST_NOW + 30.days, InviteState.PENDING).also {
            invites =
                invites + it
        }
    }
    override suspend fun revoke(inviteId: String) = next {
        invites = invites.map { if (it.id == inviteId) it.copy(state = InviteState.REVOKED) else it }
    }
    override suspend fun validate(code: String) = next { code.uppercase().replace("-", "") in validCodes }
    override suspend fun redeem(code: String): Result<Unit> {
        if (code.uppercase().replace("-", "") !in validCodes) return Result.failure(DomainException.InvalidInvite())
        return next {
            redeemed += code
            Unit
        }
    }
}

class FakeGeocodingRepository(var places: List<Place> = emptyList(), var name: String? = "Rua das Flores") : GeocodingRepository {
    val queries = mutableListOf<String>()
    override suspend fun search(query: String, near: GeoPoint?): Result<List<Place>> {
        queries += query
        return Result.success(places.filter { it.name.contains(query, ignoreCase = true) })
    }
    override suspend fun nameOf(point: GeoPoint): Result<String?> = Result.success(name)
}

class FakeLocationProvider(var location: GeoPoint? = null, var permission: Boolean = location != null) : LocationProvider {
    override fun hasPermission(): Boolean = permission
    override suspend fun currentLocation(): GeoPoint? = if (permission) location else null
}

class FakeConnectivityMonitor(online: Boolean = true) : ConnectivityMonitor {
    val state = MutableStateFlow(online)
    override val isOnline: StateFlow<Boolean> = state
}

class FakePreferencesRepository : PreferencesRepository {
    override val locationHistoryEnabled = MutableStateFlow(false)
    override val lastKnownLocation = MutableStateFlow<GeoPoint?>(null)
    override val welcomeSeen = MutableStateFlow(false)
    override suspend fun setWelcomeSeen() {
        welcomeSeen.value = true
    }
    override suspend fun setLocationHistoryEnabled(enabled: Boolean) {
        locationHistoryEnabled.value = enabled
        if (!enabled) lastKnownLocation.value = null
    }
    override suspend fun rememberLocation(point: GeoPoint) {
        if (locationHistoryEnabled.value) lastKnownLocation.value = LocationAnonymizer.snap(point)
    }
}
