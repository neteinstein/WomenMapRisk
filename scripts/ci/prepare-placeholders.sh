#!/usr/bin/env bash
# CI build validation ONLY: copy committed *.ci placeholders into the gitignored secret locations.
# Real credentials are injected from GitHub secrets as env vars (SUPABASE_URL, ...) where needed.
# GOOGLE_SERVICES_JSON, when set, replaces the placeholder androidApp/google-services.json. It may hold either the raw
# JSON file contents or its base64 encoding.
set -euo pipefail
cd "$(dirname "$0")/../.."
[ -f local.properties ] || cp local.properties.ci local.properties
if [ -n "${GOOGLE_SERVICES_JSON:-}" ]; then
  if [[ "$GOOGLE_SERVICES_JSON" =~ ^[[:space:]]*\{ ]]; then
    printf '%s\n' "$GOOGLE_SERVICES_JSON" > androidApp/google-services.json
  else
    scripts/ci/decode-base64-secret.sh GOOGLE_SERVICES_JSON androidApp/google-services.json
  fi
  python3 -c 'import json,sys; json.load(open(sys.argv[1]))' androidApp/google-services.json \
    || { echo "::error::GOOGLE_SERVICES_JSON is not valid JSON (paste the whole google-services.json, or its base64)."; exit 1; }
  echo "google-services.json written from secret"
else
  [ -f androidApp/google-services.json ] || cp androidApp/google-services.json.ci androidApp/google-services.json
fi
echo "placeholders ready"
