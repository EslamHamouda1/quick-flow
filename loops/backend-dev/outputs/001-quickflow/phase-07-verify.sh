#!/usr/bin/env bash
# phase-07 (US5 See everything on the Dashboard) API checks. Usage: phase-07-verify.sh <attempt-folder>
# Needs only bash, curl, jq and GNU date. Writes only inside the attempt folder. Expects a fresh test database.
set -u
OUT="${1:?attempt folder}"
BASE="http://localhost:8080"
DB="$BASE/api/dashboard"
LOG="$OUT/curl.log"
TMP="$(mktemp -d "$OUT/.verify.XXXX")"
trap 'rm -rf "$TMP"' EXIT
export TZ=Africa/Cairo
TODAY=$(date +%F); YDAY=$(date -d yesterday +%F)

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
jq_() { local n="$1"; shift; jq -r "$@" "$TMP/$n.body" 2>/dev/null || echo "not-JSON"; }  # name [opts] filter
jqa() { jq -r "$@" 2>/dev/null || echo "not-JSON"; }
at() { date -d "$1" -Iseconds; }
report() { if [ -z "$2" ]; then echo "PASS $1"; else echo "FAIL $1:$2"; fi; }
mk() { local c; c=$(call "$1" POST "$2" "$3"); if [ "$c" = 201 ]; then jq_ "$1" '.id'; else echo ""; fi; }
ok() { local c; c=$(call "$@"); [ "${c:0:1}" = 2 ] || echo " $1 status $c;"; }  # name method url [body]
dash() { local c; c=$(call "$1" GET "$DB"); [ "$c" = 200 ] || echo " $1 status $c;"; }
ids() { jq_ "$1" "[.$2[].id]|sort|tojson"; }                        # name field -> sorted id list
set_() { printf '%s\n' "$@" | jq -s -c 'map(tonumber)|sort'; }       # ids -> sorted json list
echo "=== run started $(date -Iseconds), today $TODAY" >> "$LOG"

# ---------- C1: contract + empty database
f=""
f="$f$(dash C1)"
[[ "$(ctype C1)" == application/json* ]] || f="$f content-type '$(ctype C1)';"
r=$(jq_ C1 '[(.dueToday,.overdue,.completedToday,.habits,.activePlans)|(type=="array" and length==0)]
  + [.taskCompletionPercent==0, .taskCounts=={"total":0,"done":0}, .habitCounts=={"active":0,"completedToday":0},
     .planCounts=={"notStarted":0,"inProgress":0,"completed":0},
     .learning=={"notStarted":0,"inProgress":0,"completed":0,"milestonesDone":0,"milestonesTotal":0},
     (keys|sort)==(["activePlans","completedToday","dueToday","habitCounts","habits","learning","now","overdue","planCounts","taskCompletionPercent","taskCounts"])]|all')
[ "$r" = true ] || f="$f empty body: $(head -c 600 "$TMP/C1.body");"
n=$(jq_ C1 .now)
if [[ "$n" =~ [+-][0-9]{2}:[0-9]{2}$|Z$ ]]; then
  d=$(( $(date +%s) - $(date -d "$n" +%s) )); (( d < 300 && d > -300 )) || f="$f now '$n' not near now;"
else f="$f now '$n' has no offset;"; fi
c=$(call C1-docs GET "$BASE/v3/api-docs")
r=$(jq_ C1-docs '[.paths["/api/dashboard"].get.operationId=="getDashboard", (.paths["/api/dashboard"].get.tags|index("dashboard")!=null),
  (.components.schemas|has("Dashboard") and has("TaskCounts") and has("HabitCounts") and has("PlanCounts") and has("LearningSnapshot"))]|all')
[ "$r" = true ] || f="$f api-docs ($c) missing getDashboard/tag/schemas;"
report C1 "$f"

# ---------- C2: AC-US5-1 task groups
f=""
tk() { mk "$1" "$BASE/api/tasks" "$2"; }
A=$(tk C2-A "{\"title\":\"A due today\",\"dueDate\":\"$TODAY\"}")
B=$(tk C2-B "{\"title\":\"B due yesterday\",\"dueDate\":\"$YDAY\"}")
C=$(tk C2-C "{\"title\":\"C done due yesterday\",\"status\":\"DONE\",\"dueDate\":\"$YDAY\"}")
D=$(tk C2-D '{"title":"D no due date"}'); f="$f$(ok C2-Dc POST "$BASE/api/tasks/$D/complete")"
E=$(tk C2-E "{\"title\":\"E due today\",\"dueDate\":\"$TODAY\"}")
f="$f$(ok C2-Eu PUT "$BASE/api/tasks/$E" "{\"title\":\"E due today\",\"status\":\"DONE\",\"priority\":\"MEDIUM\",\"dueDate\":\"$TODAY\"}")"
F=$(tk C2-F "{\"title\":\"F archived today\",\"dueDate\":\"$TODAY\"}"); f="$f$(ok C2-Fa POST "$BASE/api/tasks/$F/archive")"
G=$(tk C2-G "{\"title\":\"G archived yesterday\",\"dueDate\":\"$YDAY\"}"); f="$f$(ok C2-Ga POST "$BASE/api/tasks/$G/archive")"
H=$(tk C2-H '{"title":"H done archived"}'); f="$f$(ok C2-Hc POST "$BASE/api/tasks/$H/complete")$(ok C2-Ha POST "$BASE/api/tasks/$H/archive")"
I=$(tk C2-I "{\"title\":\"I deleted\",\"dueDate\":\"$TODAY\"}"); f="$f$(ok C2-Id DELETE "$BASE/api/tasks/$I")"
for v in A B C D E F G H I; do [ -n "${!v}" ] || f="$f create $v failed;"; done
f="$f$(dash C2)"
[ "$(ids C2 dueToday)" = "$(set_ "$A" "$E")" ] || f="$f dueToday $(ids C2 dueToday) vs [A=$A,E=$E];"
[ "$(ids C2 overdue)" = "$(set_ "$B")" ] || f="$f overdue $(ids C2 overdue) vs [B=$B];"
[ "$(jq_ C2 '[.overdue[].overdue]|all')" = true ] || f="$f overdue flag not true;"
# completedToday = non-archived DONE tasks whose completedAt falls on today (app zone): D, E, and C if created DONE today
call C2-list GET "$BASE/api/tasks?status=DONE" >/dev/null
exp=(); for t in $(jq_ C2-list '.[]|"\(.id)=\(.completedAt)"'); do
  [ "$(date -d "${t#*=}" +%F 2>/dev/null)" = "$TODAY" ] && exp+=("${t%%=*}"); done
[ "$(ids C2 completedToday)" = "$(set_ "${exp[@]}")" ] || f="$f completedToday $(ids C2 completedToday) vs today-completed ${exp[*]};"
r=$(jq_ C2 "[.completedToday[].id]|(index($D)!=null and index($E)!=null)"); [ "$r" = true ] || f="$f D/E missing from completedToday;"
r=$(jq_ C2 "[(.dueToday,.overdue,.completedToday)[].id]|map(select(.==$F or .==$G or .==$H or .==$I))|length"); [ "$r" = 0 ] || f="$f archived/deleted task listed;"
for t in $(jq_ C2 '[(.dueToday,.overdue,.completedToday)[].id]|unique|.[]'); do
  call "C2-get-$t" GET "$BASE/api/tasks/$t" >/dev/null
  [ "$(jqa --argjson i "$t" -c '[(.dueToday,.overdue,.completedToday)[]|select(.id==$i)][0]' "$TMP/C2.body")" = "$(jqa -c . "$TMP/C2-get-$t.body")" ] || f="$f task $t differs from GET;"
done
report C2 "$f"

# ---------- C3: AC-US5-3 completion percent, counts match the data
f=""
call C3-all GET "$BASE/api/tasks" >/dev/null
tot=$(jq_ C3-all length); dn=$(jq_ C3-all '[.[]|select(.status=="DONE")]|length')
f="$f$(dash C3)"
[ "$(jq_ C3 '.taskCounts|"\(.total)/\(.done)"')" = "$tot/$dn" ] || f="$f taskCounts $(jq_ C3 -c .taskCounts) vs list $tot/$dn;"
[ "$tot/$dn" = "5/3" ] || f="$f list $tot/$dn vs expected 5/3;"
[ "$(jq_ C3 .taskCompletionPercent)" = 60 ] || f="$f percent $(jq_ C3 .taskCompletionPercent) vs 60;"
tk C3-x '{"title":"C3 extra 1"}' >/dev/null; f="$f$(dash C3-b)"
[ "$(jq_ C3-b '"\(.taskCounts.total)/\(.taskCounts.done)/\(.taskCompletionPercent)"')" = "6/3/50" ] || f="$f after +1: $(jq_ C3-b -c '[.taskCounts,.taskCompletionPercent]') vs 6/3/50;"
tk C3-y '{"title":"C3 extra 2"}' >/dev/null; f="$f$(dash C3-c)"
[ "$(jq_ C3-c '"\(.taskCounts.total)/\(.taskCounts.done)/\(.taskCompletionPercent)"')" = "7/3/42" ] || f="$f after +2: $(jq_ C3-c -c '[.taskCounts,.taskCompletionPercent]') vs 7/3/42;"
report C3 "$f"

# ---------- C4: AC-US5-2 habits
f=""
H1=$(mk C4-h1 "$BASE/api/habits" '{"name":"H1 done today","frequency":"DAILY"}')
H2=$(mk C4-h2 "$BASE/api/habits" '{"name":"H2 not done","frequency":"DAILY"}')
H3=$(mk C4-h3 "$BASE/api/habits" '{"name":"H3 inactive","frequency":"DAILY"}')
f="$f$(ok C4-h1c POST "$BASE/api/habits/$H1/completions" '{}')$(ok C4-h3c POST "$BASE/api/habits/$H3/completions" '{}')$(ok C4-h3d POST "$BASE/api/habits/$H3/deactivate")"
f="$f$(dash C4)"
[ "$(ids C4 habits)" = "$(set_ "$H1" "$H2")" ] || f="$f habits $(ids C4 habits) vs [H1=$H1,H2=$H2];"
[ "$(jq_ C4 "[(.habits[]|select(.id==$H1)|.completedToday)==true, (.habits[]|select(.id==$H2)|.completedToday)==false, .habitCounts=={\"active\":2,\"completedToday\":1}]|all")" = true ] \
  || f="$f flags/counts: $(jq_ C4 -c '[.habits[]|{id,completedToday}], .habitCounts');"
for h in $H1 $H2; do
  call "C4-get-$h" GET "$BASE/api/habits/$h" >/dev/null
  [ "$(jqa --argjson i "$h" -c '.habits[]|select(.id==$i)' "$TMP/C4.body")" = "$(jqa -c . "$TMP/C4-get-$h.body")" ] || f="$f habit $h differs from GET;"
done
f="$f$(ok C4-undo DELETE "$BASE/api/habits/$H1/completions/$TODAY")$(dash C4-b)"
[ "$(jq_ C4-b -c .habitCounts)" = '{"active":2,"completedToday":0}' ] || f="$f after undo: $(jq_ C4-b -c .habitCounts);"
report C4 "$f"

# ---------- C5: AC-US5-5 learning snapshot
f=""
card() { mk "$1" "$BASE/api/learning-cards" "{\"title\":\"$2\"}"; }
st() { ok "$1" PUT "$BASE/api/learning-cards/$2" "{\"title\":\"$3\",\"status\":\"$4\"}"; }
ms() { # name card title done
  local id; id=$(mk "$1" "$BASE/api/learning-cards/$2/milestones" "{\"title\":\"$3\"}")
  [ -n "$id" ] || { echo " milestone $1 not created;"; return; }
  [ "$4" = true ] && ok "$1-d" PUT "$BASE/api/learning-cards/$2/milestones/$id" "{\"title\":\"$3\",\"done\":true}"; }
L1=$(card C5-l1 "L1"); L2=$(card C5-l2 "L2"); L3=$(card C5-l3 "L3"); L4=$(card C5-l4 "L4")
f="$f$(st C5-s2 "$L2" L2 IN_PROGRESS)$(st C5-s3 "$L3" L3 COMPLETED)$(st C5-s4 "$L4" L4 IN_PROGRESS)"
f="$f$(ms C5-m1 "$L1" m1 true)$(ms C5-m2 "$L1" m2 false)$(ms C5-m3 "$L2" m3 true)$(ms C5-m4 "$L3" m4 false)$(ms C5-m5 "$L4" m5 true)"
f="$f$(dash C5)"
[ "$(jq_ C5 -c .learning)" = '{"notStarted":1,"inProgress":2,"completed":1,"milestonesDone":3,"milestonesTotal":5}' ] || f="$f learning $(jq_ C5 -c .learning) vs 1/2/1 3 of 5;"
f="$f$(ok C5-del DELETE "$BASE/api/learning-cards/$L4")$(dash C5-b)"
[ "$(jq_ C5-b -c .learning)" = '{"notStarted":1,"inProgress":1,"completed":1,"milestonesDone":2,"milestonesTotal":4}' ] || f="$f after delete $(jq_ C5-b -c .learning) vs 1/1/1 2 of 4;"
report C5 "$f"

# ---------- C6: AC-US5-4 plans
f=""
ITEMS="[{\"sourceType\":\"HABIT\",\"sourceId\":$H2},{\"sourceType\":\"LEARNING_RESOURCE\",\"sourceId\":$L1}]"
pl() { mk "$1" "$BASE/api/plans" "{\"title\":\"$1\",\"items\":$ITEMS,\"estimatedDurationMinutes\":60,\"startDateTime\":\"$(at "$2")\",\"endDateTime\":\"$(at "$3")\",\"priorityOrder\":$4}"; }
P1=$(pl C6-p1 '-1 hour' '+1 hour' 2); P2=$(pl C6-p2 '-30 minutes' '+2 hours' 1)
P3=$(pl C6-p3 '+1 hour' '+2 hours' 1); P4=$(pl C6-p4 '-3 hours' '-1 hour' 1)
for v in P1 P2 P3 P4; do [ -n "${!v}" ] || f="$f create $v failed;"; done
f="$f$(dash C6)"
[ "$(jq_ C6 '[.activePlans[].id]|tojson')" = "[$P2,$P1]" ] || f="$f activePlans $(jq_ C6 '[.activePlans[].id]|tojson') vs [$P2,$P1];"
[ "$(jq_ C6 '[.activePlans[]|(.status=="IN_PROGRESS" and (.progressPercent|type)=="number" and (.restSeconds|type)=="number")]|all')" = true ] || f="$f plan fields: $(jq_ C6 -c '[.activePlans[]|{id,status,progressPercent,restSeconds}]');"
[ "$(jq_ C6 -c .planCounts)" = '{"notStarted":1,"inProgress":2,"completed":1}' ] || f="$f planCounts $(jq_ C6 -c .planCounts);"
for p in $P1 $P2; do
  call "C6-get-$p" GET "$BASE/api/plans/$p" >/dev/null
  [ "$(jqa --argjson i "$p" -c '.activePlans[]|select(.id==$i)|del(.restSeconds)' "$TMP/C6.body")" = "$(jqa -c 'del(.restSeconds)' "$TMP/C6-get-$p.body")" ] || f="$f plan $p differs from GET;"
done
r1=$(jq_ C6 "[.activePlans[]|select(.id==$P2)|.restSeconds][0]")
command sleep 3
f="$f$(dash C6-b)"
r2=$(jq_ C6-b "[.activePlans[]|select(.id==$P2)|.restSeconds][0]")
(( r2 < r1 )) || f="$f restSeconds $r1 -> $r2 not decreasing;"
call C6-p2g GET "$BASE/api/plans/$P2" >/dev/null
IT1=$(jq_ C6-p2g '.items[0].id'); IT2=$(jq_ C6-p2g '.items[1].id')
f="$f$(ok C6-t1 PUT "$BASE/api/plans/$P2/items/$IT1" '{"done":true}')$(dash C6-c)"
[ "$(jq_ C6-c "[.activePlans[]|select(.id==$P2)|.progressPercent][0]")" = 50 ] || f="$f after 1/2 tick: $(jq_ C6-c -c '[.activePlans[]|{id,progressPercent}]');"
f="$f$(ok C6-t2 PUT "$BASE/api/plans/$P2/items/$IT2" '{"done":true}')$(dash C6-d)"
[ "$(jq_ C6-d '[.activePlans[].id]|tojson')" = "[$P1]" ] || f="$f after 2/2 activePlans $(jq_ C6-d '[.activePlans[].id]|tojson') vs [$P1];"
[ "$(jq_ C6-d -c .planCounts)" = '{"notStarted":1,"inProgress":1,"completed":2}' ] || f="$f after 2/2 planCounts $(jq_ C6-d -c .planCounts);"
report C6 "$f"
