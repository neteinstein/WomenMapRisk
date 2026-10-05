package com.womenriskmap.feature.auth

import com.womenriskmap.feature.auth.domain.SignUpError
import com.womenriskmap.feature.auth.domain.SignUpForm
import com.womenriskmap.feature.auth.domain.SignUpValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignUpValidatorTest {
    private val valid = SignUpForm("ana@example.com", "segura123", "PT", "ABCD-EFGH", acceptedTerms = true, declaredWoman = true)

    @Test
    fun valid_form_has_no_errors() = assertEquals(emptySet(), SignUpValidator.validate(valid))

    @Test
    fun each_rule_is_reported() {
        val errors = SignUpValidator.validate(SignUpForm("nope", "short", "PT", "XX", acceptedTerms = false, declaredWoman = false))
        assertEquals(SignUpError.entries.toSet(), errors)
    }

    @Test
    fun password_needs_8_chars_letters_and_digits() {
        assertTrue(SignUpValidator.isStrongPassword("abcdefg1"))
        assertFalse(SignUpValidator.isStrongPassword("abcdefgh"))
        assertFalse(SignUpValidator.isStrongPassword("12345678"))
        assertFalse(SignUpValidator.isStrongPassword("abc1"))
    }

    @Test
    fun email_is_trimmed_before_validation() {
        assertTrue(SignUpValidator.isValidEmail("  ana@example.pt "))
        assertFalse(SignUpValidator.isValidEmail("ana@example"))
    }
}
