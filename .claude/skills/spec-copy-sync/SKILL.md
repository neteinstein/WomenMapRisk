---
name: spec-copy-sync
description: Bring Women Risk Map UI copy (PT/EN strings) in line with the functional spec. Use when adding or changing any user-facing text, when asked to "check copy against the spec", or when a screen from docs/spec needs its labels.
---

# Sync UI copy with the spec

The canonical copy source is `docs/spec/especificacao-funcional-v1.md` (PT), §4 *Ecrãs*, plus
`docs/spec/addendum-invites.md` for invites. All app strings come from ONE table: `core/designsystem/strings.py`.

## Steps

1. Find the screen in spec §4 (e.g. "Ecrã 5: Fazer um reporte"). List every quoted label or message ("…") and
   every field or option name.
2. Open `core/designsystem/strings.py`. For each spec string:
   - If a key exists, check that the PT value is **verbatim** (same words, accents, punctuation; spec quotes
     often end with a period). Mark the line with `# [spec]`.
   - If it's missing, add `("key", "EN translation", "PT verbatim")` in the section for that screen. Keys are
     `snake_case` and prefixed by screen (`map_`, `report_`, `zone_`, …).
3. Copy that isn't in the spec (helper text, errors not listed) may be written, but keep it short, neutral and
   consistent with the spec's tone (informal "tu" in PT: "Confirma o teu email").
4. Placeholders must be positional: `%1$s`, `%1$d`. Plurals go in the `P` list with `one`/`other` forms.
5. Regenerate: `python3 core/designsystem/strings.py`. It asserts there are no duplicate keys and writes
   `values/strings.xml` (EN) and `values-pt/strings.xml` (PT). Never edit those XML files by hand.
6. Use the string in UI via `stringResource(Res.string.key)` (import `com.womenriskmap.core.designsystem.resources.*`).
   Domain enums get labels in `core/designsystem/.../components/Labels.kt`, not in features.
7. Verify: `./gradlew :core:designsystem:compileAndroidMain` and `git diff` the XML. CI fails if the XML is out of
   sync with `strings.py`.

## Don'ts

- Don't "improve" or shorten spec PT copy, even if you'd phrase it differently.
- Don't add copy for spec §10 features (out of V1).
- Don't hardcode strings in composables.
