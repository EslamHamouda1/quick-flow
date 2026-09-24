#!/usr/bin/env bash
# run-loop.sh: the headless runner. One `claude -p` call = one session = one step.
#
#   scripts/run-loop.sh <loop> <requirements-file> [openapi] [ui-url]
#       [--until phase-XX | --until-story USn] [--unblock phase-XX]
#       [--auto-approve] [--allow-dirty-engine]
#
# <loop> is backend-dev | frontend-dev (dev cycle) or testing (suite cycle).
# It owns the review gate, unit tests, servers, the guard, accounting and commits.
# It never pushes: review and push with scripts/review-commits.sh.
#
# Exit codes (read by orchestrate.sh): 0 complete/until reached, 3 waiting, 4 blocked,
# 5 bugs_open, 10 quit, 11 cost_cap, 12 harness, 13 violation, 14 needs_input, 2 usage.
set -euo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"
CTL=(python3 scripts/lib/loopctl.py)
ctl() { "${CTL[@]}" "$@"; }

die() { echo "run-loop: $*" >&2; exit 2; }
say() { printf '\033[1m[run-loop %s]\033[0m %s\n' "$(date +%H:%M:%S)" "$*"; }

# ------------------------------------------------------------------ arguments
LOOP="${1:-}"; REQ="${2:-}"
[[ -n "$LOOP" && -n "$REQ" ]] || die "usage: run-loop.sh <loop> <requirements-file> [openapi] [ui-url] [--until phase-XX|--until-story USn] [--unblock phase-XX] [--auto-approve] [--allow-dirty-engine]"
shift 2
POSITIONAL=(); UNTIL=""; UNTIL_STORY=""; UNBLOCK=""; AUTO=0; ALLOW_DIRTY=0
while (($#)); do
  case "$1" in
    --until) UNTIL="$2"; shift 2 ;;
    --until-story) UNTIL_STORY="$2"; shift 2 ;;
    --unblock) UNBLOCK="$2"; shift 2 ;;
    --auto-approve) AUTO=1; shift ;;
    --allow-dirty-engine) ALLOW_DIRTY=1; shift ;;
    --*) die "unknown flag $1" ;;
    *) POSITIONAL+=("$1"); shift ;;
  esac
done
OPENAPI_OVERRIDE="${POSITIONAL[0]:-}"
UIURL_OVERRIDE="${POSITIONAL[1]:-}"
case "$LOOP" in backend-dev|frontend-dev|testing|orchestrator) ;; *) die "unknown loop: $LOOP" ;; esac
[[ -f "$REQ" ]] || die "requirements file not found: $REQ"
[[ "${AUTO_APPROVE:-0}" == 1 ]] && AUTO=1

# ------------------------------------------------------------------ lock + config
if [[ "${LOOP_LOCK_HELD:-0}" != 1 ]]; then
  mkdir -p loops
  exec 9>loops/.run.lock
  flock -n 9 || die "another run-loop.sh / orchestrate.sh / review-commits.sh holds loops/.run.lock; runs never overlap"
fi
[[ -f project.config.yaml ]] || die "project.config.yaml is missing. Copy project.config.example.yaml, fill it in, then rerun."
ctl check-config >/dev/null

FEATURE=$(ctl feature "$REQ")
export FEATURE SPECIFY_FEATURE_NO_PERSIST=1

ENGINE_PATHS=(.claude/skills .claude/settings.json CLAUDE.md .mcp.json scripts .githooks .specify)
for f in loops/*/Loop-instructions.md; do ENGINE_PATHS+=("$f"); done
ENGINE_HASH=$(git log -1 --format=%h -- "${ENGINE_PATHS[@]}" 2>/dev/null || true)
if [[ -n "$(git status --porcelain -- "${ENGINE_PATHS[@]}")" || -z "$ENGINE_HASH" ]]; then
  if ((ALLOW_DIRTY)); then
    ENGINE_VERSION="${ENGINE_HASH:-none}-dirty"
  else
    die "engine files have uncommitted changes (or were never committed); commit them first so engine_version shows what ran, or pass --allow-dirty-engine in a dry-run worktree"
  fi
else
  ENGINE_VERSION="$ENGINE_HASH"
fi

RUN_ID="${RUN_ID:-$(date +%Y%m%dT%H%M%S)-$$}"
export RUN_ID LOOP_RUNNER=1
MODEL=$(ctl cfg claude.model)
MAX_TURNS=$(ctl cfg claude.max_turns)
SESSION_TIMEOUT=$(ctl cfg claude.timeout)
MAX_COST=$(ctl cfg claude.max_run_cost_usd)
UNIT_TIMEOUT=$(ctl cfg unit_timeout 30m)
SERVER_TIMEOUT=$(ctl cfg server_timeout 180)
mapfile -t EXTRA_ARGS < <(ctl cfg claude.extra_args '[]' | jq -r '.[]')
NONCE=$(head -c 12 /dev/urandom | od -An -tx1 | tr -d ' \n')

layer_of_loop() { ctl cfg layers | jq -r --arg l "$1" 'to_entries[] | select(.value.loop==$l) | .key'; }
LAYER=""
case "$LOOP" in testing|orchestrator) ;; *) LAYER=$(layer_of_loop "$LOOP") ;; esac
[[ -n "$LAYER" ]] && export SPECIFY_FEATURE_DIRECTORY="specs/${FEATURE}-${LAYER}"

ctl ensure-progress "$LOOP"
ctl ensure-progress testing

# ------------------------------------------------------------------ servers
SERVER_PIDS=()
stop_servers() {
  local pid
  for pid in "${SERVER_PIDS[@]:-}"; do
    [[ -n "$pid" ]] || continue
    kill -TERM -- "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null || true
  done
  for pid in "${SERVER_PIDS[@]:-}"; do
    [[ -n "$pid" ]] || continue
    for _ in $(seq 1 20); do kill -0 "$pid" 2>/dev/null || break; sleep 0.5; done
    kill -KILL -- "-$pid" 2>/dev/null || true
  done
  SERVER_PIDS=()
}
trap stop_servers EXIT
trap 'stop_servers; exit 130' INT TERM

wait_url() { # url timeout-seconds
  local end=$((SECONDS + $2))
  while ((SECONDS < end)); do
    curl -fsS -o /dev/null -m 5 "$1" 2>/dev/null && return 0
    sleep 2
  done
  return 1
}

# start_servers <run-dir> <layer...>: fresh test DB, start each layer, wait for it.
start_servers() {
  local dir="$1"; shift
  local l cmd url glob
  mkdir -p "$dir"
  for l in "$@"; do
    glob=$(ctl cfg "layers.$l.test_db_glob" "")
    if [[ -n "$glob" ]]; then
      # shellcheck disable=SC2086
      rm -rf $glob
    fi
  done
  for l in "$@"; do
    cmd=$(ctl cfg "layers.$l.run_test" "")
    [[ -n "$cmd" ]] || continue
    setsid bash -c "$cmd" >"$dir/server-$l.log" 2>&1 </dev/null &
    SERVER_PIDS+=($!)
  done
  for l in "$@"; do
    url=$(ctl cfg "layers.$l.ready_url" "")
    [[ -n "$url" ]] || continue
    if ! wait_url "$url" "$SERVER_TIMEOUT"; then
      HARNESS_MSG="$l didn't start within ${SERVER_TIMEOUT}s, see $dir/server-$l.log"
      stop_servers
      return 1
    fi
  done
}

layers_for() { # layers a test of layer $1 needs running (itself + its `needs`)
  { ctl cfg "layers.$1.needs" '[]' | jq -r '.[]'; echo "$1"; } | awk '!seen[$0]++'
}

point_current() { # loops/testing/runs/current -> attempt folder (Playwright --output-dir)
  mkdir -p "$1"
  ln -sfn "$(realpath --relative-to=loops/testing/runs "$1")" loops/testing/runs/current
}

# ------------------------------------------------------------------ unit tests (runner-run)
# unit_tests <layer> <outdir> -> prints pass | fail | compile_error | harness | none
unit_tests() {
  local l="$1" out="$2" cmd ec to=0
  mkdir -p "$out"
  cmd=$(ctl cfg "layers.$l.test" "")
  if [[ -z "$cmd" ]]; then
    printf '{"layer":"%s","outcome":"none","reason":"no unit test command configured"}\n' "$l" >"$out/unit-result.json"
    echo none; return
  fi
  set +e
  timeout "$UNIT_TIMEOUT" bash -c "$cmd" >"$out/unit-run.log" 2>&1 </dev/null
  ec=$?
  set -e
  ((ec == 124)) && to=1
  ctl unit-result "$l" "$out" "$out/unit-run.log" "$ec" "$to"
}

# ------------------------------------------------------------------ finish
STOP_CODE=0
finish() { # reason [exit-code]
  local reason="$1" code="${2:-0}"
  stop_servers
  if [[ "$LOOP" == testing ]]; then
    mkdir -p "loops/testing/state/$FEATURE"
    ctl set-stop testing "$FEATURE" "$reason"
  else
    ctl set-stop "$LOOP" "$FEATURE" "$reason"
  fi
  python3 scripts/progress-table.py >/dev/null || true
  if [[ -n "$(git status --porcelain)" ]]; then
    git add -A && git commit -q -m "chore($LOOP): progress" -m "stop_reason: $reason" && say "committed chore($LOOP): progress"
  fi
  say "stop_reason: $reason"
  local n
  if git rev-parse -q --verify '@{u}' >/dev/null 2>&1; then n=$(git rev-list --count '@{u}..HEAD'); else n=$(git rev-list --count HEAD 2>/dev/null || echo 0); fi
  say "$n unpushed commits — review with scripts/review-commits.sh"
  exit "$code"
}

cost_check() {
  local spent
  spent=$(ctl cost "$RUN_ID")
  if awk -v s="$spent" -v m="$MAX_COST" 'BEGIN{exit !(s>=m)}'; then
    finish "cost_cap: run $RUN_ID spent \$$spent of \$$MAX_COST; rerun with a higher claude.max_run_cost_usd to continue" 11
  fi
}

commit() { # subject body
  git add -A
  git diff --cached --quiet && return 0
  git commit -q -m "$1" -m "${2:-}"
  say "committed: $1"
}

# ------------------------------------------------------------------ session wrapper
ERROR_STREAK=0
LAST_SESSION_ID=""
# run_session <loop> <stage> <kind> <mode> <milestone> <attempt> <target-phase> <prompt> <goal> <context>
run_session() {
  local loop="$1" stage="$2" kind="$3" mode="$4" milestone="$5" attempt="$6" target="$7"
  local prompt="$8" goal="$9" context="${10}"
  cost_check
  local snap out err started ended ec result="" viol="" flags allow deny sid
  snap=$(mktemp -d); out=$(mktemp); err=$(mktemp)
  ctl snapshot "$snap"
  flags=$(ctl flags "$loop" "$stage")
  allow=$(sed -n 1p <<<"$flags"); deny=$(sed -n 2p <<<"$flags")
  say "session: $loop $stage $milestone (attempt $attempt)"
  started=$(date -Iseconds)
  set +e
  # --foreground keeps claude in the runner's process group: without it, claude touching the
  # terminal makes job control stop the whole runner (seen in the first dry run)
  # isolation: no user-level settings (their hooks would gate the loop's commands) and only the
  # project's MCP servers, so every machine runs the same session
  timeout --foreground "$SESSION_TIMEOUT" claude -p "$prompt" \
    --setting-sources project,local --strict-mcp-config --mcp-config .mcp.json \
    --output-format json --permission-mode acceptEdits \
    --model "$MODEL" --max-turns "$MAX_TURNS" \
    --allowedTools "$allow" --disallowedTools "$deny" "${EXTRA_ARGS[@]}" \
    >"$out" 2>"$err" </dev/null
  ec=$?
  set -e
  ended=$(date -Iseconds)
  ((ec == 124)) && result="error:timeout"
  if ! viol=$(ctl guard "$loop" "$stage" "$FEATURE" "$target" "$snap"); then
    result="violation: $(tr '\n' ';' <<<"$viol")"
  fi
  sid=$(ctl account "$out" run_id="$RUN_ID" engine_version="$ENGINE_VERSION" \
    started_at="$started" ended_at="$ended" loop="$loop" mode="$mode" feature="$FEATURE" \
    milestone="$milestone" kind="$kind" attempt="$attempt" model="$MODEL" \
    phase_goal="$goal" prompt="$prompt" context="$context" result="$result")
  mkdir -p "loops/$loop/runs/sessions"
  cp "$out" "loops/$loop/runs/sessions/${sid:-nosid-$(date +%s)}.json"
  [[ -s "$err" ]] && cp "$err" "loops/$loop/runs/sessions/${sid:-nosid-$(date +%s)}.stderr.log"
  ctl human-read-by "$loop" "$milestone" "$sid"
  python3 scripts/progress-table.py >/dev/null || say "progress-table.py failed (see its output)"
  local limit_msg
  limit_msg=$(jq -r 'select(.is_error == true) | .result // empty' "$out" 2>/dev/null | grep -iE 'session limit|usage limit|rate limit' | head -1 || true)
  rm -rf "$snap" "$out" "$err"
  LAST_SESSION_ID="$sid"
  # the account's usage limit is not the loop's fault: stop cleanly, rerun after the reset
  [[ -n "$limit_msg" ]] && finish "usage_limit: $limit_msg (rerun the same command after the reset; state is kept)" 15
  if [[ "$result" == violation* ]]; then
    echo "$viol" >&2
    finish "violation: $loop $stage session $sid (restored: $(tr '\n' ';' <<<"$viol"))" 13
  fi
  if [[ "$result" == error* ]] || ((ec != 0)); then
    ERROR_STREAK=$((ERROR_STREAK + 1))
    say "session ended with an error ($result, exit $ec); the step keeps its status and will be resumed"
    ((ERROR_STREAK >= 3)) && finish "harness: 3 sessions in a row ended in error (last: $sid)" 12
  else
    ERROR_STREAK=0
  fi
  return 0
}

harness_fail() { # message
  local n
  n=$(ctl harness "$LOOP" inc "$1")
  say "harness failure $n/3: $1"
  ((n >= 3)) && finish "harness blocked: $1" 12
  return 0
}

# ------------------------------------------------------------------ review gate
human() { ctl human "loop=$LOOP" "feature=$FEATURE" "$@"; }

show_file() { [[ -f "$1" ]] && { printf '\n\033[1m===== %s =====\033[0m\n' "$1"; cat "$1"; } || true; }

gate() { # phase
  local pid="$1" rf pf q choice notes before_r before_p diff other
  rf="loops/$LOOP/outputs/$FEATURE/$pid-review.md"
  pf="loops/$LOOP/outputs/$FEATURE/$pid.md"
  q=$(ctl questions "$rf")
  if ((AUTO)); then
    if [[ -n "$q" ]]; then
      echo "$q"
      finish "needs_input: $LOOP $pid has open questions and --auto-approve has nobody to answer them" 14
    fi
    ctl approve "$LOOP" "$FEATURE" "$pid" auto ""
    ctl log-action "$LOOP" "gate $FEATURE $pid: approved (auto)"
    human milestone="$pid" gate=review decision=auto
    return
  fi
  while true; do
    show_file "$rf"; show_file "$pf"
    [[ -n "$q" ]] && printf '\n\033[1mOpen questions to answer in your notes:\033[0m\n%s\n' "$q"
    local opts="[a]pprove / [e]dit / [s]kip / [q]uit"
    [[ "$pid" == fix-BUG-* ]] && opts+=" / [r]eassign"
    read -r -p "$LOOP $FEATURE $pid — $opts: " choice </dev/tty
    case "$choice" in
      a)
        read -r -p "notes (answers to open questions; empty for none): " notes </dev/tty
        ctl approve "$LOOP" "$FEATURE" "$pid" approve "$notes"
        ctl log-action "$LOOP" "gate $FEATURE $pid: approved${notes:+ — notes: $notes}"
        human milestone="$pid" gate=review decision=approve notes="$notes"
        return ;;
      e)
        before_r=$(mktemp); before_p=$(mktemp)
        cp "$rf" "$before_r" 2>/dev/null || :; cp "$pf" "$before_p" 2>/dev/null || :
        ${EDITOR:-vi} "$rf" "$pf" </dev/tty >/dev/tty
        diff=$( { diff -u "$before_r" "$rf"; diff -u "$before_p" "$pf"; } || true)
        rm -f "$before_r" "$before_p"
        ctl log-action "$LOOP" "gate $FEATURE $pid: edited review/phase files"
        human milestone="$pid" gate=review decision=edit edit_diff="$diff"
        q=$(ctl questions "$rf") ;;
      s)
        ctl skip "$LOOP" "$FEATURE" "$pid"
        ctl log-action "$LOOP" "gate $FEATURE $pid: skipped"
        human milestone="$pid" gate=review decision=skip
        return ;;
      q)
        human milestone="$pid" gate=review decision=quit
        finish "quit: at the $pid review gate" 10 ;;
      r)
        if [[ "$pid" == fix-BUG-* ]]; then
          other=$(ctl reassign "$LOOP" "$FEATURE" "$pid")
          ctl log-action "$LOOP" "gate $FEATURE $pid: bug reassigned to $other"
          human milestone="$pid" gate=review decision=reassign notes="to $other"
          return
        fi ;;
    esac
  done
}

# ------------------------------------------------------------------ swagger input for frontend-dev
swagger_for() { # stage -> path ("" if this layer takes none)
  local from
  [[ -n "$OPENAPI_OVERRIDE" ]] && { echo "$OPENAPI_OVERRIDE"; return; }
  from=$(ctl cfg "layers.$LAYER.contract_from" "")
  [[ -n "$from" ]] || return 0
  if [[ "$1" == plan ]]; then
    echo "specs/${FEATURE}-${from}/contracts/openapi.yaml"
  else
    ctl cfg "layers.$from.openapi_copy"
  fi
}

write_task() { # extra key=value...   (writes loops/${TASK_LOOP:-$LOOP}/task.md)
  ctl write-task "${TASK_LOOP:-$LOOP}" "requirements=$REQ" "feature=$FEATURE" "loop=$LOOP" \
    "mode=${MODE:-dev}" "stops_at=${UNTIL:-${UNTIL_STORY:-end of loop}}" \
    "review_gates=$( ((AUTO)) && echo 'off (--auto-approve)' || echo on)" \
    "run_id=$RUN_ID" "runner_nonce=$NONCE" \
    ${ORCH_STEP:+"orchestrator_step=$ORCH_STEP"} "$@"
}

# ------------------------------------------------------------------ phase-mode test
test_phase() { # phase attempt
  local pid="$1" attempt="$2" run unit l outcome v verdict sid layers
  run="loops/testing/runs/$FEATURE/$LOOP/$pid/attempt-$attempt"
  mkdir -p "$run"
  rm -f "loops/testing/state/$FEATURE/verdicts/$LOOP/$pid.json"  # only this attempt's verdict counts
  outcome=$(unit_tests "$LAYER" "$run/unit")
  say "unit tests ($LAYER): $outcome"
  case "$outcome" in
    harness)
      harness_fail "unit test run: $(jq -r .reason "$run/unit/unit-result.json") (see $run/unit/unit-run.log)"
      return ;;
    compile_error)
      ctl write-verdict "$LOOP" "$FEATURE" "$pid" "$attempt" fail "compile error: see $run/unit/unit-run.log"
      mkdir -p "loops/testing/outputs/$FEATURE/$LOOP"
      { echo "# $LOOP $pid attempt $attempt: fail (compile error)"; echo
        echo "Written by the runner: the code doesn't compile, so no servers or testing session were started."; echo
        echo '```'; tail -n 80 "$run/unit/unit-run.log"; echo '```'; } >"loops/testing/outputs/$FEATURE/$LOOP/$pid-report.md"
      handle_verdict "$pid" "$attempt" ""
      return ;;
  esac
  mapfile -t layers < <(layers_for "$LAYER")
  HARNESS_MSG=""
  if ! start_servers "$run" "${layers[@]}"; then
    harness_fail "$HARNESS_MSG"
    return
  fi
  ctl harness "$LOOP" reset >/dev/null
  point_current "$run"
  TASK_LOOP=testing MODE=phase write_task "target_loop=$LOOP" "phase=$pid" \
    "dev_attempt=$attempt" "run_dir=$run" "unit_result=$run/unit/unit-result.json"
  run_session testing test test phase "$LOOP:$pid" "$attempt" "$pid" \
    "/testing phase $LOOP $pid --runner $NONCE" \
    "$(ctl phase-goal "$LOOP" "$FEATURE" "$pid")" \
    "loops/$LOOP/outputs/$FEATURE/$pid.md; $run/unit/unit-result.json"
  sid="$LAST_SESSION_ID"
  stop_servers
  handle_verdict "$pid" "$attempt" "$sid"
}

handle_verdict() { # phase attempt test-session-id
  local pid="$1" attempt="$2" sid="$3" v verdict run notes q status
  run="loops/testing/runs/$FEATURE/$LOOP/$pid/attempt-$attempt"
  v=$(ctl verdict "$LOOP" "$FEATURE" "$pid")
  verdict=$(jq -r '.verdict // empty' <<<"$v")
  [[ "$(jq -r '.attempt // empty' <<<"$v")" == "$attempt" ]] || verdict=""
  if [[ "$verdict" == fail ]]; then ctl traces "$run" keep; else ctl traces "$run" drop; fi
  ctl evidence "$LOOP" "$FEATURE" "$pid" "$attempt" >/dev/null
  case "$verdict" in
    pass)
      ctl set-phase "$LOOP" "$FEATURE" "$pid" status=passed "verified_by=$sid"
      ctl log-action "$LOOP" "test $FEATURE $pid attempt $attempt: pass (verified_by $sid)" ;;
    fail)
      status=$(ctl fail-phase "$LOOP" "$FEATURE" "$pid" "$(jq -r '.failed_checks | join("; ")' <<<"$v")")
      ctl log-action "$LOOP" "test $FEATURE $pid attempt $attempt: fail -> $status" ;;
    needs_input)
      q=$(jq -r '.questions[]?' <<<"$v")
      if ((AUTO)); then
        echo "$q"
        finish "needs_input: the testing loop has questions about $LOOP $pid" 14
      fi
      printf '\n\033[1mThe testing loop needs answers for %s:\033[0m\n%s\n' "$pid" "$q"
      read -r -p "your answers (empty = quit): " notes </dev/tty
      [[ -n "$notes" ]] || { human milestone="$pid" gate=test decision=quit; finish "quit: at the $pid test questions" 10; }
      ctl append-notes "loops/$LOOP/outputs/$FEATURE/$pid-review.md" "testing questions answered: $notes"
      human milestone="$pid" gate=test decision=answer notes="$notes"
      ctl log-action "$LOOP" "test $FEATURE $pid: questions answered, retesting" ;;
    harness)
      harness_fail "testing session reported harness (Playwright MCP not responding?) for $pid" ;;
    *)
      harness_fail "testing session wrote no verdict for $pid attempt $attempt" ;;
  esac
}

# ------------------------------------------------------------------ dev session
maybe_scaffold() {
  local dir cmd
  dir=$(ctl cfg "layers.$LAYER.dir")
  cmd=$(ctl cfg "layers.$LAYER.scaffold" "")
  if [[ ! -d "$dir" && -n "$cmd" ]]; then
    say "scaffolding $dir (runner-run: $cmd)"
    mkdir -p "loops/$LOOP/runs/$FEATURE"
    bash -c "$cmd" >"loops/$LOOP/runs/$FEATURE/scaffold.log" 2>&1 </dev/null || harness_fail "scaffold failed, see loops/$LOOP/runs/$FEATURE/scaffold.log"
    ctl log-action "$LOOP" "runner scaffolded $dir"
  fi
}

dev_session() { # next-json
  local n="$1" stage kind pid milestone attempt swagger prompt goal context status rf
  stage=$(jq -r .stage <<<"$n"); kind=$(jq -r .kind <<<"$n"); pid=$(jq -r .phase <<<"$n")
  milestone=$(jq -r .milestone <<<"$n"); attempt=$(jq -r .attempt <<<"$n")
  [[ "$stage" == implement ]] && maybe_scaffold
  # /speckit-specify auto-numbers a new specs/NNN-name unless it is given this directory (P.2)
  [[ "$stage" == plan ]] && mkdir -p "$SPECIFY_FEATURE_DIRECTORY"
  swagger=$(swagger_for "$( [[ "$pid" == phase-00 ]] && echo plan || echo impl)")
  write_task ${swagger:+"swagger=$swagger"} "current_step=$stage $pid"
  prompt="/$LOOP $REQ${swagger:+ $swagger} --runner $NONCE"
  rf="loops/$LOOP/outputs/$FEATURE/$pid-review.md"
  case "$stage" in
    plan) goal="Plan $LAYER from $REQ" ;;
    intake) goal="Intake $(jq -r .bug <<<"$n"): $(ctl bugs "$FEATURE" open | awk -F'\t' -v b="$(jq -r .bug <<<"$n")" '$1==b{print $5}')" ;;
    converge) goal="Final converge of $FEATURE $LAYER" ;;
    *) goal=$(ctl phase-goal "$LOOP" "$FEATURE" "$pid") ;;
  esac
  context="$rf"
  if [[ "$kind" == retry ]]; then context+="; loops/$LOOP/outputs/$FEATURE/$pid-report.md"; fi
  run_session "$LOOP" "$stage" "$kind" dev "$milestone" "$attempt" "$pid" "$prompt" "$goal" "$context"
  # commits
  status=$(ctl phase-get "$LOOP" "$FEATURE" "$pid" status 2>/dev/null || echo "")
  if [[ "$stage" == plan_apply && "$status" == done ]]; then
    commit "$LOOP: $FEATURE phase-00 plan" "$(ctl commit-body "$LOOP" "$FEATURE" phase-00)"
  elif [[ "$stage" == close && "$status" == done ]]; then
    [[ "$pid" == fix-BUG-* ]] && ctl mark-fixed "$FEATURE" "$pid"
    python3 scripts/progress-table.py >/dev/null || true
    commit "$LOOP: $FEATURE $pid $(ctl phase-get "$LOOP" "$FEATURE" "$pid" title | tr -d '"') (verified)" \
      "$(ctl commit-body "$LOOP" "$FEATURE" "$pid")"
  fi
}

until_reached() {
  local pid="$UNTIL" st
  if [[ -n "$UNTIL_STORY" ]]; then
    pid=$(ctl story-phase "$LOOP" "$FEATURE" "$UNTIL_STORY" 2>/dev/null) || return 1
  fi
  [[ -n "$pid" ]] || return 1
  st=$(ctl phase-get "$LOOP" "$FEATURE" "$pid" status 2>/dev/null) || return 1
  [[ "$st" == done || "$st" == skipped || "$st" == blocked ]]
}

# ------------------------------------------------------------------ dev cycle
dev_cycle() {
  if [[ -n "$UNBLOCK" ]]; then
    ctl unblock "$LOOP" "$FEATURE" "$UNBLOCK"
    ctl log-action "$LOOP" "runner: --unblock $FEATURE $UNBLOCK"
    human milestone="$UNBLOCK" gate=unblock decision=unblock
  fi
  local n action last="" same=0 reason
  while true; do
    if [[ -n "$UNTIL$UNTIL_STORY" ]] && until_reached; then
      finish "until_reached: ${UNTIL:-$UNTIL_STORY}" 0
    fi
    n=$(ctl next "$LOOP" "$FEATURE")
    if [[ "$n" == "$last" ]]; then same=$((same + 1)); else same=0; last="$n"; fi
    ((same >= 3)) && finish "harness: no progress after 3 identical steps ($n)" 12
    action=$(jq -r .action <<<"$n")
    case "$action" in
      stop)
        reason=$(jq -r .reason <<<"$n")
        case "$reason" in
          complete) finish complete 0 ;;
          waiting:*) finish "$reason" 3 ;;
          blocked:*) finish "$reason" 4 ;;
          *) finish "$reason" 0 ;;
        esac ;;
      gate) same=0; last=""; gate "$(jq -r .phase <<<"$n")" ;;
      test) test_phase "$(jq -r .phase <<<"$n")" "$(jq -r .attempt <<<"$n")" ;;
      session) dev_session "$n" ;;
    esac
  done
}

# ------------------------------------------------------------------ suite cycle
suite_cycle() {
  local prep n action tp attempt run l outcome layers passed prompt reason
  MODE=suite
  local extra="${OPENAPI_OVERRIDE:+ $OPENAPI_OVERRIDE}${UIURL_OVERRIDE:+ $UIURL_OVERRIDE}"
  prompt="/testing suite $REQ$extra --runner $NONCE"
  prep=$(ctl suite-prepare "$FEATURE")
  if [[ "$prep" == replan ]]; then
    write_task "current_step=suite plan (a not_ready story is now built)"
    run_session testing suite suite suite "suite:test-phase-00-plan" 1 "" "$prompt" \
      "Re-plan the suite: add newly built stories" "loops/testing/state/$FEATURE/suite.json"
    commit "testing: $FEATURE suite plan" "re-planned: a not_ready story is now built"
  fi
  local last="" same=0
  while true; do
    n=$(ctl suite-next "$FEATURE")
    if [[ "$n" == "$last" ]]; then same=$((same + 1)); else same=0; last="$n"; fi
    ((same >= 3)) && finish "harness: no progress after 3 identical suite steps ($n)" 12
    action=$(jq -r .action <<<"$n")
    if [[ "$action" == stop ]]; then
      reason=$(jq -r .reason <<<"$n")
      case "$reason" in
        complete) finish complete 0 ;;
        bugs_open)
          say "open bugs:"; ctl bugs "$FEATURE" open
          finish bugs_open 5 ;;
        blocked) finish "blocked: a test phase failed 3 retests" 4 ;;
        *) finish "$reason" 0 ;;
      esac
    fi
    tp=$(jq -r .test_phase <<<"$n"); attempt=$(jq -r .attempt <<<"$n")
    write_task "current_step=$tp"
    if [[ "$tp" == test-phase-00-plan ]]; then
      run_session testing suite suite suite "suite:$tp" 1 "" "$prompt" \
        "Plan the test suite for $REQ" ""
      commit "testing: $FEATURE suite plan" ""
      continue
    fi
    run="loops/testing/runs/$FEATURE/suite/$tp/attempt-$attempt"
    mkdir -p "$run"
    write_task "current_step=$tp" "run_dir=$run"
    if [[ "$tp" == test-phase-regression ]]; then
      for l in $(ctl cfg layers | jq -r 'keys[]'); do
        outcome=$(unit_tests "$l" "$run/unit/$l")
        say "unit tests ($l): $outcome"
        [[ "$outcome" == harness ]] && { harness_fail "unit test run ($l) for regression"; continue 2; }
      done
    fi
    mapfile -t layers < <(ctl cfg layers | jq -r 'keys[]')
    HARNESS_MSG=""
    if ! start_servers "$run" "${layers[@]}"; then harness_fail "$HARNESS_MSG"; continue; fi
    ctl harness testing reset >/dev/null
    point_current "$run"
    local snap; snap=$(mktemp -d); ctl snapshot "$snap"
    run_session testing suite suite suite "suite:$tp" "$attempt" "" "$prompt" \
      "$(ctl phase-goal testing "$FEATURE" "$tp")" "loops/testing/state/$FEATURE/suite.json"
    stop_servers
    while read -r passed; do
      [[ -n "$passed" ]] || continue
      ctl traces "loops/testing/runs/$FEATURE/suite/$passed/attempt-$attempt" drop
      commit "testing: $FEATURE suite $passed (passed)" "session: $LAST_SESSION_ID"
    done < <(ctl suite-passed "$FEATURE" "$snap")
    rm -rf "$snap"
    [[ "$(jq -r --arg t "$tp" '.test_phases[]? | select(.id==$t) | .status' "loops/testing/state/$FEATURE/suite.json" 2>/dev/null)" == failed_waiting_fix ]] \
      && ctl traces "$run" keep
  done
}

# ------------------------------------------------------------------ main
if [[ "$LOOP" == testing ]]; then
  suite_cycle
elif [[ "$LOOP" == orchestrator ]]; then
  # one planning session: dependency graph + order.json (called by orchestrate.sh)
  MODE=plan write_task
  run_session orchestrator orchestrator orchestrator plan plan 1 "" \
    "/orchestrator plan $REQ --runner $NONCE" "Order the stories of $REQ across layers" \
    "loops/*/state/$FEATURE/phases.json"
  commit "orchestrator: $FEATURE plan" "session: $LAST_SESSION_ID"
  [[ -s loops/orchestrator/state/$FEATURE/order.json ]] || finish "orchestrator: no order.json written" 12
  finish "until_reached: orchestrator plan" 0
else
  cur="loops/$LOOP/state/$FEATURE/current.json"
  if [[ -f "$cur" && "$(jq -r .loop_status "$cur")" == none ]]; then ctl set-loop "$LOOP" "$FEATURE" running; fi
  write_task
  dev_cycle
fi
