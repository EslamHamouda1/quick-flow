#!/usr/bin/env bash
# review-commits.sh: step through unpushed commits, approve or reject each, then push.
#
# The only way anything gets pushed: Claude sessions can't run git, settings.json denies
# `git push`, and .githooks/pre-push refuses a push without REVIEWED=1.
# A reject never rewrites history: it sends the milestone back into its loop.
set -euo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"
CTL=(python3 scripts/lib/loopctl.py)
ctl() { "${CTL[@]}" "$@"; }
LOG=docs/commit-reviews.md

mkdir -p loops docs
exec 9>loops/.run.lock
flock -n 9 || { echo "review-commits: a runner holds loops/.run.lock; wait for it to finish" >&2; exit 2; }

[[ -f "$LOG" ]] || printf '# Commit reviews\n\n| commit | at | decision | subject | notes |\n|---|---|---|---|---|\n' >"$LOG"

if git rev-parse -q --verify '@{u}' >/dev/null 2>&1; then RANGE='@{u}..HEAD'; else RANGE='HEAD'; fi
mapfile -t COMMITS < <(git rev-list --reverse "$RANGE")
((${#COMMITS[@]})) || { echo "nothing to review: no unpushed commits"; exit 0; }

decision_of() { # hash -> last recorded decision
  awk -F'|' -v h="$1" '$2 ~ h {d=$4} END {gsub(/ /,"",d); print d}' "$LOG"
}
record() { # hash decision subject notes
  printf '| %s | %s | %s | %s | %s |\n' "$1" "$(date -Iseconds)" "$2" "${3//|/\\|}" "${4//|/\\|}" >>"$LOG"
  ctl human loop=review feature="" milestone="$1" gate=commit decision="$2" notes="$4"
}

echo "WARNING: to remove a commit from history, do it yourself with git, then run"
echo "         scripts/run-loop.sh <loop> <file> --unblock <phase> (or edit state/phases.json) so state matches the code."
echo

for c in "${COMMITS[@]}"; do
  short=$(git rev-parse --short "$c")
  subject=$(git log -1 --format=%s "$c")
  prev=$(decision_of "$short")
  if [[ "$prev" == approve || "$prev" == reject ]]; then
    echo "$short already reviewed ($prev): $subject"; continue
  fi
  if [[ "$subject" == "chore: commit reviews" || "$subject" =~ ^chore\([a-z-]+\):\ reopen ]]; then
    echo "$short made by review-commits.sh itself: $subject"; continue
  fi
  while true; do
    git show --stat --format='%n%C(bold)%h %s%C(reset)%n%b' "$c" | cat
    git show "$c" | ${PAGER:-less -R} || true   # quitting the pager early is not an error
    verified=0; [[ "$subject" =~ \(verified\)$ ]] && verified=1
    opts="[a]pprove / [q]uit"; ((verified)) && opts="[a]pprove / [x] reject / [q]uit"
    read -r -p "$short $subject — $opts: " choice </dev/tty
    case "$choice" in
      a) record "$short" approve "$subject" ""; break ;;
      x)
        ((verified)) || continue
        read -r -p "what's wrong (goes under ## Reviewer notes): " notes </dev/tty
        # "<loop>: <feature> <milestone> <title> (verified)"
        if [[ "$subject" =~ ^([a-z-]+):\ ([0-9]{3}-[^ ]+)\ ([^ ]+)\  ]]; then
          loop="${BASH_REMATCH[1]}"; feature="${BASH_REMATCH[2]}"; ms="${BASH_REMATCH[3]}"
          st=$(ctl reject "$loop" "$feature" "$ms" "$short" "$notes")
          ctl log-action "$loop" "commit $short rejected: $feature $ms -> $st"
          record "$short" reject "$subject" "$notes"
          echo "$loop $feature $ms is now $st; the next run-loop.sh run fixes and re-verifies it."
          # the reopened state goes into its own commit, so the next run starts clean
          git add -A && git commit -q -m "chore($loop): reopen $feature $ms after review" -m "rejected $short: $notes" || true
        else
          echo "can't read loop/feature/milestone from: $subject"
        fi
        break ;;
      q) echo "stopped; rerun to continue from the unpushed commits"; exit 0 ;;
    esac
  done
done

# push only when every unpushed commit is approved, or rejected and followed by an approved fix
ok=1; pending_reject=""
for c in $(git rev-list --reverse "$RANGE"); do
  short=$(git rev-parse --short "$c"); subject=$(git log -1 --format=%s "$c")
  d=$(decision_of "$short")
  case "$d" in
    approve)
      if [[ -n "$pending_reject" && "$subject" == "$pending_reject_prefix"* && "$subject" =~ \(verified\)$ ]]; then
        pending_reject=""
      fi ;;
    reject)
      pending_reject="$short"
      pending_reject_prefix=$(sed -E 's/^([a-z-]+: [0-9]{3}-[^ ]+ [^ ]+ ).*/\1/' <<<"$subject") ;;
    *) [[ "$subject" == "chore: commit reviews" || "$subject" =~ ^chore\([a-z-]+\):\ reopen ]] || ok=0 ;;
  esac
done
[[ -n "$pending_reject" ]] && ok=0
if ((ok)); then
  git add "$LOG" docs/human-inputs.csv 2>/dev/null && git commit -q -m "chore: commit reviews" || true
  read -r -p "push now? [y/N] " yn </dev/tty
  if [[ "$yn" == y ]]; then
    REVIEWED=1 git push "$@"
    echo "pushed."
  fi
else
  echo "push not offered: some commits are unreviewed, or a rejected commit has no approved fix yet."
fi
