import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Shared module with Compose UI: adds the compose plugins, CMP libraries, lifecycle and Koin compose. */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("womenriskmap.kmp.library")
        pluginManager.apply(libs.pluginId("compose-multiplatform"))
        pluginManager.apply(libs.pluginId("compose-compiler"))

        extensions.configure<KotlinMultiplatformExtension> {
            // AGP 9's KMP library plugin disables Android resources by default; without this, Compose
            // resources (strings, drawables) are NOT packaged into the APK and the app crashes at runtime.
            extensions.configure<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget> {
                androidResources.enable = true
            }
            sourceSets.getByName("commonMain").dependencies {
                libs.findBundle("compose").get().get().forEach { implementation(it) }
                libs.findBundle("lifecycle").get().get().forEach { implementation(it) }
                implementation(libs.lib("koin-compose"))
                implementation(libs.lib("koin-compose-viewmodel"))
            }
        }
        extensions.configure<ComposeExtension> {
            (this as org.gradle.api.plugins.ExtensionAware).extensions.configure<ResourcesExtension> {
                packageOfResClass = "$moduleNamespace.resources"
                publicResClass = true
            }
        }
    }
}
