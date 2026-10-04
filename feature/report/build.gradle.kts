// Spec §4 Ecrã 5 (fazer um reporte) + post-"agressão" support screen; edit/delete within 24 h.
plugins {
    alias(libs.plugins.womenriskmap.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.map)
        }
    }
}
