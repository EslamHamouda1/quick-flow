#!/usr/bin/env python3
"""Guardrail: the engine (skills + Loop-instructions) must not mention the project.

Builds the list of forbidden terms from project.config.yaml (project name, feature slugs, stack
names, commands, ports, packages) and from the entity headings of every specs/*/data-model.md,
then greps the loop skills and Loop-instructions for them. Prints each hit; exit 1 if any.
"""
import re
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
ENGINE = [*ROOT.glob(".claude/skills/backend-dev/*.md"), *ROOT.glob(".claude/skills/frontend-dev/*.md"),
          *ROOT.glob(".claude/skills/testing/*.md"), *ROOT.glob(".claude/skills/orchestrator/*.md"),
          *ROOT.glob("loops/*/Loop-instructions.md")]
# generic words that also appear in config values but say nothing about the project
ALLOW = {"backend", "frontend", "layer", "layers", "test", "build", "run", "loop", "api", "url",
         "http", "https", "localhost", "npm", "npx", "cd", "and", "the", "with", "for", "from",
         "data", "file", "files", "latest", "release", "setup", "true", "false", "none", "default",
         "in", "memory", "profile", "profiles", "reports", "outputs", "loops", "target", "src",
         "app", "dev", "server", "start", "package", "verify", "generate", "client", "yes", "no",
         "curl", "playwright", "mcp", "headless", "chromium", "openapi", "swagger", "json", "yaml",
         "tests", "unit", "coverage", "domain", "spec", "kit", "specs", "state", "suite", "ui",
         "error", "errors", "ignore", "failure", "network", "unknown", "host", "could", "not",
         "resolve", "dependencies", "transfer", "artifact", "parent", "pom", "maven", "compiler",
         "plugin", "at", "on", "of", "to", "a", "an", "is", "it", "be", "or", "by", "as", "db"}


def words(v):
    if isinstance(v, dict):
        for k, x in v.items():
            yield from words(x)
    elif isinstance(v, list):
        for x in v:
            yield from words(x)
    elif v is not None:
        for w in re.findall(r"[A-Za-z][A-Za-z0-9_.@/-]{2,}|\b\d{4,5}\b", str(v)):
            for part in re.split(r"[/.@]", w):
                if len(part) >= 3 or part.isdigit():
                    yield part


def project_terms(cfg):
    """Names that identify this project or its stack (not English words)."""
    t = set()
    t.add(str((cfg.get("project") or {}).get("name", "")))
    for req, slug in (cfg.get("features") or {}).items():
        t |= {Path(req).stem, str(slug)}
    for group in (cfg.get("stack") or {}).values():
        for key, val in (group or {}).items():
            t |= set(key.split("_"))                      # spring_boot -> spring, boot
            first = re.match(r"[A-Za-z@][\w@/.-]*", str(val))
            if first:
                t.add(first.group(0))                     # "JDK 25" -> JDK
    for layer in (cfg.get("layers") or {}).values():
        for key in ("build", "test", "run", "run_test", "scaffold", "generate_client"):
            cmd = str(layer.get(key) or "")
            for prog in re.findall(r"(?:^|&&|\|\||;)\s*(?:cd \S+ && )?([./\w@-]+)", cmd):
                t.add(prog.lstrip("./"))                  # ./mvnw -> mvnw
            t |= set(re.findall(r"@[\w-]+/[\w-]+", cmd))  # scoped npm packages
        for key in ("base_url", "url", "ready_url"):
            t |= set(re.findall(r":(\d{2,5})\b", str(layer.get(key) or "")))
        pkg = ((layer.get("coverage") or {}).get("package") or "")
        t |= set(pkg.split("/"))
    return t


def main():
    cfg = yaml.safe_load((ROOT / "project.config.yaml").read_text())
    terms = project_terms(cfg)
    # entity names are matched case-sensitively as proper names; names that are also the
    # engine's own vocabulary (a phase has tasks, the orchestrator writes a plan) are skipped
    engine_vocab = {"Task", "Tasks", "Plan", "Plans", "Phase", "Loop", "Milestone", "Note", "Notes",
                    "State", "Setting", "Settings", "Item", "Status", "Question", "Report", "Bug"}
    entities = set()
    for dm in ROOT.glob("specs/*/data-model.md"):
        entities |= set(re.findall(r"^#{2,4}\s+`?([A-Z][A-Za-z0-9]+)`?", dm.read_text(), re.M))
    entities = {e for e in entities - engine_vocab if not e.isupper()}   # skip acronyms like API
    # anything the generic template also uses is not project-specific
    example = yaml.safe_load((ROOT / "project.config.example.yaml").read_text())
    generic = project_terms(example) - {p for p in project_terms(example) if p.isdigit()}
    loops = {l.get("loop") for l in (cfg.get("layers") or {}).values()}
    terms = {x for x in terms if x and len(x) >= 2 and x.lower() not in ALLOW
             and x not in generic and x not in loops and not x.startswith("<")}
    hits = []
    for f in ENGINE:
        for n, line in enumerate(f.read_text().splitlines(), 1):
            for t in terms:
                if re.search(rf"(?<![A-Za-z0-9_-]){re.escape(t)}(?![A-Za-z0-9_-])", line, re.I):
                    hits.append(f"{f.relative_to(ROOT)}:{n}: '{t}': {line.strip()[:100]}")
            for t in entities:
                if re.search(rf"(?<![A-Za-z0-9_-]){re.escape(t)}(?![A-Za-z0-9_-])", line):
                    hits.append(f"{f.relative_to(ROOT)}:{n}: entity '{t}': {line.strip()[:100]}")
    if hits:
        print("\n".join(sorted(set(hits))))
        sys.exit(1)
    print(f"ok: {len(ENGINE)} engine files, {len(terms)} project terms, {len(entities)} entity names, no hits")


if __name__ == "__main__":
    main()
