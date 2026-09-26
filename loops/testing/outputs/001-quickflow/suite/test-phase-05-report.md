# test-phase-05 report: US4 Build and follow Todo Plans (attempt 1)

Run 2026-09-26 12:58–13:14 (Africa/Cairo) against a fresh test database. Evidence folder:
`loops/testing/runs/001-quickflow/suite/test-phase-05/attempt-1/`: `curl.log` (API checks, raw responses with an
`== <check> ... HTTP <code>` line after each), `ui-results.json` (Playwright MCP return values),
`shot-01..15-*.png` (screenshots), `page-*.yml` / `console-*.log` (Playwright MCP).

API checks ran as direct curl calls (`curl --next`, appended to `curl.log`). UI checks ran through Playwright MCP,
headless, with the `data-testid` selectors from `specs/001-quickflow-frontend/contracts/ui-contract.md`; UI
read-backs used `fetch()` from the page, through the dev-server proxy. The API test plans (1–4) were deleted before
the UI checks, so `/plans` started empty. Tasks 3/4, habit 2 and card 2 were deleted during the API checks, so the
builder listed only the remaining sources.

**Result: 25/25 pass, 0 fail, 0 unclear. No bugs filed.**

| check | criterion | result | evidence |
|---|---|---|---|
| A1 | AC-US4-1 / FR-07.1 / FR-07.2 | pass | curl.log `== A1`: plan 1 from TASK 1, HABIT 1, LEARNING_RESOURCE 1 with title, 120 min, start/end, priority 2 → 201; three items `done: false` with `sourceTitle`; `createdAt` set; GET returns the same |
| A2 | AC-US4-2 / BR-10 | pass | curl.log `== A2`: `items: []` and missing items → 400 field `items` "must not be empty"; TASK 999, HABIT 999 → 400 `items[0].sourceId` "must reference an existing …"; valid task + card 999 → 400 `items[1].sourceId`; task 4 deleted then used → 400; plan list afterwards holds only plan 1 |
| A3 | AC-US4-3 / BR-11 / FR-07.4 | pass | curl.log `== A3`: create end = start and end < start, PUT plan 1 end = start and end < start → 400 field `endDateTime` "must be after startDateTime"; plan 1 unchanged in the list |
| A4 | AC-US4-4 / FR-08.3 / FR-08.5 | pass | curl.log `== A4`: plan 2 ticks → doneItems/progress 1/25, 2/50, 3/75; un-tick → 2/50; plan 1 one of three → 33; dashboard `activePlans` plan 2 progress 75 then 50 on the next read |
| A5 | AC-US4-5 / BR-13 / FR-07.5 | pass | curl.log `== A5`: TASK item 4 done → task 2 `DONE`, `completedAt` 12:59:41; un-tick → task 2 still `DONE` with the same `completedAt`; HABIT item → habit 2 `completedToday: false`, completions `[]`; LEARNING item → card 2 still `NOT_STARTED` |
| A6 | AC-US4-6 / BR-12 | pass | curl.log `== A1`, `== A6`: plan 1 (start tomorrow) `NOT_STARTED`, `restSeconds: null`, also after one item is done |
| A7 | AC-US4-7 / FR-08.2 / FR-08.4 | pass | curl.log `== A7`: plan 2 (12:00–18:00) `IN_PROGRESS`, restSeconds 18033 at 12:59:26.9 (= 18:00 − now) and 18018 at 12:59:41 (down by the elapsed 15 s); all 4 items done → `COMPLETED`; un-tick item 7 → `IN_PROGRESS`; plan 3 (end yesterday) → `COMPLETED`, restSeconds null |
| A8 | AC-US4-8 / NFR-4 | pass | curl.log `== A8`: two consecutive reads of plan 2 (`IN_PROGRESS`, 3/4, 75, 17994) and of plan 3 (`COMPLETED`, 0/2, 0, null) are identical |
| A9 | AC-US4-10 | pass | curl.log `== A9`: `group=all` → [2 (prio 1), 1 (prio 2), 4 (prio 3), 3 (COMPLETED)]; `group=active` → [2, 1, 4] by priority order, NOT_STARTED and IN_PROGRESS together |
| A10 | AC-US4-11 | pass | curl.log `== A10`: `group=completed` → [plan 3] with `progressPercent` 50 after one of its two items was done |
| A11 | FR-07.6 / BR-14 | pass | curl.log `== A11`: after deleting task 3, habit 2, card 2, plan 2 keeps items 5, 6, 7 with `sourceRemoved: true`, their done flags kept, totalItems 4, doneItems 3, progress 75; GET task 3 / habit 2 / card 2 → 404; not in the task, habit or card lists |
| A12 | AC-US4-12 / BR-14 | pass | curl.log `== A12`: DELETE plan 4 → 204; GET → 404; second DELETE → 404; not in the list; card 1 (its only source) identical before and after; dashboard planCounts inProgress 2 → 1 |
| U1 | AC-US4-13 / NFR-5 | pass | shot-01 `empty-state` "No plans yet" + `empty-state-action` "Create Plan", no builder; shot-02 the action opens `plan-builder` (title, duration, start/end `datetime-local`, priority, source checkboxes); shot-15 `/plans?add=1` opens it |
| U2 | AC-US4-1 | pass | shot-05 `plan-5` "UI future plan" in `plans-active`: `plan-status` Not Started, `plan-progress` "0% · 0/3 items", items task 5 / habit 3 / card 3, window Sep 27 9:00 AM – 11:00 AM, Priority 2; API plan 5 NOT_STARTED with the entered fields and three not-done items |
| U3 | AC-US4-2 | pass | shot-03 no item selected → `error-items` "must not be empty", builder stays open; API plan count 0 |
| U4 | AC-US4-3 | pass | end = start and (shot-04) end before start → `error-endDateTime` "must be after startDateTime", builder stays open; API count 0 both times |
| U5 | AC-US4-4 / AC-US4-5 | pass | shot-06 `plan-item-done-11/12/13` → `plan-progress` 33%, 66%, 100% right after each click (API 33 / 100); task 5 `DONE` with `completedAt`; habit 3 not completed today, no completions; card 3 still NOT_STARTED; plan stays Not Started before its start (FR-08.4) |
| U6 | AC-US4-6 / AC-US4-7 / NFR-2 | pass | shot-07 plan 6 (12:00–20:00) In Progress, `plan-rest-time` 6:48:27 (expected 24507 s = 6:48:27), 5 s later 6:48:22; Not Started plan 5 has no `plan-rest-time` |
| U7 | AC-US4-9 / FR-08.1 | pass | plan 7 created in the UI at 13:11:48 with start 13:13:00; page left open (a `window` marker survived, no reload); `plan-started-7` "Started" and `notice` "Plan “UI starts soon” has started" appeared at 13:13:00.75 (shot-08); 8 s later the card showed In Progress, rest 0:59:51 (API 3590 s) |
| U8 | AC-US4-8 | pass | after reload every plan's status and progress equal the API; rest times 6:46:32 / 0:59:32 vs API 6:46:31 / 0:59:31 (read 1 s apart, within NFR-2's one minute); shot-09 |
| U9 | AC-US4-10 / AC-US4-11 | pass | shot-09 `plans-active` [plan-6, plan-5, plan-7] = API active order (priority 1, 2, 4); `plans-completed` [plan-9, plan-8] = API order, `plan-progress` "0% · 0/1 items" and "50% · 1/2 items" = API 0 / 50 |
| U10 | FR-07.6 | pass | task 6 deleted through the API (curl.log `== U10`: item 15 `sourceRemoved: true`); after reload `plan-item-removed-15` "Removed" on item 15 only, still counted (0/2); task 6 no longer offered in the builder; shot-10 |
| U11 | FR-07.3 | pass | shot-11 `plan-edit-5` opens the builder prefilled (title, 60, 2026-09-27T09:00, 11:00, 2); shot-12 save with new title, 90 min, end 12:30, priority 3 → API plan 5 has the new values, items and done flags unchanged; card shows the new window, Priority 3, 90 min; active order stays by priority [6, 5, 7] |
| U12 | AC-US4-12 | pass | shot-13 `plan-delete-5` (no confirm dialog) → card gone; API GET plan 5 → 404, list [6, 7, 9, 8]; task 5, habit 3, card 3 JSON identical before and after |
| U13 | Navigation | pass | shot-14 `nav-plans` ("Todo Plans") from /dashboard → `/plans`, `page-title` is an H1 reading "Todo Plans" |

Notes (recorded, not filed):
- `plan-progress` reads "<n>% · <done>/<total> items"; the contract gives "<n>%", which the text starts with.
- Two of three items done shows 66% (API `progressPercent` 66 too). FR-08.3 asks for a whole percentage but doesn't say
  how to round, so only unambiguous values (0, 25, 33, 50, 75, 100) were asserted.
- A plan completed by ticking all items before its end still carries `restSeconds` in the API (swagger: "while
  start <= now < end"). The UI contract shows `plan-rest-time` only while In Progress; no check in this phase covers
  that case on the page, so it wasn't asserted.
- U7: at the moment the notice appeared the card still read Not Started; it showed In Progress with a rest time
  within 8 s, without reload.

Console: 3 × 400 on POST /api/plans (U3 no items, U4 end = start and end before start, expected), 1 × 404 on
/api/plans/5 (U12 read-back after delete, expected); no other errors.

## Unit tests
Not part of this test phase: the runner didn't run unit tests into this attempt folder (no `unit/` folder, and
`task.md` has no `unit_result`). They run in `test-phase-regression`.

## Requirement coverage
| id | covered by |
|---|---|
| FR-07.1 plan fields (title, items, duration, start, end, priority, status, createdAt) | A1, U2, U11 |
| FR-07.2 items reference an existing task/habit/card, own done flag default false | A1, A2, U2 |
| FR-07.3 create by selecting sources, edit fields, tick items, remove | A1, A4, A12, U2, U5, U11, U12 |
| FR-07.4 end after start | A3, U4 |
| FR-07.5 only TASK items propagate, one way | A5, U5 |
| FR-07.6 deleted source kept as removed, still counted | A11, U10 |
| FR-08.1 in-app notice at start | U7 |
| FR-08.2 rest time between start and end, updating | A6, A7, U6, U7 |
| FR-08.3 whole percentage done/total | A4, A10, U5, U9 |
| FR-08.4 status computed on read | A6, A7, A8, U5, U8 |
| FR-08.5 progress reflected at once on the plan and the dashboard | A4, U5 |
| BR-10 | A2, U3 |
| BR-11 | A3, U4 |
| BR-12 | A6, A7, U6 |
| BR-13 | A5, U5 |
| BR-14 | A11, A12, U12 |
| NFR-2 rest time updates | A7, U6, U7 |
| NFR-4 status consistent after reopen | A8, U8 |
| NFR-5 empty state / add and remove on the page | U1, U12 |

All ids of this phase are covered. FR-07.2's "an entity appears at most once in the same plan" has no check because
the plan lists none; the dashboard's own plan widgets are checked in test-phase-06.

## Questions
None.
