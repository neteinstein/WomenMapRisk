package com.womenriskmap.core.domain.rules

import com.womenriskmap.core.domain.rules.DescriptionGuard.Finding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DescriptionGuardTest {
    @Test
    fun detects_portuguese_plates() {
        listOf("carro AA-12-34", "matrícula 12-34-AB", "12-AB-34 parado", "AB-12-CD", "ab12cd").forEach {
            assertTrue(Finding.LICENSE_PLATE in DescriptionGuard.inspect(it), it)
        }
    }

    @Test
    fun ordinary_text_has_no_findings() {
        listOf(
            "Rua mal iluminada depois das 22h",
            "Fui seguida de 12 em 12 metros",
            "Homem gritou comentários na paragem",
            "",
            null,
        ).forEach { assertEquals(emptySet(), DescriptionGuard.inspect(it), it.toString()) }
    }

    @Test
    fun detects_phone_email_url() {
        assertTrue(Finding.PHONE_NUMBER in DescriptionGuard.inspect("liga 912 345 678"))
        assertTrue(Finding.EMAIL in DescriptionGuard.inspect("contacto: x@y.pt"))
        assertTrue(Finding.URL in DescriptionGuard.inspect("ver www.exemplo.pt"))
    }
}
