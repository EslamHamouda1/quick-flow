#!/usr/bin/env bash
# phase-02 (Polish: swagger vs tags contract, regression) API checks. Usage: phase-02-verify.sh <attempt-folder>
set -u
OUT="${1:?attempt folder}"
BASE="http://localhost:8080"
LOG="$OUT/curl.log"
HERE="$(cd "$(dirname "$0")" && pwd)"
COPY="$HERE/../../../../backend-dev/outputs/openapi.json"
TMP="$(mktemp -d "$OUT/.verify.XXXX")"
trap 'rm -rf "$TMP"' EXIT
N=0

call() { # method path [json-body]; body in $TMP/last.body, prints status code
  local method="$1" path="$2" data="${3-}" code
  N=$((N+1))
  if [ -n "$data" ]; then
    code=$(curl -sS -o "$TMP/last.body" -w '%{http_code}' -X "$method" -H 'Content-Type: application/json' -d "$data" "$BASE$path")
  else
    code=$(curl -sS -o "$TMP/last.body" -w '%{http_code}' -X "$method" "$BASE$path")
  fi
  { echo "=== p2#$N $method $path ${data:+$data}"; cat "$TMP/last.body"; echo; echo "=== status $code"; echo; } >> "$LOG"
  echo "$code"
}
body() { cat "$TMP/last.body"; }
tags_of() { body | jq -c '.tags'; }
has_tags_error() { body | jq -e '[.errors[]? | select(.field=="tags" and (.message|length>0))] | length > 0' > /dev/null; }
check() { if [ "$2" = 1 ]; then echo "PASS $1"; else echo "FAIL $1: $3"; fi; }

# C1: live swagger vs contract (T011)
curl -sS -o "$OUT/api-docs.json" "$BASE/v3/api-docs"
D="$OUT/api-docs.json"
c1=$(jq -r '
  def p(n): .parameters[] | select(.name==n);
  [
    (.paths["/api/tasks"].get | .operationId=="listTasks" and (p("tag") | .in=="query" and .required==false and .schema.type=="string")),
    (.paths["/api/tasks/{id}/tags"].post | .operationId=="addTaskTags"
       and (p("id") | .in=="path" and .required==true and .schema.type=="integer" and .schema.format=="int64")
       and .requestBody.required==true
       and .requestBody.content["application/json"].schema["$ref"]=="#/components/schemas/TaskTags"
       and .responses["200"].content["application/json"].schema["$ref"]=="#/components/schemas/Task"),
    (.paths["/api/tasks/{id}/tags"].delete | .operationId=="removeTaskTag"
       and (p("id") | .in=="path" and .required==true and .schema.format=="int64")
       and (p("tag") | .in=="query" and .required==true and .schema.type=="string" and .schema.minLength==1)
       and .responses["200"].content["application/json"].schema["$ref"]=="#/components/schemas/Task"),
    (.components.schemas.TaskTags | (.required|index("tags"))!=null and .properties.tags.type=="array"
       and .properties.tags.items.type=="string" and .properties.tags.minItems==1 and (.properties.tags|has("maxItems")|not)),
    (.components.schemas.Task | (.required|index("tags"))!=null and .properties.tags.type=="array"
       and .properties.tags.maxItems==10 and .properties.tags.uniqueItems==true
       and .properties.tags.items.type=="string" and .properties.tags.items.minLength==1 and .properties.tags.items.maxLength==30)
  ] | map(tostring) | join(",")' "$D")
ok=0; [ "$c1" = "true,true,true,true,true" ] && ok=1
check C1 $ok "listTasks,addTaskTags,removeTaskTag,TaskTags,Task.tags all true vs $c1"
echo "C1 per-item result (listTasks,addTaskTags,removeTaskTag,TaskTags,Task.tags): $c1" >> "$LOG"

# C2: app-wide copy equals the live swagger (paths + components)
if jq -S '{paths, components}' "$D" > "$TMP/live.json" && jq -S '{paths, components}' "$COPY" > "$TMP/copy.json" && cmp -s "$TMP/live.json" "$TMP/copy.json"; then
  c2=identical; ok=1
else
  c2=different; ok=0
  diff "$TMP/copy.json" "$TMP/live.json" >> "$LOG"
fi
echo "C2 outputs/openapi.json vs live (paths+components): $c2" >> "$LOG"
check C2 $ok "identical vs $c2"

# C3: phase-01 checks re-run
r3=$(bash "$HERE/phase-01-verify.sh" "$OUT")
echo "$r3" | sed 's/^/C3 phase-01 /' >> "$LOG"
ok=0; [ "$(echo "$r3" | grep -c '^PASS C[1-6]$')" = 6 ] && ok=1
check C3 $ok "PASS C1..C6 vs $(echo "$r3" | tr '\n' ' ')"

# C4: @NotEmpty body rules, 404s, required tag
call POST /api/tasks '{"title":"P2 C4 task"}' > /dev/null; V=$(body | jq -r '.id')
call POST "/api/tasks/$V/tags" '{"tags":["alpha"]}' > /dev/null
res=""; ok=1
for bad in '{"tags":[]}' '{"tags":null}' '{}'; do
  c=$(call POST "/api/tasks/$V/tags" "$bad")
  if [ "$c" = 400 ] && has_tags_error; then res="$res 400/tags"; else res="$res $c/$(body | jq -c '.errors' 2>/dev/null)"; ok=0; fi
done
cg=$(call GET "/api/tasks/$V"); tg=$(tags_of)
[ "$cg" = 200 ] && [ "$tg" = '["alpha"]' ] || ok=0
p404=$(call POST /api/tasks/999999/tags '{"tags":["x"]}')
d404=$(call DELETE "/api/tasks/999999/tags?tag=x")
dnot=$(call DELETE "/api/tasks/$V/tags")
[ "$p404" = 404 ] && [ "$d404" = 404 ] && [ "$dnot" = 400 ] || ok=0
check C4 $ok "3x 400/tags, tags [alpha], POST/DELETE unknown 404, DELETE no tag 400 vs$res; GET $cg $tg; $p404 $d404 $dnot"

# C5: dashboard task entries carry tags
today=$(TZ=Africa/Cairo date +%F)
call POST /api/tasks "{\"title\":\"P2 C5 task\",\"dueDate\":\"$today\"}" > /dev/null; T=$(body | jq -r '.id')
call POST "/api/tasks/$T/tags" '{"tags":["dash","board"]}' > /dev/null
cdash=$(call GET /api/dashboard)
td=$(body | jq -c --argjson id "$T" '[.dueToday[]? | select(.id==$id) | .tags] | .[0]')
ok=0; [ "$cdash" = 200 ] && [ "$td" = '["board","dash"]' ] && ok=1
check C5 $ok "dashboard 200, dueToday entry tags [board,dash] vs $cdash $td"
