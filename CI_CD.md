# CI/CD

## Topology

`.github/workflows/ci.yml` runs on every pull request, on pushes to `main` and on manual dispatch. All jobs run
**in parallel**, and each one is an independent signal:

| Job | Runner | What it proves | Command(s) |
|---|---|---|---|
| `build-android` | ubuntu | Debug build + **R8-minified release** of both flavours compiles, shrinks and is obfuscated | `:androidApp:assembleDebug :androidApp:assembleRelease`, `scripts/ci/verify-obfuscation.sh` (uploads APKs + mappings) |
| `unit-tests` | ubuntu | All `commonTest` suites on the JVM, plus coverage | `testDebugUnitTest koverXmlReport koverHtmlReport`. A coverage table goes to the job summary; reports are uploaded as artifacts. |
| `lint` | ubuntu | Style, Android lint, generated strings in sync | `strings.py && git diff --exit-code`, `ktlintCheck :androidApp:lintGithubDebug :androidApp:lintPlaystoreDebug` |
| `build-ios` | macos | Hand-authored Xcode project + Kotlin framework embed | `xcodebuild … -sdk iphonesimulator CODE_SIGNING_ALLOWED=NO build` |
| `build-web` | ubuntu | Kotlin/JS production bundle. **Disabled for now** (`if: false`). | `:composeApp:jsBrowserDistribution` (uploads `web-dist`) |
| `supabase-db` | ubuntu | Migrations, seed and pgTAP on the **real** Supabase stack | `supabase db start && supabase db reset && supabase test db` |

Every job first runs `scripts/ci/prepare-placeholders.sh`, which copies the committed `local.properties.ci` and
`androidApp/google-services.json.ci` into place. **These are placeholders for build validation only.** They
contain no credentials, and the app built from them runs in demo mode.

Exception: when the `GOOGLE_SERVICES_JSON` secret is set, the `build-android` job decodes it into
`androidApp/google-services.json` instead, on pushes to `main` and manual dispatch only. PR builds keep using the
placeholder, so forks and PRs never need the secret. Paste the file contents as-is, or its base64
(`base64 -i androidApp/google-services.json | pbcopy` on macOS, `base64 -w0 androidApp/google-services.json` on Linux).

`.github/workflows/release-android.yml` runs on pushes to `main` and on manual dispatch. It cuts an Android
release; see [Android release](#android-release) below.

`.github/workflows/deploy-web.yml` builds the web bundle with the Supabase secrets and publishes it to
**GitHub Pages**. Web is paused for now, so it runs on manual dispatch only (no push trigger).

Coverage is currently report-only (no failing threshold). Baseline on 2026-10-05: 63% lines and 38% branches
across shared modules; Compose UI is excluded.

## Secrets and variables

| Name | Kind | Used by | Purpose |
|---|---|---|---|
| `SUPABASE_URL` | secret | deploy-web, release-android | Supabase project URL (`https://<ref>.supabase.co`) |
| `SUPABASE_ANON_KEY` | secret | deploy-web, release-android | Public anon key. RLS + RPC authorisation protect the data, but keep it out of git anyway. |
| `GOOGLE_WEB_CLIENT_ID` | secret | release-android | Google OAuth web client id |
| `WEB_APP_URL` | variable | deploy-web, release-android | Public web URL, used in invite links (`?invite=CODE`) |
| `GOOGLE_SERVICES_JSON` | secret (raw JSON or base64) | ci `build-android` (main/dispatch), release-android (optional; placeholder + no Crashlytics without it) | Real Firebase config (both `…android` and `…android.debug` app ids) |
| `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | secrets | release-android (**required**) | Upload keystore. Passed to Gradle as `KEYSTORE_FILE`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD`; locally `keystore.properties` works too. |
| `ANDROID_PUBLISHER_CREDENTIALS` | secret | release-android (optional) | Raw JSON of a Play Console service account key. Without it the Play upload is skipped. |
| `PLAY_TRACK` | variable | release-android (optional) | Play track for the upload; defaults to `internal`. |
| `CRASHLYTICS_UPLOAD_MAPPING=true` | env | release-android | Set automatically when `GOOGLE_SERVICES_JSON` exists; uploads R8 mapping files to Crashlytics |
| `SUPABASE_ACCESS_TOKEN`, `SUPABASE_DB_PASSWORD`, `SUPABASE_PROJECT_REF` | secrets | future DB deploy job | `supabase link && supabase db push` |

Build-time config (`SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`, `WEB_APP_URL`) is read from env
vars first, then from `local.properties`, by `:core:data:generateAppConfig`.

## Signing and release status per platform

| Platform | Status | Next steps |
|---|---|---|
| Android | CI builds an **R8-minified release signed with the debug key** (validation only). `release-android.yml` builds the signed APK + AAB, publishes a GitHub Release and, optionally, uploads to Play. | Create an upload keystore and add the secrets; do the first Play upload by hand; then add `ANDROID_PUBLISHER_CREDENTIALS`. |
| iOS | CI builds **unsigned for the simulator** only. Crashlytics on iOS ⏸. | Set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig` (or via CI); add a signing certificate and provisioning profile (App Store Connect API key); archive + TestFlight. |
| Web | **Paused:** `deploy-web.yml` runs on manual dispatch only. When run, it deploys to GitHub Pages and runs in demo mode until the Supabase secrets are set. | Enable Pages (Settings → Pages → Source: GitHub Actions); add the secrets. |
| Database | Migrations are tested in CI. **Not deployed automatically.** | `supabase link --project-ref …` then `supabase db push`, manually or via a protected workflow. Configure auth redirect URLs (`womenriskmap://login-callback`, the web URL) and the Google provider in the dashboard. |

## Android release

`release-android.yml` (push to `main`, or Actions → Release Android → Run workflow), one job:

1. **Validate secrets.** Fails fast without the four `ANDROID_KEY*` secrets. Warns (doesn't fail) when
   `GOOGLE_SERVICES_JSON` or the Supabase secrets are missing; the release then ships the placeholder Firebase
   config and/or demo mode.
2. **ktlint + unit tests** on the exact commit being released (a squash/rebase merge is never built by `ci.yml`).
3. **Build** `:androidApp:assembleRelease :androidApp:bundlePlaystoreRelease`, signed with the upload key. Two flavours:
   `github` (self-updating APK, Settings → App updates) and `playstore` (APK + AAB for Play, no self-update).
   Version: `versionCode = run_number`, `versionName = 1.0.<run_number>` (`APP_VERSION_CODE`/`APP_VERSION_NAME`).
4. **Verify obfuscation** (`scripts/ci/verify-obfuscation.sh`): fails if R8 renamed no `com.womenriskmap.*` class
   in either flavour.
5. **GitHub Release** `android-v1.0.<n>` with `WomenRiskMap_version1_0_<n>-github.apk`, `-playstore.apk`,
   `-playstore.aab` and one `-mapping.txt` per flavour, plus SHA-1s in the notes. The installed `github` app finds
   updates by the `android-v` tag and the `-github.apk` suffix (`GitHubAppUpdater`); keep them in sync. Keep the mapping: R8 output differs per build, and stack traces need the exact one
   (`retrace <mapping.txt> <trace.txt>`).
6. **Play upload** (`:androidApp:publishPlaystoreReleaseBundle`, Gradle Play Publisher; never the `github` flavour) to `vars.PLAY_TRACK` or `internal`,
   only when `ANDROID_PUBLISHER_CREDENTIALS` is set. Nothing reaches production without a manual promotion.

**One-time setup**
- Upload keystore: `keytool -genkeypair -v -keystore upload.keystore -alias upload -keyalg RSA -keysize 4096 -validity 10000`,
  then `base64 -i upload.keystore | pbcopy` into `ANDROID_KEYSTORE_BASE64`. Back the keystore up outside the repo.
- Play Console: create the app, enrol in Play App Signing, and **upload the first `.aab` by hand** (from the
  GitHub Release). The API can't create an app's first release.
- Google Cloud: enable the Google Play Android Developer API, create a service account + JSON key, invite it in
  Play Console → Users and permissions with release rights for this app, and store the JSON as
  `ANDROID_PUBLISHER_CREDENTIALS`.

**Troubleshooting a 403 PERMISSION_DENIED** from the Play upload: the service account isn't invited to this app,
the first manual release hasn't happened, the Android Publisher API isn't enabled in the key's GCP project, or a
fresh permission grant is still propagating (can take up to a day). It is rarely the workflow.

## Operational notes

- **Supabase free tier pauses projects after 7 days without traffic.** Restore from the dashboard, or keep the
  project active (a scheduled ping is acceptable for a pilot).
- The free tier has no backups beyond daily snapshots of limited retention. Export before risky migrations.
- Photon and OpenFreeMap are free, fair-use services. If traffic grows, self-host Photon or move to a keyed
  provider; this only touches `core:data/geocoding` and `core:map`.
