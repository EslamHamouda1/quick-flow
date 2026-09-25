#!/usr/bin/env bash
# phase-06 (US4 Build and follow Todo Plans) API checks. Usage: phase-06-verify.sh <attempt-folder>
# Needs only bash, curl, jq and GNU date. Writes only inside the attempt folder.
set -u
OUT="${1:?attempt folder}"
BASE="http://localhost:8080"
PL="$BASE/api/plans"
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
    head -c 4000 "$TMP/$name.body"; echo
    echo "=== status $code"; echo
  } >> "$LOG"
  echo "$code"
}
ctype() { grep -i '^content-type:' "$TMP/$1.head" | tr -d '\r' | cut -d' ' -f2-; }
jq_() { jq -r "$2" "$TMP/$1.body" 2>/dev/null || echo "not-JSON"; }   # name filter
long() { printf "%${2}s" "" | tr ' ' "$1"; }                        # char count
at() { date -d "$1" -Iseconds; }                                     # relative time -> ISO with offset
near_now() { # iso date-time with offset, within 300 s of now
  local ts="$1" s
  [[ "$ts" =~ [+-][0-9]{2}:[0-9]{2}$|Z$ ]] || { echo "no offset in '$ts'"; return; }
  s=$(date -d "$ts" +%s 2>/dev/null) || { echo "not a date-time '$ts'"; return; }
  (( ( $(date +%s) - s ) < 300 && ( s - $(date +%s) ) < 300 )) && echo ok || echo "'$ts' not near now"; }
problem() { # want code name field ("" = any)
  local want="$1" c="$2" name="$3" field="$4" ct r
  ct=$(ctype "$name")
  [ "$c" = "$want" ] || { echo "status $c (want $want) $(head -c 300 "$TMP/$name.body")"; return; }
  [[ "$ct" == application/problem+json* ]] || { echo "content-type '$ct'"; return; }
  if [ -n "$field" ]; then
    r=$(jq_ "$name" "[.errors[]? | select(.field==\"$field\" and ((.message // \"\") | gsub(\"\\\\s\";\"\") | length > 0))] | length > 0")
    [ "$r" = true ] || { echo "no errors[] entry with field '$field' and a message ($(head -c 300 "$TMP/$name.body"))"; return; }
  fi
  echo ok; }
mk() { # name url json -> id (201 expected, else empty)
  local c; c=$(call "$1" POST "$2" "$3")
  if [ "$c" = 201 ]; then jq_ "$1" '.id'; else echo ""; fi; }
report() { if [ -z "$2" ]; then echo "PASS $1"; else echo "FAIL $1:$2"; fi; }
body() { # title items-json start end [duration] [priority]
  printf '{"title":"%s","items":%s,"estimatedDurationMinutes":%s,"startDateTime":"%s","endDateTime":"%s","priorityOrder":%s}' \
    "$1" "$2" "${5:-60}" "$3" "$4" "${6:-1}"; }
plan() { # name title items start end [dur] [prio] -> id
  mk "$1" "$PL" "$(body "$2" "$3" "$4" "$5" "${6:-60}" "${7:-1}")"; }
item_id() { jq_ "$1" "[.items[]|select(.sourceType==\"$2\")|.id][0]"; }   # name type
TOK="P$(date +%s)"
echo "=== token $TOK, run started $(date -Iseconds)" >> "$LOG"

# sources
T=$(mk S-task "$BASE/api/tasks" "{\"title\":\"Task $TOK\"}")
H=$(mk S-habit "$BASE/api/habits" "{\"name\":\"Habit $TOK\",\"frequency\":\"DAILY\"}")
L=$(mk S-card "$BASE/api/learning-cards" "{\"title\":\"Card $TOK\"}")
if [ -z "$T" ] || [ -z "$H" ] || [ -z "$L" ]; then
  echo "FAIL SETUP: could not create task/habit/card (task=$T habit=$H card=$L)"; exit 1; fi
ITEMS3="[{\"sourceType\":\"TASK\",\"sourceId\":$T},{\"sourceType\":\"HABIT\",\"sourceId\":$H},{\"sourceType\":\"LEARNING_RESOURCE\",\"sourceId\":$L}]"
FUT_S=$(at '+1 hour'); FUT_E=$(at '+3 hours')
WIN_S=$(at '-1 hour'); WIN_E=$(at '+1 hour')
PST_S=$(at '-3 hours'); PST_E=$(at '-1 hour')

# ---------- C1: AC-US4-1 create
f=""
c=$(call C1 POST "$PL" "$(body "C1 plan $TOK" "$ITEMS3" "$FUT_S" "$FUT_E" 90 2)")
[ "$c" = 201 ] || f="$f 201 vs $c;"
[[ "$(ctype C1)" == application/json* ]] || f="$f content-type '$(ctype C1)';"
r=$(jq_ C1 "[.title==\"C1 plan $TOK\", .estimatedDurationMinutes==90, .priorityOrder==2, (.id|type)==\"number\",
  (.items|length)==3, all(.items[]; .done==false and .sourceRemoved==false and (.id|type)==\"number\"),
  ([.items[]|\"\(.sourceType):\(.sourceId):\(.sourceTitle)\"]|sort)==([\"HABIT:$H:Habit $TOK\",\"LEARNING_RESOURCE:$L:Card $TOK\",\"TASK:$T:Task $TOK\"]|sort),
  .doneItems==0, .totalItems==3, .progressPercent==0] | all")
[ "$r" = true ] || f="$f body: $(head -c 600 "$TMP/C1.body");"
r=$(near_now "$(jq_ C1 '.createdAt')"); [ "$r" = ok ] || f="$f createdAt: $r;"
for k in startDateTime endDateTime; do
  v=$(jq_ C1 ".$k"); want=$([ $k = startDateTime ] && echo "$FUT_S" || echo "$FUT_E")
  [ "$(date -d "$v" +%s 2>/dev/null)" = "$(date -d "$want" +%s)" ] || f="$f $k '$v' vs sent '$want';"
done
P1=$(jq_ C1 .id)
c=$(call C1-get GET "$PL/$P1"); [ "$c" = 200 ] || f="$f GET 200 vs $c;"
[ "$(jq_ C1 'del(.restSeconds)|tojson')" = "$(jq_ C1-get 'del(.restSeconds)|tojson')" ] || f="$f GET differs: $(head -c 400 "$TMP/C1-get.body");"
report C1 "$f"

# ---------- C2: AC-US4-2 BR-10 no items / unknown / deleted item
f=""
call C2-before GET "$PL?group=all" >/dev/null; n0=$(jq_ C2-before length)
c=$(call C2-empty POST "$PL" "$(body "C2 $TOK" '[]' "$FUT_S" "$FUT_E")"); r=$(problem 400 "$c" C2-empty items)
[ "$r" = ok ] || f="$f items []: $r;"
c=$(call C2-missing POST "$PL" "{\"title\":\"C2 $TOK\",\"estimatedDurationMinutes\":60,\"startDateTime\":\"$FUT_S\",\"endDateTime\":\"$FUT_E\",\"priorityOrder\":1}")
r=$(problem 400 "$c" C2-missing items); [ "$r" = ok ] || f="$f items missing: $r;"
for ty in TASK HABIT LEARNING_RESOURCE; do
  c=$(call "C2-unk-$ty" POST "$PL" "$(body "C2 $TOK" "[{\"sourceType\":\"$ty\",\"sourceId\":999999999}]" "$FUT_S" "$FUT_E")")
  r=$(problem 400 "$c" "C2-unk-$ty" ""); [ "$r" = ok ] || f="$f unknown $ty: $r;"
done
TD=$(mk C2-t "$BASE/api/tasks" "{\"title\":\"C2 deleted task $TOK\"}")
c=$(call C2-tdel DELETE "$BASE/api/tasks/$TD"); [ "$c" = 204 ] || f="$f task delete $c;"
c=$(call C2-del POST "$PL" "$(body "C2 $TOK" "[{\"sourceType\":\"TASK\",\"sourceId\":$T},{\"sourceType\":\"TASK\",\"sourceId\":$TD}]" "$FUT_S" "$FUT_E")")
r=$(problem 400 "$c" C2-del "items[1].sourceId"); [ "$r" = ok ] || { r2=$(problem 400 "$c" C2-del ""); [ "$r2" = ok ] && echo "=== C2: deleted-item error field is not items[1].sourceId (info)" >> "$LOG" || f="$f deleted task: $r;"; }
call C2-after GET "$PL?group=all" >/dev/null
[ "$(jq_ C2-after length)" = "$n0" ] || f="$f plan count $n0 -> $(jq_ C2-after length) after rejected creates;"
report C2 "$f"

# ---------- C3: AC-US4-3 BR-11 end <= start rejected naming endDateTime
f=""
c=$(call C3-eq POST "$PL" "$(body "C3 $TOK" "$ITEMS3" "$FUT_S" "$FUT_S")"); r=$(problem 400 "$c" C3-eq endDateTime)
[ "$r" = ok ] || f="$f end=start: $r;"
c=$(call C3-lt POST "$PL" "$(body "C3 $TOK" "$ITEMS3" "$FUT_E" "$FUT_S")"); r=$(problem 400 "$c" C3-lt endDateTime)
[ "$r" = ok ] || f="$f end<start: $r;"
c=$(call C3-put PUT "$PL/$P1" "{\"title\":\"C3 $TOK\",\"estimatedDurationMinutes\":60,\"startDateTime\":\"$FUT_E\",\"endDateTime\":\"$FUT_S\",\"priorityOrder\":1}")
r=$(problem 400 "$c" C3-put endDateTime); [ "$r" = ok ] || f="$f PUT end<start: $r;"
call C3-get GET "$PL/$P1" >/dev/null
[ "$(jq_ C3-get .title)" = "C1 plan $TOK" ] || f="$f plan changed by rejected PUT;"
report C3 "$f"

# ---------- C4: AC-US4-4 toggle done/undone -> flag saved, progress = done/total
f=""
c=$(call C4-p POST "$PL" "$(body "C4 plan $TOK" "$ITEMS3" "$WIN_S" "$WIN_E")"); P4=$(jq_ C4-p .id)
[ "$c" = 201 ] || f="$f create $c;"
IH=$(item_id C4-p HABIT); IL=$(item_id C4-p LEARNING_RESOURCE)
c=$(call C4-h1 PUT "$PL/$P4/items/$IH" '{"done":true}'); [ "$c" = 200 ] || f="$f habit done $c;"
[ "$(jq_ C4-h1 "[.doneItems==1, .totalItems==3, .progressPercent==33, ([.items[]|select(.id==$IH)|.done][0]==true)]|all")" = true ] || f="$f after 1 done: $(head -c 400 "$TMP/C4-h1.body");"
call C4-g1 GET "$PL/$P4" >/dev/null
[ "$(jq_ C4-g1 "[.progressPercent==33, ([.items[]|select(.id==$IH)|.done][0]==true), ([.items[]|select(.id!=$IH)|.done]|all(.==false))]|all")" = true ] || f="$f GET after 1 done: $(head -c 400 "$TMP/C4-g1.body");"
c=$(call C4-h0 PUT "$PL/$P4/items/$IH" '{"done":false}'); [ "$c" = 200 ] || f="$f habit undone $c;"
[ "$(jq_ C4-h0 '[.doneItems==0, .progressPercent==0]|all')" = true ] || f="$f after undo: $(head -c 300 "$TMP/C4-h0.body");"
call C4-g0 GET "$PL/$P4" >/dev/null
[ "$(jq_ C4-g0 "[.progressPercent==0, ([.items[]|select(.id==$IH)|.done][0]==false)]|all")" = true ] || f="$f GET after undo: $(head -c 300 "$TMP/C4-g0.body");"
# two-item plan: 1/2 -> 50
c=$(call C4-q POST "$PL" "$(body "C4b plan $TOK" "[{\"sourceType\":\"HABIT\",\"sourceId\":$H},{\"sourceType\":\"LEARNING_RESOURCE\",\"sourceId\":$L}]" "$WIN_S" "$WIN_E")"); P4B=$(jq_ C4-q .id)
c=$(call C4-q1 PUT "$PL/$P4B/items/$(item_id C4-q HABIT)" '{"done":true}')
[ "$(jq_ C4-q1 '[.doneItems==1, .totalItems==2, .progressPercent==50]|all')" = true ] || f="$f 1/2: $(head -c 300 "$TMP/C4-q1.body");"
report C4 "$f"

# ---------- C5: AC-US4-5 BR-13 / FR-07.5 propagation
f=""
call C5-h-before GET "$BASE/api/habits/$H" >/dev/null; call C5-hc-before GET "$BASE/api/habits/$H/completions" >/dev/null
call C5-l-before GET "$BASE/api/learning-cards/$L" >/dev/null
c=$(call C5-hd PUT "$PL/$P4/items/$IH" '{"done":true}'); [ "$c" = 200 ] || f="$f habit item $c;"
c=$(call C5-ld PUT "$PL/$P4/items/$IL" '{"done":true}'); [ "$c" = 200 ] || f="$f card item $c;"
call C5-h-after GET "$BASE/api/habits/$H" >/dev/null; call C5-hc-after GET "$BASE/api/habits/$H/completions" >/dev/null
call C5-l-after GET "$BASE/api/learning-cards/$L" >/dev/null
[ "$(jq_ C5-h-before tojson)" = "$(jq_ C5-h-after tojson)" ] || f="$f habit changed: $(head -c 300 "$TMP/C5-h-after.body");"
[ "$(jq_ C5-hc-before tojson)" = "$(jq_ C5-hc-after tojson)" ] || f="$f habit completions changed;"
[ "$(jq_ C5-l-before tojson)" = "$(jq_ C5-l-after tojson)" ] || f="$f card changed: $(head -c 300 "$TMP/C5-l-after.body");"
# undo them again so later checks start clean
call C5-hu PUT "$PL/$P4/items/$IH" '{"done":false}' >/dev/null; call C5-lu PUT "$PL/$P4/items/$IL" '{"done":false}' >/dev/null
call C5-t-before GET "$BASE/api/tasks/$T" >/dev/null
[ "$(jq_ C5-t-before .status)" != DONE ] || f="$f task already DONE before;"
IT=$(item_id C4-p TASK)
c=$(call C5-td PUT "$PL/$P4/items/$IT" '{"done":true}'); [ "$c" = 200 ] || f="$f task item $c;"
call C5-t-after GET "$BASE/api/tasks/$T" >/dev/null
[ "$(jq_ C5-t-after .status)" = DONE ] || f="$f task status $(jq_ C5-t-after .status) (want DONE);"
r=$(near_now "$(jq_ C5-t-after .completedAt)"); [ "$r" = ok ] || f="$f task completedAt: $r;"
CA=$(jq_ C5-t-after .completedAt)
c=$(call C5-tu PUT "$PL/$P4/items/$IT" '{"done":false}'); [ "$c" = 200 ] || f="$f task item undo $c;"
call C5-t-undo GET "$BASE/api/tasks/$T" >/dev/null
[ "$(jq_ C5-t-undo .status)" = DONE ] || f="$f un-tick changed task to $(jq_ C5-t-undo .status);"
[ "$(jq_ C5-t-undo .completedAt)" = "$CA" ] || f="$f un-tick changed completedAt;"
report C5 "$f"

# ---------- C6: AC-US4-6 BR-12 future start -> NOT_STARTED, no rest time (also when all done)
f=""
call C6-g GET "$PL/$P1" >/dev/null
[ "$(jq_ C6-g '[.status=="NOT_STARTED", .restSeconds==null]|all')" = true ] || f="$f status/rest: $(jq_ C6-g '{status,restSeconds}|tojson');"
for ty in TASK HABIT LEARNING_RESOURCE; do call "C6-d-$ty" PUT "$PL/$P1/items/$(item_id C1 $ty)" '{"done":true}' >/dev/null; done
call C6-all GET "$PL/$P1" >/dev/null
[ "$(jq_ C6-all '[.status=="NOT_STARTED", .restSeconds==null, .progressPercent==100]|all')" = true ] || f="$f all done before start: $(jq_ C6-all '{status,restSeconds,progressPercent}|tojson');"
report C6 "$f"

# ---------- C7: AC-US4-7 inside window -> IN_PROGRESS, rest time = end - now, decreasing; all done -> COMPLETED
f=""
c=$(call C7-p POST "$PL" "$(body "C7 plan $TOK" "[{\"sourceType\":\"HABIT\",\"sourceId\":$H},{\"sourceType\":\"LEARNING_RESOURCE\",\"sourceId\":$L}]" "$WIN_S" "$WIN_E")"); P7=$(jq_ C7-p .id)
[ "$c" = 201 ] || f="$f create $c;"
call C7-g1 GET "$PL/$P7" >/dev/null; now1=$(date +%s); r1=$(jq_ C7-g1 .restSeconds)
[ "$(jq_ C7-g1 .status)" = IN_PROGRESS ] || f="$f status $(jq_ C7-g1 .status);"
want=$(( $(date -d "$WIN_E" +%s) - now1 ))
[[ "$r1" =~ ^[0-9]+$ ]] && (( r1 - want <= 3 && want - r1 <= 3 )) || f="$f restSeconds '$r1' vs ~$want;"
sleep 3
call C7-g2 GET "$PL/$P7" >/dev/null; r2=$(jq_ C7-g2 .restSeconds)
[[ "$r2" =~ ^[0-9]+$ ]] && (( r2 < r1 )) || f="$f restSeconds not decreasing ($r1 -> $r2);"
echo "=== C7: restSeconds $r1 then $r2 (3 s apart), expected ~$want" >> "$LOG"
for ty in HABIT LEARNING_RESOURCE; do c=$(call "C7-d-$ty" PUT "$PL/$P7/items/$(item_id C7-p $ty)" '{"done":true}'); done
[ "$(jq_ C7-d-LEARNING_RESOURCE '[.status=="COMPLETED", .progressPercent==100]|all')" = true ] || f="$f all done in window: $(jq_ C7-d-LEARNING_RESOURCE '{status,progressPercent}|tojson');"
c=$(call C7-u PUT "$PL/$P7/items/$(item_id C7-p HABIT)" '{"done":false}')
[ "$(jq_ C7-u '.status')" = IN_PROGRESS ] || f="$f un-tick in window: status $(jq_ C7-u .status) (want IN_PROGRESS, FR-08.4);"
report C7 "$f"

# ---------- C8: AC-US4-8 NFR-4 status computed on read from stored times + flags + now
f=""
c=$(call C8-p POST "$PL" "$(body "C8 past plan $TOK" "[{\"sourceType\":\"HABIT\",\"sourceId\":$H}]" "$PST_S" "$PST_E")"); P8=$(jq_ C8-p .id)
[ "$c" = 201 ] || f="$f past create $c (spec: allowed);"
[ "$(jq_ C8-p '[.status=="COMPLETED", .restSeconds==null, .progressPercent==0]|all')" = true ] || f="$f past plan: $(jq_ C8-p '{status,restSeconds,progressPercent}|tojson');"
call C8-g1 GET "$PL/$P8" >/dev/null; call C8-g2 GET "$PL/$P8" >/dev/null
[ "$(jq_ C8-g1 tojson)" = "$(jq_ C8-g2 tojson)" ] || f="$f two reads differ;"
# moving stored times changes the status with no other action (computed, not stored)
c=$(call C8-mv PUT "$PL/$P8" "{\"title\":\"C8 past plan $TOK\",\"estimatedDurationMinutes\":60,\"startDateTime\":\"$WIN_S\",\"endDateTime\":\"$WIN_E\",\"priorityOrder\":1}")
[ "$(jq_ C8-mv '.status')" = IN_PROGRESS ] || f="$f moved into window: $(jq_ C8-mv .status);"
c=$(call C8-mv2 PUT "$PL/$P8" "{\"title\":\"C8 past plan $TOK\",\"estimatedDurationMinutes\":60,\"startDateTime\":\"$FUT_S\",\"endDateTime\":\"$FUT_E\",\"priorityOrder\":1}")
[ "$(jq_ C8-mv2 '[.status=="NOT_STARTED", .restSeconds==null]|all')" = true ] || f="$f moved to future: $(jq_ C8-mv2 '{status,restSeconds}|tojson');"
call C8-g3 GET "$PL/$P8" >/dev/null
[ "$(jq_ C8-g3 .status)" = NOT_STARTED ] || f="$f GET after move: $(jq_ C8-g3 .status);"
report C8 "$f"

# ---------- C9: AC-US4-10 grouping + ordering (contract listPlans)
f=""
call C9-a GET "$PL?group=active" >/dev/null; call C9-c GET "$PL?group=completed" >/dev/null
call C9-all GET "$PL?group=all" >/dev/null; call C9-def GET "$PL" >/dev/null
[ "$(jq_ C9-a 'all(.[]; .status=="NOT_STARTED" or .status=="IN_PROGRESS")')" = true ] || f="$f active has other status;"
[ "$(jq_ C9-c 'all(.[]; .status=="COMPLETED")')" = true ] || f="$f completed has other status;"
[ "$(jq_ C9-a "any(.[]; .id==$P1) and any(.[]; .id==$P4)")" = true ] || f="$f active missing plans $P1/$P4;"
ordA=$(jq -c '[.[]|[.priorityOrder,.startDateTime]]' "$TMP/C9-a.body")
# compare starts as epoch to be offset-safe
okA=true; prev=""
while IFS=$'\t' read -r p s; do e=$(date -d "$s" +%s); cur=$(printf '%08d%012d' "$p" "$e"); [ -z "$prev" ] || [[ ! "$cur" < "$prev" ]] || okA=false; prev=$cur; done \
  < <(jq -r '.[]|[.priorityOrder,.startDateTime]|@tsv' "$TMP/C9-a.body")
[ "$okA" = true ] || f="$f active not by priorityOrder then start: $ordA;"
okC=true; prev=""
while read -r s; do e=$(date -d "$s" +%s); [ -z "$prev" ] || (( e <= prev )) || okC=false; prev=$e; done < <(jq -r '.[].endDateTime' "$TMP/C9-c.body")
[ "$okC" = true ] || f="$f completed not by end desc;"
[ "$(jq -cn --slurpfile a "$TMP/C9-a.body" --slurpfile c "$TMP/C9-c.body" '[$a[0][].id]+[$c[0][].id]')" = "$(jq -c '[.[].id]' "$TMP/C9-all.body")" ] || f="$f all != active+completed;"
[ "$(jq -c '[.[].id]' "$TMP/C9-def.body")" = "$(jq -c '[.[].id]' "$TMP/C9-all.body")" ] || f="$f no group != all;"
[ "$(jq_ C9-all 'all(.[]; has("progressPercent") and has("items") and has("status") and (.items|all(has("done")))) ')" = true ] || f="$f a listed plan lacks progress/items/status;"
[ "$(jq_ C9-a "[.[]|select(.id==$P4)|.restSeconds|type][0]")" = number ] || f="$f in-window plan has no restSeconds in list;"
# own ordering probe: priorities 3 and 2 with same start, 2 must come first
c=$(call C9-p3 POST "$PL" "$(body "C9 prio3 $TOK" "[{\"sourceType\":\"HABIT\",\"sourceId\":$H}]" "$FUT_S" "$FUT_E" 60 3)"); A3=$(jq_ C9-p3 .id)
c=$(call C9-p2 POST "$PL" "$(body "C9 prio2 $TOK" "[{\"sourceType\":\"HABIT\",\"sourceId\":$H}]" "$FUT_S" "$FUT_E" 60 2)"); A2=$(jq_ C9-p2 .id)
call C9-a2 GET "$PL?group=active" >/dev/null
[ "$(jq_ C9-a2 "([.[].id]|index($A2)) < ([.[].id]|index($A3))")" = true ] || f="$f priority 2 not before 3: $(jq -c '[.[]|[.id,.priorityOrder]]' "$TMP/C9-a2.body");"
report C9 "$f"

# ---------- C10: AC-US4-11 completed history shows percentage done
f=""
c=$(call C10-p POST "$PL" "$(body "C10 past $TOK" "$ITEMS3" "$PST_S" "$PST_E")"); P10=$(jq_ C10-p .id)
c=$(call C10-d PUT "$PL/$P10/items/$(item_id C10-p HABIT)" '{"done":true}')
call C10-c GET "$PL?group=completed" >/dev/null
[ "$(jq_ C10-c "[.[]|select(.id==$P10)|[.status==\"COMPLETED\", .doneItems==1, .totalItems==3, .progressPercent==33]|all][0]")" = true ] || f="$f history entry: $(jq -c ".[]|select(.id==$P10)|{status,doneItems,totalItems,progressPercent}" "$TMP/C10-c.body");"
[ "$(jq_ C10-c "[.[]|select(.id==$P8)]|length")" = 0 ] || f="$f moved-to-future plan still in completed;"
report C10 "$f"

# ---------- C11: FR-07.6 deleted source stays as removed item, still counted
f=""
T2=$(mk C11-t "$BASE/api/tasks" "{\"title\":\"C11 task $TOK\"}")
c=$(call C11-p POST "$PL" "$(body "C11 plan $TOK" "[{\"sourceType\":\"TASK\",\"sourceId\":$T2},{\"sourceType\":\"HABIT\",\"sourceId\":$H}]" "$FUT_S" "$FUT_E")"); P11=$(jq_ C11-p .id)
c=$(call C11-d PUT "$PL/$P11/items/$(item_id C11-p TASK)" '{"done":true}')
c=$(call C11-tdel DELETE "$BASE/api/tasks/$T2"); [ "$c" = 204 ] || f="$f task delete $c;"
c=$(call C11-g GET "$PL/$P11"); [ "$c" = 200 ] || f="$f GET $c;"
[ "$(jq_ C11-g "[.totalItems==2, .doneItems==1, .progressPercent==50, ([.items[]|select(.sourceType==\"TASK\")|[.sourceRemoved==true, .sourceTitle==\"C11 task $TOK\", .done==true, .sourceId==$T2]|all][0]), ([.items[]|select(.sourceType==\"HABIT\")|.sourceRemoved][0]==false)]|all")" = true ] || f="$f after task delete: $(head -c 500 "$TMP/C11-g.body");"
c=$(call C11-tget GET "$BASE/api/tasks/$T2"); [ "$c" = 404 ] || f="$f deleted task GET $c (want 404);"
H2=$(mk C11-h "$BASE/api/habits" "{\"name\":\"C11 habit $TOK\",\"frequency\":\"WEEKLY\"}")
L2=$(mk C11-l "$BASE/api/learning-cards" "{\"title\":\"C11 card $TOK\"}")
c=$(call C11-p2 POST "$PL" "$(body "C11b plan $TOK" "[{\"sourceType\":\"HABIT\",\"sourceId\":$H2},{\"sourceType\":\"LEARNING_RESOURCE\",\"sourceId\":$L2}]" "$FUT_S" "$FUT_E")"); P11B=$(jq_ C11-p2 .id)
call C11-hdel DELETE "$BASE/api/habits/$H2" >/dev/null; call C11-ldel DELETE "$BASE/api/learning-cards/$L2" >/dev/null
call C11-g2 GET "$PL/$P11B" >/dev/null
[ "$(jq_ C11-g2 "[.totalItems==2, all(.items[]; .sourceRemoved==true), ([.items[]|.sourceTitle]|sort)==([\"C11 card $TOK\",\"C11 habit $TOK\"])]|all")" = true ] || f="$f after habit/card delete: $(head -c 500 "$TMP/C11-g2.body");"
report C11 "$f"

# ---------- C12: AC-US4-12 BR-14 delete plan; sources unchanged
f=""
c=$(call C12-p POST "$PL" "$(body "C12 plan $TOK" "$ITEMS3" "$FUT_S" "$FUT_E")"); P12=$(jq_ C12-p .id); I12=$(item_id C12-p HABIT)
call C12-t0 GET "$BASE/api/tasks/$T" >/dev/null; call C12-h0 GET "$BASE/api/habits/$H" >/dev/null; call C12-l0 GET "$BASE/api/learning-cards/$L" >/dev/null
c=$(call C12-del DELETE "$PL/$P12"); [ "$c" = 204 ] || f="$f DELETE $c (want 204);"
c=$(call C12-get GET "$PL/$P12"); r=$(problem 404 "$c" C12-get ""); [ "$r" = ok ] || f="$f GET after delete: $r;"
c=$(call C12-item PUT "$PL/$P12/items/$I12" '{"done":true}'); [ "$c" = 404 ] || f="$f item PUT after delete $c;"
call C12-list GET "$PL?group=all" >/dev/null
[ "$(jq_ C12-list "[.[]|select(.id==$P12)]|length")" = 0 ] || f="$f still listed;"
c=$(call C12-del2 DELETE "$PL/$P12"); [ "$c" = 404 ] || f="$f second DELETE $c (want 404);"
call C12-t1 GET "$BASE/api/tasks/$T" >/dev/null; call C12-h1 GET "$BASE/api/habits/$H" >/dev/null; call C12-l1 GET "$BASE/api/learning-cards/$L" >/dev/null
for k in t h l; do [ "$(jq_ C12-${k}0 tojson)" = "$(jq_ C12-${k}1 tojson)" ] || f="$f source $k changed;"; done
report C12 "$f"

# ---------- C13: FR-07.1 / FR-07.2 field limits and duplicates (swagger PlanCreate)
f=""
one="[{\"sourceType\":\"HABIT\",\"sourceId\":$H}]"
c=$(call C13-t0 POST "$PL" "$(body "   " "$one" "$FUT_S" "$FUT_E")"); r=$(problem 400 "$c" C13-t0 title); [ "$r" = ok ] || f="$f blank title: $r;"
c=$(call C13-t201 POST "$PL" "$(body "$(long x 201)" "$one" "$FUT_S" "$FUT_E")"); r=$(problem 400 "$c" C13-t201 title); [ "$r" = ok ] || f="$f 201-char title: $r;"
c=$(call C13-t200 POST "$PL" "$(body "$(long y 200)" "$one" "$FUT_S" "$FUT_E")"); [ "$c" = 201 ] || f="$f 200-char title $c;"
c=$(call C13-tm POST "$PL" "{\"items\":$one,\"estimatedDurationMinutes\":60,\"startDateTime\":\"$FUT_S\",\"endDateTime\":\"$FUT_E\",\"priorityOrder\":1}")
r=$(problem 400 "$c" C13-tm title); [ "$r" = ok ] || f="$f missing title: $r;"
for d in 0 100001; do c=$(call "C13-d$d" POST "$PL" "$(body "C13 $TOK" "$one" "$FUT_S" "$FUT_E" $d)"); r=$(problem 400 "$c" "C13-d$d" estimatedDurationMinutes); [ "$r" = ok ] || f="$f duration $d: $r;"; done
for d in 1 100000; do c=$(call "C13-d$d" POST "$PL" "$(body "C13 $TOK" "$one" "$FUT_S" "$FUT_E" $d)"); [ "$c" = 201 ] || f="$f duration $d -> $c;"; done
c=$(call C13-p0 POST "$PL" "$(body "C13 $TOK" "$one" "$FUT_S" "$FUT_E" 60 0)"); r=$(problem 400 "$c" C13-p0 priorityOrder); [ "$r" = ok ] || f="$f priorityOrder 0: $r;"
c=$(call C13-dup POST "$PL" "$(body "C13 $TOK" "[{\"sourceType\":\"HABIT\",\"sourceId\":$H},{\"sourceType\":\"HABIT\",\"sourceId\":$H}]" "$FUT_S" "$FUT_E")")
r=$(problem 400 "$c" C13-dup ""); [ "$r" = ok ] || f="$f duplicate item: $r;"
c=$(call C13-ty POST "$PL" "$(body "C13 $TOK" "[{\"sourceType\":\"NOPE\",\"sourceId\":$H}]" "$FUT_S" "$FUT_E")")
r=$(problem 400 "$c" C13-ty ""); [ "$r" = ok ] || f="$f unknown sourceType: $r;"
report C13 "$f"

# ---------- C14: updatePlan / setPlanItemDone edges (contract 200/400/404)
f=""
c=$(call C14-u PUT "$PL/$P4" "{\"title\":\"C14 renamed $TOK\",\"estimatedDurationMinutes\":120,\"startDateTime\":\"$WIN_S\",\"endDateTime\":\"$(at '+2 hours')\",\"priorityOrder\":5}")
[ "$c" = 200 ] || f="$f PUT $c;"
[ "$(jq_ C14-u "[.title==\"C14 renamed $TOK\", .estimatedDurationMinutes==120, .priorityOrder==5]|all")" = true ] || f="$f PUT body: $(head -c 300 "$TMP/C14-u.body");"
call C14-bef GET "$PL/$P4" >/dev/null
[ "$(jq -c '[.items[]|[.id,.sourceType,.sourceId]]' "$TMP/C4-p.body")" = "$(jq -c '[.items[]|[.id,.sourceType,.sourceId]]' "$TMP/C14-bef.body")" ] || f="$f items changed by PUT;"
[ "$(jq_ C14-bef "[.items[]|select(.id==$IT)|.done][0]")" = false ] || f="$f task item flag changed by PUT;"
c=$(call C14-u404 PUT "$PL/999999999" "{\"title\":\"x\",\"estimatedDurationMinutes\":1,\"startDateTime\":\"$FUT_S\",\"endDateTime\":\"$FUT_E\",\"priorityOrder\":1}")
r=$(problem 404 "$c" C14-u404 ""); [ "$r" = ok ] || f="$f PUT unknown: $r;"
c=$(call C14-g404 GET "$PL/999999999"); r=$(problem 404 "$c" C14-g404 ""); [ "$r" = ok ] || f="$f GET unknown: $r;"
c=$(call C14-nodone PUT "$PL/$P4/items/$IH" '{}'); r=$(problem 400 "$c" C14-nodone done); [ "$r" = ok ] || f="$f missing done: $r;"
c=$(call C14-i404 PUT "$PL/999999999/items/$IH" '{"done":true}'); r=$(problem 404 "$c" C14-i404 ""); [ "$r" = ok ] || f="$f unknown plan item: $r;"
c=$(call C14-other PUT "$PL/$P7/items/$IH" '{"done":true}'); r=$(problem 404 "$c" C14-other ""); [ "$r" = ok ] || f="$f item of other plan: $r;"
report C14 "$f"

# ---------- C15: swagger
c=$(call C15 GET "$BASE/v3/api-docs")
check_doc() { jq -r '
  def op(p; m): .paths[p][m].operationId;
  [ ([["/api/plans","get","listPlans"],["/api/plans","post","createPlan"],["/api/plans/{id}","get","getPlan"],
      ["/api/plans/{id}","put","updatePlan"],["/api/plans/{id}","delete","deletePlan"],["/api/plans/{id}/items/{itemId}","put","setPlanItemDone"]][]
      as [$p,$m,$o] | if op($p;$m) != $o then $o else empty end),
    (if (.paths["/api/plans"].post.responses // {} | has("201")) then empty else "createPlan no 201" end),
    (if (.paths["/api/plans/{id}"].delete.responses // {} | has("204")) then empty else "deletePlan no 204" end),
    (if .paths["/api/plans"].get.parameters[0].schema.enum == ["active","completed","all"] then empty else "group enum" end),
    ( ["Plan","PlanItem","PlanCreate","PlanUpdate","PlanItemRef","PlanItemUpdate"][]
      as $s | if (.components.schemas | has($s)) then empty else "schema \($s) missing" end ),
    (if .components.schemas.PlanStatus.enum != ["NOT_STARTED","IN_PROGRESS","COMPLETED"] then "PlanStatus" else empty end),
    (if .components.schemas.PlanSourceType.enum != ["TASK","HABIT","LEARNING_RESOURCE"] then "PlanSourceType" else empty end)
  ] | if length == 0 then "ok" else join("; ") end' "$1" 2>/dev/null || echo "not JSON"; }
f=""
[ "$c" = 200 ] || f="$f api-docs $c;"
r=$(check_doc "$TMP/C15.body"); [ "$r" = ok ] || f="$f live: $r;"
for p in backend/target/openapi.json loops/backend-dev/outputs/openapi.json; do
  if [ -f "$p" ]; then r=$(check_doc "$p"); else r=missing; fi
  echo "=== C15: $p -> $r" >> "$LOG"
  [ "$r" = ok ] || f="$f $p: $r;"
done
report C15 "$f"
