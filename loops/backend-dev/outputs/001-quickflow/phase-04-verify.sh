#!/usr/bin/env bash
# phase-04 (US2 Track recurring habits) API checks. Usage: phase-04-verify.sh <attempt-folder>
set -u
OUT="${1:?attempt folder}"
BASE="http://localhost:8080"
LOG="$OUT/curl.log"
TMP="$(mktemp -d "$OUT/.verify.XXXX")"
trap 'rm -rf "$TMP"' EXIT

call() { # name method url [json-body]
  local name="$1" method="$2" url="$3" code
  if [ $# -ge 4 ]; then
    printf '%s' "$4" > "$TMP/$name.req"
    code=$(curl -sS -o "$TMP/$name.body" -D "$TMP/$name.head" -w '%{http_code}' -X "$method" \
      -H 'Content-Type: application/json' --data-binary "@$TMP/$name.req" "$url")
  else
    code=$(curl -sS -o "$TMP/$name.body" -D "$TMP/$name.head" -w '%{http_code}' -X "$method" "$url")
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
jf() { python3 -c 'import json,sys
try: b=json.load(open(sys.argv[1]))
except Exception as e: print(f"not-JSON({e})"); sys.exit()
try: print(eval(sys.argv[2]))
except Exception as e: print(f"err({e})")' "$TMP/$1.body" "$2"; }
jeq() { python3 -c 'import json,sys
try: b=json.load(open(sys.argv[1]))
except Exception as e: print(f"not JSON ({e})"); sys.exit()
w=json.loads(sys.argv[2]); bad=[f"{k}={b.get(k)!r} (want {v!r})" for k,v in w.items() if b.get(k)!=v]
print("ok" if not bad else ", ".join(bad))' "$TMP/$1.body" "$2"; }
mk() { python3 -c 'import json,sys; print(json.dumps(eval(sys.argv[1])))' "$1"; }
ids() { jf "$1" '" ".join(str(t["id"]) for t in b) if isinstance(b,list) else "not-a-list"'; }
dates() { jf "$1" '" ".join(t["completionDate"] for t in b) if isinstance(b,list) else "not-a-list"'; }
has() { [[ " $2 " == *" $1 "* ]]; }
dplus() { python3 -c 'import sys,datetime; print(datetime.date.fromisoformat(sys.argv[1])+datetime.timedelta(days=int(sys.argv[2])))' "$TODAY" "$1"; }
# problem code name field: problem+json with the given status naming the field ("" = any)
problem() { local want="$1" c="$2" name="$3" field="$4" ct r
  ct=$(ctype "$name")
  [ "$c" = "$want" ] || { echo "status $c (want $want)"; return; }
  [[ "$ct" == application/problem+json* ]] || { echo "content-type '$ct'"; return; }
  if [ -n "$field" ]; then
    r=$(jf "$name" "any(e.get('field')=='$field' and isinstance(e.get('message'),str) and e['message'].strip() for e in b.get('errors') or [])")
    [ "$r" = True ] || { echo "no errors[] entry with field '$field' and a message ($(head -c 300 "$TMP/$name.body"))"; return; }
  fi
  echo ok; }
new() { # name json -> id (201 expected, else empty)
  local c; c=$(call "$1" POST "$BASE/api/habits" "$2")
  if [ "$c" = 201 ]; then jf "$1" 'b["id"]'; else echo ""; fi; }
done_on() { # name id date -> status code
  call "$1" POST "$BASE/api/habits/$2/completions" "{\"date\":\"$3\"}"; }
report() { if [ -z "$2" ]; then echo "PASS $1"; else echo "FAIL $1:$2"; fi; }

c=$(call appinfo GET "$BASE/api/app-info")
TODAY=$(jf appinfo 'b["now"][:10]')
TOK="H$(date +%s)"
WS=$(python3 -c 'import sys,datetime; d=datetime.date.fromisoformat(sys.argv[1]); print(d-datetime.timedelta(days=d.weekday()))' "$TODAY")
echo "=== today (app time zone) = $TODAY, week start (Monday) = $WS, token $TOK" >> "$LOG"
Y=$(dplus -1)

# ---------- C1: create -> active, createdAt set
f=""
c=$(call C1 POST "$BASE/api/habits" "{\"name\":\"C1 Read $TOK\",\"description\":\"20 pages\",\"frequency\":\"DAILY\"}")
[ "$c" = 201 ] || f="$f 201 vs $c;"
[[ "$(ctype C1)" == application/json* ]] || f="$f content-type '$(ctype C1)';"
r=$(jeq C1 "{\"name\":\"C1 Read $TOK\",\"description\":\"20 pages\",\"frequency\":\"DAILY\",\"active\":true}")
[ "$r" = ok ] || f="$f body: $r;"
r=$(jf C1 '(lambda d: isinstance(b.get("createdAt"),str) and d.datetime.fromisoformat(b["createdAt"]).tzinfo is not None and abs((d.datetime.now(d.timezone.utc)-d.datetime.fromisoformat(b["createdAt"])).total_seconds())<300 and isinstance(b.get("id"),int))(__import__("datetime"))')
[ "$r" = True ] || f="$f createdAt/id not set as date-time near now ($r);"
ID1=$(jf C1 'b["id"]')
c=$(call C1-get GET "$BASE/api/habits/$ID1")
[ "$c" = 200 ] || f="$f GET 200 vs $c;"
r=$(python3 -c 'import json,sys
from datetime import datetime as D
a=json.load(open(sys.argv[1])); b=json.load(open(sys.argv[2]))
same=lambda d:{k:v for k,v in d.items() if k!="createdAt"}
t=abs((D.fromisoformat(a["createdAt"])-D.fromisoformat(b["createdAt"])).total_seconds())
print("ok" if same(a)==same(b) and t<0.001 else f"{a} vs {b}")' "$TMP/C1.body" "$TMP/C1-get.body")
[ "$r" = ok ] || f="$f GET differs: $r;"
c=$(call C1-w POST "$BASE/api/habits" "{\"name\":\"C1 Weekly $TOK\",\"frequency\":\"WEEKLY\"}")
[ "$c" = 201 ] || f="$f weekly 201 vs $c;"
r=$(jeq C1-w '{"frequency":"WEEKLY","active":true,"description":null}')
[ "$r" = ok ] || f="$f weekly body: $r;"
report C1 "$f"

# ---------- C2: BR-6 name
f=""
for v in empty blank missing long; do
  case $v in
    empty) b='{"name":"","frequency":"DAILY"}';; blank) b='{"name":"   ","frequency":"DAILY"}';;
    missing) b='{"description":"x","frequency":"DAILY"}';; long) b=$(mk '{"name":"n"*151,"frequency":"DAILY"}');;
  esac
  c=$(call "C2-$v" POST "$BASE/api/habits" "$b"); r=$(problem 400 "$c" "C2-$v" name)
  [ "$r" = ok ] || f="$f POST $v name: $r;"
done
c=$(call C2-150 POST "$BASE/api/habits" "$(mk '{"name":"n"*150,"frequency":"DAILY"}')")
[ "$c" = 201 ] || f="$f 150-char name 201 vs $c;"
[ "$(jf C2-150 'len(b["name"])')" = 150 ] || f="$f 150-char name not stored whole;"
c=$(call C2-put-empty PUT "$BASE/api/habits/$ID1" '{"name":"","frequency":"DAILY"}'); r=$(problem 400 "$c" C2-put-empty name)
[ "$r" = ok ] || f="$f PUT empty name: $r;"
c=$(call C2-put-long PUT "$BASE/api/habits/$ID1" "$(mk '{"name":"n"*151,"frequency":"DAILY"}')"); r=$(problem 400 "$c" C2-put-long name)
[ "$r" = ok ] || f="$f PUT 151-char name: $r;"
report C2 "$f"

# ---------- C3: description / frequency
f=""
c=$(call C3-long POST "$BASE/api/habits" "$(mk '{"name":"C3 long","description":"d"*2001,"frequency":"DAILY"}')"); r=$(problem 400 "$c" C3-long description)
[ "$r" = ok ] || f="$f 2001 description: $r;"
c=$(call C3-2000 POST "$BASE/api/habits" "$(mk '{"name":"C3 2000","description":"d"*2000,"frequency":"DAILY"}')")
[ "$c" = 201 ] || f="$f 2000 description 201 vs $c;"
[ "$(jf C3-2000 'len(b["description"] or "")')" = 2000 ] || f="$f 2000-char description not stored whole;"
c=$(call C3-nofreq POST "$BASE/api/habits" '{"name":"C3 no freq"}'); r=$(problem 400 "$c" C3-nofreq "")
[ "$r" = ok ] || f="$f missing frequency: $r;"
c=$(call C3-monthly POST "$BASE/api/habits" '{"name":"C3 monthly","frequency":"MONTHLY"}'); r=$(problem 400 "$c" C3-monthly "")
[ "$r" = ok ] || f="$f MONTHLY: $r;"
report C3 "$f"

# ---------- C4: complete for today (default)
f=""
for v in nobody empty dated; do
  id=$(new "C4-$v-new" "{\"name\":\"C4 $v $TOK\",\"frequency\":\"DAILY\"}")
  case $v in
    nobody) c=$(call "C4-$v" POST "$BASE/api/habits/$id/completions");;
    empty) c=$(call "C4-$v" POST "$BASE/api/habits/$id/completions" '{}');;
    dated) c=$(done_on "C4-$v" "$id" "$TODAY");;
  esac
  [ "$c" = 201 ] || f="$f $v 201 vs $c;"
  r=$(jf "C4-$v" "b.get('completionDate')=='$TODAY' and b.get('habitId')==$id and isinstance(b.get('id'),int) and isinstance(b.get('createdAt'),str)")
  [ "$r" = True ] || f="$f $v body: $(head -c 300 "$TMP/C4-$v.body");"
  c=$(call "C4-$v-list" GET "$BASE/api/habits/$id/completions")
  [ "$(dates "C4-$v-list")" = "$TODAY" ] || f="$f $v list: $(dates "C4-$v-list");"
done
report C4 "$f"

# ---------- C5: BR-7 duplicate
f=""
ID5=$(new C5-new "{\"name\":\"C5 dup $TOK\",\"frequency\":\"DAILY\"}")
c=$(call C5-1 POST "$BASE/api/habits/$ID5/completions"); [ "$c" = 201 ] || f="$f first 201 vs $c;"
c=$(call C5-2 POST "$BASE/api/habits/$ID5/completions"); r=$(problem 409 "$c" C5-2 "")
[ "$r" = ok ] || f="$f second (no body): $r;"
r=$(jf C5-2 'bool((b.get("detail") or b.get("title") or "").strip())'); [ "$r" = True ] || f="$f 409 has no message;"
c=$(done_on C5-3 "$ID5" "$TODAY"); r=$(problem 409 "$c" C5-3 ""); [ "$r" = ok ] || f="$f second (dated today): $r;"
c=$(done_on C5-y1 "$ID5" "$Y"); [ "$c" = 201 ] || f="$f yesterday 201 vs $c;"
c=$(done_on C5-y2 "$ID5" "$Y"); r=$(problem 409 "$c" C5-y2 ""); [ "$r" = ok ] || f="$f second yesterday: $r;"
call C5-list GET "$BASE/api/habits/$ID5/completions" >/dev/null
[ "$(dates C5-list)" = "$TODAY $Y" ] || f="$f records: [$(dates C5-list)] vs [$TODAY $Y];"
report C5 "$f"

# ---------- C6: future date rejected
f=""
ID6=$(new C6-new "{\"name\":\"C6 future $TOK\",\"frequency\":\"DAILY\"}")
c=$(done_on C6 "$ID6" "$(dplus 1)"); r=$(problem 400 "$c" C6 date)
[ "$r" = ok ] || f="$f tomorrow: $r;"
call C6-list GET "$BASE/api/habits/$ID6/completions" >/dev/null
[ "$(dates C6-list)" = "" ] || f="$f recorded: $(dates C6-list);"
report C6 "$f"

# ---------- C7: DAILY progress / streak
f=""
prog() { jf "$1" '(b.get("completedToday"), b.get("doneForCurrentPeriod"), b.get("currentStreak"), b.get("frequency"))'; }
ID7=$(new C7-new "{\"name\":\"C7 daily $TOK\",\"frequency\":\"DAILY\"}")
r=$(prog C7-new); [ "$r" = "(False, False, 0, 'DAILY')" ] || f="$f fresh: $r;"
done_on C7-a "$ID7" "$Y" >/dev/null; done_on C7-b "$ID7" "$(dplus -2)" >/dev/null
call C7-g1 GET "$BASE/api/habits/$ID7" >/dev/null
r=$(prog C7-g1); [ "$r" = "(False, False, 2, 'DAILY')" ] || f="$f yesterday+2 days ago: $r (want False, False, 2);"
call C7-c POST "$BASE/api/habits/$ID7/completions" >/dev/null
call C7-g2 GET "$BASE/api/habits/$ID7" >/dev/null
r=$(prog C7-g2); [ "$r" = "(True, True, 3, 'DAILY')" ] || f="$f +today: $r (want True, True, 3);"
IDg=$(new C7-gap "{\"name\":\"C7 gap $TOK\",\"frequency\":\"DAILY\"}")
done_on C7-g-a "$IDg" "$Y" >/dev/null; done_on C7-g-b "$IDg" "$(dplus -3)" >/dev/null
call C7-g3 GET "$BASE/api/habits/$IDg" >/dev/null
r=$(prog C7-g3); [ "$r" = "(False, False, 1, 'DAILY')" ] || f="$f gap: $r (want False, False, 1);"
call C7-list GET "$BASE/api/habits" >/dev/null
r=$(jf C7-list "[(h['currentStreak'],h['doneForCurrentPeriod']) for h in b if h['id']==$ID7]"); [ "$r" = "[(3, True)]" ] || f="$f list progress: $r;"
report C7 "$f"

# ---------- C8: WEEKLY progress / streak (Monday-Sunday)
f=""
wd() { python3 -c 'import sys,datetime; print(datetime.date.fromisoformat(sys.argv[1])+datetime.timedelta(days=int(sys.argv[2])))' "$WS" "$1"; }
ID8=$(new C8-new "{\"name\":\"C8 weekly $TOK\",\"frequency\":\"WEEKLY\"}")
for d in "$WS" "$(wd -7)" "$(wd -14)"; do c=$(done_on C8-d "$ID8" "$d"); [ "$c" = 201 ] || f="$f complete $d 201 vs $c;"; done
call C8-g GET "$BASE/api/habits/$ID8" >/dev/null
r=$(jf C8-g '(b.get("doneForCurrentPeriod"), b.get("currentStreak"), b.get("frequency"))'); [ "$r" = "(True, 3, 'WEEKLY')" ] || f="$f 3 weeks: $r (want True, 3, WEEKLY);"
ID8b=$(new C8b-new "{\"name\":\"C8 sunday $TOK\",\"frequency\":\"WEEKLY\"}")
c=$(done_on C8b-d "$ID8b" "$(wd -1)"); [ "$c" = 201 ] || f="$f complete last Sunday 201 vs $c;"
call C8b-g GET "$BASE/api/habits/$ID8b" >/dev/null
r=$(jf C8b-g '(b.get("doneForCurrentPeriod"), b.get("currentStreak"), b.get("completedToday"))'); [ "$r" = "(False, 1, False)" ] || f="$f last Sunday only: $r (want False, 1, False);"
report C8 "$f"

# ---------- C9: edit
f=""
IDe=$(new C9-new "{\"name\":\"C9 orig $TOK\",\"description\":\"d\",\"frequency\":\"DAILY\"}")
want="{\"name\":\"C9 edited $TOK\",\"description\":\"new desc\",\"frequency\":\"WEEKLY\"}"
c=$(call C9-put PUT "$BASE/api/habits/$IDe" "$want")
[ "$c" = 200 ] || f="$f PUT 200 vs $c;"
r=$(jeq C9-put "$want"); [ "$r" = ok ] || f="$f PUT body: $r;"
c=$(call C9-get GET "$BASE/api/habits/$IDe")
r=$(jeq C9-get "$want"); [ "$r" = ok ] || f="$f GET after PUT: $r;"
r=$(python3 -c 'import json,sys
from datetime import datetime as D
a=json.load(open(sys.argv[1])); b=json.load(open(sys.argv[2]))
x, y = a["createdAt"], b["createdAt"]
print("ok" if D.fromisoformat(x)==D.fromisoformat(y) else "createdAt %s -> %s" % (x, y))' "$TMP/C9-new.body" "$TMP/C9-get.body")
[ "$r" = ok ] || f="$f $r;"
report C9 "$f"

# ---------- C10: deactivate / activate
f=""
IDd=$(new C10-new "{\"name\":\"C10 deact $TOK\",\"frequency\":\"DAILY\"}")
done_on C10-y "$IDd" "$Y" >/dev/null
c=$(call C10-deact POST "$BASE/api/habits/$IDd/deactivate")
[ "$c" = 200 ] && [ "$(jf C10-deact 'b.get("active")')" = False ] || f="$f deactivate: $c $(jf C10-deact 'b.get("active")');"
call C10-act-list GET "$BASE/api/habits?active=true" >/dev/null; has "$IDd" "$(ids C10-act-list)" && f="$f still in active=true;"
[ "$(jf C10-act-list 'all(h["active"] for h in b)')" = True ] || f="$f active=true lists inactive habits;"
call C10-inact GET "$BASE/api/habits?active=false" >/dev/null; has "$IDd" "$(ids C10-inact)" || f="$f not in active=false;"
call C10-all GET "$BASE/api/habits" >/dev/null; has "$IDd" "$(ids C10-all)" || f="$f not in unfiltered list;"
call C10-hist GET "$BASE/api/habits/$IDd/completions" >/dev/null
[ "$(dates C10-hist)" = "$Y" ] || f="$f history: [$(dates C10-hist)];"
c=$(call C10-comp POST "$BASE/api/habits/$IDd/completions"); r=$(problem 409 "$c" C10-comp "")
[ "$r" = ok ] || f="$f complete inactive: $r;"
call C10-hist2 GET "$BASE/api/habits/$IDd/completions" >/dev/null
[ "$(dates C10-hist2)" = "$Y" ] || f="$f recorded while inactive: [$(dates C10-hist2)];"
c=$(call C10-act POST "$BASE/api/habits/$IDd/activate")
[ "$c" = 200 ] && [ "$(jf C10-act 'b.get("active")')" = True ] || f="$f activate: $c $(jf C10-act 'b.get("active")');"
call C10-act-list2 GET "$BASE/api/habits?active=true" >/dev/null; has "$IDd" "$(ids C10-act-list2)" || f="$f not back in active=true;"
c=$(call C10-comp2 POST "$BASE/api/habits/$IDd/completions"); [ "$c" = 201 ] || f="$f complete after activate 201 vs $c;"
report C10 "$f"

# ---------- C11: delete (BR-14)
f=""
IDx=$(new C11-new "{\"name\":\"C11 delete $TOK\",\"frequency\":\"DAILY\"}")
call C11-c1 POST "$BASE/api/habits/$IDx/completions" >/dev/null; done_on C11-c2 "$IDx" "$Y" >/dev/null
c=$(call C11-del DELETE "$BASE/api/habits/$IDx"); [ "$c" = 204 ] || f="$f DELETE 204 vs $c;"
c=$(call C11-get GET "$BASE/api/habits/$IDx"); r=$(problem 404 "$c" C11-get ""); [ "$r" = ok ] || f="$f GET: $r;"
c=$(call C11-comps GET "$BASE/api/habits/$IDx/completions"); r=$(problem 404 "$c" C11-comps ""); [ "$r" = ok ] || f="$f completions: $r;"
for q in "" "?active=true" "?active=false"; do
  call C11-list GET "$BASE/api/habits$q" >/dev/null; has "$IDx" "$(ids C11-list)" && f="$f returned by /api/habits$q;"
done
c=$(call C11-put PUT "$BASE/api/habits/$IDx" '{"name":"x","frequency":"DAILY"}'); [ "$c" = 404 ] || f="$f PUT 404 vs $c;"
for p in deactivate activate completions; do c=$(call "C11-$p" POST "$BASE/api/habits/$IDx/$p"); [ "$c" = 404 ] || f="$f POST $p 404 vs $c;"; done
c=$(call C11-undo DELETE "$BASE/api/habits/$IDx/completions/$TODAY"); [ "$c" = 404 ] || f="$f undo 404 vs $c;"
c=$(call C11-del2 DELETE "$BASE/api/habits/$IDx"); [ "$c" = 404 ] || f="$f second DELETE 404 vs $c;"
c=$(call C11-unk GET "$BASE/api/habits/999999"); [ "$c" = 404 ] || f="$f unknown id 404 vs $c;"
report C11 "$f"

# ---------- C12: undo a completion
f=""
IDu=$(new C12-new "{\"name\":\"C12 undo $TOK\",\"frequency\":\"DAILY\"}")
call C12-c POST "$BASE/api/habits/$IDu/completions" >/dev/null
c=$(call C12-undo DELETE "$BASE/api/habits/$IDu/completions/$TODAY"); [ "$c" = 204 ] || f="$f undo 204 vs $c;"
call C12-list GET "$BASE/api/habits/$IDu/completions" >/dev/null; [ "$(dates C12-list)" = "" ] || f="$f still listed: $(dates C12-list);"
c=$(call C12-undo2 DELETE "$BASE/api/habits/$IDu/completions/$TODAY"); r=$(problem 404 "$c" C12-undo2 ""); [ "$r" = ok ] || f="$f second undo: $r;"
report C12 "$f"

# ---------- C13: swagger / contract
c=$(call C13 GET "$BASE/v3/api-docs")
check_doc() { python3 - "$1" <<'EOF'
import json, sys
try: d = json.load(open(sys.argv[1]))
except Exception as e: print(f"not JSON ({e})"); sys.exit()
want = {("/api/habits","get"):"listHabits", ("/api/habits","post"):"createHabit",
        ("/api/habits/{id}","get"):"getHabit", ("/api/habits/{id}","put"):"updateHabit",
        ("/api/habits/{id}","delete"):"deleteHabit", ("/api/habits/{id}/deactivate","post"):"deactivateHabit",
        ("/api/habits/{id}/activate","post"):"activateHabit", ("/api/habits/{id}/completions","get"):"listHabitCompletions",
        ("/api/habits/{id}/completions","post"):"completeHabit", ("/api/habits/{id}/completions/{date}","delete"):"undoHabitCompletion"}
bad = []
P = d.get("paths", {})
for (p, m), op in want.items():
    got = P.get(p, {}).get(m, {}).get("operationId")
    if got != op: bad.append(f"{m.upper()} {p}: {got!r} vs {op}")
for p, m, code in (("/api/habits","post","201"), ("/api/habits/{id}/completions","post","201"),
                   ("/api/habits/{id}","delete","204"), ("/api/habits/{id}/completions/{date}","delete","204")):
    if code not in P.get(p, {}).get(m, {}).get("responses", {}): bad.append(f"{m.upper()} {p} no {code}")
S = d.get("components", {}).get("schemas", {})
for s in ("Habit", "HabitWrite", "HabitCompletion", "HabitCompletionCreate"):
    if s not in S: bad.append(f"schema {s} missing")
if S.get("HabitFrequency", {}).get("enum") != ["DAILY", "WEEKLY"]: bad.append(f"HabitFrequency {S.get('HabitFrequency')}")
print("ok" if not bad else "; ".join(bad))
EOF
}
f=""
[ "$c" = 200 ] || f="$f api-docs $c;"
r=$(check_doc "$TMP/C13.body"); [ "$r" = ok ] || f="$f live: $r;"
for p in backend/target/openapi.json loops/backend-dev/outputs/openapi.json; do
  if [ -f "$p" ]; then r=$(check_doc "$p"); else r=missing; fi
  echo "=== C13: $p -> $r" >> "$LOG"
  [ "$r" = ok ] || f="$f $p: $r;"
done
r=$(python3 -c 'import json,sys
a=json.load(open(sys.argv[1])); b=json.load(open(sys.argv[2]))
k=lambda d:({p:v for p,v in d["paths"].items() if p.startswith("/api/habits")},{n:v for n,v in d["components"]["schemas"].items() if n.startswith("Habit")})
print("ok" if k(a)==k(b) else "habit paths/schemas differ")' backend/target/openapi.json loops/backend-dev/outputs/openapi.json 2>&1)
echo "=== C13: target vs copy -> $r" >> "$LOG"
[ "$r" = ok ] || f="$f target vs copy: $r;"
report C13 "$f"
