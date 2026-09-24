# CLAUDE.md

This repo is a reusable **Claude Loops engine** plus the app it builds (QuickFlow, from `Task_PRD.md`).
This file is part of the engine: no loop session may change it.

## Layout
- `.claude/skills/{backend-dev,frontend-dev,testing,orchestrator}/`: loop entry skills; each reads
  `loops/<loop>/Loop-instructions.md`. `.claude/skills/speckit-*`: spec-kit (planning layer).
- `loops/<loop>/`: `Loop-instructions.md`, `task.md` (runner-written), `progress.md`, `state/<feature>/`,
  `outputs/<feature>/`, `runs/<feature>/`.
- `scripts/run-loop.sh` (the runner), `scripts/orchestrate.sh`, `scripts/review-commits.sh`,
  `scripts/lib/loopctl.py` (state machine, guard, accounting), `scripts/progress-table.py`,
  `scripts/export-sessions.py`, `scripts/genericity-check.py`.
- `project.config.yaml`: the only per-project file (stack, commands, URLs, thresholds, limits).
- `specs/<feature>-<layer>/`: spec-kit feature folders. `backend/`, `frontend/`: app code.

## Rules for every session
- Loops run only through `scripts/run-loop.sh` / `scripts/orchestrate.sh`, one step per `claude -p` session.
- **No guessing**: build only on what is written (requirements, spec, plan, contract, constitution,
  config, reviewer notes) or checked just now. Unknown requirements become questions; assumptions
  are listed and need approval.
- Versions only from the `stack` table in `project.config.yaml` / the constitution. Never `@latest`.
- No session runs git or pushes; the runner commits, `scripts/review-commits.sh` pushes after review.
- Dev loops never verify their own work; the testing loop does (curl for the API, Playwright MCP,
  always headless, for the UI).
- Commands run from the repo root (`cd backend && ./mvnw ...`, `cd frontend && npm ...`).
