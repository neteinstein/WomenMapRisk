import java.util.Properties

// Data layer: Supabase client, repository implementations of core:domain contracts, offline cache,
// geocoding, and the few platform services that genuinely need expect/actual (location, connectivity, storage).
plugins {
    alias(libs.plugins.womenriskmap.kmp.library)
}

// --- AppConfig: build-time configuration from local.properties (gitignored) or environment variables (CI). ---
val appConfigKeys = listOf("SUPABASE_URL", "SUPABASE_ANON_KEY", "GOOGLE_WEB_CLIENT_ID", "WEB_APP_URL")
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val appConfigValues: Map<String, String> = appConfigKeys.associateWith { key ->
    providers.environmentVariable(key).orNull ?: localProps.getProperty(key) ?: ""
}

val generateAppConfig by tasks.registering(GenerateAppConfigTask::class) {
    values.set(appConfigValues)
    packageName.set("com.womenriskmap.core.data.config")
    outputDir.set(layout.buildDirectory.dir("generated/appconfig/kotlin"))
}

kotlin {
    sourceSets {
        commonMain {
            // Task-provider srcDir: every consumer (compile, ktlint, sources jars) depends on generateAppConfig automatically.
            kotlin.srcDir(generateAppConfig.flatMap { it.outputDir })
            dependencies {
                api(projects.core.domain)
                api(project.dependencies.platform(libs.supabase.bom))
                api(libs.supabase.auth)
                api(libs.supabase.postgrest)
                api(libs.supabase.realtime)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kstore)
            }
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kstore.file)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.kstore.file)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.kstore.storage)
        }
    }
}
