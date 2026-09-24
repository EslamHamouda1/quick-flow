#!/usr/bin/env bash
# phase-01 (Setup) API checks. Usage: phase-01-verify.sh <attempt-folder>
set -u
OUT="${1:?attempt folder}"
BASE="http://localhost:8080"
LOG="$OUT/curl.log"
TMP="$(mktemp -d "$OUT/.verify.XXXX")"
trap 'rm -rf "$TMP"' EXIT

call() { # name method url extra-curl-args...
  local name="$1" method="$2" url="$3"; shift 3
  local code
  code=$(curl -sS -o "$TMP/$name.body" -D "$TMP/$name.head" -w '%{http_code}' -X "$method" "$@" "$url")
  {
    echo "=== $name: $method $url $*"
    cat "$TMP/$name.head"
    head -c 4000 "$TMP/$name.body"; echo
    echo "=== status $code"; echo
  } >> "$LOG"
  echo "$code"
}

# C1: /v3/api-docs 200 with an openapi field
c=$(call C1 GET "$BASE/v3/api-docs")
v=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get("openapi",""))' "$TMP/C1.body" 2>/dev/null)
if [ "$c" = 200 ] && [ -n "$v" ]; then echo "PASS C1 (openapi $v)"; else echo "FAIL C1: 200 + openapi field vs $c / '$v'"; fi

# C2: /swagger-ui reachable (200 after redirects, Swagger UI html)
c=$(call C2 GET "$BASE/swagger-ui" -L)
if [ "$c" = 200 ] && grep -qi 'swagger' "$TMP/C2.body"; then echo "PASS C2"; else echo "FAIL C2: 200 Swagger UI html vs $c"; fi

# C3: no /api paths yet
p=$(python3 -c 'import json,sys; print(" ".join(k for k in json.load(open(sys.argv[1])).get("paths",{}) if k.startswith("/api")))' "$TMP/C1.body" 2>&1)
rc=$?
if [ $rc -eq 0 ] && [ -z "$p" ]; then echo "PASS C3 (no /api paths)"; else echo "FAIL C3: no /api paths vs '$p'"; fi
