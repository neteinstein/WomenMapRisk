package com.womenriskmap.core.domain.rules

/** Spec §4 Ecrã 2: país. ISO 3166-1 alpha-2 codes; display names come from the UI string resources. */
object Countries {
    const val DEFAULT = "PT"
    const val OTHER = "XX"
    val supported: List<String> = listOf("PT", "ES", "FR", "GB", "DE", "IT", "NL", "BR", "US", OTHER)
}
