#!/usr/bin/env bash
# phase-05 (US3 Track learning resources) API checks. Usage: phase-05-verify.sh <attempt-folder>
# Needs only bash, curl, jq and GNU date. Writes only inside the attempt folder.
set -u
OUT="${1:?attempt folder}"
BASE="http://localhost:8080"
LC="$BASE/api/learning-cards"
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
    [ $# -ge 4 ] && { echo "--- request body:"; head -c 600 "$TMP/$name.req"; echo; [ "$(wc -c < "$TMP/$name.req")" -gt 600 ] && echo "... ($(wc -c < "$TMP/$name.req") bytes)"; }
    cat "$TMP/$name.head"
    head -c 3000 "$TMP/$name.body"; echo
    echo "=== status $code"; echo
  } >> "$LOG"
  echo "$code"
}
ctype() { grep -i '^content-type:' "$TMP/$1.head" | tr -d '\r' | cut -d' ' -f2-; }
jq_() { jq -r "$2" "$TMP/$1.body" 2>/dev/null || echo "not-JSON"; }   # name filter
long() { printf "%${2}s" "" | tr ' ' "$1"; }                        # char count
near_now() { # iso date-time with offset, within 300 s of now
  local ts="$1" s
  [[ "$ts" =~ [+-][0-9]{2}:[0-9]{2}$|Z$ ]] || { echo "no offset in '$ts'"; return; }
  s=$(date -d "$ts" +%s 2>/dev/null) || { echo "not a date-time '$ts'"; return; }
  (( ${s#-} > 0 && ( $(date +%s) - s ) < 300 && ( s - $(date +%s) ) < 300 )) && echo ok || echo "'$ts' not near now"; }
problem() { # want code name field ("" = any)
  local want="$1" c="$2" name="$3" field="$4" ct r
  ct=$(ctype "$name")
  [ "$c" = "$want" ] || { echo "status $c (want $want)"; return; }
  [[ "$ct" == application/problem+json* ]] || { echo "content-type '$ct'"; return; }
  if [ -n "$field" ]; then
    r=$(jq_ "$name" "[.errors[]? | select(.field==\"$field\" and ((.message // \"\") | gsub(\"\\\\s\";\"\") | length > 0))] | length > 0")
    [ "$r" = true ] || { echo "no errors[] entry with field '$field' and a message ($(head -c 300 "$TMP/$name.body"))"; return; }
  fi
  echo ok; }
card() { # name json -> id (201 expected, else empty)
  local c; c=$(call "$1" POST "$LC" "$2")
  if [ "$c" = 201 ]; then jq_ "$1" '.id'; else echo ""; fi; }
ms() { # name card json -> milestone id
  local c; c=$(call "$1" POST "$LC/$2/milestones" "$3")
  if [ "$c" = 201 ]; then jq_ "$1" '.id'; else echo ""; fi; }
note() { # name card json -> note id
  local c; c=$(call "$1" POST "$LC/$2/notes" "$3")
  if [ "$c" = 201 ]; then jq_ "$1" '.id'; else echo ""; fi; }
report() { if [ -z "$2" ]; then echo "PASS $1"; else echo "FAIL $1:$2"; fi; }
TOK="L$(date +%s)"
echo "=== token $TOK, run started $(date -Iseconds)" >> "$LOG"

# ---------- C1: create -> NOT_STARTED, createdAt set
f=""
c=$(call C1 POST "$LC" "{\"title\":\"C1 Spring $TOK\",\"description\":\"https://spring.io/guides\"}")
[ "$c" = 201 ] || f="$f 201 vs $c;"
[[ "$(ctype C1)" == application/json* ]] || f="$f content-type '$(ctype C1)';"
r=$(jq_ C1 "[.title==\"C1 Spring $TOK\", .description==\"https://spring.io/guides\", .status==\"NOT_STARTED\", .milestones==[], .notes==[], (.id|type)==\"number\"] | all")
[ "$r" = true ] || f="$f body: $(head -c 300 "$TMP/C1.body");"
r=$(near_now "$(jq_ C1 '.createdAt')"); [ "$r" = ok ] || f="$f createdAt: $r;"
ID1=$(jq_ C1 '.id')
c=$(call C1-get GET "$LC/$ID1"); [ "$c" = 200 ] || f="$f GET 200 vs $c;"
a=$(jq_ C1 'del(.createdAt)|tojson'); b=$(jq_ C1-get 'del(.createdAt)|tojson')
[ "$a" = "$b" ] || f="$f GET differs: $b;"
t1=$(date -d "$(jq_ C1 .createdAt)" +%s%3N); t2=$(date -d "$(jq_ C1-get .createdAt)" +%s%3N)
(( ${t1:-0} - ${t2:-1} <= 1 && ${t2:-1} - ${t1:-0} <= 1 )) || f="$f createdAt changed on GET;"
c=$(call C1-t POST "$LC" "{\"title\":\"C1 title only $TOK\"}")
[ "$c" = 201 ] || f="$f title-only 201 vs $c;"
[ "$(jq_ C1-t '[.description==null, .status=="NOT_STARTED"]|all')" = true ] || f="$f title-only body: $(head -c 300 "$TMP/C1-t.body");"
report C1 "$f"

# ---------- C2: BR-8 title required
f=""
for v in empty blank missing; do
  case $v in empty) b='{"title":""}';; blank) b='{"title":"   "}';; missing) b='{"description":"x"}';; esac
  c=$(call "C2-$v" POST "$LC" "$b"); r=$(problem 400 "$c" "C2-$v" title)
  [ "$r" = ok ] || f="$f POST $v title: $r;"
done
c=$(call C2-put PUT "$LC/$ID1" '{"title":"","status":"NOT_STARTED"}'); r=$(problem 400 "$c" C2-put title)
[ "$r" = ok ] || f="$f PUT empty title: $r;"
call C2-get GET "$LC/$ID1" >/dev/null
[ "$(jq_ C2-get .title)" = "C1 Spring $TOK" ] || f="$f card changed by rejected PUT;"
report C2 "$f"

# ---------- C3: title <= 200, description <= 2,000
f=""
c=$(call C3-t201 POST "$LC" "{\"title\":\"$(long t 201)\"}"); r=$(problem 400 "$c" C3-t201 title)
[ "$r" = ok ] || f="$f 201-char title: $r;"
c=$(call C3-t200 POST "$LC" "{\"title\":\"$(long t 200)\"}"); [ "$c" = 201 ] || f="$f 200-char title 201 vs $c;"
[ "$(jq_ C3-t200 '.title|length')" = 200 ] || f="$f 200-char title not stored whole;"
c=$(call C3-d2001 POST "$LC" "{\"title\":\"C3 d\",\"description\":\"$(long d 2001)\"}"); r=$(problem 400 "$c" C3-d2001 description)
[ "$r" = ok ] || f="$f 2001-char description: $r;"
c=$(call C3-d2000 POST "$LC" "{\"title\":\"C3 d\",\"description\":\"$(long d 2000)\"}"); [ "$c" = 201 ] || f="$f 2000-char description 201 vs $c;"
[ "$(jq_ C3-d2000 '.description|length')" = 2000 ] || f="$f 2000-char description not stored whole;"
report C3 "$f"

# ---------- C4: status change
f=""
ID4=$(card C4-new "{\"title\":\"C4 status $TOK\",\"description\":\"book\"}")
for s in IN_PROGRESS COMPLETED NOT_STARTED; do
  c=$(call "C4-$s" PUT "$LC/$ID4" "{\"title\":\"C4 $s $TOK\",\"description\":\"desc $s\",\"status\":\"$s\"}")
  [ "$c" = 200 ] || f="$f PUT $s 200 vs $c;"
  [ "$(jq_ "C4-$s" ".status==\"$s\" and .title==\"C4 $s $TOK\" and .description==\"desc $s\"")" = true ] || f="$f PUT $s body: $(head -c 200 "$TMP/C4-$s.body");"
  call "C4-$s-get" GET "$LC/$ID4" >/dev/null
  [ "$(jq_ "C4-$s-get" '.status')" = "$s" ] || f="$f GET after $s: $(jq_ "C4-$s-get" .status);"
done
[ "$(jq_ C4-new .createdAt)" = "$(jq_ C4-NOT_STARTED-get .createdAt)" ] || {
  t1=$(date -d "$(jq_ C4-new .createdAt)" +%s%3N); t2=$(date -d "$(jq_ C4-NOT_STARTED-get .createdAt)" +%s%3N)
  (( t1 - t2 <= 1 && t2 - t1 <= 1 )) || f="$f createdAt changed by PUT;"; }
c=$(call C4-done PUT "$LC/$ID4" "{\"title\":\"C4 x\",\"status\":\"DONE\"}"); r=$(problem 400 "$c" C4-done "")
[ "$r" = ok ] || f="$f status DONE: $r;"
c=$(call C4-nostatus PUT "$LC/$ID4" "{\"title\":\"C4 x\"}"); r=$(problem 400 "$c" C4-nostatus "")
[ "$r" = ok ] || f="$f missing status: $r;"
call C4-after GET "$LC/$ID4" >/dev/null
[ "$(jq_ C4-after '.status + "|" + .title')" = "NOT_STARTED|C4 NOT_STARTED $TOK" ] || f="$f card changed by rejected PUT: $(jq_ C4-after '.status + "|" + .title');"
c=$(call C4-unk PUT "$LC/999999" '{"title":"x","status":"NOT_STARTED"}'); r=$(problem 404 "$c" C4-unk "")
[ "$r" = ok ] || f="$f unknown id: $r;"
report C4 "$f"

# ---------- C5: add milestone (BR-9)
f=""
IDA=$(card C5-a "{\"title\":\"C5 card A $TOK\"}"); IDB=$(card C5-b "{\"title\":\"C5 card B $TOK\"}")
c=$(call C5-m1 POST "$LC/$IDA/milestones" '{"title":"Chapter 1","targetDate":"2026-10-15"}')
[ "$c" = 201 ] || f="$f milestone 201 vs $c;"
[ "$(jq_ C5-m1 "[.cardId==$IDA, .title==\"Chapter 1\", .done==false, .targetDate==\"2026-10-15\", (.id|type)==\"number\"]|all")" = true ] || f="$f milestone body: $(head -c 300 "$TMP/C5-m1.body");"
M1=$(jq_ C5-m1 .id)
c=$(call C5-m2 POST "$LC/$IDA/milestones" '{"title":"Chapter 2"}')
[ "$c" = 201 ] || f="$f milestone without date 201 vs $c;"
[ "$(jq_ C5-m2 "[.cardId==$IDA, .done==false, .targetDate==null]|all")" = true ] || f="$f milestone without date body: $(head -c 300 "$TMP/C5-m2.body");"
M2=$(jq_ C5-m2 .id)
call C5-ga GET "$LC/$IDA" >/dev/null; call C5-gb GET "$LC/$IDB" >/dev/null
[ "$(jq_ C5-ga '[.milestones[].id]|sort|map(tostring)|join(" ")')" = "$(printf '%s\n' "$M1" "$M2" | sort -n | tr '\n' ' ' | sed 's/ $//')" ] || f="$f card A milestones: $(jq_ C5-ga '[.milestones[].id]');"
[ "$(jq_ C5-ga '[.milestones[] | .done==false and .cardId=='"$IDA"'] | all')" = true ] || f="$f card A milestone not done/cardId;"
[ "$(jq_ C5-gb '.milestones|length')" = 0 ] || f="$f card B has milestones: $(jq_ C5-gb .milestones);"
c=$(call C5-blank POST "$LC/$IDA/milestones" '{"title":"  "}'); r=$(problem 400 "$c" C5-blank title); [ "$r" = ok ] || f="$f blank title: $r;"
c=$(call C5-long POST "$LC/$IDA/milestones" "{\"title\":\"$(long m 201)\"}"); r=$(problem 400 "$c" C5-long title); [ "$r" = ok ] || f="$f 201-char title: $r;"
c=$(call C5-unk POST "$LC/999999/milestones" '{"title":"x"}'); r=$(problem 404 "$c" C5-unk ""); [ "$r" = ok ] || f="$f unknown card: $r;"
call C5-ga2 GET "$LC/$IDA" >/dev/null; [ "$(jq_ C5-ga2 '.milestones|length')" = 2 ] || f="$f rejected milestone stored;"
report C5 "$f"

# ---------- C6: milestone done / not done / remove
f=""
c=$(call C6-done PUT "$LC/$IDA/milestones/$M1" '{"title":"Chapter 1","targetDate":"2026-10-15","done":true}')
[ "$c" = 200 ] || f="$f done 200 vs $c;"; [ "$(jq_ C6-done '.done')" = true ] || f="$f done body: $(head -c 200 "$TMP/C6-done.body");"
call C6-g1 GET "$LC/$IDA" >/dev/null
[ "$(jq_ C6-g1 "[.milestones[]|select(.id==$M1)|.done]|tostring")" = "[true]" ] || f="$f GET after done: $(jq_ C6-g1 .milestones);"
[ "$(jq_ C6-g1 '"\(.milestonesDone)/\(.milestonesTotal)"')" = "1/2" ] || f="$f counts after done: $(jq_ C6-g1 '"\(.milestonesDone)/\(.milestonesTotal)"');"
c=$(call C6-undone PUT "$LC/$IDA/milestones/$M1" '{"title":"Chapter 1","targetDate":"2026-10-15","done":false}')
[ "$c" = 200 ] || f="$f undone 200 vs $c;"; [ "$(jq_ C6-undone '.done')" = false ] || f="$f undone body: $(head -c 200 "$TMP/C6-undone.body");"
call C6-g2 GET "$LC/$IDA" >/dev/null
[ "$(jq_ C6-g2 "[.milestones[]|select(.id==$M1)|.done]|tostring")" = "[false]" ] || f="$f GET after undone: $(jq_ C6-g2 .milestones);"
[ "$(jq_ C6-g2 '"\(.milestonesDone)/\(.milestonesTotal)"')" = "0/2" ] || f="$f counts after undone: $(jq_ C6-g2 '"\(.milestonesDone)/\(.milestonesTotal)"');"
c=$(call C6-otherput PUT "$LC/$IDB/milestones/$M1" '{"title":"hijack","done":true}'); r=$(problem 404 "$c" C6-otherput ""); [ "$r" = ok ] || f="$f PUT via other card: $r;"
c=$(call C6-otherdel DELETE "$LC/$IDB/milestones/$M1"); r=$(problem 404 "$c" C6-otherdel ""); [ "$r" = ok ] || f="$f DELETE via other card: $r;"
call C6-g3 GET "$LC/$IDA" >/dev/null
[ "$(jq_ C6-g3 "[.milestones[]|select(.id==$M1)|.title + \"|\" + (.done|tostring)]|join(\",\")")" = "Chapter 1|false" ] || f="$f milestone changed via other card: $(jq_ C6-g3 .milestones);"
c=$(call C6-del DELETE "$LC/$IDA/milestones/$M1"); [ "$c" = 204 ] || f="$f DELETE 204 vs $c;"
call C6-g4 GET "$LC/$IDA" >/dev/null
[ "$(jq_ C6-g4 "[.milestones[]|.id]|tostring")" = "[$M2]" ] || f="$f after DELETE milestones: $(jq_ C6-g4 '[.milestones[].id]');"
[ "$(jq_ C6-g4 '.milestonesTotal')" = 1 ] || f="$f milestonesTotal after DELETE: $(jq_ C6-g4 .milestonesTotal);"
c=$(call C6-del2 DELETE "$LC/$IDA/milestones/$M1"); r=$(problem 404 "$c" C6-del2 ""); [ "$r" = ok ] || f="$f second DELETE: $r;"
c=$(call C6-put-gone PUT "$LC/$IDA/milestones/$M1" '{"title":"x","done":true}'); [ "$c" = 404 ] || f="$f PUT removed milestone 404 vs $c;"
report C6 "$f"

# ---------- C7: notes
f=""
c=$(call C7-n1 POST "$LC/$IDA/notes" '{"text":"DI is constructor injection by default."}')
[ "$c" = 201 ] || f="$f note 201 vs $c;"
[ "$(jq_ C7-n1 "[.cardId==$IDA, .text==\"DI is constructor injection by default.\", (.id|type)==\"number\"]|all")" = true ] || f="$f note body: $(head -c 300 "$TMP/C7-n1.body");"
r=$(near_now "$(jq_ C7-n1 .createdAt)"); [ "$r" = ok ] || f="$f note createdAt: $r;"
N1=$(jq_ C7-n1 .id)
call C7-g1 GET "$LC/$IDA" >/dev/null
[ "$(jq_ C7-g1 "[.notes[]|select(.id==$N1)|.text]|join(\"\")")" = "DI is constructor injection by default." ] || f="$f card notes: $(jq_ C7-g1 .notes);"
call C7-gb GET "$LC/$IDB" >/dev/null; [ "$(jq_ C7-gb '.notes|length')" = 0 ] || f="$f note shows under card B;"
for v in blank missing long; do
  case $v in blank) b='{"text":"   "}';; missing) b='{}';; long) b="{\"text\":\"$(long x 5001)\"}";; esac
  c=$(call "C7-$v" POST "$LC/$IDA/notes" "$b"); r=$(problem 400 "$c" "C7-$v" text); [ "$r" = ok ] || f="$f $v text: $r;"
done
c=$(call C7-5000 POST "$LC/$IDA/notes" "{\"text\":\"$(long x 5000)\"}"); [ "$c" = 201 ] || f="$f 5000-char note 201 vs $c;"
[ "$(jq_ C7-5000 '.text|length')" = 5000 ] || f="$f 5000-char note not stored whole;"
N2=$(jq_ C7-5000 .id)
c=$(call C7-unk POST "$LC/999999/notes" '{"text":"x"}'); r=$(problem 404 "$c" C7-unk ""); [ "$r" = ok ] || f="$f unknown card: $r;"
c=$(call C7-other DELETE "$LC/$IDB/notes/$N1"); r=$(problem 404 "$c" C7-other ""); [ "$r" = ok ] || f="$f DELETE via other card: $r;"
c=$(call C7-del DELETE "$LC/$IDA/notes/$N1"); [ "$c" = 204 ] || f="$f DELETE 204 vs $c;"
call C7-g2 GET "$LC/$IDA" >/dev/null
[ "$(jq_ C7-g2 '[.notes[].id]|tostring')" = "[$N2]" ] || f="$f notes after DELETE: $(jq_ C7-g2 '[.notes[].id]');"
c=$(call C7-del2 DELETE "$LC/$IDA/notes/$N1"); r=$(problem 404 "$c" C7-del2 ""); [ "$r" = ok ] || f="$f second DELETE: $r;"
report C7 "$f"

# ---------- C8: delete card (BR-14)
f=""
IDX=$(card C8-new "{\"title\":\"C8 delete $TOK\"}")
X1=$(ms C8-m1 "$IDX" '{"title":"m1"}'); X2=$(ms C8-m2 "$IDX" '{"title":"m2","targetDate":"2026-11-01"}')
Y1=$(note C8-n1 "$IDX" '{"text":"n1"}'); Y2=$(note C8-n2 "$IDX" '{"text":"n2"}')
call C8-pre GET "$LC/$IDX" >/dev/null
[ "$(jq_ C8-pre '"\(.milestones|length)/\(.notes|length)"')" = 2/2 ] || f="$f setup: $(jq_ C8-pre '"\(.milestones|length)/\(.notes|length)"');"
c=$(call C8-del DELETE "$LC/$IDX"); [ "$c" = 204 ] || f="$f DELETE 204 vs $c;"
c=$(call C8-get GET "$LC/$IDX"); r=$(problem 404 "$c" C8-get ""); [ "$r" = ok ] || f="$f GET: $r;"
call C8-list GET "$LC" >/dev/null
[ "$(jq_ C8-list "[.[]|select(.id==$IDX)]|length")" = 0 ] || f="$f still in list;"
[ "$(jq_ C8-list "[.[].milestones[].id|select(.==$X1 or .==$X2)]|length")" = 0 ] || f="$f its milestones listed;"
[ "$(jq_ C8-list "[.[].notes[].id|select(.==$Y1 or .==$Y2)]|length")" = 0 ] || f="$f its notes listed;"
c=$(call C8-mput PUT "$LC/$IDX/milestones/$X1" '{"title":"x","done":true}'); [ "$c" = 404 ] || f="$f PUT milestone 404 vs $c;"
c=$(call C8-mdel DELETE "$LC/$IDX/milestones/$X2"); [ "$c" = 404 ] || f="$f DELETE milestone 404 vs $c;"
c=$(call C8-ndel DELETE "$LC/$IDX/notes/$Y1"); [ "$c" = 404 ] || f="$f DELETE note 404 vs $c;"
c=$(call C8-madd POST "$LC/$IDX/milestones" '{"title":"x"}'); [ "$c" = 404 ] || f="$f add milestone 404 vs $c;"
c=$(call C8-nadd POST "$LC/$IDX/notes" '{"text":"x"}'); [ "$c" = 404 ] || f="$f add note 404 vs $c;"
c=$(call C8-put PUT "$LC/$IDX" '{"title":"x","status":"NOT_STARTED"}'); [ "$c" = 404 ] || f="$f PUT card 404 vs $c;"
c=$(call C8-del2 DELETE "$LC/$IDX"); [ "$c" = 404 ] || f="$f second DELETE 404 vs $c;"
report C8 "$f"

# ---------- C9: card embeds milestones and notes; list newest first
f=""
call C9-get GET "$LC/$IDA" >/dev/null
[ "$(jq_ C9-get '"\(.milestones|length)/\(.notes|length)"')" = 1/1 ] || f="$f card A GET: $(jq_ C9-get '"\(.milestones|length)/\(.notes|length)"');"
call C9-list GET "$LC" >/dev/null
[ "$(jq_ C9-list "[.[]|select(.id==$IDA)|\"\(.milestones|map(.id)|tostring)/\(.notes|map(.id)|tostring)\"]|join(\"\")")" = "[$M2]/[$N2]" ] || f="$f card A in list: $(jq_ C9-list "[.[]|select(.id==$IDA)]");"
[ "$(jq_ C9-list 'all(.[]; (.milestones|type)=="array" and (.notes|type)=="array")')" = true ] || f="$f a listed card lacks milestones/notes arrays;"
ord=$(jq_ C9-list '[.[].id]|tostring'); want=$(jq_ C9-list '[.[].id]|sort|reverse|tostring')
[ "$ord" = "$want" ] || f="$f list not newest first: $ord;"
report C9 "$f"

# ---------- C10: swagger
c=$(call C10 GET "$BASE/v3/api-docs")
check_doc() { jq -r '
  def op(p; m): .paths[p][m].operationId;
  [ (if op("/api/learning-cards";"get") != "listLearningCards" then "listLearningCards" else empty end),
    (if op("/api/learning-cards";"post") != "createLearningCard" then "createLearningCard" else empty end),
    (if op("/api/learning-cards/{id}";"get") != "getLearningCard" then "getLearningCard" else empty end),
    (if op("/api/learning-cards/{id}";"put") != "updateLearningCard" then "updateLearningCard" else empty end),
    (if op("/api/learning-cards/{id}";"delete") != "deleteLearningCard" then "deleteLearningCard" else empty end),
    (if op("/api/learning-cards/{id}/milestones";"post") != "addMilestone" then "addMilestone" else empty end),
    (if op("/api/learning-cards/{id}/milestones/{milestoneId}";"put") != "updateMilestone" then "updateMilestone" else empty end),
    (if op("/api/learning-cards/{id}/milestones/{milestoneId}";"delete") != "deleteMilestone" then "deleteMilestone" else empty end),
    (if op("/api/learning-cards/{id}/notes";"post") != "addNote" then "addNote" else empty end),
    (if op("/api/learning-cards/{id}/notes/{noteId}";"delete") != "deleteNote" then "deleteNote" else empty end),
    ( [["/api/learning-cards","post","201"],["/api/learning-cards/{id}/milestones","post","201"],["/api/learning-cards/{id}/notes","post","201"],
       ["/api/learning-cards/{id}","delete","204"],["/api/learning-cards/{id}/milestones/{milestoneId}","delete","204"],["/api/learning-cards/{id}/notes/{noteId}","delete","204"]][]
      as [$p,$m,$c] | if (.paths[$p][$m].responses // {} | has($c)) then empty else "\($m) \($p) no \($c)" end ),
    ( ["LearningCard","LearningCardCreate","LearningCardUpdate","Milestone","MilestoneCreate","MilestoneUpdate","Note","NoteCreate"][]
      as $s | if (.components.schemas | has($s)) then empty else "schema \($s) missing" end ),
    (if .components.schemas.LearningStatus.enum != ["NOT_STARTED","IN_PROGRESS","COMPLETED"] then "LearningStatus \(.components.schemas.LearningStatus)" else empty end)
  ] | if length == 0 then "ok" else join("; ") end' "$1" 2>/dev/null || echo "not JSON"; }
f=""
[ "$c" = 200 ] || f="$f api-docs $c;"
r=$(check_doc "$TMP/C10.body"); [ "$r" = ok ] || f="$f live: $r;"
for p in backend/target/openapi.json loops/backend-dev/outputs/openapi.json; do
  if [ -f "$p" ]; then r=$(check_doc "$p"); else r=missing; fi
  echo "=== C10: $p -> $r" >> "$LOG"
  [ "$r" = ok ] || f="$f $p: $r;"
done
k='[(.paths|with_entries(select(.key|startswith("/api/learning-cards")))), (.components.schemas|with_entries(select(.key|test("^(Learning|Milestone|Note)"))))]'
a=$(jq -cS "$k" backend/target/openapi.json 2>/dev/null); b=$(jq -cS "$k" loops/backend-dev/outputs/openapi.json 2>/dev/null)
if [ -n "$a" ] && [ "$a" = "$b" ]; then r=ok; else r="learning paths/schemas differ"; fi
echo "=== C10: target vs copy -> $r" >> "$LOG"
[ "$r" = ok ] || f="$f target vs copy: $r;"
report C10 "$f"
