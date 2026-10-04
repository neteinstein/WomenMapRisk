package com.womenriskmap.feature.auth.domain

import com.womenriskmap.core.domain.rules.InviteCode

/** Spec §4 Ecrã 2 fields + invite addendum. Pure validation; the server re-validates everything. */
data class SignUpForm(
    val email: String,
    val password: String,
    val country: String,
    val inviteCode: String,
    val acceptedTerms: Boolean,
    val declaredWoman: Boolean,
)

enum class SignUpError { INVALID_EMAIL, WEAK_PASSWORD, INVALID_INVITE, TERMS_REQUIRED, DECLARATION_REQUIRED }

object SignUpValidator {
    private val emailRegex = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]{2,}$""")

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    /** At least 8 characters with letters and digits (matches supabase/config.toml password_requirements). */
    fun isStrongPassword(password: String): Boolean =
        password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }

    fun validate(form: SignUpForm): Set<SignUpError> = buildSet {
        if (!isValidEmail(form.email)) add(SignUpError.INVALID_EMAIL)
        if (!isStrongPassword(form.password)) add(SignUpError.WEAK_PASSWORD)
        if (!InviteCode.isValid(form.inviteCode)) add(SignUpError.INVALID_INVITE)
        if (!form.acceptedTerms) add(SignUpError.TERMS_REQUIRED)
        if (!form.declaredWoman) add(SignUpError.DECLARATION_REQUIRED)
    }
}
