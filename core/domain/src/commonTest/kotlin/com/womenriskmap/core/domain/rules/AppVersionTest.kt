package com.womenriskmap.core.domain.rules

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppVersionTest {
    @Test
    fun higher_run_number_is_newer() {
        assertTrue(AppVersion.isNewer(current = "1.0.9", candidate = "1.0.10"))
        assertFalse(AppVersion.isNewer(current = "1.0.10", candidate = "1.0.9"))
    }

    @Test
    fun same_version_is_not_newer() = assertFalse(AppVersion.isNewer("1.0.17", "1.0.17"))

    @Test
    fun missing_parts_count_as_zero_and_suffixes_are_ignored() {
        assertFalse(AppVersion.isNewer(current = "1.0", candidate = "1.0.0"))
        assertTrue(AppVersion.isNewer(current = "1.0.0-debug", candidate = "1.0.1"))
        assertTrue(AppVersion.isNewer(current = "1.0.5", candidate = "v1.1"))
    }
}
