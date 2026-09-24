#!/usr/bin/env bash
# orchestrate.sh: run a full PRD or a single US end to end, in YOUR terminal.
#
#   scripts/orchestrate.sh <requirements-file> [--auto-approve] [--allow-dirty-engine]
#
# Drives everything one step at a time, always through run-loop.sh, so every step keeps its
# own session id and token count and the review gates prompt you directly. Holds the run lock
# for the whole run and passes one RUN_ID down, so the spending cap covers everything.
# Never pushes.
set -euo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"
CTL=(python3 scripts/lib/loopctl.py)
ctl() { "${CTL[@]}" "$@"; }
say() { printf '\033[1m[orchestrate %s]\033[0m %s\n' "$(date +%H:%M:%S)" "$*"; }
die() { echo "orchestrate: $*" >&2; exit 2; }

REQ="${1:-}"; [[ -n "$REQ" ]] || die "usage: orchestrate.sh <requirements-file> [--auto-approve] [--allow-dirty-engine]"
shift
PASS=()
for a in "$@"; do
  case "$a" in --auto-approve|--allow-dirty-engine) PASS+=("$a") ;; *) die "unknown flag $a" ;; esac
done
[[ -f "$REQ" ]] || die "requirements file not found: $REQ"
[[ -f project.config.yaml ]] || die "project.config.yaml is missing. Copy project.config.example.yaml, fill it in, then rerun."

mkdir -p loops
exec 9>loops/.run.lock
flock -n 9 || die "another runner holds loops/.run.lock"
export LOOP_LOCK_HELD=1
export RUN_ID="${RUN_ID:-orch-$(date +%Y%m%dT%H%M%S)-$$}"
FEATURE=$(ctl feature "$REQ")
MAX_FIX_ROUNDS=$(ctl cfg orchestrator.max_fix_rounds 3)
ORCH_STATE="loops/orchestrator/state/$FEATURE"
mkdir -p "$ORCH_STATE"

# layer order comes from the config: a layer with contract_from runs after that layer
mapfile -t LOOPS < <(ctl cfg layers | jq -r '
  to_entries | sort_by(if .value.contract_from then 1 else 0 end) | .[].value.loop')
BACK="${LOOPS[0]}"; FRONT="${LOOPS[1]:-}"
BACK_LAYER=$(ctl cfg layers | jq -r --arg l "$BACK" 'to_entries[]|select(.value.loop==$l)|.key')

stop() { # reason code
  python3 - "$ORCH_STATE/current.json" "$1" <<'EOF'
import json, sys, pathlib
p = pathlib.Path(sys.argv[1]); d = json.loads(p.read_text()) if p.exists() else {}
d["stop_reason"] = sys.argv[2]; d["loop_status"] = "complete" if sys.argv[2] == "complete" else "stopped"
p.write_text(json.dumps(d, indent=2) + "\n")
EOF
  local n
  if git rev-parse -q --verify '@{u}' >/dev/null 2>&1; then n=$(git rev-list --count '@{u}..HEAD'); else n=$(git rev-list --count HEAD 2>/dev/null || echo 0); fi
  say "stop_reason: $1"
  say "$n unpushed commits — review with scripts/review-commits.sh"
  exit "$2"
}

# rl <loop> [args...]: run one run-loop.sh call; stop the orchestration on early stops.
rl() {
  local code=0
  scripts/run-loop.sh "$@" "${PASS[@]}" || code=$?
  case "$code" in
    0|3|4|5) return "$code" ;;  # done/until, waiting, blocked, bugs_open: the plan handles these
    10) stop "quit" 10 ;;
    11) stop "cost_cap" 11 ;;
    12) stop "harness" 12 ;;
    13) stop "violation" 13 ;;
    14) stop "needs_input" 14 ;;
    *) stop "run-loop.sh exited $code" "$code" ;;
  esac
}

swagger_json=$(ctl cfg "layers.$BACK_LAYER.openapi_copy" "")

# 1. planning, each through its phase-00 review gate
say "1. planning"
rl "$BACK" "$REQ" --until phase-00 || true
if [[ -n "$FRONT" ]]; then
  rl "$FRONT" "$REQ" "specs/${FEATURE}-${BACK_LAYER}/contracts/openapi.yaml" --until phase-00 || true
fi

# 2. the orchestrator's own plan session (dependency graph + order.json), up to 3 tries
say "2. orchestrator plan"
for try in 1 2 3; do
  rm -f loops/orchestrator/state/$FEATURE/order.json
  code=0; scripts/run-loop.sh orchestrator "$REQ" "${PASS[@]}" || code=$?
  case "$code" in 11) stop cost_cap 11 ;; 13) stop violation 13 ;; esac
  [[ -s loops/orchestrator/state/$FEATURE/order.json ]] && break
  ((try == 3)) && stop "orchestrator plan failed 3 times" 12
done

# 3. Setup + Foundational of each layer (the phases before the first story), backend first
say "3. setup and foundational phases"
for l in "$BACK" ${FRONT:+"$FRONT"}; do
  args=("$l" "$REQ")
  [[ "$l" == "$FRONT" ]] && args+=("$swagger_json")
  last_base=$(jq -r '.phases | (map(.story_id != null) | index(true)) as $i
    | if $i == null or $i <= 1 then empty else .[$i-1].phase end' "loops/$l/state/$FEATURE/phases.json")
  [[ -n "$last_base" ]] && { rl "${args[@]}" --until "$last_base" || true; }
done

# 3a. one story at a time, backend then frontend
say "3a. stories in dependency order"
while IFS=$'\t' read -r loop story; do
  [[ -n "$story" ]] || continue
  export ORCH_STEP="$loop:$story"
  args=("$loop" "$REQ")
  [[ "$loop" == "$FRONT" ]] && args+=("$swagger_json")
  rl "${args[@]}" --until-story "$story" || true
done < <(ctl order "$FEATURE")
unset ORCH_STEP

# 3b. finish each loop: Polish, final converge, phase-polish-N
say "3b. finishing both loops"
rl "$BACK" "$REQ" || true
[[ -n "$FRONT" ]] && { rl "$FRONT" "$REQ" "$swagger_json" || true; }

# 5. suite mode, with fix rounds
for round in $(seq 1 "$MAX_FIX_ROUNDS"); do
  say "5. test suite (round $round/$MAX_FIX_ROUNDS)"
  code=0; rl testing "$REQ" || code=$?
  case "$code" in
    0) stop complete 0 ;;
    4) stop "suite blocked" 4 ;;
    5)
      say "bugs open: running the dev loops (they pick up fix-BUG milestones first)"
      rl "$BACK" "$REQ" || true
      [[ -n "$FRONT" ]] && { rl "$FRONT" "$REQ" "$swagger_json" || true; } ;;
  esac
done
stop "max_fix_rounds ($MAX_FIX_ROUNDS) reached with bugs still open" 5
