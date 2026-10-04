// Shared fakes for commonTest in feature modules (fake repositories, test clock).
plugins {
    alias(libs.plugins.womenriskmap.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(libs.kotlinx.coroutines.test)
            api(libs.kotlin.test)
        }
    }
}
