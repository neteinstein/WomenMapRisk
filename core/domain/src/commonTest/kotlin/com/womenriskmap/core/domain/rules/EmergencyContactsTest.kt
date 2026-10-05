package com.womenriskmap.core.domain.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmergencyContactsTest {
    @Test
    fun portugal_has_112_and_victim_support() {
        val pt = EmergencyContacts.forCountry("pt")
        assertTrue(pt.any { it.number == "112" })
        assertTrue(pt.any { it.service == EmergencyContacts.Service.VICTIM_SUPPORT })
    }

    @Test
    fun unknown_country_falls_back_to_112() {
        assertEquals(listOf("112"), EmergencyContacts.forCountry("XX").map { it.number })
        assertEquals(listOf("112"), EmergencyContacts.forCountry(null).map { it.number })
    }
}
