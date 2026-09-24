#!/usr/bin/env bash
# phase-03 (US1 Manage tasks) API checks. Usage: phase-03-verify.sh <attempt-folder>
set -u
OUT="${1:?attempt folder}"
BASE="http://localhost:8080"
LOG="$OUT/curl.log"
TMP="$(mktemp -d "$OUT/.verify.XXXX")"
trap 'rm -rf "$TMP"' EXIT
N=0

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
# jf name expr: evaluate a python expression on the JSON body `b`
jf() { python3 -c 'import json,sys
try: b=json.load(open(sys.argv[1]))
except Exception as e: print(f"not-JSON({e})"); sys.exit()
try: print(eval(sys.argv[2]))
except Exception as e: print(f"err({e})")' "$TMP/$1.body" "$2"; }
# jeq name json: every key of json equals the body's value -> "ok" or the differences
jeq() { python3 -c 'import json,sys
try: b=json.load(open(sys.argv[1]))
except Exception as e: print(f"not JSON ({e})"); sys.exit()
w=json.loads(sys.argv[2]); bad=[f"{k}={b.get(k)!r} (want {v!r})" for k,v in w.items() if b.get(k)!=v]
print("ok" if not bad else ", ".join(bad))' "$TMP/$1.body" "$2"; }
mk() { python3 -c 'import json,sys; print(json.dumps(eval(sys.argv[1])))' "$1"; }
ids() { jf "$1" '" ".join(str(t["id"]) for t in b) if isinstance(b,list) else "not-a-list"'; }
has() { [[ " $2 " == *" $1 "* ]]; }
dplus() { python3 -c 'import sys,datetime; print(datetime.date.fromisoformat(sys.argv[1])+datetime.timedelta(days=int(sys.argv[2])))' "$TODAY" "$1"; }
# problem name field: 400 problem+json naming the field ("" = any 400 problem)
problem() { local c="$1" name="$2" field="$3" ct r
  ct=$(ctype "$name")
  [ "$c" = 400 ] || { echo "status $c"; return; }
  [[ "$ct" == application/problem+json* ]] || { echo "content-type '$ct'"; return; }
  if [ -n "$field" ]; then
    r=$(jf "$name" "any(e.get('field')=='$field' and isinstance(e.get('message'),str) and e['message'].strip() for e in b.get('errors') or [])")
    [ "$r" = True ] || { echo "no errors[] entry with field '$field' and a message ($(head -c 300 "$TMP/$name.body"))"; return; }
  fi
  echo ok; }
new() { # name json -> id (201 expected, else empty)
  local c; c=$(call "$1" POST "$BASE/api/tasks" "$2")
  if [ "$c" = 201 ]; then jf "$1" 'b["id"]'; else echo ""; fi; }
report() { if [ -z "$2" ]; then echo "PASS $1"; else echo "FAIL $1:$2"; fi; }

c=$(call appinfo GET "$BASE/api/app-info")
TODAY=$(jf appinfo 'b["now"][:10]')
TOK="T$(date +%s)"
echo "=== today (app time zone) = $TODAY, token $TOK" >> "$LOG"

# ---------- C1: create -> TODO, not archived, created/updated set; stored; defaults
f=""
D5=$(dplus 5)
c=$(call C1 POST "$BASE/api/tasks" "{\"title\":\"C1 Buy milk $TOK\",\"description\":\"2 litres\",\"priority\":\"HIGH\",\"dueDate\":\"$D5\"}")
ct=$(ctype C1)
[ "$c" = 201 ] || f="$f 201 vs $c;"
[[ "$ct" == application/json* ]] || f="$f content-type '$ct';"
r=$(jeq C1 "{\"title\":\"C1 Buy milk $TOK\",\"description\":\"2 litres\",\"priority\":\"HIGH\",\"dueDate\":\"$D5\",\"status\":\"TODO\",\"archived\":false,\"completedAt\":null}")
[ "$r" = ok ] || f="$f body: $r;"
r=$(jf C1 '(lambda d: all(isinstance(b.get(k),str) and d.datetime.fromisoformat(b[k]).tzinfo is not None and abs((d.datetime.now(d.timezone.utc)-d.datetime.fromisoformat(b[k])).total_seconds())<300 for k in ("createdAt","updatedAt")) and isinstance(b.get("id"),int))(__import__("datetime"))')
[ "$r" = True ] || f="$f createdAt/updatedAt/id not set as date-times near now ($r);"
ID1=$(jf C1 'b["id"]')
c=$(call C1-get GET "$BASE/api/tasks/$ID1")
[ "$c" = 200 ] || f="$f GET 200 vs $c;"
r=$(python3 -c 'import json,sys; a=json.load(open(sys.argv[1])); b=json.load(open(sys.argv[2])); print("ok" if a==b else f"{a} vs {b}")' "$TMP/C1.body" "$TMP/C1-get.body")
[ "$r" = ok ] || f="$f GET differs: $r;"
c=$(call C1-def POST "$BASE/api/tasks" "{\"title\":\"C1 defaults $TOK\"}")
[ "$c" = 201 ] || f="$f title-only 201 vs $c;"
r=$(jeq C1-def '{"status":"TODO","priority":"MEDIUM","archived":false,"description":null,"dueDate":null}')
[ "$r" = ok ] || f="$f defaults: $r;"
report C1 "$f"

# ---------- C2: BR-1 title
f=""
for v in empty blank missing long; do
  case $v in
    empty) b='{"title":""}';; blank) b='{"title":"   "}';; missing) b='{"description":"x"}';;
    long) b=$(mk '{"title":"t"*201}');;
  esac
  c=$(call "C2-$v" POST "$BASE/api/tasks" "$b"); r=$(problem "$c" "C2-$v" title)
  [ "$r" = ok ] || f="$f POST $v title: $r;"
done
c=$(call C2-200 POST "$BASE/api/tasks" "$(mk '{"title":"t"*200}')")
[ "$c" = 201 ] || f="$f 200-char title 201 vs $c;"
[ "$(jf C2-200 'len(b["title"])')" = 200 ] || f="$f 200-char title not stored whole;"
c=$(call C2-put-empty PUT "$BASE/api/tasks/$ID1" '{"title":"","status":"TODO","priority":"HIGH"}'); r=$(problem "$c" C2-put-empty title)
[ "$r" = ok ] || f="$f PUT empty title: $r;"
c=$(call C2-put-long PUT "$BASE/api/tasks/$ID1" "$(mk '{"title":"t"*201,"status":"TODO","priority":"HIGH"}')"); r=$(problem "$c" C2-put-long title)
[ "$r" = ok ] || f="$f PUT 201-char title: $r;"
report C2 "$f"

# ---------- C3: BR-2 description
f=""
c=$(call C3-long POST "$BASE/api/tasks" "$(mk '{"title":"C3 long desc","description":"d"*2001}')"); r=$(problem "$c" C3-long description)
[ "$r" = ok ] || f="$f POST 2001: $r;"
c=$(call C3-2000 POST "$BASE/api/tasks" "$(mk '{"title":"C3 2000 desc","description":"d"*2000}')")
[ "$c" = 201 ] || f="$f POST 2000 201 vs $c;"
[ "$(jf C3-2000 'len(b["description"] or "")')" = 2000 ] || f="$f 2000-char description not stored whole;"
c=$(call C3-put PUT "$BASE/api/tasks/$ID1" "$(mk '{"title":"C1 x","description":"d"*2001,"status":"TODO","priority":"HIGH"}')"); r=$(problem "$c" C3-put description)
[ "$r" = ok ] || f="$f PUT 2001: $r;"
report C3 "$f"

# ---------- C4: BR-3 status / priority
f=""
c=$(call C4-ps POST "$BASE/api/tasks" '{"title":"C4 bad status","status":"FOO"}'); r=$(problem "$c" C4-ps "")
[ "$r" = ok ] || f="$f POST status FOO: $r;"
c=$(call C4-pp POST "$BASE/api/tasks" '{"title":"C4 bad priority","priority":"FOO"}'); r=$(problem "$c" C4-pp "")
[ "$r" = ok ] || f="$f POST priority FOO: $r;"
c=$(call C4-us PUT "$BASE/api/tasks/$ID1" "{\"title\":\"C4 changed\",\"status\":\"FOO\",\"priority\":\"LOW\"}"); r=$(problem "$c" C4-us "")
[ "$r" = ok ] || f="$f PUT status FOO: $r;"
c=$(call C4-up PUT "$BASE/api/tasks/$ID1" "{\"title\":\"C4 changed\",\"status\":\"DONE\",\"priority\":\"URGENT\"}"); r=$(problem "$c" C4-up "")
[ "$r" = ok ] || f="$f PUT priority URGENT: $r;"
c=$(call C4-un PUT "$BASE/api/tasks/$ID1" "{\"title\":\"C4 changed\",\"status\":null,\"priority\":\"LOW\"}"); r=$(problem "$c" C4-un "")
[ "$r" = ok ] || f="$f PUT status null: $r;"
c=$(call C4-get GET "$BASE/api/tasks/$ID1")
r=$(jeq C4-get "{\"title\":\"C1 Buy milk $TOK\",\"status\":\"TODO\",\"priority\":\"HIGH\"}")
[ "$r" = ok ] || f="$f task changed by a rejected request: $r;"
report C4 "$f"

# ---------- C5: edit -> saved, updatedAt changes
f=""
D3=$(dplus 3); D7=$(dplus 7)
ID5=$(new C5-new "{\"title\":\"C5 orig $TOK\",\"description\":\"d\",\"priority\":\"LOW\",\"dueDate\":\"$D3\"}")
if [ -z "$ID5" ]; then f=" create failed"; else
  sleep 1.2
  want="{\"title\":\"C5 edited $TOK\",\"description\":\"new desc\",\"status\":\"IN_PROGRESS\",\"priority\":\"HIGH\",\"dueDate\":\"$D7\"}"
  c=$(call C5-put PUT "$BASE/api/tasks/$ID5" "$want")
  [ "$c" = 200 ] || f="$f PUT 200 vs $c;"
  r=$(jeq C5-put "$want"); [ "$r" = ok ] || f="$f PUT body: $r;"
  c=$(call C5-get GET "$BASE/api/tasks/$ID5")
  r=$(jeq C5-get "$want"); [ "$r" = ok ] || f="$f GET after PUT: $r;"
  r=$(python3 -c 'import json,sys
from datetime import datetime as D
a=json.load(open(sys.argv[1])); b=json.load(open(sys.argv[2]))
p=D.fromisoformat
print("ok" if p(b["updatedAt"])>p(a["updatedAt"]) and p(b["createdAt"])==p(a["createdAt"]) else "updatedAt %s -> %s, createdAt %s -> %s" % (a["updatedAt"], b["updatedAt"], a["createdAt"], b["createdAt"]))' "$TMP/C5-new.body" "$TMP/C5-get.body")
  [ "$r" = ok ] || f="$f $r;"
fi
report C5 "$f"

# ---------- C6: complete -> DONE, completedAt; back to TODO clears it
f=""
c=$(call C6-complete POST "$BASE/api/tasks/$ID5/complete")
[ "$c" = 200 ] || f="$f complete 200 vs $c;"
r=$(jf C6-complete 'b.get("status")=="DONE" and isinstance(b.get("completedAt"),str)')
[ "$r" = True ] || f="$f complete body: $(head -c 300 "$TMP/C6-complete.body");"
c=$(call C6-get GET "$BASE/api/tasks/$ID5")
[ "$(jf C6-get 'b.get("status")')" = DONE ] || f="$f GET status not DONE;"
c=$(call C6-back PUT "$BASE/api/tasks/$ID5" "{\"title\":\"C5 edited $TOK\",\"status\":\"TODO\",\"priority\":\"HIGH\"}")
r=$(jeq C6-back '{"status":"TODO","completedAt":null}')
[ "$c" = 200 ] && [ "$r" = ok ] || f="$f back to TODO: $c $r;"
c=$(call C6-404 POST "$BASE/api/tasks/999999/complete")
[ "$c" = 404 ] || f="$f complete unknown id 404 vs $c;"
report C6 "$f"

# ---------- C7: archive / restore
f=""
ID7=$(new C7-new "{\"title\":\"C7 archive $TOK\"}")
c=$(call C7-arch POST "$BASE/api/tasks/$ID7/archive")
[ "$c" = 200 ] && [ "$(jf C7-arch 'b.get("archived")')" = True ] || f="$f archive: $c $(jf C7-arch 'b.get("archived")');"
call C7-def GET "$BASE/api/tasks" >/dev/null; L=$(ids C7-def)
has "$ID7" "$L" && f="$f archived task in default list;"
call C7-arl GET "$BASE/api/tasks?archived=true" >/dev/null; L=$(ids C7-arl)
has "$ID7" "$L" || f="$f archived task not in archived=true list ($L);"
[ "$(jf C7-arl 'all(t["archived"] for t in b)')" = True ] || f="$f archived=true list has non-archived tasks;"
c=$(call C7-rest POST "$BASE/api/tasks/$ID7/restore")
[ "$c" = 200 ] && [ "$(jf C7-rest 'b.get("archived")')" = False ] || f="$f restore: $c $(jf C7-rest 'b.get("archived")');"
call C7-def2 GET "$BASE/api/tasks" >/dev/null; L=$(ids C7-def2)
has "$ID7" "$L" || f="$f restored task not back in default list;"
call C7-arl2 GET "$BASE/api/tasks?archived=true" >/dev/null; L=$(ids C7-arl2)
has "$ID7" "$L" && f="$f restored task still in archived=true list;"
report C7 "$f"

# ---------- C8: delete -> never returned again
f=""
Y=$(dplus -1)
ID8=$(new C8-new "{\"title\":\"C8 delete DELQ$TOK\",\"dueDate\":\"$Y\"}")
call C8-pre GET "$BASE/api/tasks/overdue" >/dev/null
has "$ID8" "$(ids C8-pre)" || f="$f (precondition) not in overdue before delete;"
c=$(call C8-del DELETE "$BASE/api/tasks/$ID8")
[ "$c" = 204 ] || f="$f DELETE 204 vs $c;"
c=$(call C8-get GET "$BASE/api/tasks/$ID8")
[ "$c" = 404 ] || f="$f GET after delete 404 vs $c;"
[[ "$(ctype C8-get)" == application/problem+json* ]] || f="$f GET 404 content-type '$(ctype C8-get)';"
for q in "" "?archived=true" "?q=DELQ$TOK" "/overdue"; do
  call C8-list GET "$BASE/api/tasks$q" >/dev/null
  has "$ID8" "$(ids C8-list)" && f="$f returned by /api/tasks$q;"
done
for op in "PUT " "POST /complete" "POST /archive" "POST /restore" "DELETE "; do
  m=${op%% *}; p=${op#* }
  if [ "$m" = PUT ]; then c=$(call C8-op PUT "$BASE/api/tasks/$ID8" '{"title":"x","status":"TODO","priority":"LOW"}')
  else c=$(call C8-op "$m" "$BASE/api/tasks/$ID8$p"); fi
  [ "$c" = 404 ] || f="$f $m $p after delete 404 vs $c;"
done
c=$(call C8-unk GET "$BASE/api/tasks/999999")
[ "$c" = 404 ] || f="$f unknown id 404 vs $c;"
report C8 "$f"

# ---------- C9: title search, ignoring case
f=""
K="zyxq${TOK#T}"
A=$(new C9-a "{\"title\":\"Report ZYXQ${TOK#T} alpha\"}")
B=$(new C9-b "{\"title\":\"$K lower\"}")
C=$(new C9-c "{\"title\":\"Mixed ZyXq${TOK#T} case\"}")
O=$(new C9-o "{\"title\":\"Other ${TOK#T}\",\"description\":\"$K only in description\"}")
want=$(echo "$A $B $C" | tr ' ' '\n' | sort | tr '\n' ' ')
for q in "$K" "ZYXQ${TOK#T}" "yxq${TOK#T}"; do
  call C9-q GET "$BASE/api/tasks?q=$q" >/dev/null
  got=$(ids C9-q | tr ' ' '\n' | sort | tr '\n' ' ')
  [ "$got" = "$want" ] || f="$f q=$q: [$got] vs [$want];"
done
report C9 "$f"

# ---------- C10: filters + sort
f=""
F="flt${TOK#T}"
mkt() { new "C10-$1" "{\"title\":\"$F $1\",\"status\":\"$2\",\"priority\":\"$3\"$( [ -n "$4" ] && echo ",\"dueDate\":\"$(dplus "$4")\"")}"; sleep 0.05; }
Ta=$(mkt a TODO HIGH 1); Tb=$(mkt b IN_PROGRESS HIGH 3); Tc=$(mkt c TODO LOW 5)
Td=$(mkt d DONE HIGH 2); Te=$(mkt e TODO HIGH ""); Tf=$(mkt f TODO MEDIUM 3)
nm() { local s=""; for i in $1; do case $i in "$Ta") s="$s a";; "$Tb") s="$s b";; "$Tc") s="$s c";; "$Td") s="$s d";; "$Te") s="$s e";; "$Tf") s="$s f";; *) s="$s ?$i";; esac; done; echo "${s# }"; }
setq() { # label query want-set ignore-e
  call C10-q GET "$BASE/api/tasks?q=$F&$2" >/dev/null
  local got; got=$(nm "$(ids C10-q)" | tr ' ' '\n' | { if [ "$4" = 1 ]; then grep -v '^e$'; else cat; fi; } | sort | tr '\n' ' ')
  [ "$got" = "$3 " ] || f="$f $1 ($2): [$got] vs [$3];"; }
setq status "status=TODO" "a c e f" 0
setq priority "priority=HIGH" "a b d e" 0
setq both "status=TODO&priority=HIGH" "a e" 0
setq range "dueFrom=$(dplus 2)&dueTo=$(dplus 3)" "b d f" 1
setq from "dueFrom=$(dplus 3)" "b c f" 1
setq to "dueTo=$(dplus 2)" "a d" 1
setq high-from "priority=HIGH&dueFrom=$(dplus 2)" "b d" 1
ordq() { # label query expected-order (e ignored when 4th arg = 1)
  call C10-o GET "$BASE/api/tasks?q=$F&$2" >/dev/null
  local got; got=$(nm "$(ids C10-o)"); [ "$4" = 1 ] && got=$(echo "$got" | tr ' ' '\n' | grep -v '^e$' | tr '\n' ' ' | sed 's/ $//')
  [ "$got" = "$3" ] || f="$f $1 ($2): [$got] vs [$3];"; }
ordq created-asc "sort=createdAt&direction=asc" "a b c d e f" 0
ordq created-desc "sort=createdAt&direction=desc" "f e d c b a" 0
ordq default "" "f e d c b a" 0
ordq todo-due-asc "status=TODO&sort=dueDate&direction=asc" "a f c" 1
for dir in asc desc; do
  call C10-d GET "$BASE/api/tasks?q=$F&sort=dueDate&direction=$dir" >/dev/null
  r=$(jf C10-d "(lambda d: 'ok' if len(b)==6 and d==sorted(d, reverse=('$dir'=='desc')) else f'{len(b)} tasks, due dates {d}')([t['dueDate'] for t in b if t.get('dueDate')])")
  [ "$r" = ok ] || f="$f sort dueDate $dir: $r;"
done
report C10 "$f"

# ---------- C11: overdue
f=""
Y=$(dplus -1)
o1=$(new C11-o1 "{\"title\":\"C11 o1\",\"dueDate\":\"$Y\"}")
o2=$(new C11-o2 "{\"title\":\"C11 o2\",\"status\":\"IN_PROGRESS\",\"dueDate\":\"$(dplus -4)\"}")
n1=$(new C11-n1 "{\"title\":\"C11 n1 today\",\"dueDate\":\"$TODAY\"}")
n2=$(new C11-n2 "{\"title\":\"C11 n2 done\",\"status\":\"DONE\",\"dueDate\":\"$Y\"}")
n3=$(new C11-n3 "{\"title\":\"C11 n3 archived\",\"dueDate\":\"$Y\"}")
call C11-arch POST "$BASE/api/tasks/$n3/archive" >/dev/null
n4=$(new C11-n4 "{\"title\":\"C11 n4 no due\"}")
n5=$(new C11-n5 "{\"title\":\"C11 n5 tomorrow\",\"dueDate\":\"$(dplus 1)\"}")
[ -n "$o1$o2$n1$n2$n3$n4$n5" ] || f="$f creates failed;"
c=$(call C11-list GET "$BASE/api/tasks/overdue")
[ "$c" = 200 ] || f="$f overdue 200 vs $c;"
L=$(ids C11-list)
for i in "$o1" "$o2"; do has "$i" "$L" || f="$f $i (overdue) missing;"; done
for i in "$n1" "$n2" "$n3" "$n4" "$n5"; do has "$i" "$L" && f="$f $i listed as overdue;"; done
r=$(jf C11-list "[t['id'] for t in b if not (t.get('dueDate') and t['dueDate']<'$TODAY' and t['status']!='DONE' and not t['archived'] and t.get('overdue') is True)]")
[ "$r" = "[]" ] || f="$f listed tasks breaking the rule: $r;"
for p in "o1:$o1:True" "o2:$o2:True" "n1:$n1:False" "n2:$n2:False" "n3:$n3:False" "n4:$n4:False" "n5:$n5:False"; do
  IFS=: read -r lbl id want <<< "$p"
  call C11-get GET "$BASE/api/tasks/$id" >/dev/null
  r=$(jf C11-get 'b.get("overdue")'); [ "$r" = "$want" ] || f="$f $lbl overdue flag $r vs $want;"
done
report C11 "$f"

# ---------- C12: swagger / contract
c=$(call C12 GET "$BASE/v3/api-docs")
check_doc() { python3 - "$1" <<'EOF'
import json, sys
try: d = json.load(open(sys.argv[1]))
except Exception as e: print(f"not JSON ({e})"); sys.exit()
want = {("/api/tasks","get"):"listTasks", ("/api/tasks","post"):"createTask",
        ("/api/tasks/overdue","get"):"listOverdueTasks", ("/api/tasks/{id}","get"):"getTask",
        ("/api/tasks/{id}","put"):"updateTask", ("/api/tasks/{id}","delete"):"deleteTask",
        ("/api/tasks/{id}/complete","post"):"completeTask", ("/api/tasks/{id}/archive","post"):"archiveTask",
        ("/api/tasks/{id}/restore","post"):"restoreTask"}
bad = []
P = d.get("paths", {})
for (p, m), op in want.items():
    got = P.get(p, {}).get(m, {}).get("operationId")
    if got != op: bad.append(f"{m.upper()} {p}: {got!r} vs {op}")
if "201" not in P.get("/api/tasks", {}).get("post", {}).get("responses", {}): bad.append("createTask no 201")
if "204" not in P.get("/api/tasks/{id}", {}).get("delete", {}).get("responses", {}): bad.append("deleteTask no 204")
S = d.get("components", {}).get("schemas", {})
for s in ("Task", "TaskCreate", "TaskUpdate"):
    if s not in S: bad.append(f"schema {s} missing")
if S.get("TaskStatus", {}).get("enum") != ["TODO", "IN_PROGRESS", "DONE"]: bad.append(f"TaskStatus {S.get('TaskStatus')}")
if S.get("TaskPriority", {}).get("enum") != ["LOW", "MEDIUM", "HIGH"]: bad.append(f"TaskPriority {S.get('TaskPriority')}")
print("ok" if not bad else "; ".join(bad))
EOF
}
f=""
[ "$c" = 200 ] || f="$f api-docs $c;"
r=$(check_doc "$TMP/C12.body"); [ "$r" = ok ] || f="$f live: $r;"
for p in backend/target/openapi.json loops/backend-dev/outputs/openapi.json; do
  if [ -f "$p" ]; then r=$(check_doc "$p"); else r=missing; fi
  echo "=== C12: $p -> $r" >> "$LOG"
  [ "$r" = ok ] || f="$f $p: $r;"
done
r=$(python3 -c 'import json,sys
a=json.load(open(sys.argv[1])); b=json.load(open(sys.argv[2]))
k=lambda d:({p:v for p,v in d["paths"].items() if p.startswith("/api/tasks")},{n:v for n,v in d["components"]["schemas"].items() if n.startswith("Task")})
print("ok" if k(a)==k(b) else "task paths/schemas differ")' backend/target/openapi.json loops/backend-dev/outputs/openapi.json 2>&1)
echo "=== C12: target vs copy -> $r" >> "$LOG"
[ "$r" = ok ] || f="$f target vs copy: $r;"
report C12 "$f"
