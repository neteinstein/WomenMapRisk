# CI/CD

## Topology

`.github/workflows/ci.yml` runs on every pull request, on pushes to `main` and on manual dispatch. All jobs run
**in parallel**, and each one is an independent signal:

| Job | Runner | What it proves | Command(s) |
|---|---|---|---|
| `build-android` | ubuntu | Debug build + **R8-minified release** compiles and shrinks | `:androidApp:assembleDebug :androidApp:assembleRelease` (uploads APKs + `mapping.txt`) |
| `unit-tests` | ubuntu | All `commonTest` suites on the JVM, plus coverage | `testDebugUnitTest koverXmlReport koverHtmlReport`. A coverage table goes to the job summary; reports are uploaded as artifacts. |
| `lint` | ubuntu | Style, Android lint, generated strings in sync | `strings.py && git diff --exit-code`, `ktlintCheck :androidApp:lintDebug` |
| `build-ios` | macos | Hand-authored Xcode project + Kotlin framework embed | `xcodebuild … -sdk iphonesimulator CODE_SIGNING_ALLOWED=NO build` |
| `build-web` | ubuntu | Kotlin/JS production bundle | `:composeApp:jsBrowserDistribution` (uploads `web-dist`) |
| `supabase-db` | ubuntu | Migrations, seed and pgTAP on the **real** Supabase stack | `supabase db start && supabase db reset && supabase test db` |

Every job first runs `scripts/ci/prepare-placeholders.sh`, which copies the committed `local.properties.ci` and
`androidApp/google-services.json.ci` into place. **These are placeholders for build validation only.** They
contain no credentials, and the app built from them runs in demo mode.

Exception: when the `GOOGLE_SERVICES_JSON` secret is set, the `build-android` job decodes it into
`androidApp/google-services.json` instead, on pushes to `main` and manual dispatch only. PR builds keep using the
placeholder, so forks and PRs never need the secret. Create it with
`base64 -i androidApp/google-services.json | pbcopy` (macOS) or `base64 -w0 androidApp/google-services.json`.

`.github/workflows/deploy-web.yml` runs on pushes to `main`: it builds the web bundle with the Supabase secrets
and publishes it to **GitHub Pages**.

Coverage is currently report-only (no failing threshold). Baseline on 2026-10-05: 63% lines and 38% branches
across shared modules; Compose UI is excluded.

## Secrets and variables

| Name | Kind | Used by | Purpose |
|---|---|---|---|
| `SUPABASE_URL` | secret | deploy-web (and future release jobs) | Supabase project URL (`https://<ref>.supabase.co`) |
| `SUPABASE_ANON_KEY` | secret | deploy-web | Public anon key. RLS + RPC authorisation protect the data, but keep it out of git anyway. |
| `WEB_APP_URL` | variable | deploy-web | Public web URL, used in invite links (`?invite=CODE`) |
| `GOOGLE_SERVICES_JSON` | secret (base64) | ci `build-android` (main/dispatch), future Android release job | Real Firebase config (both `…android` and `…android.debug` app ids) |
| `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | secrets | future Android release job | Writes `keystore.properties` + the keystore. `androidApp/build.gradle.kts` picks them up automatically. |
| `CRASHLYTICS_UPLOAD_MAPPING=true` | env | future Android release job | Uploads R8 mapping files to Crashlytics |
| `SUPABASE_ACCESS_TOKEN`, `SUPABASE_DB_PASSWORD`, `SUPABASE_PROJECT_REF` | secrets | future DB deploy job | `supabase link && supabase db push` |

Build-time config (`SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`, `WEB_APP_URL`) is read from env
vars first, then from `local.properties`, by `:core:data:generateAppConfig`.

## Signing and release status per platform

| Platform | Status | Next steps |
|---|---|---|
| Android | CI builds an **R8-minified release signed with the debug key** (validation only). Release signing is wired but needs the keystore secrets. | Create an upload keystore; add the secrets; add a `release-android.yml` that builds an AAB and uploads it to Play (internal track). |
| iOS | CI builds **unsigned for the simulator** only. Crashlytics on iOS ⏸. | Set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig` (or via CI); add a signing certificate and provisioning profile (App Store Connect API key); archive + TestFlight. |
| Web | Deployed to GitHub Pages by `deploy-web.yml`. It runs in demo mode until the Supabase secrets are set. | Enable Pages (Settings → Pages → Source: GitHub Actions); add the secrets. |
| Database | Migrations are tested in CI. **Not deployed automatically.** | `supabase link --project-ref …` then `supabase db push`, manually or via a protected workflow. Configure auth redirect URLs (`womenriskmap://login-callback`, the web URL) and the Google provider in the dashboard. |

## Operational notes

- **Supabase free tier pauses projects after 7 days without traffic.** Restore from the dashboard, or keep the
  project active (a scheduled ping is acceptable for a pilot).
- The free tier has no backups beyond daily snapshots of limited retention. Export before risky migrations.
- Photon and OpenFreeMap are free, fair-use services. If traffic grows, self-host Photon or move to a keyed
  provider; this only touches `core:data/geocoding` and `core:map`.
