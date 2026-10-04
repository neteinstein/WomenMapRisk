#!/usr/bin/env bash
# CI build validation ONLY: copy committed *.ci placeholders into the gitignored secret locations.
# Real credentials are injected from GitHub secrets as env vars (SUPABASE_URL, ...) where needed.
set -euo pipefail
cd "$(dirname "$0")/../.."
[ -f local.properties ] || cp local.properties.ci local.properties
[ -f androidApp/google-services.json ] || cp androidApp/google-services.json.ci androidApp/google-services.json
echo "placeholders ready"
