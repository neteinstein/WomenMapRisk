package com.womenriskmap.core.domain.rules

/**
 * Compares dotted numeric version names ("1.0.17"). Release builds are versioned `1.0.<run number>` by
 * release-android.yml. Non-numeric parts (e.g. "-debug") are ignored; missing parts count as 0.
 */
object AppVersion {
    fun isNewer(current: String, candidate: String): Boolean {
        val a = parts(current)
        val b = parts(candidate)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return y > x
        }
        return false
    }

    private fun parts(version: String): List<Int> =
        version.trim().removePrefix("v").split('.').map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
}
