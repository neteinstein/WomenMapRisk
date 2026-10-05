// Root project: declares plugins (apply false) so their versions resolve once, from the catalog.
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover)
}

// Aggregated coverage across all shared modules (./gradlew koverHtmlReport / koverXmlReport).
dependencies {
    kover(projects.core.domain)
    kover(projects.core.data)
    kover(projects.feature.auth)
    kover(projects.feature.map)
    kover(projects.feature.report)
    kover(projects.feature.saved)
    kover(projects.feature.invites)
    kover(projects.feature.profile)
    kover(projects.feature.moderation)
}

kover {
    reports {
        filters {
            excludes {
                // Generated code and Compose UI are verified by previews/manual checks, not unit coverage.
                classes("*.resources.*", "*ComposableSingletons*", "*_Factory*")
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }
    }
}
