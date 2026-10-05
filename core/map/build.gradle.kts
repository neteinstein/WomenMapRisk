// The only module that touches the map engine (MapLibre Compose). Features use SafetyMap / PinPickerMap,
// which speak domain types, so the engine can be swapped without touching features.
plugins {
    alias(libs.plugins.womenriskmap.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.designsystem)
            api(libs.maplibre.compose)
        }
    }
}
