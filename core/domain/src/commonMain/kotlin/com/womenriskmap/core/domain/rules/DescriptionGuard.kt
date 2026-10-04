package com.womenriskmap.core.domain.rules

/**
 * Spec §4 Ecrã 5 / §6: "Não incluas nomes, matrículas nem dados que identifiquem pessoas."
 * Advisory heuristics only. The warning is always shown; this adds a specific nudge when the text
 * looks like it contains identifying data. Moderation remains the real control.
 */
object DescriptionGuard {
    enum class Finding { LICENSE_PLATE, PHONE_NUMBER, EMAIL, URL }

    // Portuguese plates: AA-00-00, 00-00-AA, 00-AA-00, AA-00-AA. Separators "-" or "." or none; spaces are
    // not accepted, to avoid false positives on ordinary words like "de 12 em".
    private val plate = Regex("""\b([A-Z]{2}|\d{2})[-.]?([A-Z]{2}|\d{2})[-.]?([A-Z]{2}|\d{2})\b""")
    private val phone = Regex("""(?<!\d)(\+?\d[\d\s.-]{7,}\d)(?!\d)""")
    private val email = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    private val url = Regex("""(https?://|www\.)\S+""", RegexOption.IGNORE_CASE)

    fun inspect(text: String?): Set<Finding> {
        if (text.isNullOrBlank()) return emptySet()
        val findings = mutableSetOf<Finding>()
        val upper = text.uppercase()
        if (plate.findAll(upper).any { m ->
                m.groupValues.drop(1).let { g ->
                    g.any { it.all(Char::isDigit) } &&
                        g.any { it.all(Char::isLetter) }
                }
            }
        ) {
            findings += Finding.LICENSE_PLATE
        }
        if (phone.findAll(text).any { m -> m.value.count(Char::isDigit) >= 9 }) findings += Finding.PHONE_NUMBER
        if (email.containsMatchIn(text)) findings += Finding.EMAIL
        if (url.containsMatchIn(text)) findings += Finding.URL
        return findings
    }
}
