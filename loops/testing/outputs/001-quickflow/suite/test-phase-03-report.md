# test-phase-03 report: US2 Track recurring habits (attempt 1)

Run 2026-09-26 (Africa/Cairo; today = Saturday 2026-09-26, current week Mon 09-21 – Sun 09-27) against a fresh
test database. Evidence folder: `loops/testing/runs/001-quickflow/suite/test-phase-03/attempt-1/`: `curl.log`
(API checks, raw responses and status codes, one `==` header per check), `name151.json` / `name150.json`
(A2 bodies), `ui-results.json` (Playwright MCP return values), `shot-01..16-*.png` (screenshots),
`page-*.yml` / `console-*.log` (Playwright MCP).

API checks ran as direct curl calls (`curl --next`, appended to `curl.log`). The first A2 try used hand-typed
bodies that measured 147/143 characters (a test-data error, not an app result; logged in `curl.log`). Those two
habits (ids 3, 4) were deleted and A2 was re-run with jq-generated 151/150-character bodies. UI checks ran through
Playwright MCP, headless, with the `data-testid` selectors from `specs/001-quickflow-frontend/contracts/ui-contract.md`.
UI setup and read-backs used `fetch()` from the page, through the dev-server proxy. The API test habits (2, 5, 6)
were deleted before the UI checks so `/habits` started empty.

**Result: 18/18 pass, 0 fail, 0 unclear. No bugs filed.**

| check | criterion | result | evidence |
|---|---|---|---|
| A1 | AC-US2-1 / FR-03.2 | pass | curl.log `== A1`: DAILY (id 1, with description) and WEEKLY (id 2, no description) → 201, `active: true`, `createdAt` set |
| A2 | AC-US2-2 / BR-6 / FR-03.3 | pass | curl.log `== A2` + re-run: empty and missing name → 400 `errors[].field=name`; 151 chars → 400 field `name` "size must be between 1 and 150"; 150 → 201 (id 5); MONTHLY and missing frequency → 400 field `frequency`; list = 1, 2, 5 only |
| A3 | AC-US2-3 / FR-04.1 | pass | curl.log `== A3`: POST with no body → 201 `completionDate` 2026-09-26 (app zone); `date` 2026-09-27 → 400 field `date` "must not be in the future"; completions = 1 record |
| A4 | AC-US2-4 / BR-7 / FR-04.2 | pass | curl.log `== A4`: no body, `{}` and explicit 2026-09-26 → 409 "Habit 1 is already complete for 2026-09-26"; completions still 1 record |
| A5 | AC-US2-5 / FR-04.5 | pass | curl.log `== A5`: DAILY 09-24/25/26 → done, streak 3; DAILY 09-24/25 (not today) → not done, streak 2; DAILY 09-23 only → streak 0; WEEKLY 09-15 only → not done, streak 1; +09-22 → done this week, streak 2, `completedToday` false; +today → `completedToday` true, streak 2; completions newest first |
| A6 | AC-US2-6 | pass | curl.log `== A6`: PUT name/description/frequency DAILY→WEEKLY → 200, GET returns all three changes |
| A7 | AC-US2-7 / FR-04.4 | pass | curl.log `== A7`: deactivate 6 → `active: false`; absent from `active=true`, only row in `active=false`; dashboard habits [1,2,5,6] → [1,2,5] (habitCounts.active 4 → 3); history kept (09-23 record); complete (today and 09-25) → 409 "inactive"; activate → 200, complete → 201 |
| A8 | FR-04.3 | pass | curl.log `== A8`: DELETE `/completions/2026-09-26` → 204; only the 09-23 record is left |
| A9 | AC-US2-8 / BR-14 | pass | curl.log `== A9`: DELETE habit 1 (3 completions) → 204; GET, completions list and complete → 404; absent from the full list, `active=true` and the dashboard ([2,5,6]) |
| U1 | AC-US2-9 / NFR-5 | pass | shot-01 `empty-state` "No habits yet" + `empty-state-action` "Add Habit", no form; shot-02 action opens `habit-form` (frequency Daily/Weekly); shot-05 `/habits?add=1` opens it |
| U2 | AC-US2-1 | pass | shot-06 `habit-card-7` Weekly with notice "Habit created"; shot-07 `habit-card-8` Daily; API: both active with `createdAt`, fields as entered |
| U3 | AC-US2-2 | pass | shot-03 empty name → `error-name` "must not be blank"; shot-04 151 chars → `error-name` "size must be between 1 and 150"; API count stayed 0 |
| U4 | AC-US2-3 / AC-US2-5 | pass | shot-08 after `habit-toggle-8` / `-7`: "Done today" / "Done this week", `habit-streak` 1, checked; API one record each for 2026-09-26; shot-09 same after reload |
| U5 | AC-US2-4 | pass | shot-10: habit 9 completed through the API behind the page, then toggle → `notice-error` "Habit 9 is already complete for 2026-09-26"; card shows Done today; API still 1 record |
| U6 | AC-US2-6 | pass | shot-11 `habit-edit-7` prefilled (Morning walk / 20 minutes / WEEKLY); shot-12 saved as Evening walk / 30 minutes / Daily on card and in API |
| U7 | AC-US2-7 | pass | shot-13 `habit-deactivate-8` → `habit-inactive` "Inactive", toggle disabled, `habit-activate-8` shown; API inactive, not in `active=true`, record kept; shot-14 activate → badge gone, toggle enabled, API active |
| U8 | AC-US2-8 | pass | shot-15 `habit-delete-9` → card gone (no confirm dialog); API GET and completions 404, absent from list and dashboard |
| U9 | Navigation | pass | shot-16: `nav-habits` ("Habits") from /dashboard → `/habits`, `page-title` is an H1 reading "Habits" |

Console: 2 × 400 on POST /api/habits (the U3 invalid saves, expected), 1 × 409 on POST /api/habits/9/completions
(the U5 duplicate, expected), 2 × 404 on /api/habits/9 and its completions (the U8 read-back after delete, expected);
no other errors.

## Unit tests
Not part of this test phase: the runner didn't run unit tests into this attempt folder (no `unit/` folder, and
`task.md` has no `unit_result`). They run in `test-phase-regression`.

## Requirement coverage
| id | covered by |
|---|---|
| FR-03.1 habit fields | A1 (all Habit fields returned), U2 |
| FR-03.2 create/read/update/deactivate/reactivate/delete; new = active | A1, A6, A7, A9, U2, U6, U7, U8 |
| FR-03.3 name required ≤ 150, frequency Daily/Weekly | A2, U3, U1 (options) |
| FR-04.1 completion record, default today, future rejected | A3, U4 |
| FR-04.2 no second record, user told | A4, U5 |
| FR-04.3 undo | A8 |
| FR-04.4 only active habits completable | A7, U7 |
| FR-04.5 done for current period + streak | A5, U4 |
| BR-6 | A2, U3 |
| BR-7 | A4, U5 |
| BR-14 | A9, U8 |
| NFR-5 empty state | U1 |

All ids of this phase are covered. FR-04.3 (undo) has no UI check because the plan lists none for this phase (the
ui-contract has no undo selector apart from the toggle).

## Questions
None.
