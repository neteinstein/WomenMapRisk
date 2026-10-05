package com.womenriskmap.core.domain.rules

import kotlin.random.Random

/** 8-char single-use codes from an unambiguous alphabet (no 0/O/1/I/L). Displayed as ABCD-EFGH. */
object InviteCode {
    const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val LENGTH = 8

    /** Uppercases and strips spaces, dashes and dots; returns null if the result is not a well-formed code. */
    fun normalize(input: String): String? {
        val cleaned = input.uppercase().filterNot { it == '-' || it == '.' || it.isWhitespace() }
        return cleaned.takeIf { it.length == LENGTH && it.all { c -> c in ALPHABET } }
    }

    fun isValid(input: String): Boolean = normalize(input) != null

    fun format(code: String): String = normalize(code)?.let { "${it.take(4)}-${it.drop(4)}" } ?: code

    fun generate(random: Random = Random.Default): String =
        buildString(LENGTH) {
            repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }
}
