#!/usr/bin/env bash
# suite test-phase-02 (US1 tags, 002-us-tags) API checks A1-A10. Usage: test-phase-02-verify.sh <attempt-folder>
# Calls only the API; writes only under the attempt folder (curl.log, times.tsv, ids.txt).
set -u
OUT="${1:?attempt folder}"
BASE="http://localhost:8080"
LOG="$OUT/curl.log"
TIMES="$OUT/times.tsv"
IDS="$OUT/ids.txt"
B="$OUT/.body"
: > "$TIMES"; : > "$IDS"
FAILED=""

fail() { echo "FAIL $1: $2"; FAILED="$FAILED $1"; }
# req CHECK TIMED METHOD PATH [json] -> $CODE, body in $B
req() {
  local chk="$1" timed="$2" method="$3" path="$4" data="${5-}" out t
  local args=(-sS -o "$B" -w '%{http_code} %{time_total}' -X "$method")
  [ -n "$data" ] && args+=(-H 'Content-Type: application/json' --data "$data")
  out=$(curl "${args[@]}" "$BASE$path")
  CODE=${out% *}; t=${out#* }
  { echo "=== $chk: $method $path ${data:+body=$data}"; head -c 3000 "$B"; echo; echo "=== status $CODE time_total $t"; echo; } >> "$LOG"
  [ "$timed" = 1 ] && printf '%s\t%s %s\t%s\t%s\n' "$chk" "$method" "$path" "$CODE" "$t" >> "$TIMES"
}
# expect CHECK LABEL WANT_CODE [jq-filter that must print true]
expect() {
  local chk="$1" label="$2" want="$3" filt="${4-}"
  if [ "$CODE" != "$want" ]; then fail "$chk" "$label: status $want vs $CODE ($(head -c 200 "$B"))"; return 1; fi
  if [ -n "$filt" ] && [ "$(jq -c "$filt" "$B" 2>&1)" != "true" ]; then
    fail "$chk" "$label: expected $filt vs $(head -c 300 "$B")"; return 1; fi
  return 0
}
# mk NAME JSON-FIELDS -> id; tags TAGS-JSON added after create when given
mk() {
  local name="$1" extra="${2-}" tags="${3-}"
  req setup 0 POST /api/tasks "{\"title\":\"S2 $name\"${extra:+,$extra}}"
  local id; id=$(jq -r .id "$B")
  echo "$name $id" >> "$IDS"
  if [ -n "$tags" ]; then req setup 1 POST "/api/tasks/$id/tags" "{\"tags\":$tags}"; fi
  echo "$id"
}
tagsof() { req "$1" 0 GET "/api/tasks/$2"; jq -c .tags "$B"; }
listed() { # listed QUERY -> sorted ids, ex. listed "tag=work"
  req "$1" 1 GET "/api/tasks?$2"; jq -c '[.[].id]|sort' "$B"; }
LONG31=$(printf 'a%.0s' $(seq 31)); LONG30=$(printf 'b%.0s' $(seq 30))

# A1 AC-US1-1 / FR-001 / FR-002
A=$(mk A)
req A1 1 POST "/api/tasks/$A/tags" '{"tags":["work","urgent"]}'
expect A1 add 200 '.tags == ["urgent","work"]'
req A1 0 GET "/api/tasks/$A"; expect A1 get 200 '.tags == ["urgent","work"]'
req A1 0 GET "/api/tasks"; expect A1 list 200 "map(select(.id==$A))[0].tags == [\"urgent\",\"work\"]"
case "$FAILED" in *A1*) ;; *) echo "PASS A1";; esac

# A2 AC-US1-2 / FR-004 / SC-004
W1=$(mk W1 "" '["work"]'); W2=$(mk W2 "" '["home","work"]'); N1=$(mk N1 "" '["home"]'); N2=$(mk N2); N3=$(mk N3 "" '["workshop"]')
got=$(listed A2 "tag=work"); want=$(jq -nc "[$A,$W1,$W2]|sort")
[ "$got" = "$want" ] || fail A2 "tag=work: $want vs $got"
req A2 1 GET "/api/tasks?tag=nosuchtag"; expect A2 nosuchtag 200 '. == []'
case "$FAILED" in *A2*) ;; *) echo "PASS A2";; esac

# A3 AC-US1-3 / FR-005
R=$(mk R "" '["work","home"]')
req A3 1 DELETE "/api/tasks/$R/tags?tag=work"; expect A3 remove 200 '.tags == ["home"]'
[ "$(tagsof A3 "$R")" = '["home"]' ] || fail A3 "GET R tags not [home]"
got=$(listed A3 "tag=work"); echo "$got" | jq -e "index($R) == null" >/dev/null || fail A3 "R still in tag=work: $got"
req A3 1 DELETE "/api/tasks/$R/tags?tag=nosuchtag"; expect A3 remove-absent 200 '.tags == ["home"]'
req A3 1 DELETE "/api/tasks/999999999/tags?tag=work"; expect A3 unknown-id 404
case "$FAILED" in *A3*) ;; *) echo "PASS A3";; esac

# A4 AC-US1-4 / FR-006 / BR-T1 / SC-003
V=$(mk V "" '["alpha"]')
ERR='(.errors|type=="array") and ([.errors[]|select(.field=="tags" and (.message|length>0))]|length>0)'
for bad in '[""]' '["   "]' "[\"$LONG31\"]" "[\"beta\",\"$LONG31\"]"; do
  req A4 1 POST "/api/tasks/$V/tags" "{\"tags\":$bad}"; expect A4 "reject $bad" 400 "$ERR"
  [ "$(tagsof A4 "$V")" = '["alpha"]' ] || fail A4 "tags changed after $bad"
done
req A4 1 POST "/api/tasks/$V/tags" "{\"tags\":[\"$LONG30\"]}"; expect A4 30-char 200 ".tags == [\"alpha\",\"$LONG30\"]"
req A4 1 POST "/api/tasks/$V/tags" '{"tags":["  gamma "]}'; expect A4 trim 200 '.tags | index("gamma") != null'
case "$FAILED" in *A4*) ;; *) echo "PASS A4";; esac

# A5 AC-US1-5 / FR-003 / BR-T2
K=$(mk K "" '["work"]')
req A5 1 POST "/api/tasks/$K/tags" '{"tags":["Work"]}'; expect A5 Work 200 '.tags == ["work"]'
got=$(listed A5 "tag=WORK"); echo "$got" | jq -e "index($K) != null" >/dev/null || fail A5 "K not in tag=WORK: $got"
req A5 1 POST "/api/tasks/$K/tags" '{"tags":["Home"]}'; expect A5 Home 200 '.tags == ["home","work"]'
case "$FAILED" in *A5*) ;; *) echo "PASS A5";; esac

# A6 AC-US1-6 / FR-007 / BR-T3 / SC-003
TEN='["t01","t02","t03","t04","t05","t06","t07","t08","t09","t10"]'
F=$(mk F)
req A6 1 POST "/api/tasks/$F/tags" "{\"tags\":$TEN}"; expect A6 ten 200 ".tags == $TEN"
req A6 1 POST "/api/tasks/$F/tags" '{"tags":["t11"]}'
expect A6 eleventh 400 '([.errors[]?.message|select(length>0)]|length>0) or ((.detail//"")|length>0)'
[ "$(tagsof A6 "$F")" = "$TEN" ] || fail A6 "tags changed after t11"
req A6 1 POST "/api/tasks/$F/tags" '{"tags":["T05"]}'; expect A6 T05 200 ".tags == $TEN"
case "$FAILED" in *A6*) ;; *) echo "PASS A6";; esac

# A7 FR-004 with the other filters; archived hidden by default (BR-4)
P1=$(mk "P1 alpha" '"priority":"HIGH","status":"IN_PROGRESS"' '["work"]')
P2=$(mk "P2 beta" '"priority":"LOW","status":"TODO"' '["work"]')
P3=$(mk "P3 alpha" '"priority":"HIGH","status":"TODO"' '["home"]')
got=$(listed A7 "tag=work&priority=HIGH"); [ "$got" = "[$P1]" ] || fail A7 "tag=work&priority=HIGH: [$P1] vs $got"
got=$(listed A7 "tag=work&status=TODO"); echo "$got" | jq -e "index($P2) != null and index($P1) == null and index($P3) == null" >/dev/null || fail A7 "tag=work&status=TODO: $got"
got=$(listed A7 "tag=work&q=alpha"); [ "$got" = "[$P1]" ] || fail A7 "tag=work&q=alpha: [$P1] vs $got"
AR=$(mk AR "" '["work"]'); req A7 0 POST "/api/tasks/$AR/archive"; expect A7 archive 200
got=$(listed A7 "tag=work"); echo "$got" | jq -e "index($AR) == null" >/dev/null || fail A7 "archived AR in tag=work: $got"
case "$FAILED" in *A7*) ;; *) echo "PASS A7";; esac

# A8 BR-T4: edit, complete, archive, restore keep tags
E=$(mk E "" '["keep","work"]')
req A8 0 PUT "/api/tasks/$E" '{"title":"S2 E edited","priority":"HIGH","status":"IN_PROGRESS"}'; expect A8 put 200 '.tags == ["keep","work"] and .title == "S2 E edited"'
req A8 0 POST "/api/tasks/$E/complete"; expect A8 complete 200 '.tags == ["keep","work"]'
req A8 0 POST "/api/tasks/$E/archive"; expect A8 archive 200 '.tags == ["keep","work"]'
req A8 0 POST "/api/tasks/$E/restore"; expect A8 restore 200 '.tags == ["keep","work"]'
[ "$(tagsof A8 "$E")" = '["keep","work"]' ] || fail A8 "GET E tags changed"
case "$FAILED" in *A8*) ;; *) echo "PASS A8";; esac

# A9 FR-008 / BR-T4 delete
D=$(mk D "" '["gone"]')
req A9 0 DELETE "/api/tasks/$D"; case "$CODE" in 200|204) ;; *) fail A9 "delete: 2xx vs $CODE";; esac
req A9 1 GET "/api/tasks?tag=gone"; expect A9 filter 200 '. == []'
req A9 1 POST "/api/tasks/$D/tags" '{"tags":["x"]}'; expect A9 add-deleted 404
req A9 1 DELETE "/api/tasks/$D/tags?tag=gone"; expect A9 remove-deleted 404
case "$FAILED" in *A9*) ;; *) echo "PASS A9";; esac

# A10 SC-002: every tag call (add/remove/list?tag=) < 0.500 s
slow=$(awk -F'\t' '$1!="setup" || $2 ~ /tags/ { if ($4+0 >= 0.5) print }' "$TIMES")
n=$(wc -l < "$TIMES"); max=$(cut -f4 "$TIMES" | sort -g | tail -1)
if [ -n "$slow" ]; then fail A10 "calls >= 0.5 s: $slow"; else echo "PASS A10 ($n timed calls, max $max s)"; fi
rm -f "$B"
