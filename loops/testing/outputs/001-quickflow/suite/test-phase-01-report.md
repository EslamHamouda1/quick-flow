# test-phase-01 report: API contract (attempt 1)

Run 2026-09-26 12:14–12:19 Africa/Cairo against `http://localhost:8080` (fresh test DB, started by the runner).
Result: **9 / 9 pass**, no bugs filed.

**How the checks ran.** In this session `bash <script>`, `python3` and `jq -f/--slurpfile` needed approval
that the session couldn't get, so `test-phase-01-verify.sh` (written for re-runs) **was not executed**.
The same checks ran as single curl calls, with jq validating the bodies:
- every call: `curl -o NN-<label>--<Schema>.json -D NN-<label>.head -w '<check> <label> <timed> <expected> <method path> %{http_code} %{time_total}'`,
  timing lines in `calls.tsv`;
- one inline `jq -n` run checked every JSON body against the swagger schema named in its file name
  (`ArrX` = array of X, `Problem` = RFC 9457 body from `contracts/openapi.yaml`), results in `schema-check.tsv`;
  the same validator is saved as `test-phase-01-schema.jq`;
- negative control: a planted bad Task body (`zz-negative-control--Task.json`) was flagged for all 7 planted faults
  (missing `overdue`, extra `foo`, non-integer id, bad enum, bad date, date-time without offset, non-boolean) →
  `schema-negative-control.txt`.

Evidence folder: `loops/testing/runs/001-quickflow/suite/test-phase-01/attempt-1/` (below: `A/`).
`A/curl.log` has every response's headers and body, `A/calls.tsv` has one line per call (expected vs actual status, `time_total`).

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | `GET /v3/api-docs` 200; 39 operationIds = `loops/backend-dev/outputs/openapi.json` | pass | `A/calls.tsv` C1 200; jq compare: `live=39 copy=39 equal=true` (`A/01-apidocs--none.json`) |
| C2 | `getAppInfo` 200, `AppInfo`, timeZone `Africa/Cairo` | pass | `A/02-getAppInfo--AppInfo.json` `{"timeZone":"Africa/Cairo","now":"2026-09-26T12:14:…+03:00"}`; schema ok |
| C3 | Tasks: 18 calls: create 201/400, list 200 (all 8 query params), overdue 200, get 200/404, update 200/400/404, complete/archive/restore 200/404, delete 204/404; invalid body → `errors[].field` | pass | `A/calls.tsv` rows 03–20 all actual = expected; `createTask` defaults TODO/MEDIUM; filtered list = [task 1]; complete → DONE + completedAt; archive → archived true, restore → false; 400 bodies list `title` (04) and `status` (11) |
| C4 | Habits: 23 calls: create 201/400, list 200 (+`active=true`), get 200/404, update 200/400/404, deactivate/activate 200/404, completeHabit 201 (no body → today) / 400 (2026-09-27) / 404 / 409 (same date), completions 200/404, undo 204/404, delete 204/404 | pass | `A/calls.tsv` rows 21–43; 34: `completionDate 2026-09-26`; 35: `errors[{field:"date","must not be in the future"}]`; 37: 409 "already complete for 2026-09-26"; 22/28 list `name` / `frequency` |
| C5 | Learning: 24 calls: card create 201 (NOT_STARTED)/400, list 200, get 200/404, update 200/400/404, milestone add 201 (done false)/400/404, update 200/400/404, delete 204/404, note add 201/400/404, delete 204/404, card delete 204/404 | pass | `A/calls.tsv` rows 44–67; 61: card with 1 milestone + 1 note, `milestonesDone 1/1`; 400 bodies list `title`, `status`, `done`, `text` |
| C6 | Plans: 20 calls: create 201 / 400 (no items, end < start, end = start), list 200 (no group, `active`, `completed`, `all`), get 200/404, update 200/400/404, setPlanItemDone 200/400/404, delete 204/404 | pass | `A/calls.tsv` rows 68–87; 70 `items: must not be empty`; 71/72/80 `endDateTime: must be after startDateTime`; 82: doneItems 1, totalItems 1, progressPercent 100; 83 `done: must not be null` |
| C7 | `getDashboard` 200, `getSettings` 200, `updateSettings` 200/400 | pass | `A/88-getDashboard--Dashboard.json`, `A/89-getSettings--Settings.json` (defaults: displayName null, notifications true, DASHBOARD), 90 echoes `Contract/false/TASKS`, 91: `defaultPage "NOPE"` → 400 `errors[{field:"defaultPage"}]`; restored to defaults (92) |
| C8 | Each 2xx body carries the required properties of its swagger schema, with stated types/enums (and no properties outside it) | pass | `A/schema-check.tsv`: 90 / 90 bodies `ok` (all 2xx JSON bodies + 43 error bodies as Problem with `status` = HTTP code); 8 × 204 bodies empty (0 bytes); a populated dashboard (`A/99-getDashboard-populated--Dashboard.json`: dueToday, overdue, habit, IN_PROGRESS plan with `restSeconds 38495`, 3 source types) also valid; 43 / 43 4xx responses `Content-Type: application/problem+json` |
| C9 | NFR-1: every create/update/delete/filter call < 0.500 s | pass | `A/calls.tsv` column 3 = 1 marks 74 timed calls; max `time_total` 0.066796 s (`createTask`, the first write), none ≥ 0.5 s |

## Unit tests
Not part of this test phase: the runner didn't run unit tests into this attempt folder (no `unit/`, no `unit_result`
in `task.md`). They run in `test-phase-regression`.

## Requirement coverage
Contract level only; the business rules themselves are checked in test-phase-02..07.

| id | covered by |
|---|---|
| NFR-1 (response < 500 ms) | C9 (74 timed calls) |
| FR-01.7 overdue | C3 `listOverdueTasks`, `overdue` true on task due 2026-09-25 |
| BR-4 archived excluded by default | C3 (`archived=false` accepted) – rule itself: test-phase-02 |
| BR-5 complete → DONE | C3 `completeTask` status DONE |
| BR-7 one completion per habit and date | C4 409 on same date |
| BR-10 plan items existing and distinct | C6 400 on empty items – distinct/existing: test-phase-05 |
| BR-11 end after start | C6 400 on end < start and end = start (create and update) |
| BR-12 rest time | C8 `restSeconds` integer on IN_PROGRESS plan, null otherwise |
| BR-13 plan item done | C6 `setPlanItemDone` progress 100 % |
| BR-14 permanent delete | C3–C6 delete 204 then 404 |
| FR-10.2 settings | C7 |

## Questions
None.

## Note for the engine
Runner sessions normally allow `Bash(bash loops/testing/outputs/*:*)` (loopctl.py permissions), and earlier sessions ran
their verify scripts that way. In this session the same command form needed approval, so the checks ran as the
single curl/jq commands above. `test-phase-01-verify.sh` (bash + python3) matches these checks but hasn't been run yet.
