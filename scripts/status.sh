#!/usr/bin/env bash
# status.sh: one-screen view of a run. Live: `watch -n 5 -c scripts/status.sh [feature]`
# Read-only: it never changes state, so it's safe while a run is going.
set -uo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"
FEATURE="${1:-$(ls loops/backend-dev/state 2>/dev/null | grep -E '^[0-9]{3}-' | tail -1)}"
LOG="${RUN_LOG:-$HOME/.cache/quickflow/orchestrate.log}"

icon() {
  case "$1" in
    done) printf '\033[32m✔\033[0m' ;; passed) printf '\033[32m✓\033[0m' ;;
    skipped) printf '\033[90m–\033[0m' ;; blocked) printf '\033[31m✖\033[0m' ;;
    planned) printf '\033[90m·\033[0m' ;; awaiting_approval) printf '\033[33m?\033[0m' ;;
    *) printf '\033[36m▶\033[0m' ;;   # approved, in_progress, ready_for_test
  esac
}

printf '\033[1mQuickFlow run — %s — %s\033[0m\n\n' "$FEATURE" "$(date +%H:%M:%S)"
for loop in $(ls loops | grep -vE '^(orchestrator|testing)$'); do
  f="loops/$loop/state/$FEATURE/phases.json"
  [[ -f "$f" ]] || continue
  printf '%-13s ' "$loop"
  while IFS=$'\t' read -r p s a; do
    icon "$s"; printf '%s ' "${p#phase-}"
  done < <(jq -r '.phases[] | [.phase, .status, .attempt] | @tsv' "$f")
  done_n=$(jq '[.phases[] | select(.status=="done")] | length' "$f")
  total=$(jq '.phases | length' "$f")
  printf ' (%s/%s)\n' "$done_n" "$total"
  jq -r '.phases[] | select(.status | IN("approved","in_progress","ready_for_test","passed","awaiting_approval","blocked"))
         | "              now: \(.phase) \(.title // "") — \(.status), attempt \(.attempt)"' "$f"
done
s="loops/testing/state/$FEATURE/suite.json"
[[ -f "$s" ]] && printf '%-13s %s\n' "suite" "$(jq -r '[.test_phases[] | "\(.id|sub("test-phase-";""))=\(.status)"] | join(" ")' "$s")"
printf '\n\033[90m✔ done  ✓ passed test  ▶ working  ? waiting for you  ✖ blocked  · not started\033[0m\n\n'

if [[ -f docs/sessions.csv ]]; then
  python3 - <<'EOF'
import csv
rows = list(csv.DictReader(open("docs/sessions.csv")))
cost = sum(float(r["total_cost_usd"] or 0) for r in rows)
secs = sum(float(r["duration_ms"] or 0) for r in rows) / 1000
print(f"sessions: {len(rows)}   cost: ${cost:.2f}   active time: {int(secs//3600)}h{int(secs%3600//60):02d}m")
EOF
fi
if git rev-parse -q --verify '@{u}' >/dev/null 2>&1; then
  printf 'commits: %s unpushed\n' "$(git rev-list --count '@{u}..HEAD')"
else
  printf 'commits: %s (no upstream yet)\n' "$(git rev-list --count HEAD 2>/dev/null)"
fi
if pgrep -f 'claude -p /' >/dev/null; then
  printf 'running:  %s\n' "$(pgrep -af 'claude -p /' | grep -oE 'claude -p /[a-z-]+ [^ ]*( [^-][^ ]*)?' | head -1)"
else
  printf 'running:  \033[33mno session right now\033[0m\n'
fi
if [[ -f "$LOG" ]]; then
  printf '\n\033[1mrunner log\033[0m (%s)\n' "$LOG"
  tail -n 8 "$LOG" | cut -c1-150
fi
