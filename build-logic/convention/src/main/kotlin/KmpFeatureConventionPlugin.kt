import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A feature module (`:feature:x`). Depends on the core modules only, never on another feature.
 * Koin wiring happens in composeApp's di/AppModule.kt, not here.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("womenriskmap.kmp.compose")
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":core:domain"))
                implementation(project(":core:data"))
                implementation(project(":core:designsystem"))
                implementation(libs.lib("androidx-navigation-compose"))
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(project(":core:testing"))
            }
        }
    }
}
