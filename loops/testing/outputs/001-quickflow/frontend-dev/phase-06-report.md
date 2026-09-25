# phase-06-report: US4 Build and follow Todo Plans (frontend-dev), attempt 1

**Verdict: pass.** First attempt. I wrote test plan `phase-06-test.md` in this session (C1–C13).

Evidence folder: `loops/testing/runs/001-quickflow/frontend-dev/phase-06/attempt-1/`. Playwright MCP ran headless
against `http://localhost:4200/plans`, starting on a fresh test DB. I made the setup calls and API read-backs with
`fetch` from the page, through the dev-server proxy (`/api/...`), and saved them as `api-*.json` / `C*-*.json`. I did
not use shell curl because the session's permission check blocked chained curl commands.

Scope: the "and on the Dashboard" part of AC-US4-4 is not checked here. The approved phase plan (`phase-00-review.md`)
builds the Dashboard in frontend phase-07 (AC-US5-4, FR-08.5, T038), and the Dashboard is still a placeholder in this
phase. The Todo Plans half is checked in C6.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US4-13 | pass | fresh DB: `empty-state` "No plans yet" with `empty-state-action` "Create Plan" (shot-01). Clicking it opens `plan-builder` (shot-02). `/plans?add=1` opens `plan-builder` (shot-03) |
| C2 | AC-US4-1, AC-US4-6 | pass | setup `api-00-setup.json`: task 1, archived task 2, active habit 1, inactive habit 2, card 1. The builder offers only `plan-source-task-1`, `plan-source-habit-1`, `plan-source-learning-1`. With all three ticked, "Future plan" (start 27 Sep 10:00, end 12:00, priority 1) is saved as `plan-1` in `plans-active`. The card shows "Not Started" and "0% · 0/3 items", has no `plan-rest-time` and has 3 unchecked toggles. API: 3 items (TASK/HABIT/LEARNING_RESOURCE) all `done: false`, `createdAt` 2026-09-26T00:13:03+03:00, `NOT_STARTED`, `restSeconds` null (`C2-state.json`, shot-05) |
| C3 | AC-US4-2 | pass | save with nothing ticked: API 400, form stays open with its values, `error-items` "must not be empty", API list `[]` (shot-04). Task 3 was deleted through the API after the builder listed it, then ticked: `error-items` "must reference an existing TASK", API list still `[1]` (shot-06) |
| C4 | AC-US4-3 | pass | end = start (10:00/10:00), then end before start (10:00/09:00): both rejected. `error-endDateTime` "must be after startDateTime" sits inside the End field ("End / must be after startDateTime"), and the API list stays `[1]` (shot-07) |
| C5 | AC-US4-7 | pass | plan 2 via API (start −10 min, end +2 h, `api-05-C5-create.json`): "In Progress", `plan-rest-time` "1:59:51" at 21:14:16Z, then "1:59:47" at 21:14:20Z. At that moment end − now = 7187 s = 1:59:47, and the API `restSeconds` was 7186 (`C5-state.json`, shot-08) |
| C6 | AC-US4-4, AC-US4-5, FR-08.4 | pass | ticked the task item: "33% · 1/3", API item 4 done, task 1 `DONE` with `completedAt` (`C6-step1.json`, shot-09). Ticked the habit item: "66% · 2/3", API 66 (floor, as `specs/001-quickflow-backend/research.md:50` states: `floor(100 * done / total)`). The habit still has `completedToday` false and no completions (`C6-step2.json`). Un-ticked the task item: "33%", and the task stays `DONE` (`C6-step3.json`, shot-10). Ticked every item: the plan moves to `plans-completed` as "Completed", "100% · 3/3", with no rest time. The card is still `NOT_STARTED` and the habit is unchanged (`C6-step4.json`, shot-11) |
| C7 | AC-US4-8 | pass | after a reload, every card's status and percent match `GET /api/plans`: plans 4/1 Not Started 0%, 3/5 In Progress 0%, 7 In Progress 50%, 2 Completed 100%, 6 Completed 33% (`C7-C9-C10-C13-state.json`, shot-12) |
| C8 | AC-US4-9 | pass | "Soon plan" was created in the builder with start 00:18 and the page stayed open without a reload (`C8-before.json`: Not Started, no rest, no badge, shot-13). At 21:18:00.332Z the `notice` "Plan “Soon plan” has started" appeared. 1.5 s later the card showed `plan-started-8` "Started", "In Progress" and `plan-rest-time` "0:59:58" (`C8-after.json`, shot-14). After a reload the notice is not shown again (`C8-reload.json`) |
| C9 | AC-US4-10 | pass | groups `plans-active` "Active & upcoming" and `plans-completed` "History". Active shows priorities 1, 1, 2, 3, 5 (ids 4, 1, 3, 5, 7), the same as the API `group=active`. After the edit in C12 the order is 4, 3, 5, 8, 7, 1 (priorities 1, 2, 3, 4, 5, 6). Every card has `plan-progress`, item rows with checkboxes and `plan-delete-<id>`. `plan-rest-time` appears only on the In Progress cards (`C7-C9-C10-C13-state.json`, `C12-after.json`, shot-12) |
| C10 | AC-US4-11 | pass | "Past plan" (start −5 h, end −3 h, 1 of 3 items done via API) is in `plans-completed` as "Completed", "33% · 1/3 items", with no rest time (`C7-C9-C10-C13-state.json`, shot-12) |
| C11 | AC-US4-12 | pass | `plan-delete-3` (no confirm): the card is gone. `GET /api/plans/3` returns 404 "Plan 3 not found", and 3 is missing from the list. Task 1, habit 1 (with its completions) and card 1 are byte-identical before and after the delete (`C11-sources-before.json` vs `C11-after.json`, shot-16) |
| C12 | FR-07.3 (edit) | pass | `plan-edit-1` opens the builder prefilled: "Future plan", 90, 2026-09-27T10:00 / 12:00 in the app zone, priority 1, and no source checkboxes (`C12-prefill.json`). The title was changed to "Future plan (edited)" and the priority to 6. The card shows the new values, "Priority 6" and the same window. API: the same 3 items, unchanged (`C12-after.json`, shot-15) |
| C13 | FR-07.6 | pass | plan 7 has items "Doomed task" (done) and the habit, and task 4 was then deleted. The card shows `plan-item-19` "Task Doomed task" with the `plan-item-removed-19` "Removed" badge, still checked, and the progress is still "50% · 1/2 items" (`C7-C9-C10-C13-state.json`, shot-12) |

Console errors (`console-errors-all.log`) are all expected: four 400s from `POST /api/plans` (C3 ×2, C4 ×2) and the
404 from my own `GET /api/plans/3` read-back in C11. The app logged no other errors.

Note (not a failure): after every item is done, the API still returns `restSeconds` for a plan that is inside its
window but `COMPLETED` (plan 2: 7125). The UI hides rest time there, as the contract says ("only while In Progress").

## Unit tests
From `unit/unit-result.json`: layer `frontend`, outcome `none` ("no unit test command configured"). Passed: n/a,
failed: n/a, coverage: n/a (no `coverage` configured for this layer).

## Requirement coverage
| requirement | unit tests | checks |
|---|---|---|
| AC-US4-1 / FR-07.1, FR-07.2, FR-07.3 | none (layer has none) | C2 |
| AC-US4-2 / BR-10 | none | C3 |
| AC-US4-3 / BR-11, FR-07.4 | none | C4 |
| AC-US4-4 / FR-08.3, FR-08.5 | none | C6 (Todo Plans; Dashboard part → phase-07) |
| AC-US4-5 / BR-13, FR-07.5 | none | C6 |
| AC-US4-6 / BR-12 | none | C2, C8 (before) |
| AC-US4-7 / FR-08.2 | none | C5, C8 |
| AC-US4-8 / NFR-4, FR-08.4 | none | C6, C7 |
| AC-US4-9 / FR-08.1 | none | C8 |
| AC-US4-10 | none | C9 |
| AC-US4-11 | none | C10 |
| AC-US4-12 / BR-14 | none | C11 |
| AC-US4-13 | none | C1 |
| FR-07.3 (edit) | none | C12 |
| FR-07.6 | none | C13 |

At least one check covers every requirement of this phase. Unit tests cover none of them, because the layer has none.
FR-08.5's Dashboard summary is flagged for phase-07.

## Questions
None. No check is `unclear`.
