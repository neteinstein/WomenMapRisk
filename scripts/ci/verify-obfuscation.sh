#!/usr/bin/env bash
# Asserts that every R8 release variant (github, playstore) really is obfuscated. Run after
# `./gradlew :androidApp:assembleRelease`. `isMinifyEnabled` and the keep rules in androidApp/proguard-rules.pro can
# regress silently (a stray blanket `-keep`, minification switched off) and the build still succeeds; only
# mapping.txt shows what R8 did.
set -euo pipefail
cd "$(dirname "$0")/../.."

readonly APP_PACKAGE_PREFIX="com.womenriskmap."

mappings=()
while IFS= read -r line; do mappings+=("$line"); done < <(find androidApp/build/outputs/mapping -mindepth 2 -maxdepth 2 -name mapping.txt -path '*Release/*' | sort)
if [ ${#mappings[@]} -eq 0 ]; then
    echo "::error::No release mapping.txt under androidApp/build/outputs/mapping/. Run ':androidApp:assembleRelease' first; if it did run, R8 is not minifying."
    exit 1
fi

failures=0
for mapping in "${mappings[@]}"; do
    variant="$(basename "$(dirname "$mapping")")"
    # Class lines are unindented and read "<original> -> <obfuscated>:"; members are indented.
    renamed="$(awk -F' -> ' -v prefix="$APP_PACKAGE_PREFIX" '
        index($0, prefix) == 1 && NF == 2 {
            obfuscated = $2
            sub(/:$/, "", obfuscated)
            if (obfuscated != $1) renamed++
        }
        END { print renamed + 0 }
    ' "$mapping")"
    if [ "$renamed" -eq 0 ]; then
        echo "::error::$variant: no ${APP_PACKAGE_PREFIX}* class was renamed: not obfuscated. Check isMinifyEnabled in androidApp/build.gradle.kts and for an over-broad -keep in androidApp/proguard-rules.pro."
        failures=$((failures + 1))
    else
        echo "  $variant: $renamed app classes renamed"
    fi
done
[ "$failures" -eq 0 ] || exit 1
echo "Obfuscation verified across ${#mappings[@]} release variant(s)."
