# AGENTS.md: Women Risk Map

Guidance for AI agents and humans working in this repo. **Read `docs/PLAN.md` first.** It is the living
tracker: what's done, what's deferred (⏸), and the deviations log. Update it at the end of every stage or
meaningful change, and commit the tick together with the work.

## What this is

Women Risk Map is a women's safety map. Women anonymously mark streets where they felt unsafe; other women see
coloured zones (green/yellow/red, each with a symbol). The app is invite-only, the pilot city is Porto, and it
ships in PT and EN. It runs on Android, iOS and Web from one Kotlin Multiplatform codebase.

## Authoritative sources: do not invent content

| File | Role |
|---|---|
| `docs/spec/especificacao-funcional-v1.md` | **Canonical functional spec (PT).** §4 *Ecrãs* gives the screens and their copy. §6 *Regras de negócio* gives the rules. §7 *Casos limite* gives the edge cases. §12 gives the acceptance criteria. |
| `docs/spec/addendum-invites.md` | Invite-only rules added by the product owner after the spec (3 consecutive or 5 distinct days, 5 invites, women-only reminder). |
| `docs/spec/decisions.md` | Resolutions of spec §9 and deliberate deviations (Porto, PT+EN, web user app, Supabase, …). |

- PT strings marked `[spec]` in `core/designsystem/strings.py` are **verbatim from the spec**. Do not "clean up",
  reword or "improve" them. EN strings are translations.
- Do **not** implement anything from spec §10 (*Fora da versão 1*) unless `decisions.md` says so.
- Section 2 is missing in the original spec. That is intentional; don't renumber.
- Open product questions (spec §9: moderation staffing, business model, legal review) are **not** engineering
  decisions. Don't decide them in code.
- The spec arrived as pasted text. No external origin link is needed, so nothing is unreachable from a sandbox.

## Stack

Kotlin 2.4 · Compose Multiplatform 1.12 (Material 3) · AGP 9.4 · Gradle 9.8 · MVVM with Coroutines/StateFlow ·
Koin 4.2 · Supabase (supabase-kt 3.8: Auth, PostgREST RPC, Realtime) · Ktor 3.6 · MapLibre Compose 0.19 +
OpenFreeMap tiles · Photon geocoding · KStore (offline cache) · Firebase Crashlytics (Android) · kotlin.test +
kotlinx-coroutines-test · Kover · ktlint.

Targets: `android`, `iosArm64`, `iosSimulatorArm64`, `js` (browser). **No iosX64** (removed upstream). The web
target is **Kotlin/JS, not wasmJs**, because maplibre-compose ships no wasm artifact.

SDK levels: compileSdk/targetSdk 37, minSdk 32. iOS deployment target 17.0.

### Free integrations (all usable at small scale with no payment method)

| Need | Chosen | Free alternatives considered |
|---|---|---|
| Auth + DB + realtime | **Supabase** free tier. All rules are enforced in SQL. Free projects pause after 7 days idle. | Firebase Spark (rules need Cloud Functions → Blaze plan); Appwrite Cloud; PocketBase (self-host) |
| Map tiles | **OpenFreeMap** (no key, no limits) | MapTiler free (key, 100k req/mo); Stadia free tier |
| Geocoding | **Photon** (komoot, fair use) | Nominatim (1 req/s); MapTiler geocoding |
| Crash reporting | **Firebase Crashlytics** (Android; iOS ⏸) | Sentry free tier |
| Web hosting | **GitHub Pages** (`deploy-web.yml`) | Cloudflare Pages, Netlify |
| CI | **GitHub Actions** | — |

## Module layout and why

```
androidApp/          Android application only (manifest, MainActivity, Application, R8). AGP 9 forbids applying
                     org.jetbrains.kotlin.multiplatform and com.android.application in one module.
composeApp/          KMP library (com.android.kotlin.multiplatform.library): App(), AppNavHost, the single Koin
                     module di/AppModule.kt, the bootstrap di/InitKoin.kt, the iOS MainViewController and the web entry.
core/domain/         Pure Kotlin: models, repository interfaces, use cases, business rules. NO Compose, NO platform.
core/data/           Supabase repositories, KStore cache, Photon, demo repository, expect/actual platform services.
core/designsystem/   Theme, components, icons, label mappers, ALL strings (EN + PT), platform UI helpers.
core/map/            The ONLY MapLibre-aware code (SafetyMap, PinPickerMap). Swapping the engine touches only this.
core/testing/        Shared fakes for commonTest.
feature/<name>/      One module per feature, each with domain/ data/ ui/{screens,components,navigation}:
                     onboarding · auth · map · report · saved · invites · profile · moderation
iosApp/              HAND-AUTHORED Xcode project (see gotchas).
supabase/            config.toml, migrations, seed.sql (Porto sample data), pgTAP tests.
scripts/             sql-harness (Docker-free SQL tests), ci helpers.
build-logic/         Convention plugins: womenriskmap.kmp.library / .kmp.compose / .kmp.feature.
```

Dependency rules: features depend on `core:*` only, **never on another feature**. Cross-feature navigation is
wired with callbacks in `composeApp/.../AppNavHost.kt`. Code defaults to `commonMain`. Use
`androidMain`/`iosMain`/`jsMain` and expect/actual only for what a platform genuinely can't share: location,
connectivity, file storage, share sheet, permission prompts, opening system settings, and the tz database on JS.

## Conventions

**MVVM**
- `XxxScreen(viewModel)` collects `state: StateFlow<XxxUiState>` with `collectAsStateWithLifecycle` and routes
  one-shot `effects` (a `Channel`, e.g. navigation, snackbars, share).
- `XxxContent(state, callbacks…)` is stateless and has an `@Preview`. No business logic in composables.
- ViewModels expose one immutable `data class` state. UI events are plain method calls (`vm.onTypeSelected(t)`).
- Business rules live in `core:domain/rules` (pure, unit-tested) or in feature `domain/` use cases.
  **The database is the source of truth**: client checks exist only for instant feedback.

**Koin**
- Register everything in `composeApp/src/commonMain/.../di/AppModule.kt`. Never register in feature modules or per platform.
- Route parameters reach a ViewModel via `koinViewModel { parametersOf(...) }` and `viewModel { (a: A) -> ... }`.
- `di/InitKoin.kt` only starts Koin; it holds no definitions.

**Strings**
- Edit `core/designsystem/strings.py`, then run `python3 core/designsystem/strings.py`. Never hand-edit the generated XML.
- Placeholders must be positional (`%1$s`, `%1$d`). Compose resources ignore bare `%d`.

**Testing**
- All shared tests live in `commonTest` with `kotlin.test` and run on the JVM via the Android host test.
- Use `runViewModelTest {}` and the fakes from `core:testing`.
- New domain/data/VM code arrives with tests. ~117 tests today.
- SQL changes arrive with pgTAP tests in `supabase/tests/`.

**Commits**
- Use Conventional Commits: `feat(scope): …`, `fix(scope): …`, `docs(scope): …`, `build: …`, `ci: …`, `test(scope): …`.
- Scopes are module names (`map`, `report`, `domain`, `backend`, `app`, …).

## Build and verify

```bash
./gradlew testDebugUnitTest                 # all shared tests (JVM)
./gradlew ktlintCheck                       # style (ktlintFormat to fix)
./gradlew :androidApp:assembleGithubDebug   # Android debug APK (flavours: github = self-updating, playstore)
./gradlew :androidApp:assembleRelease       # R8-minified release, both flavours (debug-signed without a keystore)
./gradlew :androidApp:lintGithubDebug :androidApp:lintPlaystoreDebug
./gradlew :composeApp:jsBrowserDistribution # web bundle → composeApp/build/dist/js/productionExecutable
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous   # web dev server on :8080 (.claude/launch.json "web")
./gradlew koverHtmlReport                   # coverage → build/reports/kover/html
scripts/sql-harness/run.sh                  # migrations + seed + pgTAP on a throwaway Postgres, no Docker
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath iosApp/build CODE_SIGNING_ALLOWED=NO build
```

**Local secrets:** copy `local.properties.ci` → `local.properties` (and add `sdk.dir`), and
`androidApp/google-services.json.ci` → `google-services.json`. Without a real Supabase project the app runs in
**demo mode**: read-only sample Porto data, everyone is a visitor.

### Sandboxed / cloud agents

Containers often **cannot build**: Google Maven, Gradle distributions, Kotlin/Native toolchains, npm and
Supabase endpoints may be blocked, and there is no emulator, Xcode or Docker. What you can still check offline:
- Read-only review: architecture rules (features independent, no Compose in `core:domain`), copy against the spec.
- `python3 core/designsystem/strings.py && git diff --exit-code` (strings parity).
- SQL logic, via `scripts/sql-harness/run.sh`, *if* `pip install pgserver` is reachable.
- `./gradlew testDebugUnitTest` **only** if the Gradle/Maven caches are already warm.

**CI on the PR is the real build signal.** State in the PR's test plan what you could not run and why.

## Gotchas (read before changing things)

1. **iOS Koin init:** Swift calls `InitKoinKt.doInitKoin()`. Kotlin/Native's Obj-C exporter renames top-level
   functions starting with `init` to `doInit…`. A no-arg `initKoin()` overload exists for Swift.
2. **Hand-authored `iosApp.xcodeproj`:** do not regenerate it with Xcode templates, CocoaPods or xcodegen.
   - Object IDs follow `A1B2C3D4E5F6` + counter. When adding a file, add PBXFileReference + PBXBuildFile + group child + build phase entry, all with new unique IDs.
   - The "Compile Kotlin Framework" Run Script calls `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`.
   - `ENABLE_USER_SCRIPT_SANDBOXING = NO`.
   - The simulator excludes x86_64 (no iosX64 target).
   - MapLibre needs only the linker flags already present (no SPM).
   - Check with `plutil -lint` + `xcodebuild -list`.
3. **Version catalog only:** every version, plugin and npm version lives in `gradle/libs.versions.toml`. Never inline one.
   Check KMP/CMP compatibility before bumping (maplibre-compose pins the CMP/Kotlin versions it was built with).
4. **AGP 9 KMP library and Android resources:** the compose convention sets `androidResources.enable = true`.
   Without it Compose resources aren't packaged and the app crashes at launch with `MissingResourceException`.
5. **JVM test task:** the KMP library plugin names it `testAndroidHostTest`. Every module also has a `testDebugUnitTest` alias.
   Tests needing the Android `Context` type go in `src/androidHostTest` (see `AppModuleTest`).
6. **Koin `singleOf(::X)` also injects defaulted constructor params.** Use `single { X() }` for those (e.g. `ZoneRiskCalculator`).
   `verify()` on the JVM doesn't catch this.
7. **Never store exact locations.** Use `LocationAnonymizer` (Kotlin) and `public.snap_to_zone` (SQL), and keep them
   numerically identical (shared vector `z45721_-7205`). The server re-snaps every write. Reports never carry a user id
   to clients; only `is_mine`.
8. **Server is the source of truth:** RLS denies all table access. Every read/write is a `SECURITY DEFINER` RPC in
   `supabase/migrations/*_api.sql`. Error codes raised there (`daily_report_limit`, …) are mapped in
   `core/data/.../ErrorMapping.kt`; keep both lists in sync. New functions need explicit `grant execute`.
9. **Kotlin/JS:**
   - kotlinx-datetime needs `@js-joda/timezone`, loaded by `loadTimeZoneDatabase()`.
   - After npm dependency changes run `./gradlew kotlinUpgradeYarnLock` and commit `kotlin-js-store/yarn.lock`.
   - Web `main()` must call `installMapLibreCompose()` inside `onWasmReady` before `ComposeViewport`.
10. **Don't run Gradle while the `--continuous` web dev server is running.** Concurrent builds corrupt the JS outputs
    and the page breaks in confusing ways. Stop the server, `./gradlew --stop`, then rebuild.
11. **Map engine:** only `core:map` imports MapLibre.
    - Declared GeoJSON sources are immutable; `SafetyMap` re-keys them when zones change.
    - Viewport changes come from `snapshotFlow { isCameraMoving }` (`MapEvent.CameraMoveEnded` doesn't fire on web).
    - Keep the OSM attribution visible (license).
12. **Placeholders vs secrets:** `*.ci` files are committed placeholders for CI build validation. The real
    `google-services.json`, `local.properties`, keystores and `.env` are gitignored. The Firebase project must
    register both `com.womenriskmap.android` and `com.womenriskmap.android.debug`.
13. **Language** comes from the OS per-app language setting (Android 13+ `generateLocaleConfig`, iOS
    `CFBundleLocalizations`, browser on web). There is no in-app language switch.
14. **Time-based ViewModel loops** (polling) take an injectable interval. Pass `null` in tests, or `runTest` never goes idle.
15. **Android flavours (`distribution`):** `github` (APK on GitHub Releases, self-updates from Settings → App updates)
    and `playstore` (Play handles updates). Both share the applicationId. Everything self-update-specific in the
    APK comes from `androidApp/src/github/AndroidManifest.xml`; `PlatformAppInstaller` reads its
    `com.womenriskmap.self_update` meta-data, so the shared code and the single `AppModule` need no flavour checks.
    `GitHubAppUpdater` expects release tags `android-v<version>` and an asset ending `-github.apk`, as produced by
    `release-android.yml`. Change both together. Only `playstore` is ever uploaded to Play.

## Skills (`.claude/skills/`)

- `spec-copy-sync`: bring UI copy in line with the spec (PT verbatim, EN translation, key parity).
- `add-feature-module`: scaffold a new `feature/x` correctly (convention plugin, settings, DI, nav, tests).
- `supabase-migration`: add or change a DB rule with migration + pgTAP + Kotlin mirror + error mapping.

See `CI_CD.md` for pipeline topology, secrets and signing status.
