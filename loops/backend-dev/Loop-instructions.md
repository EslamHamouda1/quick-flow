# Loop-instructions: backend-dev

You are one session of the **backend-dev** loop. A session does **exactly one step**, then ends.
The runner (`scripts/run-loop.sh`) decides which step it is, starts you, checks what you changed,
counts your tokens and makes every commit. State lives on disk, not in your memory: read it first.

These instructions are project-agnostic. Everything about the stack, commands, URLs and paths
comes from `project.config.yaml` and the spec-kit files. Never assume a stack detail that isn't there.

## 1. Read first
1. `loops/backend-dev/task.md`: the requirements file, `feature`, `current_step` (`<stage> <phase>`),
   `swagger` (if your layer takes one), stop point and whether review gates are on.
2. `project.config.yaml`: **your layer** is the entry under `layers:` whose `loop` is `backend-dev`.
   Call its key `<layer>` and its settings `L` (`L.dir`, `L.build`, `L.test`, ...).
3. `.specify/memory/constitution.md`: the project's non-negotiable rules and pinned versions.

Names used below:
| Name | Path |
|---|---|
| `FEATURE` | the `feature` in task.md, e.g. `001-myapp` |
| `FEATURE_DIR` | `specs/<FEATURE>-<layer>` (also exported as `SPECIFY_FEATURE_DIRECTORY`) |
| `STATE` | `loops/backend-dev/state/<FEATURE>/` |
| `OUT` | `loops/backend-dev/outputs/<FEATURE>/` |
| `RUNS` | `loops/backend-dev/runs/<FEATURE>/` (build logs) |
| `VERDICTS` | `loops/testing/state/<FEATURE>/verdicts/backend-dev/` (read only) |
| `BUGS` | `loops/testing/outputs/<FEATURE>/suite/bugs/` (read only) |

## 2. Hard rules (the runner's guard enforces them; a violation stops the run and is restored)
- **One step, then stop.** Never start the next step yourself.
- **No guessing.** Something is known only if it is written in the requirements file, `spec.md`,
  `plan.md`, the contract, the constitution, `project.config.yaml` or the reviewer notes, **or** you
  checked it just now (the code, the installed library's sources or docs, a command's `--help`, a
  small compile). A missing *requirement* becomes a question (`[NEEDS CLARIFICATION]` while
  planning; `## Questions` while implementing, see step I). A *technical fact* is checked before
  code relies on it; if it can't be checked, it becomes a question. Every assumption goes under
  `## Assumptions` in the review file and is only built once approved.
- **Versions** come only from `project.config.yaml`'s `stack` table / the constitution. Never upgrade
  or pick one from memory.
- **Never** run git, curl or any Playwright tool, start or stop servers, or kill processes.
- **Write only** inside `FEATURE_DIR`, `STATE`, `OUT`, `RUNS` and the action log of
  `loops/backend-dev/progress.md`. Implement/close sessions may also write `L.dir/**` and the files
  in `L.app_wide_files`. Never touch another layer's code, `loops/testing/**`, any `task.md`, the
  milestone table between the `<!-- milestones -->` markers, `docs/`, `.claude/`, `scripts/`,
  `CLAUDE.md`, `.mcp.json`, `project.config.yaml`, the constitution or any `Loop-instructions.md`.
- **Status moves you may make** (only on the phase in `current_step`): plan: create entries
  (`phase-00` as `awaiting_approval`, all others `planned`); plan-apply: `approved → done` (or back
  to `awaiting_approval`); review: `planned → awaiting_approval`; implement: `approved/in_progress →
  in_progress → ready_for_test`, or back to `awaiting_approval` with a question; close:
  `passed → done`, or `passed → in_progress` / `blocked` with `attempt + 1`. Only the runner sets
  `approved`, `passed`, `skipped`, changes attempts after a test, or unblocks. **You never verify
  your own work**: no verdicts, no "tested" claims.
- **Log every action** (task ticked, file written, question raised) as one line at the end of
  `loops/backend-dev/progress.md`: `- <ISO time> <FEATURE> <phase>: <what>`.

## 3. State files (in `STATE`)
- `phases.json`: `{"phases": [ {"phase": "phase-03", "title": "US1 <story title>", "story_id": "US1",
  "layer": "<layer>", "spec_phase": 3, "task_ids": ["T012", "..."], "depends_on": ["phase-02"],
  "attempt": 1, "status": "planned", "approval": null, "block_reason": null} ]}` in `tasks.md` order.
  `phase-00` is the plan (`spec_phase: null`, `story_id: null`). Setup, Foundational and Polish have
  `story_id: null`. Statuses: `planned → awaiting_approval → approved → in_progress →
  ready_for_test → passed → done`, plus `skipped`, `blocked`.
- `current.json`: `{"feature_dir", "current_phase", "loop_status", "stop_reason"}`; `loop_status`
  is `none | running | complete | blocked` (you only set `complete`/`blocked`, in close/converge).
- `requirements.json`: the normalized requirements (see step P).
- `tasks.json`: `{"T012": {"phase": "phase-03", "state": " " | "~" | "x"}}`.

## 4. The phase file `OUT/<phase>.md`
```
# <phase>: <title>
layer: <layer>
story_id: <USn or none>
spec_phase: <N>            (the "## Phase N" section of FEATURE_DIR/tasks.md)
depends_on: [<phases>]
## Goal
<one line>
## Acceptance criteria
- AC-<story>-<n>: Given ... When ... Then ...   (copied from spec.md; empty for Setup)
## Tasks
- [ ] T012 [P] [US1] <task text copied from tasks.md>
```
`[ ]` not started, `[~]` code written (not yet verified), `[x]` verified and closed.

## 5. Do the step named in `current_step`
Check first that the phase's status in `phases.json` matches the step; if it doesn't, change
nothing, log the mismatch and stop.

### P. `plan phase-00` (no `phases.json` yet)
1. Use `FEATURE_DIR` exactly. Never let spec-kit pick another directory; if a spec-kit script
   prints a different `FEATURE_DIR`, stop and write that under `## Questions` in
   `OUT/phase-00-review.md`, create `phases.json` with only `phase-00` as `awaiting_approval`, and end.
2. **spec.md**: if a sibling layer's `specs/<FEATURE>-<other layer>/spec.md` exists, copy it and its
   `checklists/` into `FEATURE_DIR`. Otherwise run `/speckit-specify` with the requirements file's
   content (a full PRD or a single user story), and state in its arguments
   `SPECIFY_FEATURE_DIRECTORY=<FEATURE_DIR>` so it uses that directory (the runner has already
   created it) instead of auto-numbering a new `specs/NNN-<short-name>`.
3. Build `STATE/requirements.json` from `spec.md`:
   `{"feature", "source", "stories": [{"id": "US1", "title", "priority": "P1", "acceptance":
   [{"id": "AC-US1-1", "given", "when", "then"}], "requirements": ["FR-001"], "depends_on": [],
   "layers": ["<layer>", ...]}], "requirements": [{"id", "text"}], "rules": [{"id", "text"}]}`.
   A single user story becomes a one-story list. Use the ids the spec uses.
4. Run `/speckit-plan` with the tech context from `project.config.yaml` (`stack` + your layer).
   - If another layer's config says `contract_from: <layer>` (it depends on your API), the plan
     **must** write `FEATURE_DIR/contracts/openapi.yaml` as an OpenAPI 3.1 document covering every
     endpoint of every story. That file is the other layer's swagger input for its planning.
   - If your layer has `contract_from`, plan against the swagger in task.md (`swagger`).
5. Run `/speckit-tasks`, **always asking for tests** (tests first for every user story phase).
6. Run `/speckit-analyze` (read-only).
7. For each `## Phase N` of `tasks.md` write `OUT/phase-NN.md` (two digits, `phase-01` = Phase 1) as
   in section 4, and create its entry in `phases.json` as `planned`. `depends_on`:
   Setup → `[]`; Foundational → `[phase-01]`; each story phase → the Foundational phase plus the
   phases of the stories in its `depends_on`; if your layer has `contract_from: <other>`, add
   `"<that layer's loop>:<story id>"` to each story phase; Polish → every story phase.
8. Write `OUT/phase-00-review.md` with: `## Phases` (a table: phase, title, story, tasks, depends_on),
   `## Open questions` (every `[NEEDS CLARIFICATION]` left in spec.md, one per line, with your
   recommended answer), `## Analyze findings` (CRITICAL and HIGH only), `## Unchecked checklist
   items`, `## Assumptions`, and, if your layer takes a swagger, `## Swagger input` naming the file.
9. Create `phase-00` in `phases.json` as `awaiting_approval` and set `current.json`
   (`feature_dir`, `current_phase: phase-00`, `loop_status: running`). Stop.

### A. `plan_apply phase-00` (phase-00 `approved`) — never write code here
1. Read `## Reviewer notes` in `OUT/phase-00-review.md`. Write each answer into `spec.md`, replacing
   its `[NEEDS CLARIFICATION]` marker. Update `STATE/requirements.json` to match. Rename the
   answered `## Open questions` heading to `## Answered questions` (the runner treats any
   remaining `## Open questions` / `## Questions` section as unanswered).
2. Apply any edits the reviewer made to the phase files back to `tasks.md` (removed tasks deleted,
   new tasks added with new T-ids, changed wording copied), then refresh `phases.json`'s `task_ids`.
3. Tick the items in `FEATURE_DIR/checklists/` that are now satisfied.
4. Re-run `/speckit-analyze`. If a CRITICAL finding remains, write it under `## Open questions` and
   set phase-00 back to `awaiting_approval`. Otherwise set phase-00 `done`. Stop.

### R. `review <phase>` (status `planned`)
Write `OUT/<phase>-review.md`:
`## Goal`, `## Tasks`, `## Acceptance criteria`, `## Files` (files, endpoints or components it will
create or change), `## Planned checks` (including **the unit tests to write, one per business
rule**, named with the rule id, when your layer has `coverage` in the config), `## Risks`,
`## Assumptions`, `## Open questions`; for a layer with `contract_from`: `## Swagger input` (the
file in task.md) and `## Contract differences` (endpoints of this story where the generated
swagger differs from `specs/<FEATURE>-<contract layer>/contracts/openapi.yaml`). If the review file
already exists (an unblocked or reopened phase), keep its `## Reviewer notes` and history and
rewrite the rest. Set status `awaiting_approval`. Stop.

### I. `implement <phase>` (status `approved`, or `in_progress` on a retry)
1. **Sync review edits into tasks.md first**: compare the task list in `OUT/<phase>.md` with the
   `## Phase <spec_phase>` section of `tasks.md` and make `tasks.md` match (removed, added with new
   T-ids, reworded). Log each change. `/speckit-implement` only reads `tasks.md`.
2. Read `## Reviewer notes` (and `## Questions` answers) in the review file. Rename an answered
   `## Questions` heading to `## Answered questions`, so the runner doesn't ask it again. On a retry
   (`attempt ≥ 2`, or a reopened phase) read the failing report
   `loops/testing/outputs/<FEATURE>/backend-dev/<phase>-report.md` and fix what it names.
3. Set status `in_progress`.
4. If any file in `FEATURE_DIR/checklists/` has an unchecked item, don't call `/speckit-implement`
   (it would stop to ask). Write the items under `## Questions`, set `awaiting_approval`, stop.
5. Run `/speckit-implement only Phase <spec_phase>, tasks <first>–<last>` (the phase's `task_ids`).
   - **Parallel `[P]` tasks**: for each group of `[P]` tasks, start one subagent per task with the
     Agent tool and wait for all of them. Tell each subagent: write **only** the code files of your
     own task, report back what you did, never touch `tasks.md`, phase files, `progress.md` or
     state files, never run git/curl/servers. Tasks without `[P]` run one after another. You (the
     parent) mark every task yourself.
   - When your layer has `coverage` in the config, write the unit tests **before** the code they test.
6. Mark each finished task `- [~]` in `OUT/<phase>.md` and `STATE/tasks.json`.
7. Run `L.build` and, if `L.test` is set, `L.test` for quick feedback (save output to
   `RUNS/<phase>-build.log`). Fix compile errors and failing tests you caused.
8. App-wide outputs:
   - if `L.openapi_output` is set: copy it to `L.openapi_copy`;
   - if `L.generate_client` is set: run it (it reads the other layer's generated swagger);
   - if `L.ui_url_file` is set: write `L.url` into it, with one line saying how to start the app.
9. **If you hit something you don't know** (a requirement, or a fact you can't check): stop coding,
   write the question under `## Questions` in the review file (what you need, why, your recommended
   answer), set the phase back to `awaiting_approval` (attempt unchanged), log it, stop.
10. Otherwise set `ready_for_test`. **No curl, no Playwright, no "verified" claims.** Stop.

### C. `close <phase>` (status `passed`, set by the runner after a `pass` verdict)
1. Check: every task of the phase is `[X]` in `tasks.md`, and `VERDICTS/<phase>.json` has
   `verdict: pass` for the phase's current `attempt`.
   If a task isn't `[X]`: set `in_progress` with `attempt + 1` (at attempt 3 set `blocked` with the
   reason), write the missing task ids under `## Close check` in the review file, stop.
2. Turn `[~]` into `[x]` in `OUT/<phase>.md`, in `STATE/tasks.json` and log it. Set the phase `done`.
3. If no phase is left that is not `done`, `skipped` or `blocked`, do step V now in this session.
4. Stop. (The next phase's review runs in its own session.)

### V. `converge` (every phase finished; also the end of step C)
Run `/speckit-converge` once over the whole feature.
- If it appended a `## Phase N: Convergence` section: create `OUT/phase-polish-<k>.md` from it
  (next free k) and its `phases.json` entry as `planned` (`spec_phase: N`,
  `depends_on: []`). Stop.
- If it reports **Converged**: set `current.json` `loop_status` to `complete`, or to `blocked` if any
  phase is `blocked` (with a one-line summary per blocked phase in `stop_reason`). Stop.

### B. `intake fix-BUG-NNN` (an `open` bug in `BUGS` aimed at `backend-dev`)
Read the bug file (never edit it). Append to `tasks.md` a `## Phase <next N>: Fix BUG-NNN` section
with new T-ids: the **first task is a test that reproduces the bug and fails**, the next ones fix
it. Write `OUT/fix-BUG-NNN.md` from that section (goal = the bug title, acceptance criteria = the
bug's expected behaviour) and add `fix-BUG-NNN` to `phases.json` as `planned` with
`story_id` from the bug and `depends_on: []`. Never create a milestone that already exists. Stop.
The milestone then goes through review → implement → test → close like any phase.

## Stop condition
The loop stops when no phase is left to pick. It ends **`complete`** when every phase is `done` or
`skipped` and the final `/speckit-converge` reports Converged, or **`blocked`** when at least one
phase failed 3 attempts (or depends on one that did) and everything else that could run is done.
A run can also stop early: `waiting` (a phase waits for another layer's story), `quit` (you pressed
q), `needs_input` (a question under `--auto-approve`), `until_reached`, `cost_cap`, `harness` or
`violation`. The runner writes the reason to `STATE/current.json` as `stop_reason` and prints it.
Each milestone has at most **3 attempts**.
