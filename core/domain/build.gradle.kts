// Pure domain layer: models, repository interfaces, use cases, business rules. NO Compose, NO platform code.
plugins {
    alias(libs.plugins.womenriskmap.kmp.library)
}

kotlin {
    sourceSets {
        jsMain.dependencies {
            // kotlinx-datetime needs the IANA tz database in the browser ("Europe/Lisbon" for policy days).
            implementation(npm("@js-joda/timezone", libs.versions.js.joda.timezone.get()))
        }
    }
}
