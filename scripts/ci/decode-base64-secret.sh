#!/usr/bin/env bash
# Decode a base64 secret held in env var $1 into file $2.
# Tolerates what copy-pasting into GitHub secrets tends to add: spaces, CR/LF line wrapping, a missing
# trailing '=' padding, and the URL-safe alphabet (-_). Fails with an actionable message otherwise.
set -euo pipefail
name="$1"
out="$2"
NAME="$name" OUT="$out" python3 - <<'PY'
import base64, binascii, os, re, sys
name, out = os.environ["NAME"], os.environ["OUT"]
raw = os.environ.get(name, "")
data = re.sub(r"\s+", "", raw).translate(str.maketrans("-_", "+/"))
data += "=" * (-len(data) % 4)
try:
    decoded = base64.b64decode(data, validate=True)
except (binascii.Error, ValueError) as e:
    bad = sorted({c for c in data if not re.match(r"[A-Za-z0-9+/=]", c)})
    print(f"::error::{name} is not valid base64 ({e}; {len(raw)} chars"
          + (f", unexpected characters: {bad!r}" if bad else "")
          + "). Re-create it with `base64 -i <file> | pbcopy` (macOS) or `base64 -w0 <file>` (Linux).")
    sys.exit(1)
if not decoded:
    print(f"::error::{name} decoded to an empty file.")
    sys.exit(1)
with open(out, "wb") as f:
    f.write(decoded)
print(f"{name} decoded ({len(decoded)} bytes)")
PY
