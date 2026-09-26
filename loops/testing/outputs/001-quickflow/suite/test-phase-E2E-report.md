# test-phase-E2E report: End-to-end flow (PRD §11) (attempt 1)

Run 2026-09-26 14:01–14:48 (Africa/Cairo) against a fresh test database, in one headless Playwright MCP browser
session. Every step went through the UI using the `data-testid` selectors from
`specs/001-quickflow-frontend/contracts/ui-contract.md`, and each one was read back from the API with `fetch()` from the
page, through the dev-server proxy. Evidence folder: `loops/testing/runs/001-quickflow/suite/test-phase-E2E/attempt-1/`,
which holds `ui-results.json` (Playwright MCP return values with timestamps), `shot-01..12-*.png` (screenshots),
`console-errors.log` and the `page-*.yml` / `console-*.log` files from Playwright MCP.

The flow followed PRD §11 in order: create task, complete task, create habit, complete habit, add a learning card
with milestones, build a plan from existing items, mark the plan items done, verify the plan achievement and the
dashboard update.

**Result: 10/10 pass, 0 fail, 0 unclear. No bugs filed.** E7 needed a re-check under clean browser storage (see below).

| check | criterion | result | evidence |
|---|---|---|---|
| E1 | create task (AC-US1-1) | pass | shot-01: `task-row-1` "E2E task A" and `task-row-2` "E2E task B", both Todo; API tasks 1 and 2 are `TODO` with `completedAt` null (ui-results `E1`) |
| E2 | complete task (AC-US1, FR-01) | pass | shot-02: after `task-complete-1`, `task-row-status` reads "Done"; API task 1 is `DONE` with `completedAt` 14:01:58 (`E2`) |
| E3 | create habit (AC-US2-1) | pass | shot-03: `habit-card-1` "E2E daily habit", `habit-frequency-label` Daily, no `habit-inactive` badge; API `DAILY`, `active: true` (`E3`) |
| E4 | complete habit (AC-US2) | pass | shot-04: after `habit-toggle-1` the toggle is checked, `habit-period-done` reads "Done today" and the streak is 1; API `completedToday: true` with exactly one completion, dated 2026-09-26 (`E4`) |
| E5 | learning card with milestones (AC-US3) | pass | shot-05: `card-1` "E2E learning card", Not Started; after two milestones were added, `card-milestone-count` went from "0/0" to "0/2"; API has 2 not-done milestones under card 1 (`E5`) |
| E6 | build plan from existing items (AC-US4-1) | pass | shot-06: in the builder, task B (`plan-source-task-2`), habit 1 and card 1 were selected, start 14:04 (about 80 s ahead), end 15:04, priority 1. `plan-1` is in `plans-active`, Not Started, "0% · 0/3 items", with no `plan-rest-time`; API `NOT_STARTED`, `restSeconds` null, three not-done items (`E6`) |
| E7 | live start: notice, In Progress, rest time counting down (AC-US4-6/7/9, FR-08.1/2, NFR-2) | pass | Plan 1 on the open page (no reload, marker kept): In Progress at 14:05:26 with rest 0:58:34 (API 3513). Reads 5 s apart gave 0:58:15 then 0:58:10 (API 3494 / 3489) (shot-08). The notice and badge did **not** show for plan 1, because stale browser storage from earlier runs already listed its id (shot-07, see note). Re-check with that storage cleared: plan 3 started at 14:42:00, and by 14:42:02.8 `plan-started-3` "Started" and the `notice` "Plan “E2E notice recheck 2” has started" had appeared with no reload, status In Progress (shot-09, `E7_recheck_plan3`) |
| E8 | mark plan items done (AC-US4-4/5, BR-13) | pass | shot-10: ticking items 1, 2 and 3 moved `plan-progress` to "33%", "66%" and "100%" after each click (API 33 / 66 / 100). The TASK item set task B to `DONE` with `completedAt` 14:47:36 (BR-13). The habit is unchanged (still one completion, streak 1) and so is the card (NOT_STARTED, 0/2) (`E8`) |
| E9 | verify plan achievement (AC-US4-7/11) | pass | shot-11: `plan-1` is in `plans-completed` and not in `plans-active`, status Completed, "100% · 3/3 items", no `plan-rest-time`; API `COMPLETED` with 3/3 items and 100 % (`E9`) |
| E10 | dashboard updates accordingly (AC-US5, FR-09.2) | pass | shot-12: `dash-completed-today` = [task 2, task 1], `dash-task-percent` "100%", `dash-due-today` / `dash-overdue` empty, `dash-habit-count` "1/1" with `dash-habit-done-1`, `dash-plans` empty, learning not-started 1 / in-progress 0 / completed 0, `dash-milestones` "0/2". All of these equal `GET /api/dashboard` read at the same moment (`completedToday` [2, 1], `taskCompletionPercent` 100, habitCounts 1/1, `activePlans` [], planCounts 0/0/1, learning 1/0/0 with 0/2) (`E10`) |

Notes (recorded, not filed):
- **E7 on plan 1, stale browser storage.** `localStorage['quickflow.notifiedPlanIds']` was `[1,2,3]` before this
  phase started. Those ids belong to earlier test phases' databases: the Playwright MCP browser profile keeps its
  storage between runs, while the runner deletes the test database, so plan ids start at 1 again. The frontend design
  says each plan id is notified only once, tracked in that key (frontend `research.md` R-6, `data-model.md` FA-2,
  `tasks.md` T035). Nothing in the requirements says what should happen when the database is reset under an existing
  browser, so this isn't a requirement failure. With the key removed, the behaviour matches AC-US4-9: plan 1 (already
  In Progress) was notified on app open, and plan 3 was notified live at its start. **Harness suggestion:** clear the
  Playwright MCP profile's storage whenever the test database is deleted. Otherwise any phase that checks a live plan
  start on low plan ids can miss the notice.
- The first re-check (plan 2) was discarded. The console shows `[vite] server connection lost. Polling for restart...`
  at about 14:07:42 and then a page reload, and the next timestamp was 14:36:34. That 28-minute clock gap suggests the
  host was suspended. Both servers answered afterwards (backend 200, frontend 200). Plan 2 was deleted (GET 404)
  before the clean re-check with plan 3, which was also deleted afterwards (GET 404). Card 1 was unchanged each time
  (BR-14).
- Plan 1 was completed before its end time, so the API still returns `restSeconds` 981. This matches the swagger
  ("while start <= now < end"), and the UI hides `plan-rest-time` for Completed plans. The same was noted in
  test-phase-05.
- `metric-learning` reads "0 in progress · 0 completed" and leaves out the Not Started count, which is shown in
  `dash-learning-not-started`. The same was noted in test-phase-06.

Console: 2 × 404, on `/api/plans/2` and `/api/plans/3`. These were my own read-backs after deleting the re-check
plans, so they're expected. There are no other errors.

## Unit tests
Not part of this test phase. The runner didn't run unit tests into this attempt folder (there is no `unit/` folder,
and `task.md` has no `unit_result`). They run in `test-phase-regression`.

## Requirement coverage
| id | covered by |
|---|---|
| PRD §11 create task (FR-01, AC-US1-1) | E1 |
| PRD §11 complete task (FR-01) | E2, E10 |
| PRD §11 create habit (AC-US2-1) | E3 |
| PRD §11 complete habit (BR-7, AC-US2) | E4, E10 |
| PRD §11 learning card with milestones (AC-US3) | E5, E10 |
| PRD §11 build plan from existing items (FR-07.1/07.2/07.3, AC-US4-1) | E6 |
| FR-08.1 / AC-US4-9 in-app notice at start | E7 (re-check with plan 3) |
| FR-08.2 / NFR-2 rest time counting down | E7 |
| FR-08.4 status computed (Not Started → In Progress → Completed) | E6, E7, E8, E9 |
| PRD §11 mark plan items done (FR-08.3, AC-US4-4) | E8 |
| BR-13 / FR-07.5 only TASK items propagate | E8 |
| PRD §11 verify plan achievement (AC-US4-7, AC-US4-11) | E9 |
| PRD §11 dashboard updates accordingly (FR-09.1, FR-09.2, AC-US5-3) | E10 |

Every step of the PRD §11 end-to-end flow is covered.

## Questions
None.
