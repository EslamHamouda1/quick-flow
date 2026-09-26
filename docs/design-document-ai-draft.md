# QuickFlow Claude Loops — design and implementation (AI-assisted draft)

> **Authorship note.** This document was drafted by Claude (Anthropic's AI assistant) at the
> repository owner's request, from the project's own records: the plan, the engine code,
> `docs/sessions.csv`, `docs/human-inputs.csv`, `docs/commit-reviews.md` and the git history.
> It is **not** the owner's own written account, which the task asks for separately.
> Facts and numbers here are taken from those records; the reasoning describes the design as built.

## 1. What was built

Four reusable loops that take a requirements file (a full PRD or a single user story) and plan,
build and verify an application:

| Loop | Job |
|---|---|
| `backend-dev` (loop-1) | plans backend phases with spec-kit, writes the API and its swagger, unit tests first |
| `frontend-dev` (loop-2) | plans frontend phases against the backend swagger, writes the UI from a generated client |
| `testing` (loop-3) | verifies every dev phase (curl for the API, Playwright MCP headless for the UI) and runs a standalone suite |
| `orchestrator` | orders the user stories across both layers (dependency graph) |

A bash runner (`scripts/run-loop.sh`) drives each loop one step at a time, and
`scripts/orchestrate.sh` drives all of them for a whole PRD. The app built with them is QuickFlow
(Spring Boot 4.1.1 / Java 25, Angular 22.2).

## 2. Why loops driven by a runner

Each step (plan, review, implement, test, close) is a separate `claude -p` session, and all state
lives in files (`state/<feature>/phases.json`, `current.json`), not in a conversation. This gives:

- a fresh, small context per step, so a long project doesn't degrade one session;
- one session id, token count and cost per step, which the task's accounting needs;
- a place outside Claude for the things Claude shouldn't control: running unit tests, starting and
  stopping servers, counting attempts, committing.

Claude Code's built-in `/loop` re-runs a prompt inside one session, so it provides none of these.

## 3. The state machine

A phase moves `planned → awaiting_approval → approved → in_progress → ready_for_test → passed →
done`, or ends `skipped` / `blocked`. Sessions may only make their own moves (a review session
`planned → awaiting_approval`, an implement session `approved → ready_for_test`, a close session
`passed → done`). Only the runner sets `approved` (the human decision), `passed` (the test verdict),
`skipped`, and attempt counts after a failed test. A frontend story phase depends on the matching
backend story (`backend-dev:US3`): it waits while that isn't done and is blocked if it's blocked.
Each milestone has at most 3 attempts; tooling failures are counted separately so they don't use
the dev loop's attempts.

## 4. Testing as a separate loop

The dev loops never verify their own work. The testing loop writes its checks from the acceptance
criteria and the swagger, not from the code, reuses the same test plan on every retry of a phase,
and cannot change application code (permissions plus the guard). The runner copies the test plan,
curl script and report into the dev loop's `outputs/` and writes an `INDEX.md` per attempt linking
to the evidence, so the task's wording ("loop-1 tests with curl, loop-2 with Playwright MCP") holds.

In the real run this caught one genuine UI bug: frontend Foundational failed its first test because
an unknown route didn't redirect to `/dashboard`; attempt 2 fixed it.

## 5. Human checkpoints and the guard

- **Before each phase:** the dev loop writes a review file (goal, tasks, files, planned unit tests,
  risks, assumptions, questions) and the runner waits for approve / edit / skip / quit.
- **Before each push:** the runner commits locally, one `(verified)` commit per phase; nothing is
  pushed except through `scripts/review-commits.sh` (a pre-push hook refuses anything else).
- **The guard** runs after every session: only allowed paths may change and only allowed status
  moves may happen; anything else is restored and stops the run. This turns "no code before
  approval" and "never self-verify" into enforced rules.

## 6. No guessing

A session builds only on what is written (requirements, spec, plan, contract, constitution,
config, reviewer notes) or checked just now (code, library sources, `--help`). Unknown requirements
become questions; assumptions are listed and need approval. In practice the backend plan for the
PRD asked four questions (what a done plan item changes, when a plan is Completed, what Settings
holds, which plugin versions to pin), and the loops looked up plugin versions on Maven Central
through `./mvnw` instead of using remembered numbers.

## 7. spec-kit as the planning layer

spec-kit v1.0.11 provides specify → plan → tasks → analyze → implement → converge as files the next
session reads. Each requirements file becomes one spec-kit feature per layer
(`specs/001-quickflow-backend`, `…-frontend`). The loops add what spec-kit doesn't do: sessions,
retries, testing, accounting, gates and commits. Two spec-kit behaviours needed handling:
`/speckit-specify` picks its own directory unless given one, and interactive steps (clarify,
checklist prompts) can't run headless, so their questions go to the review file instead.

## 8. Reusability

Skills and `Loop-instructions.md` contain no project, entity, port or stack terms
(`scripts/genericity-check.py` enforces this); `project.config.yaml` is the only per-project file.
The proof is a user story not in the PRD, `examples/single-us/US-tags.md`, run through the unchanged
loops as its own feature `002-us-tags`.

## 9. Accounting

The runner records one row per session in `docs/sessions.csv` (tokens, cost, duration, session id,
engine version, result); `scripts/progress-table.py` rebuilds each loop's milestone table from it;
human decisions go to `docs/human-inputs.csv`; `scripts/export-sessions.py` produces
`docs/prompts-sessions.xlsx`.

## 10. Results

| | |
|---|---|
| Real run | 2026-09-24 16:54 → 2026-09-26 21:54, 104 sessions, ≈ $140 (≈ 6 h of session time) |
| Backend | 10/10 phases verified |
| Frontend | 11/11 phases verified (converge added one) |
| Test suite | 9/9 test phases passed, 0 bugs filed, including the PRD §11 end-to-end flow |
| Commits | 123, pushed after review |

## 11. What failed and what changed

| Problem | Fix |
|---|---|
| The runner froze after the first session (terminal job control) | `timeout --foreground` |
| A user-level shell hook blocked `./mvnw` inside loop sessions | sessions load only project settings and `.mcp.json` |
| An answered question would be asked again at the next gate | answered questions move to `## Answered questions` |
| The machine had the Java runtime but no compiler | full JDK installed; README prerequisite |
| A phase built over several sessions lost its task ticks and failed its close check | implement always ticks `tasks.md` |
| Read-only commands were refused, sessions worked around them | read-only helpers allowed |
| The account usage limit was hit three times | the runner stops cleanly as `usage_limit`; an auto-resume wrapper restarts after the reset |
| Three sessions hung and ignored the timeout's SIGTERM | `timeout -k 60` |
| A "None." answer was read as an open question | the check treats "None…" as nothing asked |

## 12. Tools considered (Hint 3)

Taken: spec-kit (planning) and Playwright MCP (UI verification). Considered: documentation-lookup
MCP servers (Context7, Ref, Augments). Left: skills that duplicate spec-kit's test-first flow or the
existing git guardrails, a Playwright CLI skill (the task asks for the MCP), OpenAPI MCP servers
(reading the generated file is enough) and Git/GitHub MCP servers (loop sessions must not run git).
