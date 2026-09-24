#!/usr/bin/env python3
"""Rebuild the milestone table at the top of each loops/*/progress.md from docs/sessions.csv.

  scripts/progress-table.py          rewrite the tables (the runner calls this after every session)
  scripts/progress-table.py --check  exit 1 if any table differs from what the CSV gives

Only the text between <!-- milestones:start --> and <!-- milestones:end --> is touched;
the action log below it is left alone. Commits are looked up in git, never stored.
"""
import csv
import json
import re
import subprocess
import sys
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV = ROOT / "docs/sessions.csv"
START, END = "<!-- milestones:start -->", "<!-- milestones:end -->"
COLS = ["milestone", "start", "end", "wall time", "active time", "sessions", "attempts",
        "input tokens", "cache tokens", "output tokens", "cost USD", "session_ids",
        "verified_by", "commit", "result"]


def num(v, cast=int):
    try:
        return cast(float(v))
    except (TypeError, ValueError):
        return 0


def dur(ms):
    s = int(ms // 1000)
    return f"{s // 3600}h{s % 3600 // 60:02d}m{s % 60:02d}s"


def commit_for(loop, feature, milestone):
    if loop == "testing":
        m = re.match(r"suite:(.+)", milestone)
        grep = f"testing: {feature} suite {m.group(1)}" if m else None
    elif milestone == "phase-00-plan":
        grep = f"{loop}: {feature} phase-00 plan"
    else:
        grep = f"{loop}: {feature} {milestone} "
    if not grep:
        return ""
    out = subprocess.run(["git", "log", "--format=%h", "--fixed-strings", f"--grep={grep}"],
                         cwd=ROOT, capture_output=True, text=True).stdout.split()
    return out[0] if out else ""


def verified_by(loop, feature, milestone):
    f = ROOT / "loops" / loop / "state" / feature / "phases.json"
    if not f.exists():
        return ""
    for p in json.loads(f.read_text()).get("phases", []):
        if p["phase"] == milestone:
            return p.get("verified_by") or ""
    return ""


def groups_for(loop, rows):
    """-> {(feature, milestone_label, dev_milestone): [rows]} in first-seen order."""
    out = {}
    for r in rows:
        if loop in ("backend-dev", "frontend-dev"):
            if r["loop"] == loop:
                key = (r["feature"], r["milestone"])
            elif r["loop"] == "testing" and r["mode"] == "phase" and r["milestone"].startswith(loop + ":"):
                key = (r["feature"], r["milestone"].split(":", 1)[1])
            else:
                continue
        elif r["loop"] == loop:
            m = r["milestone"]
            key = (r["feature"], f"test:{m}" if loop == "testing" and r["mode"] == "phase" else m)
        else:
            continue
        out.setdefault(key, []).append(r)
    return out


def table(loop, rows):
    groups = groups_for(loop, rows)
    if not groups:
        return "_no sessions yet_"
    lines = ["| " + " | ".join(COLS) + " |", "|" + "---|" * len(COLS)]
    tot = {"sessions": 0, "active": 0, "in": 0, "cache": 0, "out": 0, "cost": 0.0}
    seen = set()
    for (feature, ms), rs in groups.items():
        starts = [datetime.fromisoformat(r["started_at"]) for r in rs if r["started_at"]]
        ends = [datetime.fromisoformat(r["ended_at"]) for r in rs if r["ended_at"]]
        active = sum(num(r["duration_ms"]) for r in rs)
        tin = sum(num(r["input_tokens"]) for r in rs)
        tcache = sum(num(r["cache_creation_input_tokens"]) + num(r["cache_read_input_tokens"]) for r in rs)
        tout = sum(num(r["output_tokens"]) for r in rs)
        cost = sum(num(r["total_cost_usd"], float) for r in rs)
        wall = (max(ends) - min(starts)).total_seconds() * 1000 if starts and ends else 0
        dev_ms = ms[len("test:"):].split(":", 1)[-1] if ms.startswith("test:") else ms
        dev_loop = ms[len("test:"):].split(":", 1)[0] if ms.startswith("test:") else loop
        vb = verified_by(dev_loop, feature, dev_ms)
        lines.append("| " + " | ".join([
            f"{loop}:{feature}:{ms}",
            min(starts).isoformat(timespec="seconds") if starts else "",
            max(ends).isoformat(timespec="seconds") if ends else "",
            dur(wall), dur(active), str(len(rs)),
            str(max(num(r["attempt"]) for r in rs)),
            str(tin), str(tcache), str(tout), f"{cost:.4f}",
            " ".join(r["session_id"] for r in rs if r["session_id"]),
            vb, commit_for(loop, feature, ms) if not ms.startswith("test:") else "",
            rs[-1]["result"],
        ]) + " |")
        for r in rs:
            if r["session_id"] in seen:
                continue
            seen.add(r["session_id"])
            tot["sessions"] += 1
            tot["active"] += num(r["duration_ms"])
            tot["in"] += num(r["input_tokens"])
            tot["cache"] += num(r["cache_creation_input_tokens"]) + num(r["cache_read_input_tokens"])
            tot["out"] += num(r["output_tokens"])
            tot["cost"] += num(r["total_cost_usd"], float)
    lines.append("| " + " | ".join([
        "**Total**", "", "", "", dur(tot["active"]), str(tot["sessions"]), "",
        str(tot["in"]), str(tot["cache"]), str(tot["out"]), f"{tot['cost']:.4f}", "", "", "", ""]) + " |")
    return "\n".join(lines)


def main():
    check = "--check" in sys.argv
    rows = list(csv.DictReader(CSV.open())) if CSV.exists() else []
    bad = []
    for pm in sorted(ROOT.glob("loops/*/progress.md")):
        loop = pm.parent.name
        text = pm.read_text()
        if START not in text or END not in text:
            bad.append(f"{pm}: markers missing")
            continue
        new = re.sub(re.escape(START) + r".*?" + re.escape(END),
                     lambda _: f"{START}\n{table(loop, rows)}\n{END}", text, count=1, flags=re.S)
        if check:
            if new != text:
                bad.append(f"{pm.relative_to(ROOT)}: table differs from docs/sessions.csv")
        elif new != text:
            pm.write_text(new)
    if bad:
        print("\n".join(bad))
        sys.exit(1)


if __name__ == "__main__":
    main()
