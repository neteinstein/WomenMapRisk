package com.womenriskmap.core.data.remote.dto

import com.womenriskmap.core.domain.model.AccountStatus
import com.womenriskmap.core.domain.model.DayPeriod
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.Invite
import com.womenriskmap.core.domain.model.InviteState
import com.womenriskmap.core.domain.model.OccurredWhen
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.ReportStatus
import com.womenriskmap.core.domain.model.ReportType
import com.womenriskmap.core.domain.model.SavedZone
import com.womenriskmap.core.domain.model.UserProfile
import com.womenriskmap.core.domain.model.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Wire format of `public.report_view` rows returned by the report RPCs. Enums travel as lowercase strings. */
@Serializable
data class ReportDto(
    val id: String,
    @SerialName("zone_id") val zoneId: String,
    val lat: Double,
    val lng: Double,
    val type: String,
    @SerialName("occurred_when") val occurredWhen: String,
    @SerialName("day_period") val dayPeriod: String,
    val description: String? = null,
    val confirmations: Int = 0,
    val status: String,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("is_establishment") val isEstablishment: Boolean = false,
    @SerialName("is_mine") val isMine: Boolean = false,
    @SerialName("confirmed_by_me") val confirmedByMe: Boolean = false,
) {
    fun toDomain() = Report(
        id = id,
        zoneId = zoneId,
        location = GeoPoint(lat, lng),
        type = enumOf(type, ReportType.OTHER),
        occurredWhen = enumOf(occurredWhen, OccurredWhen.EARLIER),
        dayPeriod = enumOf(dayPeriod, DayPeriod.DAY),
        description = description,
        confirmations = confirmations,
        status = enumOf(status, ReportStatus.PENDING),
        createdAt = createdAt,
        isEstablishment = isEstablishment,
        isMine = isMine,
        confirmedByMe = confirmedByMe,
    )
}

@Serializable
data class ProfileDto(
    val id: String,
    val email: String = "",
    val pseudonym: String? = null,
    val country: String = "PT",
    @SerialName("created_at") val createdAt: Instant,
    val status: String,
    val role: String,
    @SerialName("email_confirmed") val emailConfirmed: Boolean,
) {
    fun toDomain() = UserProfile(
        id = id,
        email = email,
        pseudonym = pseudonym,
        country = country,
        createdAt = createdAt,
        status = enumOf(status, AccountStatus.PENDING_INVITE),
        role = enumOf(role, UserRole.USER),
        emailConfirmed = emailConfirmed,
    )
}

@Serializable
data class SavedZoneDto(
    val id: String,
    @SerialName("zone_id") val zoneId: String,
    val lat: Double,
    val lng: Double,
    val name: String,
) {
    fun toDomain() = SavedZone(id = id, zoneId = zoneId, center = GeoPoint(lat, lng), name = name)
}

@Serializable
data class InviteDto(
    val id: String,
    val code: String,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("expires_at") val expiresAt: Instant,
    val state: String,
) {
    fun toDomain() = Invite(id = id, code = code, createdAt = createdAt, expiresAt = expiresAt, state = enumOf(state, InviteState.EXPIRED))
}

@Serializable
data class InviteDataDto(
    @SerialName("usage_days") val usageDays: List<String> = emptyList(),
    val invites: List<InviteDto> = emptyList(),
)

/** Lowercase snake_case wire value -> enum constant, tolerant to unknown values (forward compatibility). */
inline fun <reified E : Enum<E>> enumOf(value: String, fallback: E): E =
    enumValues<E>().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: fallback

val Enum<*>.wire: String get() = name.lowercase()
