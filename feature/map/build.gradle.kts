// Spec §4 Ecrãs 3 (mapa), 4 (detalhe de zona) e 6 (filtros).
plugins {
    alias(libs.plugins.womenriskmap.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.maplibre.compose)
        }
    }
}
