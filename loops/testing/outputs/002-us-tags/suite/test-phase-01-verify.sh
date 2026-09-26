#!/usr/bin/env bash
# suite test-phase-01 (API contract, 002-us-tags) checks C1-C10. Usage: test-phase-01-verify.sh <attempt-folder>
# Based on the 001-quickflow suite script; adds the task-tag operations (C4) and Task.tags checks (C9).
# Calls only the API; writes only under the attempt folder.
set -u
OUT="${1:?attempt folder}"
BASE="http://localhost:8080"
SWAGGER="loops/backend-dev/outputs/openapi.json"
LOG="$OUT/curl.log"
TIMES="$OUT/times.tsv"
TMP="$(mktemp -d "$OUT/.verify.XXXX")"
trap 'rm -rf "$TMP"' EXIT
: > "$TIMES"
NF=999999999
TODAY=$(TZ=Africa/Cairo date +%F)
YESTERDAY=$(TZ=Africa/Cairo date -d yesterday +%F)
TOMORROW=$(TZ=Africa/Cairo date -d tomorrow +%F)
START=$(TZ=Africa/Cairo date -d '+1 hour' --iso-8601=seconds)
END=$(TZ=Africa/Cairo date -d '+2 hour' --iso-8601=seconds)

# Validator: body file + expected schema ("Task", "[Task]", "Problem", "-" = empty body) -> "ok" or reason
cat > "$TMP/v.py" <<'EOF'
import json, sys, re
from datetime import date, datetime
sw, body, spec, code = sys.argv[1], sys.argv[2], sys.argv[3], int(sys.argv[4])
S = json.load(open(sw))["components"]["schemas"]
S["Problem"] = {"type": "object", "required": ["title", "status"], "extensible": True, "properties": {
    "type": {"type": "string"}, "title": {"type": "string"}, "status": {"type": "integer"},
    "detail": {"type": "string"}, "instance": {"type": "string"},
    "errors": {"type": "array", "items": {"type": "object", "required": ["field", "message"],
               "properties": {"field": {"type": "string"}, "message": {"type": "string"}}}}}}
raw = open(body, "rb").read()
if spec == "-":
    print("ok" if not raw.strip() else f"non-empty body {raw[:80]!r}"); sys.exit()
try:
    b = json.loads(raw)
except Exception as e:
    print(f"not JSON ({e})"); sys.exit()
def res(s):
    while "$ref" in s: s = S[s["$ref"].split("/")[-1]]
    return s
def chk(v, s, p):
    s = res(s)
    t = s.get("type")
    ts = t if isinstance(t, list) else [t] if t else []
    if v is None:
        return None if "null" in ts else f"{p}: null not allowed"
    ts = [x for x in ts if x != "null"]
    t = ts[0] if ts else None
    if t == "object":
        if not isinstance(v, dict): return f"{p}: not an object"
        props = s.get("properties", {})
        miss = [k for k in s.get("required", []) if k not in v]
        if miss: return f"{p}: missing {miss}"
        if not s.get("extensible"):
            extra = [k for k in v if k not in props]
            if extra: return f"{p}: extra {extra}"
        for k, x in v.items():
            if k in props:
                e = chk(x, props[k], f"{p}.{k}")
                if e: return e
        if s is S["Task"]:
            tg = v.get("tags")
            if isinstance(tg, list) and tg != sorted(x.lower() for x in tg):
                return f"{p}.tags: {tg!r} not lower case and alphabetical"
    elif t == "array":
        if not isinstance(v, list): return f"{p}: not an array"
        if "maxItems" in s and len(v) > s["maxItems"]: return f"{p}: {len(v)} items > {s['maxItems']}"
        if "minItems" in s and len(v) < s["minItems"]: return f"{p}: {len(v)} items < {s['minItems']}"
        if s.get("uniqueItems") and len(set(map(json.dumps, v))) != len(v): return f"{p}: items not unique"
        for i, x in enumerate(v):
            e = chk(x, s["items"], f"{p}[{i}]")
            if e: return e
    elif t == "integer":
        if not isinstance(v, int) or isinstance(v, bool): return f"{p}: {v!r} not integer"
        if "minimum" in s and v < s["minimum"]: return f"{p}: {v} < {s['minimum']}"
        if "maximum" in s and v > s["maximum"]: return f"{p}: {v} > {s['maximum']}"
    elif t == "boolean":
        if not isinstance(v, bool): return f"{p}: {v!r} not boolean"
    elif t == "string":
        if not isinstance(v, str): return f"{p}: {v!r} not string"
        if "enum" in s and v not in s["enum"]: return f"{p}: {v!r} not in {s['enum']}"
        if "minLength" in s and len(v) < s["minLength"]: return f"{p}: {v!r} shorter than {s['minLength']}"
        if "maxLength" in s and len(v) > s["maxLength"]: return f"{p}: {v!r} longer than {s['maxLength']}"
        f = s.get("format")
        try:
            if f == "date": date.fromisoformat(v)
            if f == "date-time":
                if datetime.fromisoformat(v).tzinfo is None: return f"{p}: {v!r} has no offset"
        except ValueError:
            return f"{p}: {v!r} not {f}"
    return None
arr = spec.startswith("[")
name = spec.strip("[]")
e = chk(b, {"type": "array", "items": {"$ref": name}} if arr else {"$ref": name}, "$")
if not e and name == "Problem" and b.get("status") != code: e = f"$.status {b.get('status')} vs HTTP {code}"
print(e or "ok")
EOF

# Per-check result files: $TMP/<check>.fail holds failure lines.
fail() { echo "$2" >> "$TMP/$1.fail"; }
LAST_ID=""
# call CHECK LABEL METHOD PATH EXPECT SCHEMA TIMED [json-body]
call() {
  local chk="$1" label="$2" method="$3" path="$4" want="$5" schema="$6" timed="$7" data="${8-}"
  local args=(-sS -o "$TMP/body" -D "$TMP/head" -w '%{http_code} %{time_total}' -X "$method")
  [ -n "$data" ] && args+=(-H 'Content-Type: application/json' --data "$data")
  local out code t v
  out=$(curl "${args[@]}" "$BASE$path")
  code=${out% *}; t=${out#* }
  {
    echo "=== $chk $label: $method $path ${data:+body=$data}"
    tr -d '\r' < "$TMP/head"
    head -c 3000 "$TMP/body"; echo
    echo "=== status $code time_total $t (expected $want)"; echo
  } >> "$LOG"
  [ "$timed" = 1 ] && printf '%s\t%s %s\t%s\t%s\n' "$label" "$method" "$path" "$code" "$t" >> "$TIMES"
  if [ "$code" != "$want" ]; then fail "$chk" "$label $method $path: $want vs $code"; return; fi
  if [ "$schema" != "" ]; then
    v=$(python3 "$TMP/v.py" "$SWAGGER" "$TMP/body" "$schema" "$code")
    if [ "$v" != ok ]; then
      case "$code" in 2*) fail C9 "$label ($schema): $v" ;; *) fail "$chk" "$label error body ($schema): $v" ;; esac
    fi
    case "$code" in 4*)
      ct=$(grep -i '^content-type:' "$TMP/head" | tr -d '\r' | cut -d' ' -f2-)
      [[ "$ct" == application/problem+json* ]] || fail "$chk" "$label content-type '$ct' vs application/problem+json" ;;
    esac
  fi
  cp "$TMP/body" "$TMP/last"
}
jf() { python3 -c 'import json,sys; b=json.load(open(sys.argv[1])); exec("print("+sys.argv[2]+")")' "$TMP/last" "$1"; }
field_err() { # CHECK LABEL FIELD : the last 400 body lists errors[].field == FIELD
  local f; f=$(jf "' '.join(e.get('field','') for e in b.get('errors') or [])")
  [[ " $f " == *" $3 "* ]] || fail "$1" "$2 errors[].field '$f' lacks '$3'"
}
cp /dev/null "$TMP/last"

# ---------------------------------------------------------------- C1 swagger
call C1 api-docs GET /v3/api-docs 200 "" 0
r=$(python3 - "$TMP/last" "$SWAGGER" <<'EOF'
import json, sys
def ops(f):
    d = json.load(open(f))
    return sorted(o["operationId"] for p in d["paths"].values() for o in p.values() if isinstance(o, dict) and "operationId" in o)
a, b = ops(sys.argv[1]), ops(sys.argv[2])
print(f"ok {len(a)}" if a == b and len(a) == 41 else f"live {len(a)} vs copy {len(b)}: {sorted(set(a) ^ set(b))}")
EOF
)
[[ "$r" == ok* ]] || fail C1 "operationIds: $r"

# ---------------------------------------------------------------- C2 app info
call C2 getAppInfo GET /api/app-info 200 AppInfo 0
[ "$(jf "b['timeZone']")" = Africa/Cairo ] || fail C2 "timeZone $(jf "b.get('timeZone')") vs Africa/Cairo"

# ---------------------------------------------------------------- C3 tasks
call C3 createTask POST /api/tasks 201 Task 1 "{\"title\":\"Contract task\",\"dueDate\":\"$YESTERDAY\"}"
TID=$(jf "b['id']")
[ "$(jf "b['status']+' '+b['priority']")" = "TODO MEDIUM" ] || fail C3 "createTask defaults $(jf "b['status']+' '+b['priority']") vs TODO MEDIUM"
[ "$(jf "b.get('tags')")" = "[]" ] || fail C3 "createTask tags $(jf "b.get('tags')") vs []"
call C3 createTask-invalid POST /api/tasks 400 Problem 1 '{"title":""}'
field_err C3 createTask-invalid title
call C3 listTasks GET /api/tasks 200 "[Task]" 1
call C3 listTasks-filtered GET "/api/tasks?q=contract&status=TODO&priority=MEDIUM&dueFrom=$YESTERDAY&dueTo=$TODAY&archived=false&sort=dueDate&direction=asc" 200 "[Task]" 1
[ "$(jf "[t['id'] for t in b]")" = "[$TID]" ] || fail C3 "filtered list ids $(jf "[t['id'] for t in b]") vs [$TID]"
call C3 listOverdueTasks GET /api/tasks/overdue 200 "[Task]" 1
call C3 getTask GET "/api/tasks/$TID" 200 Task 0
call C3 getTask-404 GET "/api/tasks/$NF" 404 Problem 0
call C3 updateTask PUT "/api/tasks/$TID" 200 Task 1 '{"title":"Contract task 2","status":"IN_PROGRESS","priority":"HIGH","dueDate":null}'
call C3 updateTask-invalid PUT "/api/tasks/$TID" 400 Problem 1 '{"title":"x","priority":"HIGH"}'
field_err C3 updateTask-invalid status
call C3 updateTask-404 PUT "/api/tasks/$NF" 404 Problem 1 '{"title":"x","status":"TODO","priority":"LOW"}'
call C3 completeTask POST "/api/tasks/$TID/complete" 200 Task 1
[ "$(jf "b['status']")" = DONE ] || fail C3 "completeTask status $(jf "b['status']") vs DONE"
call C3 completeTask-404 POST "/api/tasks/$NF/complete" 404 Problem 1
call C3 archiveTask POST "/api/tasks/$TID/archive" 200 Task 1
[ "$(jf "b['archived']")" = True ] || fail C3 "archiveTask archived $(jf "b['archived']") vs true"
call C3 archiveTask-404 POST "/api/tasks/$NF/archive" 404 Problem 1
call C3 restoreTask POST "/api/tasks/$TID/restore" 200 Task 1
[ "$(jf "b['archived']")" = False ] || fail C3 "restoreTask archived $(jf "b['archived']") vs false"
call C3 restoreTask-404 POST "/api/tasks/$NF/restore" 404 Problem 1
call C3 deleteTask DELETE "/api/tasks/$TID" 204 - 1
call C3 deleteTask-404 DELETE "/api/tasks/$TID" 404 Problem 1

# ---------------------------------------------------------------- C4 task tags
call C4 setup-createTask POST /api/tasks 201 Task 0 "{\"title\":\"Tagged contract task\",\"dueDate\":\"$TODAY\"}"
TG=$(jf "b['id']")
call C4 addTaskTags POST "/api/tasks/$TG/tags" 200 Task 1 '{"tags":["Work"," urgent "]}'
[ "$(jf "b['tags']")" = "['urgent', 'work']" ] || fail C4 "addTaskTags tags $(jf "b['tags']") vs ['urgent', 'work']"
call C4 addTaskTags-empty POST "/api/tasks/$TG/tags" 400 Problem 1 '{"tags":[]}'
field_err C4 addTaskTags-empty tags
call C4 addTaskTags-404 POST "/api/tasks/$NF/tags" 404 Problem 1 '{"tags":["work"]}'
call C4 listTasks-tag GET "/api/tasks?tag=work" 200 "[Task]" 1
[ "$(jf "[t['id'] for t in b]")" = "[$TG]" ] || fail C4 "listTasks?tag=work ids $(jf "[t['id'] for t in b]") vs [$TG]"
call C4 listTasks-tag-unknown GET "/api/tasks?tag=nosuchtag" 200 "[Task]" 1
[ "$(jf "b")" = "[]" ] || fail C4 "listTasks?tag=nosuchtag $(jf "b") vs []"
call C4 removeTaskTag DELETE "/api/tasks/$TG/tags?tag=work" 200 Task 1
[ "$(jf "b['tags']")" = "['urgent']" ] || fail C4 "removeTaskTag tags $(jf "b['tags']") vs ['urgent']"
call C4 removeTaskTag-noTag DELETE "/api/tasks/$TG/tags" 400 Problem 1
call C4 removeTaskTag-404 DELETE "/api/tasks/$NF/tags?tag=work" 404 Problem 1

# ---------------------------------------------------------------- C5 habits
call C5 createHabit POST /api/habits 201 Habit 1 '{"name":"Contract habit","frequency":"DAILY"}'
HID=$(jf "b['id']")
[ "$(jf "b['active']")" = True ] || fail C5 "createHabit active $(jf "b['active']") vs true (Created (active))"
call C5 createHabit-invalid POST /api/habits 400 Problem 1 '{"name":"","frequency":"DAILY"}'
field_err C5 createHabit-invalid name
call C5 listHabits GET /api/habits 200 "[Habit]" 1
call C5 listHabits-active GET "/api/habits?active=true" 200 "[Habit]" 1
call C5 getHabit GET "/api/habits/$HID" 200 Habit 0
call C5 getHabit-404 GET "/api/habits/$NF" 404 Problem 0
call C5 updateHabit PUT "/api/habits/$HID" 200 Habit 1 '{"name":"Contract habit 2","description":"d","frequency":"WEEKLY"}'
call C5 updateHabit-invalid PUT "/api/habits/$HID" 400 Problem 1 '{"name":"x"}'
field_err C5 updateHabit-invalid frequency
call C5 updateHabit-404 PUT "/api/habits/$NF" 404 Problem 1 '{"name":"x","frequency":"DAILY"}'
call C5 deactivateHabit POST "/api/habits/$HID/deactivate" 200 Habit 1
[ "$(jf "b['active']")" = False ] || fail C5 "deactivateHabit active $(jf "b['active']") vs false"
call C5 deactivateHabit-404 POST "/api/habits/$NF/deactivate" 404 Problem 1
call C5 activateHabit POST "/api/habits/$HID/activate" 200 Habit 1
[ "$(jf "b['active']")" = True ] || fail C5 "activateHabit active $(jf "b['active']") vs true"
call C5 activateHabit-404 POST "/api/habits/$NF/activate" 404 Problem 1
call C5 completeHabit POST "/api/habits/$HID/completions" 201 HabitCompletion 1
[ "$(jf "b['completionDate']")" = "$TODAY" ] || fail C5 "completeHabit (no body) date $(jf "b['completionDate']") vs today $TODAY"
call C5 completeHabit-future POST "/api/habits/$HID/completions" 400 Problem 1 "{\"date\":\"$TOMORROW\"}"
call C5 completeHabit-404 POST "/api/habits/$NF/completions" 404 Problem 1 "{\"date\":\"$TODAY\"}"
call C5 completeHabit-409 POST "/api/habits/$HID/completions" 409 Problem 1 "{\"date\":\"$TODAY\"}"
call C5 listHabitCompletions GET "/api/habits/$HID/completions" 200 "[HabitCompletion]" 0
call C5 listHabitCompletions-404 GET "/api/habits/$NF/completions" 404 Problem 0
call C5 undoHabitCompletion DELETE "/api/habits/$HID/completions/$TODAY" 204 - 1
call C5 undoHabitCompletion-404 DELETE "/api/habits/$NF/completions/$TODAY" 404 Problem 1
call C5 deleteHabit DELETE "/api/habits/$HID" 204 - 1
call C5 deleteHabit-404 DELETE "/api/habits/$HID" 404 Problem 1

# ---------------------------------------------------------------- C6 learning cards
call C6 createLearningCard POST /api/learning-cards 201 LearningCard 1 '{"title":"Contract card","description":"d"}'
LID=$(jf "b['id']")
[ "$(jf "b['status']")" = NOT_STARTED ] || fail C6 "createLearningCard status $(jf "b['status']") vs NOT_STARTED"
call C6 createLearningCard-invalid POST /api/learning-cards 400 Problem 1 '{"title":""}'
field_err C6 createLearningCard-invalid title
call C6 listLearningCards GET /api/learning-cards 200 "[LearningCard]" 1
call C6 getLearningCard GET "/api/learning-cards/$LID" 200 LearningCard 0
call C6 getLearningCard-404 GET "/api/learning-cards/$NF" 404 Problem 0
call C6 updateLearningCard PUT "/api/learning-cards/$LID" 200 LearningCard 1 '{"title":"Contract card 2","description":null,"status":"IN_PROGRESS"}'
call C6 updateLearningCard-invalid PUT "/api/learning-cards/$LID" 400 Problem 1 '{"title":"x"}'
field_err C6 updateLearningCard-invalid status
call C6 updateLearningCard-404 PUT "/api/learning-cards/$NF" 404 Problem 1 '{"title":"x","status":"NOT_STARTED"}'
call C6 addMilestone POST "/api/learning-cards/$LID/milestones" 201 Milestone 1 "{\"title\":\"M1\",\"targetDate\":\"$TOMORROW\"}"
MID=$(jf "b['id']")
[ "$(jf "b['done']")" = False ] || fail C6 "addMilestone done $(jf "b['done']") vs false (Added (not done))"
call C6 addMilestone-invalid POST "/api/learning-cards/$LID/milestones" 400 Problem 1 '{"title":""}'
field_err C6 addMilestone-invalid title
call C6 addMilestone-404 POST "/api/learning-cards/$NF/milestones" 404 Problem 1 '{"title":"M"}'
call C6 updateMilestone PUT "/api/learning-cards/$LID/milestones/$MID" 200 Milestone 1 '{"title":"M1b","targetDate":null,"done":true}'
call C6 updateMilestone-invalid PUT "/api/learning-cards/$LID/milestones/$MID" 400 Problem 1 '{"title":"M1b"}'
field_err C6 updateMilestone-invalid done
call C6 updateMilestone-404 PUT "/api/learning-cards/$LID/milestones/$NF" 404 Problem 1 '{"title":"M","done":false}'
call C6 addNote POST "/api/learning-cards/$LID/notes" 201 Note 1 '{"text":"A note"}'
NID=$(jf "b['id']")
call C6 addNote-invalid POST "/api/learning-cards/$LID/notes" 400 Problem 1 '{"text":""}'
field_err C6 addNote-invalid text
call C6 addNote-404 POST "/api/learning-cards/$NF/notes" 404 Problem 1 '{"text":"n"}'
call C6 getLearningCard-full GET "/api/learning-cards/$LID" 200 LearningCard 0
call C6 deleteNote DELETE "/api/learning-cards/$LID/notes/$NID" 204 - 1
call C6 deleteNote-404 DELETE "/api/learning-cards/$LID/notes/$NID" 404 Problem 1
call C6 deleteMilestone DELETE "/api/learning-cards/$LID/milestones/$MID" 204 - 1
call C6 deleteMilestone-404 DELETE "/api/learning-cards/$LID/milestones/$MID" 404 Problem 1
call C6 deleteLearningCard DELETE "/api/learning-cards/$LID" 204 - 1
call C6 deleteLearningCard-404 DELETE "/api/learning-cards/$LID" 404 Problem 1

# ---------------------------------------------------------------- C7 plans
call C7 setup-createTask POST /api/tasks 201 Task 0 '{"title":"Plan source task"}'
PT=$(jf "b['id']")
P="\"title\":\"Contract plan\",\"estimatedDurationMinutes\":60,\"startDateTime\":\"$START\",\"endDateTime\":\"$END\",\"priorityOrder\":1"
call C7 createPlan POST /api/plans 201 Plan 1 "{$P,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$PT}]}"
PID=$(jf "b['id']"); PIID=$(jf "b['items'][0]['id']")
call C7 createPlan-noItems POST /api/plans 400 Problem 1 "{$P,\"items\":[]}"
field_err C7 createPlan-noItems items
call C7 createPlan-endBeforeStart POST /api/plans 400 Problem 1 "{\"title\":\"Bad plan\",\"estimatedDurationMinutes\":60,\"startDateTime\":\"$END\",\"endDateTime\":\"$START\",\"priorityOrder\":1,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$PT}]}"
call C7 createPlan-endEqualsStart POST /api/plans 400 Problem 1 "{\"title\":\"Bad plan\",\"estimatedDurationMinutes\":60,\"startDateTime\":\"$START\",\"endDateTime\":\"$START\",\"priorityOrder\":1,\"items\":[{\"sourceType\":\"TASK\",\"sourceId\":$PT}]}"
call C7 listPlans GET /api/plans 200 "[Plan]" 1
for g in active completed all; do call C7 "listPlans-$g" GET "/api/plans?group=$g" 200 "[Plan]" 1; done
call C7 getPlan GET "/api/plans/$PID" 200 Plan 0
call C7 getPlan-404 GET "/api/plans/$NF" 404 Problem 0
call C7 updatePlan PUT "/api/plans/$PID" 200 Plan 1 "{\"title\":\"Contract plan 2\",\"estimatedDurationMinutes\":30,\"startDateTime\":\"$START\",\"endDateTime\":\"$END\",\"priorityOrder\":2}"
call C7 updatePlan-invalid PUT "/api/plans/$PID" 400 Problem 1 "{\"title\":\"Contract plan 2\",\"estimatedDurationMinutes\":30,\"startDateTime\":\"$END\",\"endDateTime\":\"$START\",\"priorityOrder\":2}"
call C7 updatePlan-404 PUT "/api/plans/$NF" 404 Problem 1 "{\"title\":\"x\",\"estimatedDurationMinutes\":30,\"startDateTime\":\"$START\",\"endDateTime\":\"$END\",\"priorityOrder\":1}"
call C7 setPlanItemDone PUT "/api/plans/$PID/items/$PIID" 200 Plan 1 '{"done":true}'
[ "$(jf "(b['doneItems'], b['totalItems'], b['progressPercent'])")" = "(1, 1, 100)" ] || fail C7 "setPlanItemDone progress $(jf "(b['doneItems'], b['totalItems'], b['progressPercent'])") vs (1, 1, 100)"
call C7 setPlanItemDone-invalid PUT "/api/plans/$PID/items/$PIID" 400 Problem 1 '{}'
field_err C7 setPlanItemDone-invalid done
call C7 setPlanItemDone-404 PUT "/api/plans/$PID/items/$NF" 404 Problem 1 '{"done":false}'
call C7 deletePlan DELETE "/api/plans/$PID" 204 - 1
call C7 deletePlan-404 DELETE "/api/plans/$PID" 404 Problem 1
call C7 cleanup-deleteTask DELETE "/api/tasks/$PT" 204 - 0

# ---------------------------------------------------------------- C8 dashboard, settings
call C8 getDashboard GET /api/dashboard 200 Dashboard 0
[ "$(jf "[t.get('tags') for t in b['dueToday'] if t['id'] == $TG]")" = "[['urgent']]" ] || fail C8 "dashboard dueToday entry of task $TG tags $(jf "[t.get('tags') for t in b['dueToday'] if t['id'] == $TG]") vs [['urgent']]"
call C8 cleanup-deleteTask DELETE "/api/tasks/$TG" 204 - 0
call C8 getSettings GET /api/settings 200 Settings 0
call C8 updateSettings PUT /api/settings 200 Settings 1 '{"displayName":"Contract","planStartNotifications":false,"defaultPage":"TASKS"}'
[ "$(jf "(b['displayName'], b['planStartNotifications'], b['defaultPage'])")" = "('Contract', False, 'TASKS')" ] || fail C8 "updateSettings echo $(jf "(b.get('displayName'), b.get('planStartNotifications'), b.get('defaultPage'))")"
call C8 updateSettings-invalid PUT /api/settings 400 Problem 1 '{"displayName":"x","planStartNotifications":true,"defaultPage":"NOPE"}'
call C8 updateSettings-restore PUT /api/settings 200 Settings 0 '{"displayName":null,"planStartNotifications":true,"defaultPage":"DASHBOARD"}'

# ---------------------------------------------------------------- C10 response time (< 0.5 s, SC-002 / PRD §10)
n=$(wc -l < "$TIMES")
slow=$(awk -F'\t' '$4 >= 0.5 {print $1" "$4"s"}' "$TIMES")
max=$(sort -t$'\t' -k4 -g "$TIMES" | tail -1 | cut -f1,4 | tr '\t' ' ')
[ -z "$slow" ] || fail C10 "slow: $slow"

for c in C1 C2 C3 C4 C5 C6 C7 C8 C9 C10; do
  if [ -s "$TMP/$c.fail" ]; then
    echo "FAIL $c: $(paste -sd';' "$TMP/$c.fail")"
  else
    case $c in C10) echo "PASS C10 ($n timed calls, max $max)" ;; *) echo "PASS $c" ;; esac
  fi
done
