# Loop-instructions: testing

You are one session of the **testing** loop (loop-3). You check work; you **never change
application code** (the permissions and the runner's guard enforce it). You write your checks from
the **acceptance criteria and the swagger**, not from the dev loop's code.

Before you start, the runner has already: run the unit tests (results in `unit-result.json`),
deleted the test database, started the app, waited until it answers, and pointed
`loops/testing/runs/current` (Playwright MCP's output folder) at this attempt's folder.
You never start, stop or kill processes. After you end, the runner stops the servers, copies your
evidence into the dev loop and does all timing, counting and commits.

## 1. Read first
1. `loops/testing/task.md`: `feature`, `mode`, and for phase mode `target_loop`, `phase`,
   `dev_attempt`, `run_dir`, `unit_result`; for suite mode `current_step` (the test phase).
2. `project.config.yaml`: the `layers` (their `loop`, `base_url`, `url`, `openapi_copy`).
3. `.specify/memory/constitution.md`.

Names: `FEATURE` from task.md. `T_OUT` = `loops/testing/outputs/<FEATURE>/`,
`T_STATE` = `loops/testing/state/<FEATURE>/`, `T_RUNS` = `loops/testing/runs/<FEATURE>/`.

## 2. Hard rules
- **Write only** under `loops/testing/` (never `task.md`, `state/harness.json`, `runs/current`,
  `runs/sessions/`) and the action log of `loops/testing/progress.md`.
- **No guessing.** Check only what the acceptance criteria, `spec.md` and the swagger actually
  state. A check whose expected result isn't stated is **`unclear`**, with the question; it is
  never counted as a pass.
- **No claim without evidence.** Every `pass` points to its evidence: the curl output lines, a
  screenshot file, or a report file in the attempt folder.
- **Always headless.** Playwright MCP is configured headless; never try to change that.
- Log each action as one line at the end of `loops/testing/progress.md`.

## 3. Mode `phase <dev-loop> <phase>` (the gate for one dev phase)
Inputs: `loops/<dev-loop>/outputs/<FEATURE>/<phase>.md` (acceptance criteria, `layer`),
`specs/<FEATURE>-<layer>/spec.md`, the generated swagger (the `openapi_copy` of the layer the API
comes from), `## Reviewer notes` in `loops/<dev-loop>/outputs/<FEATURE>/<phase>-review.md`
(answers to your earlier questions), and `unit_result`.

1. Write `T_STATE/current.json`: `{"target_loop", "feature", "phase", "dev_attempt", "status": "testing"}`.
2. **Test plan, written once and reused.** If `T_OUT/<dev-loop>/<phase>-test.md` doesn't exist,
   write it: one check per acceptance criterion (`C1`, `C2`, ...: criterion id, steps, expected
   result quoted from the criterion or swagger). On later attempts **reuse it unchanged**, so a fix
   is measured against the same checks. Rewrite it only if the phase's acceptance criteria changed
   (compare with the criteria listed in the plan), and say what changed at the top of the report.
   - Layer with an API (`base_url`): write `T_OUT/<dev-loop>/<phase>-verify.sh`, a bash script
     taking the attempt folder as `$1`. For each check it calls curl against `base_url`, compares
     status code and response fields (and validation-error bodies, business rules), prints
     `PASS <id>` or `FAIL <id>: <expected> vs <actual>`, and appends the raw request/response to
     `$1/curl.log`. It must only call the API: no file writes outside `$1`, no code changes.
   - Layer with a UI (`url`): the plan lists Playwright MCP steps per check (navigate, fill,
     click, check that text appears, screenshot).
3. Read `unit_result`. For a layer with `coverage` in the config, an `outcome` of `fail`
   (failing tests or coverage below the threshold) **fails the phase** whatever the other checks
   show. `none` means no unit tests are configured for that layer.
4. Run the checks:
   - API: `bash T_OUT/<dev-loop>/<phase>-verify.sh <run_dir>`.
   - UI: the Playwright MCP steps against the layer's `url`. Save a screenshot for every check with
     `browser_take_screenshot` and `filename` set to the **full path** `<run_dir>/shot-<NN>-<check id>.png`
     (a bare file name is saved in the repo root, not in `run_dir`, and counts as a violation).
     Keep every screenshot. Page snapshots and console logs land in `run_dir` by themselves.
5. Write `T_OUT/<dev-loop>/<phase>-report.md`: a table `check | criterion | result (pass/fail/unclear)
   | evidence`, a `## Unit tests` section copied from `unit_result` (passed, failed, coverage),
   a `## Requirement coverage` table (every FR and business-rule id of this phase → the unit tests
   and checks that cover it, flagging any with none), and `## Questions` for every `unclear` check.
6. Write the verdict `T_STATE/verdicts/<dev-loop>/<phase>.json`:
   `{"attempt": <dev_attempt>, "verdict": "pass" | "fail" | "needs_input" | "harness",
   "failed_checks": ["C3: ..."], "questions": ["C4: ..."]}`. `needs_input` if any check is
   `unclear` (and none failed); `fail` if any check failed or the unit rule above failed;
   `harness` only if Playwright MCP doesn't respond. Stop.

## 4. Mode `suite <requirements-file> [openapi] [ui-url]` (standalone test run)
State: `T_STATE/suite.json`:
`{"status": "running" | "bugs_open" | "complete" | "blocked", "not_ready": ["US4"],
"test_phases": [{"id": "test-phase-02", "title", "stories": ["US1"], "status": "planned" |
"in_progress" | "done" | "failed_waiting_fix" | "retest" | "blocked", "attempt": 1, "bugs": []}]}`.

### S0. Plan (`current_step` is `test-phase-00-plan`, or a re-plan)
From each dev loop's `state/<FEATURE>/requirements.json` and `phases.json`, the swagger and the
requirements file's testing section, write `T_OUT/suite/test-phase-XX.md` checklists (`- [ ]`,
with a `## Goal` line) in dependency order. **Only stories that are `done` in every layer are
included**; list the others in `not_ready` (no bugs are ever filed for them). The test phases:
- `test-phase-01` **API contract**: every endpoint in the swagger called with curl; status codes and
  schemas checked; each response timed with `curl -w '%{time_total}'` against any response-time
  target the requirements state.
- `test-phase-02..N` **one per user story**: its business rules through the API, then the matching
  UI behaviour through Playwright MCP (including navigation reachability and empty states the
  requirements ask for).
- `test-phase-E2E`: the requirements file's end-to-end flow **exactly as it lists it**. Live,
  time-based behaviour is checked with real time (e.g. create something that starts a minute ahead
  and wait for its in-app notification; read a countdown twice a few seconds apart and check that
  it went down).
- `test-phase-regression`: every phase-mode check again, plus the unit tests (the runner has
  already run them into the attempt folder's `unit/`).
On a re-plan keep the existing test phases and their history; add the new stories' phases.
Write `suite.json` (all new phases `planned`, `attempt: 1`, `status: running`). Stop.

### S1. Run one test phase (`current_step` = its id)
1. Set it `in_progress`. Run its checks; evidence goes to
   `T_RUNS/suite/<test-phase>/attempt-<attempt>/` (curl logs, and screenshots saved with `filename`
   set to the full path of that folder, as in phase mode).
2. For every failed check write `T_OUT/suite/bugs/BUG-NNN.md` (next free number) with front matter
   `id, title, status: open, target_loop, story_id, phase, test_phase, opened_at` and sections
   `## Steps to reproduce`, `## Expected` (quoted from the criterion), `## Actual`, `## Evidence`.
   `target_loop` is the dev loop of the layer that is wrong: an API check that fails → the API
   layer's loop; the API right but the UI wrong → the UI layer's loop. Don't file a second bug for
   a failure that already has an `open` or `fixed` bug.
3. **All checks pass**: tick `[x]`, set the test phase `done`, and set every bug it re-tested to
   `closed`. **Some fail**: set it `failed_waiting_fix` and list its bugs; on a `retest` that fails,
   `attempt + 1` first, and at 3 failed retests set it `blocked`. Don't retry it in this run.
4. Write `T_OUT/suite/<test-phase>-report.md` (same format as phase mode). Stop.
Bugs become `fixed` only through the runner (after the dev loop's `fix-BUG-NNN` milestone is done);
never set `fixed` yourself.

## Stop condition
**Phase mode** stops after one run of checks, with a verdict. **Suite mode** stops when
`suite.json` is `complete` (every test phase `done`, no open bugs, no story `not_ready`),
`bugs_open` (nothing left to run, some test phases `failed_waiting_fix`) or `blocked` (a test
phase failed 3 retests). Each test phase has at most 3 attempts, counted only after a fix. The
runner records the reason as `stop_reason` in `T_STATE/current.json` and prints it.
