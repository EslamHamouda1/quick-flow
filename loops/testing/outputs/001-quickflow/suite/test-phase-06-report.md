# test-phase-06 report: US5 See everything on the Dashboard (attempt 1)

Run 2026-09-26 13:17–13:21 (Africa/Cairo) against a fresh test database. Evidence folder:
`loops/testing/runs/001-quickflow/suite/test-phase-06/attempt-1/`: `curl.log` (API checks, raw responses with an
`== <check> ... HTTP <code>` line after each), `ui-results.json` (Playwright MCP return values),
`shot-01..08-*.png` (screenshots), `page-*.yml` / `console-*.log` (Playwright MCP).

API checks ran as direct curl calls (`curl --next`, appended to `curl.log`). UI checks ran through Playwright MCP,
headless, with the `data-testid` selectors from `specs/001-quickflow-frontend/contracts/ui-contract.md`; UI
read-backs used `fetch('/api/dashboard')` from the page, through the dev-server proxy, at the same moment as the DOM read.

Seed data (curl.log `== seed …`): tasks T1/T2 due today (TODO / IN_PROGRESS), T3/T4 overdue, T5/T6 archived (due today /
overdue), T7/T8 completed today (T8 past due), T9 completed today then archived, T10 due today then deleted, T11/T12 due
tomorrow; habits H1 daily done today, H2 weekly, H3 daily done yesterday, H4 done today then deactivated; cards C1
Not Started (M1 done, M2 open), C2 In Progress (M3 done), C3 Completed (M4 open), C4 Not Started; plans P1 12:00–20:00
priority 2 and P2 13:00–18:00 priority 1 (both In Progress), P3 tomorrow, P4 yesterday.

**Result: 13/13 pass, 0 fail, 0 unclear. No bugs filed.**

| check | criterion | result | evidence |
|---|---|---|---|
| A1 | FR-09.1 / AC-US5-3 | pass | curl.log `== A1 empty dashboard`: all 11 fields present, every list `[]`, `taskCompletionPercent` 0, all counts 0; settings `displayName` null |
| A2 | AC-US5-1 | pass | curl.log `== A2-A6 dashboard read 1`: `dueToday` [1, 2] (T5 archived, T10 deleted, T11/T12 tomorrow excluded); `overdue` [4, 3] (T6 archived, T8 Done excluded); `completedToday` [8, 7] (T9 archived excluded); matches `GET /api/tasks` (`== tasks list`) and the archived list [9, 6, 5] |
| A3 | AC-US5-3 | pass | read 1: `taskCounts` 8/2, 25 % = 2/8 (non-archived T1–T4, T7, T8, T11, T12); `== A3 dashboard after completing T3,T4`: 4/8 = 50 %, `overdue` [] and `completedToday` [4, 3, 8, 7] |
| A4 | AC-US5-2 | pass | read 1: `habits` [1, 2, 3] (inactive H4 left out although done today), completedToday flags [true, false, false], `habitCounts` 3/1; `== A4 … H2 complete`: H2 true, 3/2; `== A4 … H1 undo`: H1 false, 3/1 |
| A5 | AC-US5-5 | pass | read 1: `learning` notStarted 2, inProgress 1, completed 1, milestones 2/4 (= card list); `== A5 dashboard after C1 delete`: 1/1/1, 1/2, same as the card list `== A5 cards list` |
| A6 | AC-US5-4 / FR-08.5 | pass | read 1: `activePlans` [P2 (priority 1), P1 (priority 2)] only (P3 Not Started, P4 Completed in `planCounts` 1/2/1), 0 %, restSeconds 16903 / 24103 = 18:00 / 20:00 − 13:18:16.8; after ticking P1 item 2 the next read shows P1 33 % (1/3), after P2 item 5 P2 50 % (1/2); restSeconds 16870 / 24070 at 13:18:49.5 (down by the elapsed 33 s); habits unchanged by the HABIT ticks (BR-13) |
| U1 | FR-09.1 | pass | shot-01; ui-results `U1-U4`: `dash-greeting` "Hello" (displayName null), `metric-tasks` "4/8 tasks done", `metric-habits` "1/3 habits today", `metric-plans` "2 in progress · 1 upcoming · 1 completed", `metric-learning` "1 in progress · 1 completed", `dash-task-percent` "50%", all = API read at the same moment |
| U2 | AC-US5-1 | pass | shot-01: `dash-due-today` rows [dash-task-1, dash-task-2], `dash-overdue` no rows ("No overdue tasks"), `dash-completed-today` [4, 3, 8, 7] = API lists in API order |
| U3 | AC-US5-2 | pass | shot-01: `dash-habits` [dash-habit-1, 2, 3] (active only), `dash-habit-done-2` only (= API completedToday), `dash-habit-count` "1/3" |
| U4 | AC-US5-5 | pass | shot-01: `dash-learning-not-started` 1, `-in-progress` 1, `-completed` 1, `dash-milestones` "1/2" = API `learning` |
| U5 | AC-US5-4 / FR-08.5 | pass | shot-01: `dash-plan-2` "50% · 1/2 items" rest 4:40:48, `dash-plan-1` "33% · 1/3 items" rest 6:40:48 (API 16847 / 24047 s); rest read 4:40:05 → 4:40:00 and 6:40:05 → 6:40:00 over 5 s (API 16799 / 23999 at the second read); tick `plan-item-done-3` on `/plans` (shot-02, 66 % · 2/3) → next Dashboard view `dash-plan-1` "66% · 2/3 items" = API 66 (shot-03) |
| U6 | AC-US5-6 | pass | shots 04–07: `quick-add-task` "Add Task" → `/tasks?add=1` with `task-form` visible; `quick-add-habit` → `habit-form`; `quick-add-learning` → `card-form`; `quick-add-plan` "Create Plan" → `plan-builder` |
| U7 | AC-US5-7 / FR-10.1 | pass | ui-results `U7`: 36 moves (each of the 6 routes × each `nav-*`), link text = label, route reached and `page-title` an H1 equal to the label, 0 bad; shot-08 last page (/settings) |

Notes (recorded, not filed):
- `metric-learning` reads "1 in progress · 1 completed" and leaves out the Not Started count. The contract only says the
  card shows `learning`, and FR-09.1 lists "summary metric cards" without their content; the Not Started count is shown
  in `dash-learning-not-started` (1).
- `plan-progress` reads "<n>% · <done>/<total> items"; the contract gives "<n>%", which the text starts with.
- 2/3 shows 66 % in the API and on the page. FR-08.3 asks for a whole percentage without a rounding rule, so only
  UI = API was asserted for it; the other values checked are exact (0, 25, 33, 50).
- Whether a Done task due today stays in `dueToday` isn't stated, so the seed data has no Done task due today.
- The Dashboard shows the Started badge (`plan-started-1`) on P1, which started at 12:00, before the page was opened at 13:18.
  The badge isn't in the Dashboard contract and no US5 criterion covers it. It is probably the stale
  `quickflow.notifiedPlanIds` in localStorage seen in the phase-08 run (plan ids reused across test databases). Not asserted.
- U5: `locator.check()` on `plan-item-done-3` reported "did not change its state" (the list re-renders from the response,
  as in test-phase-04), but the checkbox then reads checked and the API saved it (items 2 and 3 done, 66 %).

Console: no errors or warnings.

## Unit tests
Not part of this test phase: the runner didn't run unit tests into this attempt folder (no `unit/` folder, and
`task.md` has no `unit_result`). They run in `test-phase-regression`.

## Requirement coverage
| id | covered by |
|---|---|
| FR-09.1 greeting, metric cards, due today, overdue, completed today, habits, percent, in-progress plans, learning snapshot, quick-adds | A1, U1, U2, U3, U4, U5, U6 |
| FR-09.2 figures computed from current data on read | A2–A6 (reads after each change), U5 (tick on /plans shows on next view) |
| FR-08.5 item ticks reflected on the Dashboard at once | A6, U5 |
| FR-10.1 persistent navigation to six pages | U7 |
| AC-US5-1 | A2, U2 |
| AC-US5-2 | A4, U3 |
| AC-US5-3 | A1, A3, U1 |
| AC-US5-4 | A6, U5 |
| AC-US5-5 | A5, U4 |
| AC-US5-6 | U6 |
| AC-US5-7 | U7 |

All ids of this phase are covered.

## Questions
None.
