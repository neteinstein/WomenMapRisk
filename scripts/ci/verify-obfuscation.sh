#!/usr/bin/env bash
# Asserts that the R8 release build really is obfuscated. Run after `./gradlew :androidApp:assembleRelease`.
# `isMinifyEnabled` and the keep rules in androidApp/proguard-rules.pro can regress silently (a stray blanket
# `-keep`, minification switched off) and the build still succeeds; only mapping.txt shows what R8 did.
set -euo pipefail
cd "$(dirname "$0")/../.."

readonly APP_PACKAGE_PREFIX="com.womenriskmap."
readonly MAPPING="androidApp/build/outputs/mapping/release/mapping.txt"

if [ ! -f "$MAPPING" ]; then
    echo "::error::$MAPPING not found. Run ':androidApp:assembleRelease' first; if it did run, R8 is not minifying."
    exit 1
fi

# Class lines are unindented and read "<original> -> <obfuscated>:"; members are indented.
renamed="$(awk -F' -> ' -v prefix="$APP_PACKAGE_PREFIX" '
    index($0, prefix) == 1 && NF == 2 {
        obfuscated = $2
        sub(/:$/, "", obfuscated)
        if (obfuscated != $1) renamed++
    }
    END { print renamed + 0 }
' "$MAPPING")"

if [ "$renamed" -eq 0 ]; then
    echo "::error::No ${APP_PACKAGE_PREFIX}* class was renamed: the release build is not obfuscated. Check isMinifyEnabled in androidApp/build.gradle.kts and for an over-broad -keep in androidApp/proguard-rules.pro."
    exit 1
fi
echo "Obfuscation verified: $renamed app classes renamed."
