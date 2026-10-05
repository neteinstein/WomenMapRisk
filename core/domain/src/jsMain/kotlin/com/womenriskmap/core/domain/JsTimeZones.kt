package com.womenriskmap.core.domain

@JsModule("@js-joda/timezone")
@JsNonModule
external object JsJodaTimeZoneModule

/** Touching the module registers the IANA tz database with js-joda (used by kotlinx-datetime). */
internal actual fun loadTimeZoneDatabase() {
    JsJodaTimeZoneModule
}
