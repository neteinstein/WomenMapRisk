package com.womenriskmap.core.domain

/** Browser only: kotlinx-datetime has no tz database on Kotlin/JS until one is loaded. No-op elsewhere. */
internal expect fun loadTimeZoneDatabase()
