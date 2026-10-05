plugins {
    `kotlin-dsl`
}

group = "com.womenriskmap.buildlogic"

dependencies {
    compileOnly(libs.gradlePlugin.kotlin)
    compileOnly(libs.gradlePlugin.android)
    compileOnly(libs.gradlePlugin.compose)
    compileOnly(libs.gradlePlugin.composeCompiler)
    compileOnly(libs.gradlePlugin.kover)
    compileOnly(libs.gradlePlugin.ktlint)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "womenriskmap.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "womenriskmap.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "womenriskmap.kmp.feature"
            implementationClass = "KmpFeatureConventionPlugin"
        }
    }
}
