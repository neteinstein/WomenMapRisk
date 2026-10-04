package com.womenriskmap.core.domain.rules

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InviteCodeTest {
    @Test
    fun normalizes_case_spaces_and_dashes() {
        assertEquals("ABCDEFGH", InviteCode.normalize(" abcd-efgh "))
        assertEquals("ABCDEFGH", InviteCode.normalize("ab cd ef gh"))
    }

    @Test
    fun rejects_ambiguous_characters_and_wrong_length() {
        assertNull(InviteCode.normalize("ABCDEFG0")) // zero
        assertNull(InviteCode.normalize("ABCDEFGI")) // I
        assertNull(InviteCode.normalize("ABCDEFG"))
        assertFalse(InviteCode.isValid("ABCDEFGHJ"))
    }

    @Test
    fun generated_codes_are_valid_and_formatted() {
        val random = Random(42)
        repeat(100) { assertTrue(InviteCode.isValid(InviteCode.generate(random))) }
        assertEquals("ABCD-EFGH", InviteCode.format("abcdefgh"))
    }
}
