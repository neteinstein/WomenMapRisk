## Summary
<!-- What does this PR do and why? Link the spec section (docs/spec/…) or PLAN.md stage it relates to. -->

## Changes
<!-- Bullet list of notable changes, grouped by module (core:domain, feature:map, supabase/, …). -->
-

## Test plan
<!-- What you ran and the result. Be specific: commands, devices, screenshots for UI changes. -->
- [ ] `./gradlew testDebugUnitTest` (shared tests on JVM)
- [ ] `./gradlew ktlintCheck`
- [ ] `scripts/sql-harness/run.sh` (if `supabase/` changed)
- [ ] Manual check on: <!-- Android / iOS simulator / web -->

**Could NOT run, and why** (required; write "nothing" if everything ran):
<!-- e.g. "iOS build: sandbox has no Xcode; relying on CI build-ios job." -->

## Risk
<!-- Low / Medium / High, and what could break (data migrations, auth, privacy, store rules). Rollback plan if relevant. -->
