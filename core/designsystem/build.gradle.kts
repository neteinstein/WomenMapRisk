// Theme, shared components and ALL user-facing strings (EN default + PT) as Compose resources.
plugins {
    alias(libs.plugins.womenriskmap.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
        }
    }
}
