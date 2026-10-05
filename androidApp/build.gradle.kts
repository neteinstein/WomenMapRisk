import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.Properties

// The Android application: manifest, MainActivity, Application (Koin + Firebase init), R8 config.
// All UI and logic live in :composeApp (KMP library). AGP 9 has built-in Kotlin, so no kotlin-android plugin.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.play.publisher)
}

val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Release signing: env vars (release-android.yml) win over a local keystore.properties.
val releaseKeystoreFile = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
    ?: keystoreProps.getProperty("storeFile")?.let { rootProject.file(it).path }

android {
    namespace = "com.womenriskmap.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.womenriskmap.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // release-android.yml sets these from the run number so every release has a higher versionCode
        // without editing this file. Local/debug builds fall back to the defaults.
        versionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("APP_VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.0.0"
    }

    // Two distribution channels from one codebase, same applicationId:
    // - "github": APK published on GitHub Releases. Updates itself from Settings (src/github/AndroidManifest.xml adds
    //   the install permission, FileProvider and the meta-data flag that turns the Updates section on).
    // - "playstore": uploaded to Play, which handles updates; no self-update code path is reachable.
    flavorDimensions += "distribution"
    productFlavors {
        create("github") { dimension = "distribution" }
        create("playstore") { dimension = "distribution" }
    }
    // Gradle Play Publisher is off by default (see `play {}` below) and on for "playstore" only.
    playConfigs {
        register("playstore") { enabled.set(true) }
    }

    signingConfigs {
        // Release signing only when a keystore is provided (CI secrets / local keystore.properties).
        if (releaseKeystoreFile != null) {
            create("release") {
                storeFile = file(releaseKeystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
                keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias")
                keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Falls back to debug signing so CI can still validate the R8 build without secrets.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            // Only upload R8 mapping files when real Firebase credentials are configured (release CI).
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = System.getenv("CRASHLYTICS_UPLOAD_MAPPING") == "true"
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    androidResources {
        // Per-app language (Android 13+): generates locales_config from res/values* (en default, pt).
        generateLocaleConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/versions/9/previous-compilation-data.bin")
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkDependencies = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

kotlin {
    jvmToolchain(17)
}

// Gradle Play Publisher, enabled for the "playstore" flavour only ("github" shares the applicationId and must never
// be uploaded). Authenticates via the ANDROID_PUBLISHER_CREDENTIALS env var (the raw JSON of a
// Play Console service account key). Only release-android.yml invokes a publish task. Uploads go to the
// "internal" track unless PLAY_TRACK overrides it, so nothing reaches production without an explicit
// promotion in the Play Console.
play {
    enabled.set(false)
    track.set(System.getenv("PLAY_TRACK")?.takeIf { it.isNotBlank() } ?: "internal")
    defaultToAppBundles.set(true)
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.core.data)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.maplibre.compose.runtime.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    debugImplementation(libs.compose.ui.tooling)
}
