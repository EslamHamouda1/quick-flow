#!/usr/bin/env bash
# phase-09 (Polish) API checks. Usage: phase-09-verify.sh <attempt-folder>
# Needs bash, curl, jq. Writes only inside the attempt folder. Expects a fresh test database.
set -u
OUT="${1:?attempt folder}"
BASE="http://localhost:8080"
LOG="$OUT/curl.log"
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../../../../.." && pwd)"
report() { if [ -z "$2" ]; then echo "PASS $1"; else echo "FAIL $1:$2"; fi; }

# C1: live operationIds = contract operationIds (38)
code=$(curl -sS -o "$OUT/C1-api-docs.json" -w '%{http_code}' "$BASE/v3/api-docs")
{ echo "=== C1: GET $BASE/v3/api-docs"; echo "=== status $code"; echo; } >> "$LOG"
jq -r '.paths[][] | .operationId? // empty' "$OUT/C1-api-docs.json" | sort > "$OUT/C1-live-ops.txt"
grep -E '^\s+operationId:' "$ROOT/specs/001-quickflow-backend/contracts/openapi.yaml" \
  | sed -E 's/.*operationId:\s*//; s/["'\'' ]//g' | sort > "$OUT/C1-contract-ops.txt"
f=""
[ "$code" = 200 ] || f="$f status $code;"
d=$(diff "$OUT/C1-contract-ops.txt" "$OUT/C1-live-ops.txt")
[ -z "$d" ] || f="$f diff: $(echo "$d" | tr '\n' ' ');"
n=$(wc -l < "$OUT/C1-live-ops.txt"); [ "$n" = 38 ] || f="$f live count $n;"
report C1 "$f"

# C2: copy equals target, same operationIds
f=""
cmp -s "$ROOT/loops/backend-dev/outputs/openapi.json" "$ROOT/backend/target/openapi.json" || f="$f files differ;"
jq -r '.paths[][] | .operationId? // empty' "$ROOT/loops/backend-dev/outputs/openapi.json" | sort > "$OUT/C2-copy-ops.txt"
cmp -s "$OUT/C2-copy-ops.txt" "$OUT/C1-live-ops.txt" || f="$f copy operationIds differ from live;"
report C2 "$f"

# C3: earlier phase checks, unchanged, empty-DB ones first
f=""
for p in phase-08 phase-07 phase-03 phase-04 phase-05 phase-06; do
  mkdir -p "$OUT/regression/$p"
  bash "$HERE/$p-verify.sh" "$OUT/regression/$p" > "$OUT/regression/$p/result.txt" 2>&1
  bad=$(grep -E '^FAIL' "$OUT/regression/$p/result.txt" | cut -c1-200 | tr '\n' ' ')
  [ -z "$bad" ] || f="$f $p: $bad;"
done
report C3 "$f"
