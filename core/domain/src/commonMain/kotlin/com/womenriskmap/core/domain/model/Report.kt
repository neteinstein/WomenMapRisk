package com.womenriskmap.core.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Spec §4 Ecrã 5: tipo de situação (single choice). */
@Serializable
enum class ReportType { VERBAL_HARASSMENT, FOLLOWED, POORLY_LIT, DESERTED, ROBBERY, ASSAULT, OTHER }

/** Spec §4 Ecrã 5: quando aconteceu. */
@Serializable
enum class OccurredWhen { NOW, TODAY, THIS_WEEK, EARLIER }

/** Spec §4 Ecrã 5: período do dia. */
@Serializable
enum class DayPeriod { DAY, NIGHT }

/** Spec §8: estado (pendente, publicado, removido). HIDDEN = 3+ flags, awaiting review (spec §6). */
@Serializable
enum class ReportStatus { PENDING, PUBLISHED, HIDDEN, REMOVED }

/**
 * A report as shown to users. [location] is ALWAYS the zone centre, never the exact point (spec §6 Privacidade).
 * There is deliberately no author field: reports are anonymous to other users. [isMine] is computed server-side.
 */
@Serializable
data class Report(
    val id: String,
    val zoneId: String,
    val location: GeoPoint,
    val type: ReportType,
    val occurredWhen: OccurredWhen,
    val dayPeriod: DayPeriod,
    val description: String?,
    val confirmations: Int,
    val status: ReportStatus,
    val createdAt: Instant,
    val isEstablishment: Boolean = false,
    val isMine: Boolean = false,
    val confirmedByMe: Boolean = false,
)

/** A new report, before submission. The exact [location] is snapped to a zone by client AND server. */
data class ReportDraft(
    val location: GeoPoint,
    val type: ReportType,
    val occurredWhen: OccurredWhen,
    val dayPeriod: DayPeriod,
    val description: String?,
    val isEstablishment: Boolean = false,
)

/** Spec §8 Denúncia: motivo. */
@Serializable
enum class FlagReason { FALSE_INFORMATION, PERSONAL_DATA, OFFENSIVE, SPAM, OTHER }
