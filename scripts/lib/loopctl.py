#!/usr/bin/env python3
"""loopctl: the runner's state machine, guard and accounting helpers.

Every file the runner owns is read and written here, so the bash scripts only
deal with processes, prompts and git. Claude sessions never call this script
(scripts/** is a protected path and `python3` is not on any allowlist).

Usage: loopctl.py <command> [args...]   (see the COMMANDS table at the bottom)
"""
import csv
import fnmatch
import glob
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile
from datetime import datetime, timezone
from pathlib import Path

import yaml

ROOT = Path(os.environ.get("LOOP_ROOT") or Path(__file__).resolve().parents[2])
os.chdir(ROOT)

CONFIG_FILE = ROOT / "project.config.yaml"
DEV_LOOPS = ("backend-dev", "frontend-dev")
TERMINAL = ("done", "skipped", "blocked")
SESSIONS_CSV = ROOT / "docs/sessions.csv"
HUMAN_CSV = ROOT / "docs/human-inputs.csv"
CSV_COLUMNS = [
    "run_id", "engine_version", "started_at", "ended_at", "loop", "mode", "feature",
    "milestone", "kind", "attempt", "session_id", "model", "phase_goal", "prompt",
    "context", "input_tokens", "cache_creation_input_tokens", "cache_read_input_tokens",
    "output_tokens", "total_cost_usd", "duration_ms", "num_turns", "result",
]
HUMAN_COLUMNS = ["at", "loop", "feature", "milestone", "gate", "decision", "notes",
                 "edit_diff", "read_by_session"]

# Files no Claude session may change (see "Permissions" in the README).
PROTECTED = [".claude/**", "CLAUDE.md", ".mcp.json", "loops/*/Loop-instructions.md",
             "scripts/**", ".githooks/**", "project.config.yaml",
             "project.config.example.yaml", ".specify/memory/constitution.md"]
# Files only the runner (or review-commits.sh) writes.
RUNNER_OWNED = ["docs/sessions.csv", "docs/human-inputs.csv", "docs/commit-reviews.md",
                "docs/dry-run-*.csv", "loops/*/runs/sessions/**", "loops/*/state/harness.json",
                "loops/*/task.md", "loops/testing/runs/current"]
MARK_START, MARK_END = "<!-- milestones:start -->", "<!-- milestones:end -->"


def die(msg, code=2):
    print(msg, file=sys.stderr)
    sys.exit(code)


def now_iso():
    return datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")


def load_json(path, default=None):
    p = Path(path)
    if not p.exists():
        return default
    with p.open() as f:
        return json.load(f)


def save_json(path, data):
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    tmp = p.with_suffix(p.suffix + ".tmp")
    tmp.write_text(json.dumps(data, indent=2) + "\n")
    tmp.replace(p)


# --------------------------------------------------------------------------- config

def config():
    if not CONFIG_FILE.exists():
        die("project.config.yaml is missing. Copy project.config.example.yaml to "
            "project.config.yaml and fill it in (you write it once; no session may change it).")
    with CONFIG_FILE.open() as f:
        return yaml.safe_load(f) or {}


def cfg_get(key, default=None, required=True):
    node = config()
    for part in key.split("."):
        if isinstance(node, dict) and part in node:
            node = node[part]
        else:
            if default is not None or not required:
                return default
            die(f"project.config.yaml has no key '{key}'")
    return node


def layer_of(loop):
    for name, layer in cfg_get("layers").items():
        if layer.get("loop") == loop:
            return name, layer
    die(f"no layer in project.config.yaml has loop: {loop}")


def loop_of_layer(layer_name):
    return cfg_get(f"layers.{layer_name}.loop")


def cmd_cfg(args):
    val = cfg_get(args[0], default=args[1] if len(args) > 1 else None)
    if isinstance(val, (dict, list)):
        print(json.dumps(val))
    elif isinstance(val, bool):
        print("true" if val else "false")
    else:
        print("" if val is None else val)


def cmd_check_config(_):
    c = config()
    missing = [k for k in ("layers", "claude", "app") if k not in c]
    for k in ("model", "max_turns", "timeout", "max_run_cost_usd"):
        if k not in (c.get("claude") or {}):
            missing.append(f"claude.{k}")
    if missing:
        die("project.config.yaml is missing: " + ", ".join(missing))
    print("ok")


# --------------------------------------------------------------------------- features

def feature_name(reqfile):
    req = Path(reqfile)
    names = cfg_get("features", default={}, required=False) or {}
    slug = names.get(req.name) or names.get(str(reqfile)) or req.stem.lower()
    slug = re.sub(r"[^a-z0-9]+", "-", slug.lower()).strip("-")
    layers = "|".join(re.escape(n) for n in (cfg_get("layers") or {}))
    used, highest = {}, 0
    # specs/<NNN-slug>-<layer> and loops/*/state/<NNN-slug>
    for d in list(Path("specs").glob("*")) + list(Path("loops").glob("*/state/*")):
        m = re.match(rf"^(\d{{3}})-(.+?)(?:-(?:{layers}))?$", d.name)
        if d.is_dir() and m:
            used.setdefault(m.group(2), m.group(1))
            highest = max(highest, int(m.group(1)))
    if slug in used:
        return f"{used[slug]}-{slug}"
    return f"{highest + 1:03d}-{slug}"


def cmd_feature(args):
    print(feature_name(args[0]))


def state_dir(loop, feature):
    return Path("loops") / loop / "state" / feature


def outputs_dir(loop, feature):
    return Path("loops") / loop / "outputs" / feature


def phases_file(loop, feature):
    return state_dir(loop, feature) / "phases.json"


def current_file(loop, feature):
    return state_dir(loop, feature) / "current.json"


def load_phases(loop, feature):
    return load_json(phases_file(loop, feature), {"phases": []})


def save_phases(loop, feature, data):
    save_json(phases_file(loop, feature), data)


def load_current(loop, feature):
    return load_json(current_file(loop, feature), {"loop_status": "none"})


def save_current(loop, feature, data):
    save_json(current_file(loop, feature), data)


def find_phase(data, pid):
    for p in data["phases"]:
        if p["phase"] == pid:
            return p
    return None


def review_file(loop, feature, pid):
    return outputs_dir(loop, feature) / f"{pid}-review.md"


def phase_file(loop, feature, pid):
    return outputs_dir(loop, feature) / f"{pid}.md"


# --------------------------------------------------------------------------- bugs

def bugs_dir(feature):
    return Path("loops/testing/outputs") / feature / "suite" / "bugs"


def read_front_matter(path):
    text = Path(path).read_text()
    m = re.match(r"^---\n(.*?)\n---\n", text, re.S)
    return (yaml.safe_load(m.group(1)) or {}) if m else {}, text


def write_front_matter(path, updates):
    meta, text = read_front_matter(path)
    meta.update(updates)
    body = re.sub(r"^---\n.*?\n---\n", "", text, count=1, flags=re.S)
    Path(path).write_text("---\n" + yaml.safe_dump(meta, sort_keys=False) + "---\n" + body)


def list_bugs(feature):
    out = []
    for f in sorted(bugs_dir(feature).glob("BUG-*.md")):
        meta, _ = read_front_matter(f)
        meta["id"] = f.stem
        meta["file"] = str(f)
        out.append(meta)
    return out


def cmd_bugs(args):
    feature = args[0]
    status = args[1] if len(args) > 1 else None
    for b in list_bugs(feature):
        if status is None or b.get("status") == status:
            print(f"{b['id']}\t{b.get('status')}\t{b.get('target_loop')}\t{b.get('test_phase')}\t{b.get('title', '')}")


def cmd_bug_set(args):
    feature, bug, field, value = args
    write_front_matter(bugs_dir(feature) / f"{bug}.md", {field: value})


# --------------------------------------------------------------------------- picker

def dep_state(loop, feature, dep, data):
    """Return 'done' | 'blocked' | 'pending' | ('waiting', text) for one depends_on entry."""
    if ":" in dep:  # cross-layer: "backend-dev:US3"
        other_loop, story = dep.split(":", 1)
        other = load_phases(other_loop, feature)
        match = [p for p in other["phases"] if p.get("story_id") == story]
        if not match:
            return ("waiting", f"{other_loop} {story}")
        st = match[0]["status"]
        if st == "done":
            return "done"
        if st in ("blocked", "skipped"):
            return "blocked"
        return ("waiting", f"{other_loop} {story}")
    p = find_phase(data, dep)
    if p is None:
        return "pending"
    if p["status"] == "done":
        return "done"
    if p["status"] in ("blocked", "skipped"):
        return "blocked"
    return "pending"


def propagate_blocks(loop, feature, data):
    changed = True
    while changed:
        changed = False
        for p in data["phases"]:
            if p["status"] in TERMINAL:
                continue
            for dep in p.get("depends_on") or []:
                if dep_state(loop, feature, dep, data) == "blocked":
                    p["status"] = "blocked"
                    p["block_reason"] = f"dependency {dep} blocked"
                    p["blocked_by"] = dep
                    changed = True
                    break
    return data


def is_fix(pid):
    return pid.startswith("fix-BUG-")


def stage_for(p):
    st, pid = p["status"], p["phase"]
    if st == "awaiting_approval":
        return "gate"
    if st == "ready_for_test":
        return "test"
    if st == "passed":
        return "close"
    if st == "planned":
        return "review"
    if st == "approved" and pid == "phase-00":
        return "plan_apply"
    if st in ("approved", "in_progress"):
        return "implement"
    return None


def kind_for(stage, p):
    if stage in ("plan", "plan_apply"):
        return "plan"
    if p and is_fix(p["phase"]):
        return "fix"
    if stage == "implement" and p and p.get("attempt", 1) >= 2:
        return "retry"
    if stage == "converge":
        return "close"
    return stage


def pick(loop, feature):
    data = load_phases(loop, feature)
    cur = load_current(loop, feature)
    if not data["phases"]:
        return {"action": "session", "stage": "plan", "kind": "plan", "phase": "phase-00",
                "milestone": "phase-00-plan", "attempt": 1}
    data = propagate_blocks(loop, feature, data)
    save_phases(loop, feature, data)
    p00 = find_phase(data, "phase-00")
    # dev step 6: open bugs aimed at this loop become fix milestones first
    if p00 and p00["status"] == "done":
        known = {p["phase"] for p in data["phases"]}
        for b in list_bugs(feature):
            if b.get("status") == "open" and b.get("target_loop") == loop \
                    and f"fix-{b['id']}" not in known:
                return {"action": "session", "stage": "intake", "kind": "fix",
                        "phase": f"fix-{b['id']}", "milestone": f"fix-{b['id']}", "attempt": 1,
                        "bug": b["id"]}
    waiting = None
    for p in data["phases"]:
        if p["status"] in TERMINAL:
            continue
        # every phase waits for the plan
        if p["phase"] != "phase-00" and p00 and p00["status"] != "done":
            continue
        ok = True
        for dep in p.get("depends_on") or []:
            s = dep_state(loop, feature, dep, data)
            if s != "done":
                ok = False
                if isinstance(s, tuple) and waiting is None:
                    waiting = s[1]
                break
        if not ok:
            continue
        stage = stage_for(p)
        if stage is None:
            continue
        action = stage if stage in ("gate", "test") else "session"
        return {"action": action, "stage": stage, "kind": kind_for(stage, p),
                "phase": p["phase"],
                "milestone": "phase-00-plan" if p["phase"] == "phase-00" else p["phase"],
                "attempt": p.get("attempt", 1), "story_id": p.get("story_id")}
    if waiting:
        return {"action": "stop", "reason": f"waiting: {waiting}"}
    if cur.get("loop_status") == "complete":
        return {"action": "stop", "reason": "complete"}
    blocked = [p for p in data["phases"] if p["status"] == "blocked"]
    if blocked:
        summary = "; ".join(f"{p['phase']} ({p.get('block_reason')})" for p in blocked)
        return {"action": "stop", "reason": f"blocked: {summary}", "set_loop": "blocked"}
    # everything done/skipped but the final converge has not reported Converged yet
    return {"action": "session", "stage": "converge", "kind": "close", "phase": "converge",
            "milestone": "converge", "attempt": 1}


def cmd_next(args):
    loop, feature = args
    r = pick(loop, feature)
    if r.get("set_loop"):
        cur = load_current(loop, feature)
        cur["loop_status"] = r["set_loop"]
        cur["stop_reason"] = r["reason"]
        save_current(loop, feature, cur)
    print(json.dumps(r))


def cmd_phase_get(args):
    loop, feature, pid = args[:3]
    p = find_phase(load_phases(loop, feature), pid)
    if p is None:
        die(f"{loop} {feature}: no phase {pid}")
    print(json.dumps(p) if len(args) == 3 else json.dumps(p.get(args[3])).strip('"'))


def cmd_story_phase(args):
    loop, feature, story = args
    for p in load_phases(loop, feature)["phases"]:
        if p.get("story_id") == story:
            print(p["phase"])
            return
    die(f"{loop} {feature}: no phase for story {story}")


def cmd_set_phase(args):
    """set-phase LOOP FEATURE PHASE key=value... (values are JSON if they parse)."""
    loop, feature, pid = args[:3]
    data = load_phases(loop, feature)
    p = find_phase(data, pid)
    if p is None:
        die(f"{loop} {feature}: no phase {pid}")
    for kv in args[3:]:
        k, v = kv.split("=", 1)
        try:
            p[k] = json.loads(v)
        except json.JSONDecodeError:
            p[k] = v
    save_phases(loop, feature, data)


def cmd_set_loop(args):
    loop, feature, status = args[:3]
    cur = load_current(loop, feature)
    cur["loop_status"] = status
    cur["stop_reason"] = args[3] if len(args) > 3 else None
    save_current(loop, feature, cur)


def fail_phase(loop, feature, pid, reason):
    """Runner step 2 'fail': attempt +1; at 3 the phase is blocked."""
    data = load_phases(loop, feature)
    p = find_phase(data, pid)
    attempt = p.get("attempt", 1)
    if attempt >= 3:
        p["status"] = "blocked"
        p["block_reason"] = f"failed 3 attempts: {reason}"
    else:
        p["attempt"] = attempt + 1
        p["status"] = "in_progress"
    save_phases(loop, feature, propagate_blocks(loop, feature, data))
    return p["status"]


def cmd_fail_phase(args):
    print(fail_phase(args[0], args[1], args[2], args[3] if len(args) > 3 else "test failed"))


def cmd_unblock(args):
    loop, feature, pid = args
    data = load_phases(loop, feature)
    p = find_phase(data, pid)
    if p is None or p["status"] != "blocked":
        die(f"{pid} is not blocked")
    reason = p.get("block_reason")
    p.update(status="planned", attempt=1, block_reason=None, approval=None)
    rf = review_file(loop, feature, pid)
    rf.parent.mkdir(parents=True, exist_ok=True)
    with rf.open("a") as f:
        f.write(f"\n## Unblocked\n- {now_iso()}: unblocked by you; was blocked because: {reason}\n")
    save_phases(loop, feature, data)
    # phases (in both layers) blocked only because of this one, directly or through a chain of
    # dependency blocks, become pickable again
    freed = {(loop, pid, p.get("story_id"))}
    books = {l: load_phases(l, feature) for l in DEV_LOOPS}
    changed = True
    while changed:
        changed = False
        for other, od in books.items():
            for q in od["phases"]:
                by = q.get("blocked_by")
                if q["status"] != "blocked" or not by:
                    continue
                if any((other == fl and by == fp) or (fs and by == f"{fl}:{fs}")
                       for fl, fp, fs in freed):
                    q.update(status="planned", block_reason=None, blocked_by=None)
                    freed.add((other, q["phase"], q.get("story_id")))
                    changed = True
    for other, od in books.items():
        if not od["phases"]:
            continue
        save_phases(other, feature, od)
        cur = load_current(other, feature)
        if cur.get("loop_status") == "blocked":
            cur.update(loop_status="running", stop_reason=None)
            save_current(other, feature, cur)
    cur = load_current(loop, feature)
    cur.update(loop_status="running", stop_reason=None)
    save_current(loop, feature, cur)


# --------------------------------------------------------------------------- review files

def section(text, heading):
    m = re.search(rf"^##\s+{re.escape(heading)}\s*$(.*?)(?=^##\s|\Z)", text, re.M | re.S | re.I)
    return m.group(1).strip() if m else ""


def cmd_questions(args):
    """Print the open-question sections of a review file (empty = nothing to ask)."""
    p = Path(args[0])
    if not p.exists():
        return
    text = p.read_text()
    out = []
    for h in ("Questions", "Open questions"):
        s = section(text, h)
        if s and not re.fullmatch(r"(?:[-*]\s*)?(none\.?|n/a|-)?", s.strip(), re.I):
            out.append(f"## {h}\n{s}")
    print("\n\n".join(out))


def append_notes(path, notes, heading="Reviewer notes"):
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    text = p.read_text() if p.exists() else ""
    entry = f"- {now_iso()}: {notes.strip()}\n"
    if re.search(rf"^##\s+{heading}\s*$", text, re.M):
        text = re.sub(rf"(^##\s+{heading}\s*$.*?)(?=^##\s|\Z)",
                      lambda m: m.group(1).rstrip("\n") + "\n" + entry + "\n", text,
                      count=1, flags=re.M | re.S)
    else:
        text = text.rstrip("\n") + f"\n\n## {heading}\n{entry}"
    p.write_text(text)


def cmd_append_notes(args):
    append_notes(args[0], args[1], args[2] if len(args) > 2 else "Reviewer notes")


def cmd_approve(args):
    loop, feature, pid, decision, notes = args
    data = load_phases(loop, feature)
    p = find_phase(data, pid)
    p["status"] = "approved"
    p["approval"] = {"decision": decision, "at": now_iso(), "notes": notes}
    save_phases(loop, feature, data)
    if notes.strip():
        append_notes(review_file(loop, feature, pid), notes)


def cmd_skip(args):
    loop, feature, pid = args
    data = load_phases(loop, feature)
    p = find_phase(data, pid)
    p["status"] = "skipped"
    p["approval"] = {"decision": "skip", "at": now_iso(), "notes": ""}
    save_phases(loop, feature, propagate_blocks(loop, feature, data))


def remove_tasks_section(tasks_md, title_regex):
    p = Path(tasks_md)
    if not p.exists():
        return
    text = p.read_text()
    text = re.sub(rf"^##\s+Phase\s+\d+:\s*{title_regex}.*?(?=^##\s|\Z)", "", text,
                  flags=re.M | re.S)
    p.write_text(text)


def cmd_reassign(args):
    """Move a bug to the other dev loop and drop its fix milestone here."""
    loop, feature, pid = args
    bug = pid[len("fix-"):]
    other = [l for l in DEV_LOOPS if l != loop][0]
    write_front_matter(bugs_dir(feature) / f"{bug}.md", {"target_loop": other, "status": "open"})
    data = load_phases(loop, feature)
    data["phases"] = [p for p in data["phases"] if p["phase"] != pid]
    save_phases(loop, feature, data)
    layer, _ = layer_of(loop)
    remove_tasks_section(f"specs/{feature}-{layer}/tasks.md", rf"Fix\s+{re.escape(bug)}\b")
    for f in (phase_file(loop, feature, pid), review_file(loop, feature, pid)):
        if f.exists():
            f.unlink()
    print(other)


def cmd_reject(args):
    """review-commits.sh reject: reopen the milestone of a (verified) commit."""
    loop, feature, milestone, commit, notes = args
    data = load_phases(loop, feature)
    p = find_phase(data, milestone)
    if p is None:
        die(f"{loop} {feature}: no milestone {milestone}")
    attempt = p.get("attempt", 1) + 1
    if attempt > 3:
        p.update(status="blocked", block_reason=f"commit {commit} rejected at attempt 3")
    else:
        p.update(status="approved", attempt=attempt)
    p["reopened_from"] = commit
    save_phases(loop, feature, data)
    append_notes(review_file(loop, feature, milestone), f"commit {commit} rejected: {notes}")
    cur = load_current(loop, feature)
    cur.update(loop_status="running", stop_reason=None)
    save_current(loop, feature, cur)
    print(p["status"])


# --------------------------------------------------------------------------- verdicts

def verdict_file(dev_loop, feature, pid):
    return Path("loops/testing/state") / feature / "verdicts" / dev_loop / f"{pid}.json"


def cmd_verdict(args):
    dev_loop, feature, pid = args
    print(json.dumps(load_json(verdict_file(dev_loop, feature, pid), {})))


def cmd_write_verdict(args):
    """The runner writes a fail verdict itself on a compile error (no session runs)."""
    dev_loop, feature, pid, attempt, verdict, summary = args
    save_json(verdict_file(dev_loop, feature, pid), {
        "attempt": int(attempt), "verdict": verdict, "failed_checks": [summary],
        "questions": [], "written_by": "runner"})


# --------------------------------------------------------------------------- unit tests

def cmd_unit_result(args):
    """unit-result LAYER OUTDIR LOG EXITCODE TIMED_OUT -> writes OUTDIR/unit-result.json."""
    layer_name, outdir, log, exit_code, timed_out = args
    layer = cfg_get(f"layers.{layer_name}")
    out = Path(outdir)
    out.mkdir(parents=True, exist_ok=True)
    log_text = Path(log).read_text(errors="replace") if Path(log).exists() else ""
    res = {"layer": layer_name, "exit_code": int(exit_code), "passed": 0, "failed": 0,
           "errors": 0, "skipped": 0, "coverage": None, "coverage_skipped": False,
           "outcome": None, "reason": ""}
    rep = layer.get("reports") or {}
    junit = sorted(glob.glob(rep.get("junit_glob", ""))) if rep.get("junit_glob") else []
    for f in junit:
        try:
            r = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        suites = [r] if r.tag == "testsuite" else r.findall("testsuite")
        for s in suites:
            t = int(s.get("tests", 0))
            fl, er, sk = (int(s.get(k, 0)) for k in ("failures", "errors", "skipped"))
            res["failed"] += fl
            res["errors"] += er
            res["skipped"] += sk
            res["passed"] += t - fl - er - sk
    if junit:
        (out / "surefire").mkdir(exist_ok=True)
        for f in junit:
            shutil.copy(f, out / "surefire")
    cov = layer.get("coverage") or {}
    jx = rep.get("coverage_xml")
    if jx and Path(jx).exists():
        root = ET.parse(jx).getroot()
        pkg = cov.get("package")
        node = next((p for p in root.iter("package") if p.get("name") == pkg), None)
        if node is None or not list(node.iter("class")):
            res["coverage_skipped"] = True
            res["reason"] = f"coverage skipped: no classes in {pkg}"
        else:
            line = next((c for c in node.findall("counter") if c.get("type") == "LINE"), None)
            if line is not None:
                m, c = int(line.get("missed")), int(line.get("covered"))
                res["coverage"] = round(c / (m + c), 4) if m + c else None
        html = rep.get("coverage_html")
        if html and Path(html).exists():
            shutil.copytree(html, out / "coverage", dirs_exist_ok=True)
    elif cov:
        res["coverage_skipped"] = True
        res["reason"] = res["reason"] or "coverage skipped: no coverage report"
    shutil.copy(log, out / "unit.log") if Path(log).exists() else None

    compile_pats = layer.get("compile_error_patterns") or []
    harness_pats = layer.get("harness_error_patterns") or []
    if timed_out == "1":
        res["outcome"], res["reason"] = "harness", "unit test run hit unit_timeout"
    elif any(p in log_text for p in harness_pats):
        res["outcome"], res["reason"] = "harness", "build tool could not start or fetch dependencies"
    elif any(p in log_text for p in compile_pats):
        res["outcome"], res["reason"] = "compile_error", "compile error (see unit.log)"
    elif res["failed"] or res["errors"]:
        res["outcome"] = "fail"
        res["reason"] = f"{res['failed']} failed, {res['errors']} errors"
    elif res["coverage"] is not None and cov.get("min_line") is not None \
            and res["coverage"] < float(cov["min_line"]):
        res["outcome"] = "fail"
        res["reason"] = f"line coverage {res['coverage']:.0%} on {cov.get('package')} " \
                        f"< {float(cov['min_line']):.0%}"
    elif int(exit_code) != 0 and not junit:
        res["outcome"], res["reason"] = "harness", f"exit {exit_code} with no test reports"
    elif int(exit_code) != 0:
        res["outcome"], res["reason"] = "fail", f"exit {exit_code}"
    else:
        res["outcome"] = "pass"
    save_json(out / "unit-result.json", res)
    print(res["outcome"])


# --------------------------------------------------------------------------- harness

def harness_file(loop):
    return Path("loops") / loop / "state" / "harness.json"


def cmd_harness(args):
    """harness LOOP inc|reset [message] -> prints the current count."""
    loop, op = args[:2]
    h = load_json(harness_file(loop), {"harness_failures": 0, "log": []})
    if op == "inc":
        h["harness_failures"] += 1
        h["log"].append({"at": now_iso(), "error": args[2] if len(args) > 2 else ""})
    elif op == "reset":
        h["harness_failures"] = 0
    save_json(harness_file(loop), h)
    print(h["harness_failures"])


# --------------------------------------------------------------------------- snapshot / guard

def git(*a, check=True):
    return subprocess.run(["git", *a], capture_output=True, text=True, check=check).stdout


def porcelain_paths():
    out = git("status", "--porcelain", "-z", "-uall")
    paths, items = [], out.split("\0")
    i = 0
    while i < len(items):
        it = items[i]
        if not it:
            i += 1
            continue
        code, path = it[:2], it[3:]
        paths.append(path)
        if code[0] in "RC":  # rename: next item is the source
            i += 1
            paths.append(items[i])
        i += 1
    return paths


def file_hash(p):
    p = Path(p)
    if p.is_symlink():
        return "link:" + os.readlink(p)
    if not p.exists():
        return None
    if p.is_dir():
        return "dir"
    return hashlib.sha256(p.read_bytes()).hexdigest()


def cmd_snapshot(args):
    snap = Path(args[0])
    shutil.rmtree(snap, ignore_errors=True)
    (snap / "files").mkdir(parents=True)
    manifest = {}
    for path in porcelain_paths():
        h = file_hash(path)
        manifest[path] = h
        if h and h != "dir" and not h.startswith("link:"):
            dst = snap / "files" / path
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, dst)
    # state files are compared field by field afterwards
    for f in glob.glob("loops/*/state/**/*.json", recursive=True):
        dst = snap / "state" / f
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(f, dst)
    for f in glob.glob("loops/*/progress.md"):
        dst = snap / "files" / f
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(f, dst)
    save_json(snap / "manifest.json", manifest)


def changed_since(snap):
    manifest = load_json(Path(snap) / "manifest.json", {})
    changed = set()
    for path in porcelain_paths():
        if path not in manifest or manifest[path] != file_hash(path):
            changed.add(path)
    for path, h in manifest.items():
        if path not in changed and file_hash(path) != h:
            changed.add(path)  # e.g. a dirty file reverted to HEAD
    return sorted(changed)


def match_any(path, patterns):
    return any(fnmatch.fnmatchcase(path, pat) or path == pat.rstrip("/**")
               for pat in patterns)


def whitelist(loop, stage, feature):
    if loop == "testing":
        return ["loops/testing/state/*/**", "loops/testing/outputs/**", "loops/testing/runs/*/**",
                "loops/testing/progress.md"]
    if loop == "orchestrator":
        return ["loops/orchestrator/state/**", "loops/orchestrator/outputs/**",
                "loops/orchestrator/progress.md"]
    layer, lcfg = layer_of(loop)
    allowed = [f"specs/{feature}-{layer}/**", f"loops/{loop}/state/{feature}/**",
               f"loops/{loop}/outputs/{feature}/**", f"loops/{loop}/runs/{feature}/**",
               f"loops/{loop}/progress.md"]
    allowed += cfg_get("guard.extra_plan_paths", default=[], required=False) or []
    if stage in ("implement", "close", "converge"):
        allowed.append(f"{lcfg['dir']}/**")
        allowed += lcfg.get("app_wide_files") or []
    return allowed


def status_violations(loop, stage, feature, target, snap):
    if loop not in DEV_LOOPS:
        return []
    before = load_json(Path(snap) / "state" / phases_file(loop, feature), {"phases": []})
    after = load_phases(loop, feature)
    bmap = {p["phase"]: p for p in before["phases"]}
    amap = {p["phase"]: p for p in after["phases"]}
    v = []
    for pid, b in bmap.items():
        if pid not in amap:
            v.append(f"phase {pid} was deleted")
    for pid, a in amap.items():
        b = bmap.get(pid)
        if b is None:
            ok_new = a["status"] == "planned" or (stage == "plan" and pid == "phase-00"
                                                  and a["status"] == "awaiting_approval")
            creator = {"plan": True, "intake": is_fix(pid),
                       "close": pid.startswith("phase-polish-"),
                       "converge": pid.startswith("phase-polish-")}.get(stage, False)
            if not (ok_new and creator):
                v.append(f"{stage} session may not create {pid} with status {a['status']}")
            continue
        if a.get("approval") != b.get("approval"):
            v.append(f"{pid}: approval changed (only the runner sets it)")
        moved = (b["status"], a["status"])
        att = (b.get("attempt", 1), a.get("attempt", 1))
        if moved[0] == moved[1] and att[0] == att[1]:
            continue
        if pid != target:
            v.append(f"{pid}: {moved[0]} -> {moved[1]} is not this session's phase ({target})")
            continue
        allowed = {
            "plan_apply": {("approved", "done"), ("approved", "awaiting_approval")},
            "review": {("planned", "awaiting_approval")},
            "implement": {("approved", "in_progress"), ("approved", "ready_for_test"),
                          ("in_progress", "ready_for_test"), ("approved", "awaiting_approval"),
                          ("in_progress", "awaiting_approval"), ("approved", "approved")},
            "close": {("passed", "done"), ("passed", "in_progress"), ("passed", "blocked")},
        }.get(stage, set())
        if moved not in allowed:
            v.append(f"{pid}: {stage} session may not move {moved[0]} -> {moved[1]}")
        elif att[0] != att[1] and not (stage == "close" and att[1] == att[0] + 1
                                       and moved[1] in ("in_progress", "blocked")):
            v.append(f"{pid}: attempt {att[0]} -> {att[1]} (only the runner changes attempts)")
    bc = load_json(Path(snap) / "state" / current_file(loop, feature), {})
    ac = load_current(loop, feature)
    if bc.get("loop_status") != ac.get("loop_status") and not (
            stage in ("close", "converge") and ac.get("loop_status") in ("complete", "blocked")) \
            and not (stage == "plan" and ac.get("loop_status") in ("running", "none")):
        v.append(f"loop_status {bc.get('loop_status')} -> {ac.get('loop_status')} "
                 f"not allowed in a {stage} session")
    return v


def restore(snap, paths):
    manifest = load_json(Path(snap) / "manifest.json", {})
    tracked = set(git("ls-files", "-z").split("\0"))
    for path in paths:
        saved = Path(snap) / "files" / path
        if path in manifest and manifest[path] and saved.exists():
            Path(path).parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(saved, path)
        elif path in tracked:
            git("checkout", "--", path, check=False)
        else:
            p = Path(path)
            if p.is_symlink() or p.is_file():
                p.unlink()
            elif p.is_dir():
                shutil.rmtree(p)
    # state files come back as they were
    for f in glob.glob(str(Path(snap) / "state" / "loops/**/*.json"), recursive=True):
        rel = Path(f).relative_to(Path(snap) / "state")
        shutil.copy2(f, rel)


def progress_table_changed(snap):
    bad = []
    for f in glob.glob("loops/*/progress.md"):
        saved = Path(snap) / "files" / f
        if not saved.exists():
            continue
        def block(t):
            m = re.search(re.escape(MARK_START) + r".*?" + re.escape(MARK_END), t, re.S)
            return m.group(0) if m else ""
        if block(saved.read_text()) != block(Path(f).read_text()):
            bad.append(f"{f}: milestone table is runner-owned")
    return bad


def cmd_guard(args):
    """guard LOOP STAGE FEATURE TARGET_PHASE SNAPDIR -> exit 1 and restore on a violation."""
    loop, stage, feature, target, snap = args
    violations = []
    allowed = whitelist(loop, stage, feature)
    for path in changed_since(snap):
        if match_any(path, PROTECTED):
            violations.append(f"protected file changed: {path}")
        elif match_any(path, RUNNER_OWNED):
            violations.append(f"runner-owned file changed: {path}")
        elif not match_any(path, allowed):
            violations.append(f"outside the {loop} {stage} whitelist: {path}")
    violations += progress_table_changed(snap)
    violations += status_violations(loop, stage, feature, target, snap)
    if violations:
        bad_paths = [v.split(": ", 1)[1] for v in violations
                     if v.startswith(("protected", "runner-owned", "outside"))]
        bad_paths += [v.split(":", 1)[0] for v in violations if v.endswith("runner-owned")]
        restore(snap, bad_paths)
        print("\n".join(violations))
        sys.exit(1)


# --------------------------------------------------------------------------- permissions

def cmd_flags(args):
    """flags LOOP STAGE -> line 1: --allowedTools value, line 2: --disallowedTools value."""
    loop, stage = args
    protected = []
    for pat in [".claude/**", "CLAUDE.md", ".mcp.json", "loops/*/Loop-instructions.md",
                "scripts/**", ".githooks/**", "project.config.yaml",
                ".specify/memory/constitution.md"]:
        protected += [f"Edit({pat})", f"Write({pat})"]
    deny = protected + ["Bash(git:*)", "Bash(kill:*)", "Bash(pkill:*)"]
    # read-only helpers (dry run: sessions were refused grep/sed -n/unzip -l and worked around it)
    allow = ["Skill", "Bash(grep:*)", "Bash(head:*)", "Bash(tail:*)", "Bash(wc:*)", "Bash(sed -n:*)",
             "Bash(unzip -l:*)", "Bash(java -version)", "Bash(javac -version)", "Bash(date:*)"]
    if loop in DEV_LOOPS:
        _, lcfg = layer_of(loop)
        allow += ["Agent", "Bash(.specify/scripts/bash/*:*)", "Bash(mkdir:*)", "Bash(cp:*)",
                  "Bash(mv:*)", "Bash(rm:*)", "Bash(ls:*)"]
        allow += [f"Bash({c}:*)" for c in lcfg.get("allow_commands") or []]
        deny += ["Bash(curl:*)", "mcp__playwright__*"]
    elif loop == "testing":
        allow += ["Bash(mkdir:*)", "Bash(cp:*)", "Bash(ls:*)", "Bash(bash loops/testing/outputs/*:*)",
                  "Bash(curl:*)", "mcp__playwright__*"]
        for lname, l in cfg_get("layers").items():
            allow += [f"Bash({c}:*)" for c in l.get("test_commands") or []]
            deny += [f"Edit({l['dir']}/**)", f"Write({l['dir']}/**)"]
    elif loop == "orchestrator":
        allow += ["Bash(ls:*)", "Bash(mkdir:*)"]
    print(",".join(allow))
    print(",".join(deny))


# --------------------------------------------------------------------------- accounting

def cmd_account(args):
    """account RESULT_JSON key=value... -> appends one row to docs/sessions.csv."""
    result_file, kvs = args[0], args[1:]
    row = {k: "" for k in CSV_COLUMNS}
    for kv in kvs:
        k, v = kv.split("=", 1)
        row[k] = v
    res = {}
    try:
        res = json.loads(Path(result_file).read_text() or "{}")
        if isinstance(res, list):  # some versions print a list of events; the last is the result
            res = next((e for e in reversed(res) if e.get("type") == "result"), {})
    except (json.JSONDecodeError, FileNotFoundError):
        res = {}
    usage = res.get("usage") or {}
    row.update({
        "session_id": res.get("session_id", row["session_id"]) or "",
        "input_tokens": usage.get("input_tokens", ""),
        "cache_creation_input_tokens": usage.get("cache_creation_input_tokens", ""),
        "cache_read_input_tokens": usage.get("cache_read_input_tokens", ""),
        "output_tokens": usage.get("output_tokens", ""),
        "total_cost_usd": res.get("total_cost_usd", ""),
        "duration_ms": res.get("duration_ms", ""),
        "num_turns": res.get("num_turns", ""),
    })
    if not row["result"]:
        if res.get("is_error") or res.get("subtype", "success") != "success":
            row["result"] = f"error:{res.get('subtype') or 'crash'}"
        else:
            row["result"] = "success" if res else "error:crash"
    SESSIONS_CSV.parent.mkdir(parents=True, exist_ok=True)
    new = not SESSIONS_CSV.exists()
    with SESSIONS_CSV.open("a", newline="") as f:
        w = csv.DictWriter(f, fieldnames=CSV_COLUMNS)
        if new:
            w.writeheader()
        w.writerow(row)
    print(row["session_id"])


def cmd_human(args):
    """human key=value... -> appends one row to docs/human-inputs.csv."""
    row = {k: "" for k in HUMAN_COLUMNS}
    row["at"] = now_iso()
    for kv in args:
        k, v = kv.split("=", 1)
        row[k] = v
    new = not HUMAN_CSV.exists()
    HUMAN_CSV.parent.mkdir(parents=True, exist_ok=True)
    with HUMAN_CSV.open("a", newline="") as f:
        w = csv.DictWriter(f, fieldnames=HUMAN_COLUMNS)
        if new:
            w.writeheader()
        w.writerow(row)


def cmd_human_read_by(args):
    """Fill read_by_session on rows of this loop/milestone that no session has read yet."""
    loop, milestone, session_id = args
    if not HUMAN_CSV.exists() or not session_id:
        return
    with HUMAN_CSV.open() as f:
        rows = list(csv.DictReader(f))
    for r in rows:
        if r["loop"] == loop and r["milestone"] == milestone and not r["read_by_session"] \
                and r["decision"] not in ("quit",):
            r["read_by_session"] = session_id
    with HUMAN_CSV.open("w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=HUMAN_COLUMNS)
        w.writeheader()
        w.writerows(rows)


def cmd_cost(args):
    run_id = args[0]
    total = 0.0
    if SESSIONS_CSV.exists():
        with SESSIONS_CSV.open() as f:
            for r in csv.DictReader(f):
                if r["run_id"] == run_id and r["total_cost_usd"]:
                    total += float(r["total_cost_usd"])
    print(f"{total:.4f}")


def cmd_phase_goal(args):
    """phase-goal LOOP FEATURE PHASE -> the '## Goal' line of the phase file."""
    loop, feature, pid = args
    f = phase_file(loop, feature, pid) if loop != "testing" else \
        Path("loops/testing/outputs") / feature / "suite" / f"{pid}.md"
    if f.exists():
        m = re.search(r"^##\s+Goal\s*\n+(.+)$", f.read_text(), re.M)
        if m:
            print(m.group(1).strip())
            return
        m = re.search(r"^#\s+(.+)$", f.read_text(), re.M)
        if m:
            print(m.group(1).strip())


# --------------------------------------------------------------------------- evidence

def rel(target, start):
    return os.path.relpath(target, start)


def cmd_evidence(args):
    """evidence DEV_LOOP FEATURE PHASE ATTEMPT -> copy test plan/script/report + write INDEX.md."""
    dev_loop, feature, pid, attempt = args
    src = Path("loops/testing/outputs") / feature / dev_loop
    dst = outputs_dir(dev_loop, feature)
    dst.mkdir(parents=True, exist_ok=True)
    copied = []
    for suffix in ("-test.md", "-verify.sh", "-report.md"):
        f = src / f"{pid}{suffix}"
        if f.exists():
            shutil.copy2(f, dst / f.name)
            copied.append(dst / f.name)
    run = Path("loops/testing/runs") / feature / dev_loop / pid / f"attempt-{attempt}"
    idx_dir = Path("loops") / dev_loop / "runs" / feature / pid / f"attempt-{attempt}"
    idx_dir.mkdir(parents=True, exist_ok=True)
    v = load_json(verdict_file(dev_loop, feature, pid), {})
    lines = [f"# {dev_loop} {feature} {pid} attempt {attempt}", "",
             f"Verdict: **{v.get('verdict', 'n/a')}**", ""]
    if v.get("failed_checks"):
        lines += ["Failed checks:", *[f"- {c}" for c in v["failed_checks"]], ""]
    lines += ["## Copied into this loop's outputs"]
    lines += [f"- [{c.name}]({rel(c, idx_dir)})" for c in copied] or ["- (none)"]
    lines += ["", "## Evidence in the testing loop's run folder"]
    files = sorted(p for p in run.rglob("*") if p.is_file()) if run.exists() else []
    lines += [f"- [{p.relative_to(run)}]({rel(p, idx_dir)})" for p in files] or ["- (none)"]
    (idx_dir / "INDEX.md").write_text("\n".join(lines) + "\n")
    print(idx_dir / "INDEX.md")


def cmd_traces(args):
    """traces RUN_DIR keep|drop -> zip Playwright trace/session files into trace.zip or delete them."""
    run, mode = Path(args[0]), args[1]
    pats = cfg_get("testing.trace_globs", default=[], required=False) or []
    found = [p for pat in pats for p in run.glob(pat) if p.exists()]
    if not found:
        return
    if mode == "keep":
        with zipfile.ZipFile(run / "trace.zip", "w", zipfile.ZIP_DEFLATED) as z:
            for p in found:
                for f in ([p] if p.is_file() else p.rglob("*")):
                    if f.is_file():
                        z.write(f, f.relative_to(run))
    for p in found:
        shutil.rmtree(p) if p.is_dir() else p.unlink()


# --------------------------------------------------------------------------- suite

def suite_file(feature):
    return Path("loops/testing/state") / feature / "suite.json"


def cmd_suite_prepare(args):
    """At the start of a suite run: failed_waiting_fix -> retest when all its bugs are fixed.
    Prints 'replan' if a not_ready story is now done in both layers."""
    feature = args[0]
    s = load_json(suite_file(feature))
    if not s:
        print("plan")
        return
    bugs = {b["id"]: b for b in list_bugs(feature)}
    for tp in s.get("test_phases", []):
        if tp.get("status") == "failed_waiting_fix" and tp.get("bugs") and all(
                bugs.get(b, {}).get("status") in ("fixed", "closed") for b in tp["bugs"]):
            tp["status"] = "retest"
    replan = False
    for story in s.get("not_ready", []):
        if all(any(p.get("story_id") == story and p["status"] == "done"
                   for p in load_phases(l, feature)["phases"]) for l in DEV_LOOPS):
            replan = True
    if replan:
        for tp in s.get("test_phases", []):
            if tp["id"] in ("test-phase-E2E", "test-phase-regression"):
                tp["status"] = "planned"
        s["status"] = "running"
    if s.get("status") in ("bugs_open",) and any(
            tp.get("status") in ("planned", "retest") for tp in s.get("test_phases", [])):
        s["status"] = "running"
    save_json(suite_file(feature), s)
    print("replan" if replan else "run")


def cmd_suite_next(args):
    """suite-next FEATURE -> JSON {action, test_phase, attempt} or {action: stop, reason}."""
    feature = args[0]
    s = load_json(suite_file(feature))
    if not s:
        print(json.dumps({"action": "session", "test_phase": "test-phase-00-plan", "attempt": 1}))
        return
    if s.get("status") in ("complete", "blocked"):
        print(json.dumps({"action": "stop", "reason": s["status"]}))
        return
    for tp in s.get("test_phases", []):
        if tp.get("status") in ("planned", "retest", "in_progress"):
            print(json.dumps({"action": "session", "test_phase": tp["id"],
                              "attempt": tp.get("attempt", 1)}))
            return
    if any(tp.get("status") == "failed_waiting_fix" for tp in s.get("test_phases", [])):
        s["status"] = "bugs_open"
    elif any(tp.get("status") == "blocked" for tp in s.get("test_phases", [])):
        s["status"] = "blocked"
    elif s.get("not_ready"):
        s["status"] = "bugs_open" if list_open(feature) else "not_ready"
    else:
        s["status"] = "complete" if not list_open(feature) else "bugs_open"
    save_json(suite_file(feature), s)
    print(json.dumps({"action": "stop", "reason": s["status"]}))


def list_open(feature):
    return [b for b in list_bugs(feature) if b.get("status") == "open"]


def cmd_suite_passed(args):
    """suite-passed FEATURE -> test phases that became done since the snapshot (for commits)."""
    feature, snap = args
    before = load_json(Path(snap) / "state" / suite_file(feature), {"test_phases": []})
    after = load_json(suite_file(feature), {"test_phases": []})
    bmap = {t["id"]: t.get("status") for t in before.get("test_phases", [])}
    for t in after.get("test_phases", []):
        if t.get("status") == "done" and bmap.get(t["id"]) != "done":
            print(t["id"])


def cmd_mark_fixed(args):
    """After a fix-BUG-NNN milestone is done, the runner marks the bug fixed."""
    feature, pid = args
    bug = pid[len("fix-"):]
    f = bugs_dir(feature) / f"{bug}.md"
    if f.exists():
        write_front_matter(f, {"status": "fixed", "fixed_at": now_iso()})


# --------------------------------------------------------------------------- misc

def cmd_write_task(args):
    """write-task LOOP key=value... -> loops/LOOP/task.md (runner-owned)."""
    loop, kvs = args[0], args[1:]
    vals = dict(kv.split("=", 1) for kv in kvs)
    lines = [f"# task.md: {loop}", "",
             "Written by the runner at the start of every run. Sessions read it first and never edit it.", ""]
    for k, v in vals.items():
        lines.append(f"- **{k}:** {v}")
    step = vals.get("orchestrator_step")
    if step:
        order = load_json(order_file(vals.get("feature", "")), {})
        for s in order.get("steps", []):
            if f"{s.get('loop')}:{s.get('story_id')}" == step:
                lines += ["", "## Current orchestrator step",
                          f"- loop: {s.get('loop')}", f"- story: {s.get('story_id')}",
                          f"- goal: {s.get('goal')}"]
    Path("loops", loop, "task.md").write_text("\n".join(lines) + "\n")


def cmd_ensure_progress(args):
    loop = args[0]
    p = Path("loops", loop, "progress.md")
    if not p.exists() or MARK_START not in p.read_text():
        head = f"# progress.md: {loop}\n\n## Milestones (generated from docs/sessions.csv)\n\n" \
               f"{MARK_START}\n_no sessions yet_\n{MARK_END}\n\n## Action log\n"
        old = p.read_text() if p.exists() else ""
        p.write_text(head + old)


def cmd_log_action(args):
    """log-action LOOP text -> appends to the action log of progress.md."""
    loop, text = args
    cmd_ensure_progress([loop])
    with Path("loops", loop, "progress.md").open("a") as f:
        f.write(f"- {now_iso()} {text}\n")


def cmd_set_stop(args):
    """set-stop LOOP FEATURE reason -> records stop_reason without touching loop_status."""
    loop, feature, reason = args
    cur = load_current(loop, feature)
    cur["stop_reason"] = reason
    cur.setdefault("loop_status", "none")
    save_current(loop, feature, cur)


def milestone_sessions(loop, feature, milestone):
    out = []
    if SESSIONS_CSV.exists():
        with SESSIONS_CSV.open() as f:
            for r in csv.DictReader(f):
                if r["feature"] != feature:
                    continue
                if (r["loop"] == loop and r["milestone"] == milestone) or \
                        (r["loop"] == "testing" and r["milestone"] == f"{loop}:{milestone}"):
                    out.append(r)
    return out


def cmd_commit_body(args):
    """commit-body LOOP FEATURE MILESTONE ATTEMPT_DIR -> body of a (verified) commit."""
    loop, feature, milestone = args[:3]
    lines = ["Tasks:"]
    pf = phase_file(loop, feature, milestone)
    if pf.exists():
        lines += [l.strip() for l in pf.read_text().splitlines()
                  if re.match(r"\s*- \[[xX~ ]\]", l)]
    lines += ["", "Sessions:"]
    lines += [f"- {r['session_id']} ({r['loop']} {r['kind']} attempt {r['attempt']}, {r['result']})"
              for r in milestone_sessions(loop, feature, milestone)]
    v = load_json(verdict_file(loop, feature, milestone), {})
    lines += ["", f"Verdict: {v.get('verdict', 'n/a')} (attempt {v.get('attempt', '?')})"]
    p = find_phase(load_phases(loop, feature), milestone) or {}
    ur = load_json(Path("loops/testing/runs") / feature / loop / milestone /
                   f"attempt-{p.get('attempt', 1)}" / "unit" / "unit-result.json", {})
    if ur:
        cov = "skipped" if ur.get("coverage_skipped") else (
            f"{ur['coverage']:.1%}" if ur.get("coverage") is not None else "n/a")
        lines.append(f"Unit tests: {ur.get('passed')} passed, {ur.get('failed')} failed; "
                     f"coverage: {cov}")
    if p.get("verified_by"):
        lines.append(f"Verified by testing session {p['verified_by']}")
    print("\n".join(lines))


def order_file(feature):
    return Path("loops/orchestrator/state") / feature / "order.json"


def cmd_order(args):
    """order FEATURE -> prints loop<TAB>story_id lines from the orchestrator's order.json."""
    for s in load_json(order_file(args[0]), {}).get("steps", []):
        print(f"{s['loop']}\t{s['story_id']}")


COMMANDS = {
    "cfg": cmd_cfg, "check-config": cmd_check_config, "feature": cmd_feature,
    "next": cmd_next, "phase-get": cmd_phase_get, "story-phase": cmd_story_phase,
    "set-phase": cmd_set_phase, "set-loop": cmd_set_loop, "fail-phase": cmd_fail_phase,
    "unblock": cmd_unblock, "questions": cmd_questions, "append-notes": cmd_append_notes,
    "approve": cmd_approve, "skip": cmd_skip, "reassign": cmd_reassign, "reject": cmd_reject,
    "verdict": cmd_verdict, "write-verdict": cmd_write_verdict, "unit-result": cmd_unit_result,
    "harness": cmd_harness, "snapshot": cmd_snapshot, "guard": cmd_guard, "flags": cmd_flags,
    "account": cmd_account, "human": cmd_human, "human-read-by": cmd_human_read_by,
    "cost": cmd_cost, "phase-goal": cmd_phase_goal, "evidence": cmd_evidence,
    "traces": cmd_traces, "suite-prepare": cmd_suite_prepare, "suite-next": cmd_suite_next,
    "suite-passed": cmd_suite_passed, "mark-fixed": cmd_mark_fixed, "bugs": cmd_bugs,
    "bug-set": cmd_bug_set, "write-task": cmd_write_task, "ensure-progress": cmd_ensure_progress,
    "log-action": cmd_log_action, "order": cmd_order, "set-stop": cmd_set_stop,
    "commit-body": cmd_commit_body,
}

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in COMMANDS:
        die("usage: loopctl.py <" + "|".join(sorted(COMMANDS)) + "> [args...]")
    COMMANDS[sys.argv[1]](sys.argv[2:])
