#!/usr/bin/env bash
# phase-08 (US6 Adjust settings) API checks. Usage: phase-08-verify.sh <attempt-folder>
# Needs only bash, curl and jq. Writes only inside the attempt folder. Expects a fresh test database.
set -u
OUT="${1:?attempt folder}"
BASE="http://localhost:8080"
S="$BASE/api/settings"
LOG="$OUT/curl.log"
TMP="$(mktemp -d "$OUT/.verify.XXXX")"
trap 'rm -rf "$TMP"' EXIT

call() { # name method url [json-body]
  local name="$1" method="$2" url="$3" code
  if [ $# -ge 4 ]; then
    printf '%s' "$4" > "$TMP/$name.req"
    code=$(command curl -sS -o "$TMP/$name.body" -D "$TMP/$name.head" -w '%{http_code}' -X "$method" \
      -H 'Content-Type: application/json' --data-binary "@$TMP/$name.req" "$url")
  else
    code=$(command curl -sS -o "$TMP/$name.body" -D "$TMP/$name.head" -w '%{http_code}' -X "$method" "$url")
  fi
  {
    echo "=== $name: $method $url"
    [ $# -ge 4 ] && { echo "--- request body:"; head -c 600 "$TMP/$name.req"; echo; }
    cat "$TMP/$name.head"
    head -c 6000 "$TMP/$name.body"; echo
    echo "=== status $code"; echo
  } >> "$LOG"
  echo "$code"
}
ctype() { grep -i '^content-type:' "$TMP/$1.head" | tr -d '\r' | cut -d' ' -f2-; }
jq_() { local n="$1"; shift; jq -r "$@" "$TMP/$n.body" 2>/dev/null || echo "not-JSON"; }
body() { jq_ "$1" -c '{displayName,planStartNotifications,defaultPage}'; }
report() { if [ -z "$2" ]; then echo "PASS $1"; else echo "FAIL $1:$2"; fi; }
put_ok() { # name json expected-body
  local c; c=$(call "$1" PUT "$S" "$2")
  [ "$c" = 200 ] || { echo " $1 status $c;"; return; }
  [ "$(body "$1")" = "$3" ] || echo " $1 body $(body "$1") vs $3;"
  c=$(call "$1-get" GET "$S"); [ "$c" = 200 ] || echo " $1-get status $c;"
  [ "$(body "$1-get")" = "$3" ] || echo " $1-get $(body "$1-get") vs $3;"
}
bad() { # name json [field] ; expects 400 problem (+ field) and settings unchanged vs $KEEP
  local c; c=$(call "$1" PUT "$S" "$2")
  [ "$c" = 400 ] || echo " $1 status $c (expected 400);"
  [[ "$(ctype "$1")" == application/problem+json* ]] || echo " $1 content-type '$(ctype "$1")';"
  [ "$(jq_ "$1" .status)" = 400 ] || echo " $1 problem status '$(jq_ "$1" .status)';"
  if [ $# -ge 3 ]; then
    [ "$(jq_ "$1" --arg f "$3" '[.errors[]?|select(.field==$f and (.message|type=="string" and length>0))]|length>0')" = true ] \
      || echo " $1 no errors[] entry for field $3: $(head -c 300 "$TMP/$1.body");"
  fi
  c=$(call "$1-get" GET "$S")
  [ "$(body "$1-get")" = "$KEEP" ] || echo " $1 changed settings: $(body "$1-get") vs $KEEP;"
}
echo "=== run started $(date -Iseconds)" >> "$LOG"

# ---------- C1: contract + defaults on first read
f=""
c=$(call C1-docs GET "$BASE/v3/api-docs")
r=$(jq_ C1-docs '[.paths["/api/settings"].get.operationId=="getSettings", .paths["/api/settings"].put.operationId=="updateSettings",
  (.paths["/api/settings"].get.tags|index("settings")!=null), (.paths["/api/settings"].put.tags|index("settings")!=null),
  (.components.schemas.Settings.required|sort)==["defaultPage","planStartNotifications"],
  (.components.schemas.Settings.properties.displayName.type|sort)==["null","string"],
  .components.schemas.Settings.properties.displayName.maxLength==100,
  .components.schemas.Settings.properties.planStartNotifications.type=="boolean",
  (.components.schemas.Settings.properties.defaultPage["$ref"]|endswith("/DefaultPage")),
  (.components.schemas.DefaultPage.enum|sort)==(["DASHBOARD","TASKS","HABITS","LEARNING","PLANS","SETTINGS"]|sort)]|all')
[ "$r" = true ] || f="$f api-docs ($c) settings operations/schemas differ;"
c=$(call C1 GET "$S"); [ "$c" = 200 ] || f="$f GET status $c;"
[[ "$(ctype C1)" == application/json* ]] || f="$f content-type '$(ctype C1)';"
[ "$(jq_ C1 -c 'keys')" = '["defaultPage","displayName","planStartNotifications"]' ] || f="$f keys $(jq_ C1 -c keys);"
DEF='{"displayName":null,"planStartNotifications":true,"defaultPage":"DASHBOARD"}'
[ "$(body C1)" = "$DEF" ] || f="$f defaults $(body C1) vs $DEF;"
c=$(call C1-b GET "$S"); [ "$c" = 200 ] || f="$f second GET status $c;"
[ "$(jq_ C1-b -c .)" = "$(jq_ C1 -c .)" ] || f="$f second GET $(jq_ C1-b -c .) vs first;"
report C1 "$f"

# ---------- C2: save, read back, every default page, clear display name
f=""
f="$f$(put_ok C2 '{"displayName":"Eve","planStartNotifications":false,"defaultPage":"HABITS"}' '{"displayName":"Eve","planStartNotifications":false,"defaultPage":"HABITS"}')"
for p in DASHBOARD TASKS HABITS LEARNING PLANS SETTINGS; do
  f="$f$(put_ok "C2-$p" "{\"displayName\":\"Eve\",\"planStartNotifications\":true,\"defaultPage\":\"$p\"}" "{\"displayName\":\"Eve\",\"planStartNotifications\":true,\"defaultPage\":\"$p\"}")"
done
f="$f$(put_ok C2-null '{"displayName":null,"planStartNotifications":false,"defaultPage":"PLANS"}' '{"displayName":null,"planStartNotifications":false,"defaultPage":"PLANS"}')"
f="$f$(put_ok C2-set '{"displayName":"Eve","planStartNotifications":false,"defaultPage":"PLANS"}' '{"displayName":"Eve","planStartNotifications":false,"defaultPage":"PLANS"}')"
f="$f$(put_ok C2-omit '{"planStartNotifications":true,"defaultPage":"TASKS"}' '{"displayName":null,"planStartNotifications":true,"defaultPage":"TASKS"}')"
report C2 "$f"

# ---------- C3: displayName length
f=""
N100=$(printf 'a%.0s' $(seq 1 100)); N101="${N100}b"
f="$f$(put_ok C3-100 "{\"displayName\":\"$N100\",\"planStartNotifications\":false,\"defaultPage\":\"LEARNING\"}" "{\"displayName\":\"$N100\",\"planStartNotifications\":false,\"defaultPage\":\"LEARNING\"}")"
KEEP="{\"displayName\":\"$N100\",\"planStartNotifications\":false,\"defaultPage\":\"LEARNING\"}"
f="$f$(bad C3-101 "{\"displayName\":\"$N101\",\"planStartNotifications\":true,\"defaultPage\":\"TASKS\"}" displayName)"
report C3 "$f"

# ---------- C4: defaultPage validation
f=""
f="$f$(put_ok C4-set '{"displayName":"Keep","planStartNotifications":false,"defaultPage":"PLANS"}' '{"displayName":"Keep","planStartNotifications":false,"defaultPage":"PLANS"}')"
KEEP='{"displayName":"Keep","planStartNotifications":false,"defaultPage":"PLANS"}'
f="$f$(bad C4-foo '{"displayName":"X","planStartNotifications":true,"defaultPage":"FOO"}' defaultPage)"
f="$f$(bad C4-missing '{"displayName":"X","planStartNotifications":true}' defaultPage)"
f="$f$(bad C4-null '{"displayName":"X","planStartNotifications":true,"defaultPage":null}' defaultPage)"
f="$f$(bad C4-lower '{"displayName":"X","planStartNotifications":true,"defaultPage":"dashboard"}')"
report C4 "$f"

# ---------- C5: planStartNotifications validation
f=""
f="$f$(bad C5-missing '{"displayName":"X","defaultPage":"TASKS"}' planStartNotifications)"
f="$f$(bad C5-null '{"displayName":"X","planStartNotifications":null,"defaultPage":"TASKS"}' planStartNotifications)"
f="$f$(bad C5-yes '{"displayName":"X","planStartNotifications":"yes","defaultPage":"TASKS"}')"
report C5 "$f"
