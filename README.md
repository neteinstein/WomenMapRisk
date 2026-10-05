# Women Risk Map

A safety map **made by women, for women travelling alone**. Women anonymously mark streets and places where
they felt unsafe; others see coloured zones (green / yellow "!" / red "×") and know what to avoid.

Invite-only · Porto pilot · Português + English · Android, iOS and Web from one Kotlin Multiplatform codebase.

- **Spec:** [`docs/spec/especificacao-funcional-v1.md`](docs/spec/especificacao-funcional-v1.md) (+ [decisions](docs/spec/decisions.md), [invites addendum](docs/spec/addendum-invites.md))
- **Plan and progress tracker:** [`docs/PLAN.md`](docs/PLAN.md)
- **Architecture, conventions and gotchas:** [`AGENTS.md`](AGENTS.md)
- **CI/CD, secrets and signing:** [`CI_CD.md`](CI_CD.md)

## Quick start

```bash
cp local.properties.ci local.properties          # then add sdk.dir=… (and real Supabase values, optional)
cp androidApp/google-services.json.ci androidApp/google-services.json
./gradlew testDebugUnitTest                      # shared tests on the JVM
./gradlew :androidApp:installDebug               # Android
./gradlew :composeApp:jsBrowserDevelopmentRun    # Web on http://localhost:8080
open iosApp/iosApp.xcodeproj                     # iOS (run the iosApp scheme on a simulator)
```

Without a Supabase project the app runs in **demo mode** with sample Porto data. To run the backend locally:
`supabase start` (Docker), then set `SUPABASE_URL` and `SUPABASE_ANON_KEY` in `local.properties`. The seed
creates an invite code `SEGURA26` and dev users (see `supabase/seed.sql`).

## Stack

Kotlin 2.4 · Compose Multiplatform 1.12 · AGP 9 · Koin · Supabase (Postgres + RLS + RPC) · MapLibre + OpenFreeMap
· Photon · Firebase Crashlytics · GitHub Actions. All integrations are free at pilot scale.
