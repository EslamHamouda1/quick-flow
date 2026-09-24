#!/usr/bin/env bash
# phase-02 (Foundational) API checks. Usage: phase-02-verify.sh <attempt-folder>
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

# C1: GET /api/app-info -> 200 application/json, timeZone Africa/Cairo, required fields
c=$(call C1 GET "$BASE/api/app-info")
ct=$(grep -i '^content-type:' "$TMP/C1.head" | tr -d '\r' | cut -d' ' -f2-)
r=$(python3 - "$TMP/C1.body" <<'EOF'
import json, sys
try:
    b = json.load(open(sys.argv[1]))
except Exception as e:
    print(f"not JSON ({e})"); sys.exit()
if sorted(b) != ["now", "timeZone"]: print(f"fields {sorted(b)}"); sys.exit()
if b["timeZone"] != "Africa/Cairo": print(f"timeZone {b['timeZone']!r}"); sys.exit()
print("ok")
EOF
)
if [ "$c" = 200 ] && [[ "$ct" == application/json* ]] && [ "$r" = ok ]; then
  echo "PASS C1 ($c, $ct)"
else
  echo "FAIL C1: 200 application/json {timeZone: Africa/Cairo, now} vs $c / '$ct' / $r"
fi

# C2: now = ISO-8601 with offset, offset = Cairo's current offset, within 60 s of this clock
r=$(python3 - "$TMP/C1.body" <<'EOF'
import json, sys
from datetime import datetime, timezone
from zoneinfo import ZoneInfo
try:
    s = json.load(open(sys.argv[1]))["now"]
    t = datetime.fromisoformat(s)
except Exception as e:
    print(f"bad now ({e})"); sys.exit()
if t.tzinfo is None: print(f"no offset in {s!r}"); sys.exit()
now = datetime.now(timezone.utc)
want = now.astimezone(ZoneInfo("Africa/Cairo")).utcoffset()
if t.utcoffset() != want: print(f"offset {t.utcoffset()} vs Cairo {want} ({s})"); sys.exit()
d = abs((now - t).total_seconds())
if d > 60: print(f"{d:.1f}s from checker clock ({s})"); sys.exit()
print(f"ok {s} offset {want} gap {d:.2f}s")
EOF
)
if [[ "$r" == ok* ]]; then echo "PASS C2 (${r#ok })"; else echo "FAIL C2: ISO offset date-time in Cairo, close to now vs $r"; fi

# C3: unknown /api path -> 404 (no mapping)
c=$(call C3 GET "$BASE/api/does-not-exist")
if [ "$c" = 404 ]; then echo "PASS C3 ($c)"; else echo "FAIL C3: 404 vs $c"; fi

# C4: /v3/api-docs info + /api/app-info operation + AppInfo schema
c=$(call C4 GET "$BASE/v3/api-docs")
check_doc() { # file -> "ok" or reason
python3 - "$1" <<'EOF'
import json, sys
try:
    d = json.load(open(sys.argv[1]))
except Exception as e:
    print(f"not JSON ({e})"); sys.exit()
i = d.get("info", {})
if i.get("title") != "QuickFlow API" or i.get("version") != "0.1.0": print(f"info {i}"); sys.exit()
op = d.get("paths", {}).get("/api/app-info", {}).get("get", {})
if op.get("operationId") != "getAppInfo": print(f"operationId {op.get('operationId')!r}"); sys.exit()
ref = op.get("responses", {}).get("200", {}).get("content", {}).get("application/json", {}).get("schema", {}).get("$ref")
if ref != "#/components/schemas/AppInfo": print(f"200 schema {ref!r}"); sys.exit()
s = d.get("components", {}).get("schemas", {}).get("AppInfo", {})
p = s.get("properties", {})
if p.get("timeZone", {}).get("type") != "string" or p.get("now", {}).get("type") != "string" \
   or p.get("now", {}).get("format") != "date-time" or sorted(s.get("required", [])) != ["now", "timeZone"]:
    print(f"AppInfo {s}"); sys.exit()
print("ok")
EOF
}
r=$(check_doc "$TMP/C4.body")
if [ "$c" = 200 ] && [ "$r" = ok ]; then echo "PASS C4"; else echo "FAIL C4: QuickFlow API 0.1.0, getAppInfo -> AppInfo vs $c / $r"; fi

# C5: generated openapi.json and its copy carry the same (files only read)
fail=""
for f in backend/target/openapi.json loops/backend-dev/outputs/openapi.json; do
  if [ -f "$f" ]; then r=$(check_doc "$f"); else r="missing"; fi
  echo "=== C5: $f -> $r" >> "$LOG"
  [ "$r" = ok ] || fail="$fail $f: $r;"
done
if [ -z "$fail" ]; then echo "PASS C5"; else echo "FAIL C5: same info/path/operationId vs$fail"; fi
