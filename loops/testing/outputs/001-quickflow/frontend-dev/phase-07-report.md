# phase-07-report: US5 See everything on the Dashboard (frontend-dev), attempt 1

Test plan `phase-07-test.md` reused unchanged (criteria AC-US5-1..7 unchanged). An earlier session of this attempt
crashed after C1 and the setup (`C1-state.json`, `api-00-setup.json`, `C2-C6-state.json`, `shot-01-C1.png`, kept as
history); the runner restarted the app on a fresh test database and every check was run again. Evidence of this run
has the `-r2` suffix or screenshot numbers 10–24. All files are in
`loops/testing/runs/001-quickflow/frontend-dev/phase-07/attempt-1/`. Playwright MCP headless on
`http://localhost:4200`; data set up and read back through the dev-server proxy (`/api/...`). Today = 2026-09-26
(Africa/Cairo), yesterday = 2026-09-25.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US5-3 (empty), FR-09.1 | pass | `C1-state-r2.json`: fresh DB, `dash-task-percent` "0%", metrics 0/0, every list shows its empty text, learning 0/0/0 and "0/0", `GET /api/dashboard` all zeros/empty; `shot-10-C1.png` |
| C2 | AC-US5-1 | pass | `api-01-setup-r2.json` (no non-2xx), `C2-C6-state-r2.json`: `dash-due-today` = [1 A, 5 E] = `dueToday`; `dash-overdue` = [2 B] = `overdue`; `dash-completed-today` = [5, 4, 3] = `completedToday`; archived F(6), G(7), H(8) in no list; `shot-11-C2.png` |
| C3 | AC-US5-3 | pass | `C2-C6-state-r2.json`: `GET /api/tasks` 6 non-archived, 3 DONE → "50%" = `taskCompletionPercent` 50, `metric-tasks` "3/6 tasks done"; after the Dashboard tick 4/6 → "66%" (floor, = API) in `C6-after-tick-r2.json`; `shot-21-C3.png` |
| C4 | AC-US5-2 | pass | `C2-C6-state-r2.json`: `dash-habits` has `dash-habit-1`, `dash-habit-2` only (H3 inactive absent), `dash-habit-done-1` only, `dash-habit-count` "1/2", `metric-habits` "1/2 habits today" = `habitCounts` {active 2, completedToday 1}; `shot-22-C4.png` |
| C5 | AC-US5-5 | pass | `C2-C6-state-r2.json`: not-started 1, in-progress 2, completed 1, `dash-milestones` "3/5" = `learning` and the card list (statuses NOT_STARTED/IN_PROGRESS×2/COMPLETED, 3 of 5 milestones done); `shot-23-C5.png` |
| C6 | AC-US5-4, FR-08.5 | pass | `C2-C6-state-r2.json`: `dash-plan-2`, `dash-plan-1` only (P3 not started, P4 past absent), rest "1:59:43"/"0:59:43" = `restSeconds` 7183/3583, 12.8 s later "1:59:31"/"0:59:31"; `metric-plans` "2 in progress · 1 upcoming · 1 completed" = `planCounts`. Tick P1 TASK item on the Dashboard (`C6-after-tick-r2.json`, 1 page load, no reload): P1 "50% · 1/2 items", percent 50%→66%, `metric-tasks` 4/6, task 9 added to `dash-completed-today`, all = API. Tick P2 item on `/plans` then nav back (`C6-after-plans-tick-r2.json`): P2 "50% · 1/2 items". Tick P1's last item (`C6-all-ticked-r2.json`): P1 leaves `dash-plans`, `metric-plans` "1 in progress · 1 upcoming · 2 completed" = API; `shot-12`..`shot-15` |
| C7 | AC-US5-6 | pass | `quick-add-task` → `/tasks?add=1` with `task-form` (`shot-16-C7-task.png`); `quick-add-habit` → `/habits?add=1` with `habit-form` (`shot-17`); `quick-add-learning` → `/learning?add=1` with `card-form` (`shot-18`); `quick-add-plan` → `/plans?add=1` with `plan-builder` (`shot-19`) |
| C8 | AC-US5-7, FR-10.1 | pass | `C8-nav-r2.json`: from each of the 6 pages, each of the 6 `nav-*` links (36 moves) reaches its route with `page-title` = link text ("Dashboard", "Tasks", "Habits", "Learning Resources", "Todo Plans", "Settings") and `nav-main` present; 0 failures; `shot-20-C8.png` |
| C9 | FR-09.1 (greeting) | pass | `C1-state-r2.json`: `dash-greeting` "Hello" (no name before US6, FA-34); `shot-24-C9.png` |

Console: only the Angular dev-mode log lines, no errors (`console-*.log`).

## Unit tests
From `unit/unit-result.json`: layer `frontend`, outcome `none` ("no unit test command configured"). Passed: n/a,
failed: n/a, coverage: n/a (the frontend layer has no `coverage` rule).

## Requirement coverage
| requirement | unit tests | checks |
|---|---|---|
| AC-US5-1 | none (frontend) | C2 |
| AC-US5-2 | none (frontend) | C4 |
| AC-US5-3 | none (frontend) | C1, C3 |
| AC-US5-4 | none (frontend) | C6 |
| AC-US5-5 | none (frontend) | C5 |
| AC-US5-6 | none (frontend) | C7 |
| AC-US5-7 | none (frontend) | C8 |
| FR-09.1 | none (frontend) | C1, C2, C4, C5, C9 (the "Hello, <displayName>" form waits for US6) |
| FR-09.2 | none (frontend) | C1–C6 (every figure compared with `GET /api/dashboard`) |
| FR-08.5 | none (frontend) | C6 |
| FR-10.1 | none (frontend) | C8 |

Frontend unit tests are optional (constitution III); the business rules behind these figures are covered by the
backend phase-07 unit tests.

## Questions
None.
