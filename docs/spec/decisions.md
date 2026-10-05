# Product decisions (resolving spec §9 and deviations)

These were decided by the product owner on 2026-10-04 at kickoff. They override the spec where the two conflict.

| Topic | Spec says | Decision |
|---|---|---|
| App name | (none) | **Women Risk Map** (all platforms, PT and EN) |
| Woman verification (§9) | open | Trust + ToS self-declaration at sign-up, with moderation afterwards (blocking). No ID documents. Invite-only adds social trust (see `addendum-invites.md`). |
| Languages (§9) | PT only or PT+EN | **PT and EN from V1.** PT copy comes verbatim from the spec. |
| Pilot city (§9) | Lisboa or other | **Porto area.** The map defaults to Porto (41.1496, -8.6110). |
| Web (§10 lists "versão web para utilizadoras" as out of V1) | moderation panel only | **Override: the full user app runs on Web too**, plus the moderation panel (Screen 10), gated by the moderator role. |
| Saved zones (§11 places them in Fase 2) | Fase 2 | Built in V1, since Screen 7 is specified. |
| Registration | open | **Invite-only.** See `addendum-invites.md`. |
| Backend | (none) | Supabase (free tier) for auth and data. Firebase Crashlytics on mobile, with analytics off. |
| Maps | (none) | MapLibre Compose + OpenFreeMap tiles; Photon geocoding. Free, no API keys. |
| Moderation staffing, business model, legal review (§9) | open | **Still open.** Not decided by engineering. |
