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
}

val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.womenriskmap.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.womenriskmap.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0.0"
    }

    signingConfigs {
        // Release signing only when a keystore is provided (CI secret / local keystore.properties).
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
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
