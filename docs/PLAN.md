# Plan: Women Risk Map, a KMP safety map app (Android + iOS + Web)

> **Living tracker.** This file is the source of truth for what's done and what's next. Read it at the start of every session. When a stage's exit check passes, tick its box, add a dated note with the commit, and commit. Deferred items stay unchecked with `⏸ reason`.

## Naming
- **App display name (all platforms, PT and EN):** "Women Risk Map". This covers `app_name` in strings, the iOS `CFBundleDisplayName`, the web `<title>` and the PWA manifest.
- **Package / applicationId / iOS bundle id:** `com.womenriskmap` (`com.womenriskmap.android` for the app id, `com.womenriskmap.ios` for the bundle).
- **Code names:**
  - Gradle `rootProject.name = "WomenRiskMap"`
  - Application class `WomenRiskMapApp`
  - Theme `WomenRiskMapTheme`
  - Kotlin/Native framework name `ComposeApp`
- The repo directory stays `WomenMapRisk`. Only the app is named Women Risk Map.

## Context
The repo is empty (only README.md). We are building, from scratch, the V1/MVP of the women's safety map described in the functional spec (PT, v1.0). Women anonymously report places where they felt unsafe; others see coloured zones on a map. Stack: Kotlin Multiplatform + Compose Multiplatform, MVVM, Koin, clean architecture, one module per feature, full CI/CD, and agent docs.

**Decisions from the user:**
- **Backend:** Supabase (free tier) for auth and data. Firebase is used for extras only: Crashlytics, with analytics off for privacy.
- **Web:** the full user app, plus a moderation panel gated by the moderator role.
- **Pilot city:** Porto area. Women are verified by trust: a self-declaration when accepting the ToS, plus moderation afterwards.
- **Maps:** MapLibre Compose with free OpenFreeMap tiles; search uses Photon (komoot), with Nominatim as fallback. No API keys.
- **Languages:** PT (spec copy kept verbatim) and EN.
- **Invite-only registration.** This is a user addition on top of the spec. See the next section.

## Invite-only registration (user requirement, not in the original spec)
**Rules:**
- Only registered users with an *enabled* account can invite: email confirmed, invite redeemed, and status `active` (not blocked).
- Each user may issue up to **5 invites** in total. Revoking an unused invite gives the slot back.
- Invites unlock after **3 consecutive days** of usage **or 5 distinct days** of usage in total (intermittent).
- Moderators/admins are exempt from both the unlock rule and the limit. They seed the first users.
- **Usage day:** a distinct calendar day (Europe/Lisbon) on which a signed-in user opened the app. Only `(user_id, day)` is stored: no times and no locations. It is deleted along with the account.

**Women-only reminder:**
- Before generating or sharing an invite, the user sees a confirmation step and must tick it: "Women Risk Map é só para mulheres. Convida apenas mulheres em quem confias." (EN equivalent.)
- The invitee sees the same reminder on the sign-up screen, next to the ToS self-declaration.

**Visitors:** "Explorar sem conta" stays as in the spec. Invites only gate *registration*, which is what reporting, confirming and saving need.

**Flow:**
- Profile → "Convidar" opens one of two screens:
  - Locked: shows progress, e.g. "2/3 dias seguidos · 3/5 dias".
  - Unlocked: shows "x/5 convites usados", a list of invites (pending, used, expired) with revoke, and a "Criar convite" button that opens the share sheet. The share carries the code and a link `https://<web-host>/?invite=CODE` (the web app pre-fills it; mobile deep-link handling is a follow-up).
- **Codes:** 8-character, unambiguous alphabet, single use, expire after 30 days.

**Sign-up (Screen 2):**
- The sign-up screen has a required "Código de convite" field.
- **Email sign-up:** `validate_invite(code)` is checked first. The code goes into the user metadata, and `redeem_invite` runs after the email is confirmed and on first login.
- **Google sign-in:** the account is created with status `pending_invite`. The app then routes to an "Introduz o teu código de convite" gate until `redeem_invite(code)` succeeds.
- RLS only allows `active` profiles to write. A `pending_invite` user is effectively a visitor.

**Backend:**
- **Tables:**
  - `invites` (code, inviter_id, created_at, expires_at, redeemed_by, redeemed_at, revoked_at)
  - `usage_days` (user_id, day; PK on both)
- **New `profiles` columns:** `invited_by` (visible only to moderators, so abuse can be traced along the invite tree) and the status value `pending_invite`.
- **RPCs:** `touch_usage()` (idempotent, called on app start or resume when signed in), `invite_status()` (unlocked?, streak, distinct days, used/remaining), `create_invite()`, `revoke_invite(id)`, `validate_invite(code)`, `redeem_invite(code)`. Eligibility and limits are enforced in SQL. `delete_my_account()` also removes usage_days and unredeemed invites.
- **Moderation panel:** shows the inviter chain for a blocked account and can revoke a user's pending invites.

**Domain mirror (tested in commonTest):**
- `InviteEligibility.evaluate(usageDays, today, issuedCount, isModerator)` returns Locked(streak, distinct) or Unlocked(remaining). It covers streaks across month and year boundaries, gaps, duplicate days, the 5-invite cap, and the moderator exemption.
- `InviteCode` checks format and normalisation (case-insensitive; trims and strips spaces and dashes).

**Toolchain found locally:** JDK 17, Xcode 27, and Android SDK platforms up to android-37.1.

## Versions and SDK
- Before writing `gradle/libs.versions.toml`, check the latest stable version of each library (Maven Central or GitHub releases) and confirm it works with KMP and CMP:
  - Kotlin, Compose Multiplatform, AGP 9.x, Koin (+ koin-compose-viewmodel)
  - supabase-kt 3.x (auth, postgrest, realtime, compose-auth), Ktor 3.x, kotlinx-serialization, coroutines, datetime
  - androidx lifecycle-viewmodel / navigation-compose (JetBrains CMP artifacts), maplibre-compose
  - KStore (offline cache), BuildKonfig, Kover, ktlint/detekt, Firebase BoM, google-services and crashlytics plugins
- All versions, plugins and bundles live in the catalog. Never inline a version in a build file.
- `compileSdk`/`targetSdk` = 37 (confirm Android 17 is stable and supported by the chosen AGP; otherwise 36). `minSdk` = 32 (5 below).
- KMP targets: `androidLibrary`, `iosArm64`, `iosSimulatorArm64`, `wasmJs { browser() }`. No `iosX64`.
- MapLibre on Web is **alpha**. The map sits behind a single `ui/components/SafetyMap` composable so a fallback is easy if wasm breaks.

## Module layout
```
build-logic/convention/   # convention plugins: kmp-library, kmp-feature (compose+koin+tests), android-app
androidApp/               # com.android.application: manifest, MainActivity (enableEdgeToEdge), WomenRiskMapApp (Koin + Firebase init), proguard-rules.pro
composeApp/               # com.android.kotlin.multiplatform.library: App(), ui/navigation, di/AppModule.kt, di/InitKoin.kt,
                          #   iosMain MainViewController, wasmJsMain main.kt + index.html
core/domain/              # pure KMP (no Compose): models, repo interfaces, shared use cases, rules (ZoneRiskCalculator, LocationAnonymizer, ReportPolicy)
core/data/                # Supabase client, KStore cache, Photon geocoder (Ktor), connectivity, expect/actual LocationProvider
core/map/                 # SafetyMap + PinPickerMap: the ONLY MapLibre-aware code (engine swappable)
core/testing/             # shared fakes for commonTest
core/designsystem/        # ui/theme (M3 light/dark, dynamic colour on Android 12+), shared components, compose resources (strings EN + PT)
feature/onboarding/       # Screen 1 Welcome
feature/auth/             # Screen 2 Sign-up/login (email + Google via compose-auth), errors
feature/map/              # Screens 3, 4, 6: map, zone sheet, filters, empty/offline states
feature/report/           # Screen 5 + post-aggression support/emergency contacts; edit/delete within 24 h
feature/saved/            # Screen 7: saved zones, swipe to delete
feature/invites/          # invite status, progress, create/share/revoke, women-only reminder; invite-code gate for pending_invite users
feature/profile/          # Screens 8 + 9: profile, my reports, settings and privacy, data export, account deletion
feature/moderation/       # Screen 10: queue, approve/remove/block with reason, counters (moderator role only)
iosApp/                   # hand-authored iosApp.xcodeproj + Swift sources
supabase/                 # config.toml, migrations/*.sql, seed.sql (Porto sample data), tests (pgTAP)
docs/spec/                # authoritative spec
```
- Each feature module uses `domain/`, `data/` and `ui/{screens,components,navigation}` packages. Theme lives only in `core/designsystem`.
- Features depend on core modules only, never on each other.
- `composeApp` aggregates the features and owns navigation, using a type-safe `@Serializable` route graph and a bottom bar with Mapa · Guardados · Perfil.
- **DI:** a single `composeApp/.../di/AppModule.kt` registers every repository, use case and ViewModel (`viewModelOf(::MapViewModel)` etc.).
- **Bootstrap:** `di/InitKoin.kt` holds `fun initKoin(platform: KoinAppDeclaration = {})`. Android calls it from `WomenRiskMapApp`, iOS from `iOSApp.swift` via `InitKoinKt.doInitKoin()`, and wasm from `main()`.
- **Platform-only code (expect/actual):**
  - `LocationProvider`: FusedLocation on Android, CLLocationManager on iOS, `navigator.geolocation` on web.
  - KStore file vs. localStorage storage.
  - Platform `openUrl`/dialer for emergency numbers. Use `LocalUriHandler` where it suffices.

## MVVM conventions
- `XxxScreen(vm = koinViewModel())` collects `StateFlow<XxxUiState>` with `collectAsStateWithLifecycle`.
- `XxxContent(state, onEvent…)` is stateless, with an `@Preview`.
- ViewModels expose an immutable `data class` state and public methods for events. One-shot effects go through a `Channel`.

## Domain rules (commonMain, unit-tested)
- **`ZoneRiskCalculator`:** counts only published reports from the last 12 months. Each report gets a weight from recency decay and confirmations (`1 + 0.25·confirmations` × recency factor). The score maps to GREEN (0) / YELLOW / RED (≥3). Thresholds sit in a `RiskThresholds` config ("to be defined with Porto data").
- **Colour-blind support:** each risk level carries an icon/shape (✓ circle / ! triangle / ✕ octagon) and a legend.
- **`LocationAnonymizer`:** snaps coordinates to a ~100 m grid (≈50 m radius). Zones are geohash-7 cells.
- **The server also snaps**, so the exact point is never stored or shown (acceptance criterion).
- **`ReportPolicy`:** 5/day limit, same-place-same-day duplicate check, 300-character description, 24 h edit window, can't confirm own report, confirm once. The client checks these for UX; the DB enforces them.
- **`DescriptionGuard`:** warns about likely plates or names (a heuristic regex). Advisory only.
- **`EmergencyContacts`** by country. PT: 112, APAV 116 006, SOS Violência Doméstica 800 202 148. Flag the list as "verify before launch".

## Supabase backend (`supabase/migrations`)
**Tables:**
- `profiles` (email via auth, pseudonym, country, created_at, status active/blocked, role user/moderator, `declared_woman` + ToS timestamp)
- `reports` (geohash, snapped lat/lng, type enum, occurred_bucket, day_period, description ≤300, establishment bool, status pending/published/hidden/removed, counts, created_at, user_id nullable)
- `confirmations` (unique report+user)
- `flags` (unique report+user, reason)
- `saved_zones`
- `moderation_actions` (moderator, target, action, reason)

**Row-level security, triggers and RPCs:**
- Only confirmed-email, active users may insert.
- **Triggers:**
  - snap the location
  - enforce 5/day and same-place same-day rules
  - set `pending` when `establishment`
  - hide at ≥3 flags
  - maintain the counters
  - block self-confirmation
  - allow update/delete only within 24 h
- **Public view `public_reports`:** exposes no `user_id`, only published reports from the last 12 months.
- **RPCs:**
  - `zones_in_bbox(bbox, filters)` returns per-cell aggregates
  - `export_my_data()` returns JSON
  - `delete_my_account()` removes the profile, saved zones, confirmations and flags, sets `reports.user_id = null`, then deletes the auth user
  - moderation RPCs check the moderator role
- **Realtime:** published inserts on `reports` drive the map refresh (<1 min), with 30 s polling as fallback.
- **Free-tier caveat:** free projects pause after 7 days without traffic. Documented in CI_CD.md.

## UX, polish and platform guidelines
- Material 3 with a custom brand palette, light and dark.
- Edge-to-edge on all platforms: `enableEdgeToEdge`, insets via `WindowInsets.safeDrawing`.
- Animated: bottom-sheet zone detail, `AnimatedContent` state transitions, shared-element-style FAB→report sheet, animated zone fill, a confetti-free confirmation animation (a heart pulse), and haptics on confirm.
- **Report flow in ≤6 taps:** Reportar → type chip → when (default "agora") → period (pre-selected from the clock) → Enviar. The pin is pre-set to the current location.
- **Offline:** shows the cached zones with a "Dados desatualizados" banner and disables Reportar.
- **Location denied:** the map opens on Porto and search still works.
- **Permission rationale screen** (Screen 9). Location history is off by default.
- **Store requirements:** the app icon is an adaptive themed icon, plus a splash screen (core-splashscreen / iOS LaunchScreen). An account-deletion path is in the app, as both stores require. The iOS privacy manifest `PrivacyInfo.xcprivacy` is included. The Android 13+ per-app language setting comes via `locales_config.xml`.
- **Strings:** CMP resources, with `values/strings.xml` (EN) and `values-pt/strings.xml` (PT copy taken verbatim from the spec).

## Android build and R8
- `androidApp` release build: `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-android-optimize.txt` + `proguard-rules.pro`.
- Keep rules: kotlinx.serialization `@Serializable` classes and nav routes, Ktor/Supabase (per their docs), Koin reflection-free (no rules needed), Crashlytics mapping upload.
- Library modules ship `consumer-rules.pro`.
- CI runs `:androidApp:assembleRelease` (debug-signed when no keystore secret is present) to prove R8 passes.

## Secrets
- **Gitignored:** `local.properties` (`SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`), `androidApp/google-services.json`, `iosApp/iosApp/GoogleService-Info.plist`, `*.jks`/`*.keystore`, `.env`.
- **Committed placeholders:** `google-services.json.ci`, `GoogleService-Info.plist.ci`, `local.properties.ci`.
- BuildKonfig generates `AppConfig` in commonMain from `local.properties` or env vars.
- CI copies the `*.ci` files into place. Real values are GitHub secrets, used only on the release/main workflow.
  - 2026-10-06: `build-android` decodes the `GOOGLE_SERVICES_JSON` secret (base64) into `androidApp/google-services.json` on main/dispatch; PRs still use the placeholder.

## iOS (hand-authored `iosApp/iosApp.xcodeproj/project.pbxproj`)
- One app target. Run Script phase: `cd "$SRCROOT/.." && ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`.
- `FRAMEWORK_SEARCH_PATHS` and `OTHER_LDFLAGS = -framework ComposeApp`. Set `ENABLE_USER_SCRIPT_SANDBOXING = NO`.
- Sources:
  - `iOSApp.swift`: `init() { InitKoinKt.doInitKoin() }`
  - `ContentView.swift`: a `UIViewControllerRepresentable` around `MainViewControllerKt.MainViewController()`, `.ignoresSafeArea()`
  - `Info.plist` (location usage strings, PT/EN `CFBundleLocalizations`)
  - Assets, LaunchScreen, PrivacyInfo
- Firebase Crashlytics on iOS goes in via an SPM package reference in the pbxproj. If the hand-authored SPM entry turns out fragile, Crashlytics on iOS is deferred and documented as a gotcha.

## Tests (commonTest, kotlin.test, run on JVM via `testAndroidHostTest`/`testDebugUnitTest`)
- **Domain:** ZoneRiskCalculator, LocationAnonymizer, ReportPolicy, DescriptionGuard, InviteEligibility, InviteCode, filter logic, EmergencyContacts.
- **Data:** repositories against fake data sources / Ktor `MockEngine` (Photon parsing, Supabase DTO mapping), cache fallback.
- **ViewModels:** `kotlinx-coroutines-test` + fakes (MapViewModel offline state, ReportViewModel validation and success/aggression path, AuthViewModel error mapping).
- **Coverage:** Kover reports aggregated in the root project.
- **SQL:** pgTAP tests in `supabase/tests`, run in CI with `supabase start` (Docker) on a separate job.

## CI/CD (`.github/workflows/ci.yml`, triggered on PRs and main)
These jobs run in parallel:
1. `build-android`: `:androidApp:assembleDebug assembleRelease` (R8)
2. `unit-tests`: `allTests`-JVM + `koverXmlReport`, upload the artifact and a coverage summary comment
3. `lint`: ktlint/detekt + Android lint
4. `build-ios` (macos runner): `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination generic… CODE_SIGNING_ALLOWED=NO`
5. `build-web`: `:composeApp:wasmJsBrowserDistribution`
6. `supabase-db`: migrations + pgTAP

Setup shared by all jobs: Gradle cache via `gradle/actions/setup-gradle`, JDK 17, and the `*.ci` copy step.

Optional `deploy-web.yml` on main publishes the wasm build to GitHub Pages (free).

## Docs and agent guidance
- **`AGENTS.md`:** stack, module layout and why (AGP 9 split, feature isolation), MVVM/Koin/testing conventions, build/verify commands, "gotchas".
  - Gotchas to cover: `doInitKoin`, no iosX64, versions only in the catalog, hand-authored pbxproj (edit by hand and keep UUIDs unique), no Compose in domain, `*.ci` placeholders, the MapLibre web alpha, the server as source of truth for rules, never storing exact locations.
  - Authoritative-content section: `docs/spec/especificacao-funcional-v1.md` is canonical. `docs/spec/addendum-invites.md` records the invite-only rules the user added after the spec (3 consecutive or 5 distinct days, 5 invites, women-only reminder). Sections 4 (Ecrãs), 6 (Regras de negócio) and 7 (Casos limite) drive the copy and rules. Don't "clean up" PT copy or invent features from section 10 (out of V1). Includes the decision log for section 9 (Porto, trust + ToS, PT+EN).
  - Note that sandboxed/cloud agents may be unable to reach `dl.google.com`/Maven/Supabase. Offline they can still run `./gradlew help` only if the cache is warm, plus read-only review and SQL review. CI on the PR is the real signal.
- **`CLAUDE.md`:** `@AGENTS.md`.
- **`CI_CD.md`:** the jobs, required secrets, and signing status per platform:
  - Android: debug-signed, release keystore TODO
  - iOS: unsigned simulator build, signing TODO
  - Web: Pages
  - Plus the Supabase pause caveat.
- **`.claude/skills/`:**
  - `spec-copy-sync`: read `docs/spec`, update PT strings verbatim and EN translations, check every key exists in both files
  - `add-feature-module`: scaffold `feature/x` using the convention plugin, register it in settings + AppModule + nav, add tests
  - `supabase-migration`: write a migration + pgTAP test + update the domain rule mirror
- **`.github/pull_request_template.md`:** Summary / Changes / Test plan (must state what couldn't run and why) / Risk.
- **Conventional Commits:** documented in AGENTS.md. Implementation work is committed in logical `feat(scope):`/`build:`/`ci:`/`docs:` commits on the current branch.
- **`.gitignore`:** Gradle, IDE, Kotlin/Native, Xcode (xcuserdata, DerivedData), `local.properties`, the secrets above, `.kotlin/`, `build/`, `.DS_Store`.

## Deviations log (decided during implementation)
- **2026-10-04: web target is Kotlin/JS (`js { browser() }`), not wasmJs.** maplibre-compose 0.19.0 publishes `js` but no `wasmJs` artifact. Supabase, Koin, KStore and Compose all support `js`. Revisit once maplibre-compose ships wasm.
- **2026-10-04: ktlint only, no detekt.** detekt 1.23.8 is built against Kotlin 2.0 and breaks with Kotlin 2.4. detekt 2.x is still alpha. Lint = ktlint-gradle + Android lint.
- **2026-10-04: no BuildKonfig.** A small Gradle task in `core:data` generates `AppConfig.kt` from `local.properties` or env vars. It is simpler and has no plugin compatibility risk with AGP 9's KMP library plugin.
- **2026-10-04: new `core:testing` module** with shared fakes for feature commonTests.
- **2026-10-04: feature modules also depend on `core:data`**, because features own their `data/` repository implementations, built on the shared Supabase client.
- **2026-10-04: JVM test task.** With the AGP 9 KMP library plugin the task is `testAndroidHostTest`. Each shared module also registers a `testDebugUnitTest` alias, so `./gradlew testDebugUnitTest` runs all commonTests.
- **2026-10-04: all strings live in `core:designsystem`**, with a public `Res` class. This keeps one PT/EN pair of files, so translation completeness is checkable in one place.
- **2026-10-04: maplibre iOS integration** needs only linker flags (native FFI is bundled), with no SPM package. Android uses the OpenGL runtime, because Vulkan is unreliable on emulators.

- **2026-10-04: fixed brand palette instead of Android dynamic colour.** Brand and risk colours stay identical across platforms.
- **2026-10-04: language is chosen with the OS per-app language setting** (Android 13+ `locales_config`, iOS per-app language, browser language on web). There is no in-app language preference. Settings links to the system page.
- **2026-10-04: strings are generated from one table.** `core/designsystem/strings.py` writes both `values/strings.xml` (EN) and `values-pt/strings.xml` (PT), which guarantees key parity. Spec-verbatim lines are marked `[spec]`.
- **2026-10-04: Google sign-in uses Supabase OAuth** (browser + `womenriskmap://login-callback` deep link) instead of compose-auth native one-tap. This keeps auth out of Composables and works on all three platforms. Native one-tap is a follow-up.
- **2026-10-04: realtime uses DB broadcast** (`realtime.send` to topic `reports-feed`, no personal data) instead of postgres_changes, which would leak row data under RLS.

- **2026-10-05: demo mode.** Without a configured Supabase project (`AppConfig.isConfigured == false`), `DemoReportRepository` serves read-only sample Porto reports, so the app can be explored (web preview, reviews) with no backend.
- **2026-10-05: Kotlin/JS timezone data.** kotlinx-datetime on JS needs `@js-joda/timezone` (npm, version in the catalog), loaded via `loadTimeZoneDatabase()` expect/actual. `kotlin-js-store/yarn.lock` is committed; run `./gradlew kotlinUpgradeYarnLock` after npm dependency changes.
- **2026-10-05: map viewport detection** uses `snapshotFlow { isCameraMoving }` instead of `MapEvent.CameraMoveEnded`, which did not fire on web.
- **2026-10-05: Koin `singleOf(::X)` injects defaulted constructor params too.** Use `single { X() }` for classes with defaults (e.g. `ZoneRiskCalculator`). `verify()` on the JVM does not catch this.

- **2026-10-05: new `core:map` module.** It is the only module that touches MapLibre; `feature:map` and `feature:report` both use it, because features must not depend on each other.

## Progress tracking: `docs/PLAN.md` in the repo
The first action of implementation is to copy this whole plan into the repo as **`docs/PLAN.md`**. It becomes the living tracker.
- The stage list below goes into the file as GitHub task-list checkboxes (`- [ ]`).
- **When a stage is done**, meaning its exit check passed:
  - tick its boxes in `docs/PLAN.md`
  - add a one-line note under it (date, commit SHA, and anything skipped and why)
  - commit the tick together with the stage's work, e.g. `feat(map): zone sheet and filters` + `docs(plan): complete stage 7`
- **Unfinished or deferred items** stay unchecked with a `⏸ reason` note. They are never silently dropped.
- **Plan changes** are made in `docs/PLAN.md` first, then implemented.
- `AGENTS.md` points agents to `docs/PLAN.md` as the source of truth for what's done and what's next. It tells them to read it at the start of every session and update it at the end of each stage.

## Stages (each ends with a commit; tick in docs/PLAN.md when done)
- [x] **Stage 0: Repo bootstrap.**
  - `docs/PLAN.md` (this plan), `docs/spec/especificacao-funcional-v1.md` (spec verbatim), `docs/spec/addendum-invites.md`, `docs/spec/decisions.md`, `.gitignore`.
  - Exit: committed.
  - ✅ 2026-10-04: done (see the `docs(plan): complete stage 0` commit).
- [x] **Stage 1: Gradle skeleton.**
  - Verified version catalog, wrapper, settings, `build-logic` convention plugins, `*.ci` placeholders, ~~BuildKonfig~~ (see deviations).
  - Exit: `./gradlew help` green.
  - ✅ 2026-10-04: Gradle 9.8.0, Kotlin 2.4.20, CMP 1.12.1, AGP 9.4.1. `./gradlew help` and `:core:domain:testDebugUnitTest` are green.
- [x] **Stage 2: core:domain + tests.**
  - Models, repo interfaces, ZoneRiskCalculator, LocationAnonymizer, ReportPolicy, DescriptionGuard, InviteEligibility, InviteCode, EmergencyContacts.
  - Exit: domain tests green on JVM.
  - ✅ 2026-10-04: 47 tests green (`:core:domain:testDebugUnitTest`), ktlint clean. Zone grid is ~100 m cells, zone id `z{lat}_{lng}`, shared SQL vector `z45721_-7205`. The ktlint style is `intellij_idea`, since `ktlint_official` was too noisy.
- [x] **Stage 3: core:data + core:designsystem.**
  - Supabase client, KStore cache, Photon geocoder, LocationProvider expect/actual.
  - Theme (light/dark), shared components, EN/PT string resources.
  - Exit: data tests green (MockEngine/fakes).
  - ✅ 2026-10-04: core:data compiles for Android, iOS and JS. Photon and error-mapping tests are green. The designsystem has the theme, risk symbols, components, platform UI helpers, and 262 strings + 7 plurals in EN/PT, generated by `core/designsystem/strings.py`. `core:testing` holds the shared fakes. Supabase RPC contract: see `SupabaseReportRepository` and `ErrorMapping.kt`; implemented in Stage 5.
- [x] **Stage 4: App shells.**
  - composeApp: navigation, `di/AppModule.kt`, `di/InitKoin.kt`.
  - androidApp: edge-to-edge, splash, R8.
  - wasmJs entry and index.html.
  - Hand-authored iosApp.xcodeproj.
  - Exit: `assembleDebug`, `assembleRelease` (R8), ~~`wasmJsBrowserDistribution`~~ `jsBrowserDistribution` and the `xcodebuild` simulator build all succeed with a placeholder screen.
  - ✅ 2026-10-04: all four builds are green.
    - R8 mapping confirms app classes are obfuscated (e.g. `AppViewModel -> g8`).
    - iOS launched on the iPhone 17 Pro simulator: `InitKoinKt.doInitKoin()` works and the UI is edge-to-edge.
    - The Koin graph is verified by `AppModuleTest` (androidHostTest).
    - The simulator build excludes x86_64 (no iosX64 target).
    - Crashlytics on iOS ⏸: deferred, because the hand-authored SPM package entry is fragile. Android Crashlytics is wired.
- [x] **Stage 5: Supabase backend.**
  - Migrations (tables, RLS, triggers, RPCs including invites/usage), seed with Porto sample data, pgTAP tests.
  - Exit: `supabase test db` green, or ⏸ if Docker is unavailable locally.
  - ✅ 2026-10-05: 3 migrations (schema, rules/triggers, API), `seed.sql` (Porto) and 3 pgTAP files (37 assertions).
    - All green on real Postgres 16 via the new Docker-free `scripts/sql-harness/run.sh`, which emulates auth, pgcrypto, pgTAP and realtime.
    - The shared zone vector `z45721_-7205` matches Kotlin.
    - ⏸ `supabase test db` on the real Supabase stack has not run locally (no Docker); the CI `supabase-db` job covers it.
- [x] **Stage 6: feature:onboarding + feature:auth.**
  - Welcome screen, sign-up/login with invite code and women-only declaration, Google sign-in, invite-code gate for `pending_invite` users, error mapping.
  - Exit: VM tests green.
  - ✅ 2026-10-05: Auth, InviteGate and CheckEmail VM tests and the SignUpValidator tests are green.
    - App navigation reacts to session gates (welcome, invite gate, main).
    - The web `?invite=CODE` link pre-fills sign-up.
    - Welcome and sign-up were checked rendering on web (dark theme).
    - `.claude/launch.json` holds the `web` dev-server config.
- [x] **Stage 7: feature:map.**
  - Map centred on the user or Porto, coloured zones with symbols and legend, search, filters, zone bottom sheet (confirm/flag/save/report-here), empty and offline states, realtime refresh.
  - Exit: VM tests green, plus the map verified on web/Android.
  - ✅ 2026-10-05: 14 MapViewModel tests are green.
    - Verified on web (MapLibre JS, dark style): zones with "!" and "×" symbols, legend, zone sheet with the Photon street name, "location off → Porto" banner, bottom navigation.
    - Android map rendering will be verified with the polish pass (Stage 12).
- [x] **Stage 8: feature:report.**
  - Report form in ≤6 taps, description warning, confirmation message, aggression → emergency contacts and support, edit/delete within 24 h.
  - Exit: VM tests green.
  - ✅ 2026-10-05: 11 ReportViewModel tests are green.
    - Fastest path is 3 taps: Reportar → type → Enviar.
    - New `core:map` module (SafetyMap + PinPickerMap) shared by the map and report features.
    - The visitor prompt on "Reportar" was checked on web.
- [x] **Stage 9: feature:saved + feature:invites.**
  - Saved list with swipe to delete.
  - Invite progress (locked/unlocked), create/share/revoke, women-only reminder, `touch_usage` on app start.
  - Exit: VM tests green.
  - ✅ 2026-10-05: 3 SavedViewModel and 7 InvitesViewModel tests are green.
    - Saved zones show their current colour and support swipe to delete with undo. Tapping a zone opens the map focused on it.
    - Invites: progress bars, a required women-only confirmation, share text with a `?invite=` link, revoke.
    - `touch_usage` runs from AppViewModel (Stage 4).
- [x] **Stage 10: feature:profile.**
  - Profile, my reports, settings and privacy (location permission rationale, location history toggle), data export, account deletion, logout, PT/EN switch.
  - Exit: VM tests green.
  - ✅ 2026-10-05: 6 Profile/Settings VM tests are green.
    - Visitor profile and Settings were checked on web.
    - The PT/EN switch opens the OS per-app language page (see deviations).
    - `CountryField` moved to designsystem.
    - Gotcha found: running Gradle builds while the `--continuous` web dev server is running corrupts the JS outputs; stop the server first.
- [x] **Stage 11: feature:moderation.**
  - Queue (flagged reports and establishment reports), approve/remove/block with reason, counters, inviter chain, revoke invites.
  - Exit: VM tests green, verified on web.
  - ✅ 2026-10-05: 4 ModerationViewModel tests are green.
    - SQL side covered by `rules_test.sql` (queue, approve, action log).
    - ⏸ Visual check on web needs a configured Supabase project and a moderator account (seed: `mod@womenriskmap.local`); demo mode has no auth.
- [x] **Stage 12: Polish.**
  - Animations, haptics, adaptive/themed icon, launch screens, PrivacyInfo.xcprivacy, locales_config, accessibility pass (contrast, content descriptions, colour-blind symbols).
  - Exit: manual check on Android, iOS and web.
  - ✅ 2026-10-05: checked on the Android emulator (API 37), the iOS simulator (iPhone 17 Pro, iOS 27) and web.
    - Fixed a release blocker: Compose resources were not packaged on Android. AGP 9's KMP library needs `androidResources.enable = true`, now set in the compose convention plugin.
    - The legend wraps on narrow screens.
    - The OSM attribution is lifted above the FAB (license requirement).
    - Animations: welcome entrance, sheet and content transitions, confirm heart pulse plus haptic, animated banners and progress.
    - Accessibility: risk symbols carry content descriptions, checkbox rows use the full row as the touch target, and colour is never the only cue.
    - ⏸ Known limitation: map zones are not individually exposed to screen readers (MapLibre canvas). The zone sheet and Saved list are accessible.
- [x] **Stage 13: CI/CD.**
  - `ci.yml` parallel jobs (android build+R8, unit tests+Kover, lint, iOS, web, supabase-db), optional `deploy-web.yml`, PR template.
  - Exit: workflow YAML validated, and a PR opened so CI runs.
  - ✅ 2026-10-05: `ci.yml` has 6 parallel jobs (build-android with R8, unit-tests with Kover summary, lint with ktlint + Android lint + strings parity, build-ios, build-web, supabase-db). Also `deploy-web.yml` (GitHub Pages) and the PR template.
    - YAML parses. Every job's command was run locally and is green, except `supabase-db` (no Docker locally; the SQL harness equivalent is green).
    - PR opened on 2026-10-05, at the user's request.
- [x] **Stage 14: Docs and agent guidance.**
  - `AGENTS.md`, `CLAUDE.md`, `CI_CD.md`, `.claude/skills/*`, README refresh.
  - Exit: final verification below, all boxes ticked or ⏸ with reasons.
  - ✅ 2026-10-05: AGENTS.md (stack, layout, conventions, sandbox notes, 14 gotchas), CLAUDE.md → @AGENTS.md, CI_CD.md, README, and 3 skills (spec-copy-sync, add-feature-module, supabase-migration).
    - Final run green: 117 JVM tests, ktlint, Android lint, R8 release (470 app classes obfuscated), web bundle, iOS simulator build, SQL harness 37/37.

## Verification
- `./gradlew allTests koverHtmlReport` (JVM tests green, including commonTest).
- `./gradlew :androidApp:assembleRelease`: R8 succeeds. Inspect `mapping.txt` and check that the APK has obfuscated classes.
- `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`: open in the browser pane, check the map renders around Porto, plus light/dark and PT/EN.
- `xcodebuild … -sdk iphonesimulator` then launch in the iOS Simulator. Check edge-to-edge, that Koin init doesn't crash, and the map.
- Android emulator/device: walk through the report flow in ≤6 taps, the offline banner, and location-denied → Porto.
- `supabase start && supabase db reset && supabase test db` (if Docker is available): run the RLS/trigger tests.
- `ktlintCheck detekt lint` clean.
