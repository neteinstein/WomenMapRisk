import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Base for every shared module: Android (KMP library plugin, AGP 9+), iosArm64, iosSimulatorArm64, js (browser).
 * No iosX64 (removed upstream). The web target is Kotlin/JS, not wasmJs, because maplibre-compose ships no wasm artifact.
 * commonTest runs on the JVM through the Android host test (`testAndroidHostTest`).
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-multiplatform"))
        pluginManager.apply(libs.pluginId("android-kmp-library"))
        pluginManager.apply(libs.pluginId("kotlin-serialization"))
        pluginManager.apply(libs.pluginId("kover"))
        pluginManager.apply(libs.pluginId("ktlint"))

        extensions.configure<KotlinMultiplatformExtension> {
            jvmToolchain(17)
            extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                namespace = moduleNamespace
                compileSdk = libs.version("android-compileSdk").toInt()
                minSdk = libs.version("android-minSdk").toInt()
                withHostTest { }
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }
            iosArm64()
            iosSimulatorArm64()
            js {
                useEsModules()
                browser {
                    testTask { enabled = false } // JVM host tests are the CI signal; no headless browser needed.
                }
            }

            compilerOptions {
                freeCompilerArgs.addAll("-Xexpect-actual-classes")
                optIn.addAll("kotlin.time.ExperimentalTime")
            }

            sourceSets.getByName("commonMain").dependencies {
                implementation(libs.lib("kotlinx-coroutines-core"))
                implementation(libs.lib("kotlinx-serialization-json"))
                implementation(libs.lib("kotlinx-datetime"))
                implementation(project.dependencies.platform(libs.lib("koin-bom")))
                implementation(libs.lib("koin-core"))
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(libs.lib("kotlin-test"))
                implementation(libs.lib("kotlinx-coroutines-test"))
            }
        }
        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            filter { exclude { it.file.path.contains("${java.io.File.separator}build${java.io.File.separator}") } }
        }

        // Alias so `./gradlew testDebugUnitTest` runs every module's commonTest on the JVM, matching androidApp.
        tasks.register("testDebugUnitTest") {
            group = "verification"
            description = "Runs commonTest on the JVM via the Android host test."
            dependsOn("testAndroidHostTest")
        }
    }
}
