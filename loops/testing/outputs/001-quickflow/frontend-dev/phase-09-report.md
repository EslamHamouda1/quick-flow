# phase-09-report: Polish (frontend-dev), attempt 1

Plan: `phase-09-test.md`, written in this attempt. The phase has no acceptance criteria of its own, so the
checks come from the approved review's `## Planned checks` (R1–R3). Evidence folder:
`loops/testing/runs/001-quickflow/frontend-dev/phase-09/attempt-1/`.
Playwright MCP, headless chromium, against `http://localhost:4200` (backend on `http://localhost:8080`, profile
`test`, fresh DB). Data set up through the API from the page (`setup.json`).

**Verdict: pass** (9/9).

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | R1 Shell on empty lists | pass | `C1-shell-empty.json`: on `/tasks`, `/habits`, `/learning`, `/plans` `nav-main` + six `nav-*` with contract texts and hrefs, `page-title` H1 = nav label, `empty-state` + `empty-state-action` ("Add Task", "Add Habit", "Add Learning Card", "Create Plan"); `shot-01-C1.png` |
| C2 | R1 Tasks, `error-title`, `notice` | pass | `C2-tasks.json`: every Tasks selector found (search, 7 filter/sort controls with `createdAt`/`dueDate`, `asc`/`desc`, rows with title/status/priority/due, `task-row-overdue` only on the overdue row, row actions, `task-restore-4` with `filter-archived`, overdue view lists only row 1, `task-status` in the edit form only, `?add=1` opens the form); empty save → `error-title` "size must be between 1 and 200"; complete → `notice` "Task completed"; `shot-02..05` |
| C3 | R1 Habits, `notice-error` | pass | `C3-habits.json`: all Habits selectors; card fields Daily/"Done today"/1 and Weekly/"Not done"/0/`habit-inactive`; toggle checked on the done card, disabled on the inactive one; activate/deactivate shown by state; stale toggle → `notice` with `notice-error` "Habit 3 is already complete for 2026-09-26"; `shot-06..08` |
| C4 | R1 Learning | pass | `C4-learning.json`: grid, card fields (`card-status` three labels, `card-milestone-count` "0/1"), expand shows `card-details-1` with milestone and note rows and inputs; form fields; `shot-09`, `shot-10` |
| C5 | R1 Plans | pass | `C5-plans.json`: `plans-active` holds the In Progress plan, `plans-completed` the past one; `plan-status`/`plan-progress`/`plan-window`/`plan-priority-order` on both, `plan-rest-time` only on In Progress ("1:58:21"); items with done checkboxes, `plan-item-removed-5` only on the removed source; builder with `datetime-local` start/end and sources (archived task and inactive habit not offered); empty save → `error-items`; `plan-started-3` "Started" + notice when a plan's start was reached with the page open; `shot-11..13` |
| C6 | R1 Dashboard | pass | `C6-C7-dashboard-settings.json`: every Dashboard selector found; lists and figures equal `GET /api/dashboard` (50%, due today [2], overdue [1], completed today [2,3], habits 2/2 with done marks, plans 2 and 3 with progress and rest time, learning 1/0/0 and 0/1); four quick-adds; `shot-14-C6.png` |
| C7 | R1 Settings | pass | `C6-C7-dashboard-settings.json`: all six Settings selectors; notifications checkbox, default page select with the six pages, time zone read-only text "Africa/Cairo"; `shot-15-C7.png` |
| C8 | R2 smoke run | pass | `C8-smoke.json`: six `nav-*` clicks from `/settings` each land on its route with `page-title` = label; `C8-console-errors.txt` "Errors: 0, Warnings: 0"; `shot-16-C8.png` |
| C9 | R3 quickstart step 3 | pass | `C9-root.txt` `http://localhost:4200/` → 200; `C9-app-info.txt` `/api/app-info` through the dev-server proxy → 200 JSON `{"timeZone":"Africa/Cairo",...}`. Steps 1–2 not run (they write under `frontend/`, outside what this loop may write); the runner's `npm start` compiled the sources |

Notes on evidence:
- Console errors seen during C2–C5 are the browser's "Failed to load resource: 400" for the invalid saves the checks
  made on purpose, plus two 400s from a test-side mistake in the first setup call (`LEARNING_CARD` instead of the
  swagger's `LEARNING_RESOURCE`, see `setup.json`). The smoke run (C8) after a fresh load had none.
- C2's first read of the add/edit form ran before it rendered (test timing); re-read with a wait, all present.

## Unit tests
From `unit/unit-result.json`: outcome **none** ("no unit test command configured"). The frontend layer has no
`test` command and no `coverage`, so the unit rule does not apply.

## Requirement coverage
| id | unit tests | checks |
|---|---|---|
| T043 / ui-contract (all sections) | none (FA-1) | C1–C7 (pass) |
| AC-US5-7 / FR-10.1 (navigation, regression) | none | C1, C8 (pass) |
| NFR-5 empty states | none | C1 (pass) |
| T044 / Constitution IV (no rules in components) | none | not observable in the UI; C6 shows the dashboard figures equal the API values; no check reads code (the testing loop checks behaviour only) |
| T045 / quickstart | none | C9 (step 3 pass; steps 1–2 not run) |

No FR or business-rule ids belong to this phase.

## Questions
None.
