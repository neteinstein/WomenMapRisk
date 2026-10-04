package com.womenriskmap.core.domain.rules

/**
 * Spec §4 Ecrã 5: if the type is "agressão", show the country's emergency contacts after confirmation.
 * ⚠ VERIFY BEFORE LAUNCH: numbers must be re-checked with official sources for every supported country.
 * Labels are resolved in the UI from [Service] (strings PT/EN), so the domain stays UI-free.
 */
object EmergencyContacts {
    enum class Service { EMERGENCY, VICTIM_SUPPORT, DOMESTIC_VIOLENCE }

    data class Contact(val service: Service, val number: String)

    /** EU-wide emergency number, used for any country without a specific list. */
    private val fallback = listOf(Contact(Service.EMERGENCY, "112"))

    private val byCountry: Map<String, List<Contact>> =
        mapOf(
            "PT" to
                listOf(
                    Contact(Service.EMERGENCY, "112"),
                    Contact(Service.VICTIM_SUPPORT, "116006"), // APAV, Linha de Apoio à Vítima
                    Contact(Service.DOMESTIC_VIOLENCE, "800202148"), // Serviço de Informação a Vítimas de Violência Doméstica
                ),
            "ES" to
                listOf(
                    Contact(Service.EMERGENCY, "112"),
                    Contact(Service.DOMESTIC_VIOLENCE, "016"),
                ),
        )

    fun forCountry(isoCode: String?): List<Contact> = byCountry[isoCode?.uppercase()] ?: fallback
}
