---
name: supabase-migration
description: Add or change a Women Risk Map backend rule (table, RLS, trigger or RPC) safely, with migration, pgTAP test, Kotlin mirror and error mapping. Use for any change under supabase/ or any new server-side business rule.
---

# Supabase migration workflow

The access model is **deny-by-default**: RLS is on and there are no policies. Clients only call
`SECURITY DEFINER` RPCs with `set search_path = ''`. Never add table policies that expose rows directly, and
never return `user_id` to clients.

1. **New migration file**: `supabase/migrations/<YYYYMMDDHHMMSS>_<topic>.sql`. Never edit an applied migration.
   Fully qualify everything (`public.reports`, `auth.uid()`, `extensions.gen_random_bytes`).
2. **Authorisation**: start RPCs with `public.require_enabled()` (contributors) or `public.require_moderator()`.
   Raise stable lowercase codes: `raise exception 'my_new_code';`.
3. **Grants**: default privileges revoke execute. Add `grant execute on function public.fn(<arg types>) to authenticated;`
   (and `anon` only for public reads).
4. **Privacy**: any stored location goes through `public.snap_to_zone` (the reports trigger already re-snaps).
   Personal data must cascade on `auth.users` deletion; reports must survive with `user_id = null`.
5. **Tests**: add assertions to `supabase/tests/*.sql` (pgTAP, inside `begin … rollback`) and bump `plan(n)`.
   Use the helpers in `rules_test.sql` (`th.mk_user`, `th.login`, `th.logout`). Test both the allowed path and
   the error code.
6. **Run locally**: `scripts/sql-harness/run.sh` (no Docker; emulates auth, pgcrypto, pgTAP and realtime) or
   `supabase start && supabase db reset && supabase test db`.
7. **Kotlin side**:
   - Wire the RPC in the relevant `core/data/.../repository/Supabase*Repository.kt` (or feature `data/`), with
     the DTO in `remote/dto/Dtos.kt`. Wire names are snake_case and enums are lowercase strings.
   - Map the new error code in `core/data/.../remote/ErrorMapping.kt` → a `DomainException` subclass, then its
     message in `core/designsystem/.../Labels.kt` (`messageRes()`) + strings.
   - If the rule has a client-side mirror (`core/domain/rules/*`: ReportPolicy, InviteEligibility,
     LocationAnonymizer, ZoneRiskCalculator), update it and its tests in the same PR so they stay equivalent.
8. **Docs**: if this changes a product rule, cite the spec/addendum section in the SQL comment and update
   `docs/PLAN.md`. Remember the DB is **not auto-deployed** (see CI_CD.md).
