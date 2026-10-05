#!/usr/bin/env bash
# Runs all Supabase migrations, the seed and the pgTAP tests against a throwaway local Postgres,
# WITHOUT Docker or the Supabase CLI (handy in sandboxed/cloud agent containers).
#   scripts/sql-harness/run.sh
# Uses PG_BIN if set (dir with postgres/initdb/psql); otherwise installs the `pgserver` wheel (bundled
# Postgres 16) into build/sql-harness/venv. pgcrypto/pgTAP/auth/realtime are emulated by bootstrap.sql.
# CI's `supabase-db` job (real Supabase stack) remains the source of truth.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
WORK="$ROOT/build/sql-harness"
PORT="${PG_PORT:-55432}"
mkdir -p "$WORK"

if [[ -z "${PG_BIN:-}" ]]; then
  if [[ ! -x "$WORK/venv/bin/python" ]]; then
    python3 -m venv "$WORK/venv"
    "$WORK/venv/bin/pip" install -q pgserver
  fi
  PG_BIN="$("$WORK/venv/bin/python" -c 'import pgserver, os; print(os.path.join(os.path.dirname(pgserver.__file__), "pginstall", "bin"))')"
fi

DATA="$WORK/pgdata"
cleanup() { "$PG_BIN/pg_ctl" -D "$DATA" stop -m fast >/dev/null 2>&1 || true; }
trap cleanup EXIT
rm -rf "$DATA"
"$PG_BIN/initdb" -D "$DATA" -U postgres -A trust -E UTF8 >/dev/null
# TCP only: unix socket paths can exceed the 103-byte limit in deep checkouts.
"$PG_BIN/pg_ctl" -D "$DATA" -o "-p $PORT -k '' -c listen_addresses=127.0.0.1" -l "$WORK/pg.log" -w start >/dev/null

PSQL=("$PG_BIN/psql" -h 127.0.0.1 -p "$PORT" -U postgres -v ON_ERROR_STOP=1 -q)
"${PSQL[@]}" -c "create database wrm" >/dev/null
PSQL+=(-d wrm)
"${PSQL[@]}" -f "$ROOT/scripts/sql-harness/bootstrap.sql" >/dev/null
"${PSQL[@]}" -c 'alter database wrm set search_path = "$user", public, extensions' >/dev/null

for f in "$ROOT"/supabase/migrations/*.sql; do
  sed 's/^create extension if not exists pgcrypto.*$/-- pgcrypto emulated/' "$f" | "${PSQL[@]}" -f - >/dev/null
done
echo "✓ migrations"
"${PSQL[@]}" -f "$ROOT/supabase/seed.sql" >/dev/null
echo "✓ seed"

status=0
for t in "$ROOT"/supabase/tests/*.sql; do
  out=$(sed 's/^create extension if not exists pgtap.*$//' "$t" | "${PSQL[@]}" -tA -f - 2>&1) || status=1
  echo "$out" | grep -E "NOT OK|passed|FAILED|ERROR" | sed "s|^|$(basename "$t"): |"
done
exit $status
