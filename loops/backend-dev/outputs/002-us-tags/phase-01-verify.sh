#!/usr/bin/env bash
# phase-01 (US1 tags) API checks. Usage: phase-01-verify.sh <attempt-folder>
set -u
OUT="${1:?attempt folder}"
BASE="http://localhost:8080"
LOG="$OUT/curl.log"
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
  { echo "=== #$N $method $path ${data:+$data}"; cat "$TMP/last.body"; echo; echo "=== status $code"; echo; } >> "$LOG"
  echo "$code"
}
body() { cat "$TMP/last.body"; }
tags_of() { body | jq -c '.tags'; }
new_task() { # title -> id
  call POST /api/tasks "{\"title\":\"$1\"}" > /dev/null
  body | jq -r '.id'
}
add() { call POST "/api/tasks/$1/tags" "$(jq -cn --argjson t "$2" '{tags:$t}')"; }
ids_for() { # query -> sorted id list
  call GET "/api/tasks?$1" > "$TMP/code"
  body | jq -c '[.[].id] | sort'
}
has_tags_error() { body | jq -e '[.errors[]? | select(.field=="tags" and (.message|length>0))] | length > 0' > /dev/null; }
has_message() { body | jq -e '[.errors[]? | select(.message|length>0)] | length > 0' > /dev/null; }
check() { # id ok-flag detail
  if [ "$2" = 1 ]; then echo "PASS $1"; else echo "FAIL $1: $3"; fi
}

# C1: AC-US1-1 add work + urgent, read back (single and list)
A=$(new_task "C1 task")
c1=$(add "$A" '["work","urgent"]'); t1=$(tags_of)
c2=$(call GET "/api/tasks/$A"); t2=$(tags_of)
call GET /api/tasks > /dev/null; t3=$(body | jq -c --argjson id "$A" '.[] | select(.id==$id) | .tags')
ok=0; [ "$c1" = 200 ] && [ "$t1" = '["urgent","work"]' ] && [ "$c2" = 200 ] && [ "$t2" = '["urgent","work"]' ] && [ "$t3" = '["urgent","work"]' ] && ok=1
check C1 $ok "POST 200 [urgent,work], GET same, list same vs POST $c1 $t1, GET $c2 $t2, list $t3"

# C2: AC-US1-2 filter by work lists only tasks tagged work
W1=$(new_task "C2 w1"); add "$W1" '["work"]' > /dev/null
W2=$(new_task "C2 w2"); add "$W2" '["home","work"]' > /dev/null
N1=$(new_task "C2 n1"); add "$N1" '["home"]' > /dev/null
N2=$(new_task "C2 n2")
N3=$(new_task "C2 n3"); add "$N3" '["workshop"]' > /dev/null
want=$(jq -cn --argjson a "$W1" --argjson b "$W2" --argjson c "$A" '[$a,$b,$c] | sort')
got=$(ids_for "tag=work"); cf=$(cat "$TMP/code")
empty=$(ids_for "tag=nosuchtag"); ce=$(cat "$TMP/code")
ok=0; [ "$cf" = 200 ] && [ "$got" = "$want" ] && [ "$ce" = 200 ] && [ "$empty" = '[]' ] && ok=1
check C2 $ok "tag=work 200 ids $want (C1 task A is tagged work too), nosuchtag 200 [] vs $cf $got, $ce $empty"

# C3: AC-US1-3 remove work
R=$(new_task "C3 task"); add "$R" '["work","home"]' > /dev/null
cd=$(call DELETE "/api/tasks/$R/tags?tag=work"); td=$(tags_of)
cg=$(call GET "/api/tasks/$R"); tg=$(tags_of)
inf=$(ids_for "tag=work" | jq --argjson id "$R" 'index($id) != null')
ok=0; [ "$cd" = 200 ] && [ "$td" = '["home"]' ] && [ "$cg" = 200 ] && [ "$tg" = '["home"]' ] && [ "$inf" = false ] && ok=1
check C3 $ok "DELETE 200 [home], GET [home], not in tag=work vs $cd $td, $cg $tg, listed=$inf"

# C4: AC-US1-4 empty / blank / 31-char rejected on field tags, tags unchanged; 30 chars ok
V=$(new_task "C4 task"); add "$V" '["alpha"]' > /dev/null
t31=$(printf 'a%.0s' $(seq 31)); t30=$(printf 'b%.0s' $(seq 30))
res=""; ok=1
for bad in '[""]' '["   "]' "[\"$t31\"]" "[\"beta\",\"$t31\"]"; do
  c=$(add "$V" "$bad")
  if [ "$c" = 400 ] && has_tags_error; then res="$res 400/tags"; else res="$res $c/$(body | jq -c '.errors' 2>/dev/null)"; ok=0; fi
done
cg=$(call GET "/api/tasks/$V"); tg=$(tags_of)
[ "$cg" = 200 ] && [ "$tg" = '["alpha"]' ] || ok=0
c30=$(add "$V" "[\"$t30\"]"); t30got=$(tags_of)
want30=$(jq -cn --arg b "$t30" '["alpha",$b]')
[ "$c30" = 200 ] && [ "$t30got" = "$want30" ] || ok=0
check C4 $ok "4x 400 with errors[].field=tags, tags [alpha] after, 30 chars 200 vs$res; GET $cg $tg; 30-char $c30 $t30got"

# C5: AC-US1-5 Work / WORK same as work
K=$(new_task "C5 task"); add "$K" '["work"]' > /dev/null
ca=$(add "$K" '["Work"]'); ta=$(tags_of)
cg=$(call GET "/api/tasks/$K"); tg=$(tags_of)
inf=$(ids_for "tag=WORK" | jq --argjson id "$K" 'index($id) != null'); cf=$(cat "$TMP/code")
ok=0; [ "$ca" = 200 ] && [ "$ta" = '["work"]' ] && [ "$cg" = 200 ] && [ "$tg" = '["work"]' ] && [ "$cf" = 200 ] && [ "$inf" = true ] && ok=1
check C5 $ok "add Work 200 [work], GET [work], listed by tag=WORK vs $ca $ta, $cg $tg, $cf listed=$inf"

# C6: AC-US1-6 11th tag rejected, 10 kept
F=$(new_task "C6 task")
ten='["t01","t02","t03","t04","t05","t06","t07","t08","t09","t10"]'
c10=$(add "$F" "$ten"); t10=$(tags_of)
c11=$(add "$F" '["t11"]'); msg=0; has_message && msg=1; fld=$(body | jq -c '[.errors[]?.field]')
cg=$(call GET "/api/tasks/$F"); tg=$(tags_of)
ok=0; [ "$c10" = 200 ] && [ "$t10" = "$ten" ] && [ "$c11" = 400 ] && [ "$msg" = 1 ] && [ "$tg" = "$ten" ] && ok=1
check C6 $ok "10 tags 200, 11th 400 with a message, 10 kept vs $c10 $t10, $c11 msg=$msg fields=$fld, GET $cg $tg"
echo "C6 error fields: $fld" >> "$LOG"
