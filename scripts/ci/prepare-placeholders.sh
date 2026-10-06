#!/usr/bin/env bash
# CI build validation ONLY: copy committed *.ci placeholders into the gitignored secret locations.
# Real credentials are injected from GitHub secrets as env vars (SUPABASE_URL, ...) where needed.
# GOOGLE_SERVICES_JSON (base64), when set, is decoded into androidApp/google-services.json instead of the placeholder.
set -euo pipefail
cd "$(dirname "$0")/../.."
[ -f local.properties ] || cp local.properties.ci local.properties
if [ -n "${GOOGLE_SERVICES_JSON:-}" ]; then
  scripts/ci/decode-base64-secret.sh GOOGLE_SERVICES_JSON androidApp/google-services.json
  python3 -c 'import json,sys; json.load(open(sys.argv[1]))' androidApp/google-services.json
  echo "google-services.json written from secret"
else
  [ -f androidApp/google-services.json ] || cp androidApp/google-services.json.ci androidApp/google-services.json
fi
echo "placeholders ready"
