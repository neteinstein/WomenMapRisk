// Shared app shell (KMP library, AGP 9 `com.android.kotlin.multiplatform.library`): App(), navigation,
// the single Koin module (di/AppModule.kt) and the platform bootstrap (di/InitKoin.kt).
// The Android *application* lives in :androidApp, because AGP 9 forbids applying KMP and com.android.application
// in the same module. iOS consumes the `ComposeApp` framework; web runs from the js executable below.
plugins {
    alias(libs.plugins.womenriskmap.kmp.compose)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    js {
        browser {
            commonWebpackConfig { outputFileName = "composeApp.js" }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.data)
            implementation(projects.core.designsystem)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.auth)
            implementation(projects.feature.map)
            implementation(projects.feature.report)
            implementation(projects.feature.saved)
            implementation(projects.feature.invites)
            implementation(projects.feature.profile)
            implementation(projects.feature.moderation)
            implementation(libs.androidx.navigation.compose)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.koin.test)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}

kotlin {
    sourceSets {
        getByName("androidHostTest").dependencies {
            implementation(libs.koin.test)
        }
    }
}
