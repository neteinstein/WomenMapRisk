---
name: add-feature-module
description: Scaffold a new feature module in Women Risk Map (KMP, Compose, MVVM, Koin) following the repo conventions. Use when a new screen or flow doesn't belong to an existing feature/* module.
---

# Add a feature module

Follow these steps exactly; the conventions are enforced by convention plugins and CI.

1. **Create `feature/<name>/build.gradle.kts`**:
   ```kotlin
   plugins { alias(libs.plugins.womenriskmap.kmp.feature) }
   ```
   The plugin adds the KMP targets (android, iosArm64, iosSimulatorArm64, js), Compose, lifecycle, Koin compose,
   navigation, and dependencies on `core:domain`, `core:data`, `core:designsystem` and (tests) `core:testing`.
   If you need the map, add `implementation(projects.core.map)`. **Never depend on another `feature:*`.**
2. **Register** `include(":feature:<name>")` in `settings.gradle.kts`, `kover(projects.feature.<name>)` in the root
   `build.gradle.kts`, and `implementation(projects.feature.<name>)` in `composeApp/build.gradle.kts`.
3. **Packages** under `src/commonMain/kotlin/com/womenriskmap/feature/<name>/`:
   - `domain/`: use cases, feature-local repository interfaces, models. No Compose imports.
   - `data/`: repository implementations (Supabase RPCs via `remote { client.postgrest.rpc(...) }`).
   - `ui/screens/`: `XxxViewModel` (immutable `XxxUiState` as a StateFlow, effects as a Channel, events as methods),
     `XxxScreen(viewModel, …callbacks)` (collects state and effects) and `XxxContent(state, …)` (stateless + `@Preview`).
   - `ui/components/`: feature-private composables.
   - `ui/navigation/`: an `@Serializable` route + `fun NavGraphBuilder.xxxScreen(callbacks…)` using `koinViewModel()`.
4. **DI**: register the ViewModel, use cases and repositories in `composeApp/.../di/AppModule.kt` only
   (`viewModelOf(::XxxViewModel)`, or `viewModel { (p: P) -> XxxViewModel(get(), p) }` for route params). Use
   `single { X() }` for classes with defaulted constructor params.
5. **Navigation**: call the builder in `composeApp/.../ui/navigation/AppNavHost.kt` and wire cross-feature
   navigation through callbacks there.
6. **Strings**: add them via the `spec-copy-sync` skill (`core/designsystem/strings.py`).
7. **Tests** in `src/commonTest/kotlin/...`: ViewModel tests with `runViewModelTest {}` and fakes from `core:testing`
   (add new fakes there if other features will need them). Cover the spec rules the feature implements.
8. **Verify**: `./gradlew :feature:<name>:testDebugUnitTest ktlintCheck :composeApp:testDebugUnitTest`.
   `AppModuleTest` checks the Koin graph. Then update `docs/PLAN.md` and commit as `feat(<name>): …`.
