# test-phase-02 report: US1 Manage tasks (attempt 1)

Run 2026-09-26 (Africa/Cairo; today = 2026-09-26) against a fresh test database. Evidence folder:
`loops/testing/runs/001-quickflow/suite/test-phase-02/attempt-1/`: `curl.log` (API checks, raw responses),
`bodies.json` (201/200-char titles, 2001/2000-char descriptions), `ui-results.json` (Playwright MCP return
values), `shot-01..34-*.png` (screenshots), `page-*.yml` / `console-*.log` (Playwright MCP).

API checks ran as direct curl calls (several requests per call with `curl --next`) because `bash <script>`,
`tee` and shell loops need approval in this session. The raw output is recorded in `curl.log`. UI checks ran through
Playwright MCP, headless, using the `data-testid` selectors from `specs/001-quickflow-frontend/contracts/ui-contract.md`.
UI test data was set up and read back with `fetch()` from the page, which goes through the dev-server proxy to the API.

**Result: 22/22 pass, 0 fail, 0 unclear. No bugs filed.**

| check | criterion | result | evidence |
|---|---|---|---|
| A1 | AC-US1-1 / FR-01.3 | pass | curl.log `== A1`: 201, status TODO, archived false, createdAt = updatedAt set; no priority → `MEDIUM` |
| A2 | AC-US1-2 / BR-1 | pass | curl.log `== A2`: empty / missing title → 400 `errors[].field=title`; 201 chars → 400 field `title`; 200 chars → 201 |
| A3 | AC-US1-3 / BR-2 | pass | curl.log `== A3`: 2001 → 400 field `description`; 2000 → 201 |
| A4 | AC-US1-4 / BR-3 | pass | curl.log `== A4`: status BLOCKED / priority URGENT → 400 on POST and PUT; task 2 unchanged afterwards |
| A5 | AC-US1-5 | pass | curl.log `== A5`: all five fields saved, updatedAt 12:39:39 → 12:40:06, GET agrees |
| A6 | AC-US1-6 / BR-5 / FR-01.4 | pass | curl.log `== A6`: complete → DONE + completedAt; back to TODO → completedAt null; to IN_PROGRESS → null |
| A7 | AC-US1-7 / BR-4 | pass | curl.log `== A7`: archived task 1 absent from default list, the only row with `archived=true`; restore → back in default, archived list [] |
| A8 | AC-US1-8 / BR-14 | pass | curl.log `== A8`: before delete, in overdue + dashboard.overdue; DELETE 204, GET 404, absent from list, `q=A8`, archived, overdue, dashboard (overdue [], taskCounts.total 5 → 4) |
| A9 | AC-US1-9 / FR-02.1 | pass | curl.log `q=alpha` and `q=ALPHA` → [8,7,6] (Alpha report, alphabet soup, Beta ALPHA), `q=zzz` → [] |
| A10 | AC-US1-10 / FR-02.2 / FR-02.3 | pass | curl.log: status, priority, dueFrom, dueTo, both (inclusive on both ends), combined, sort dueDate/createdAt asc/desc, default = createdAt desc; all 19 results equal the expected ids |
| A11 | AC-US1-11 / FR-01.7 | pass | curl.log `/api/tasks/overdue` → [6] (due 09-20, TODO); not 7 (due today), 11 (Done), 10 (archived) |
| U1 | AC-US1-12 / NFR-5 | pass | shot-01 `empty-state` "No tasks yet" + `empty-state-action` "Add Task"; shot-02 action opens `task-form`; shot-03 `/tasks?add=1` opens the form |
| U2 | AC-US1-1 | pass | shot-07: row 12 shows status "Todo", priority High, due Sep 28, 2026; API read-back matches title/description/priority/dueDate, status TODO, archived false; notice "Task created" |
| U3 | AC-US1-2 / AC-US1-3 | pass | shot-04 `error-title` "must not be blank"; shot-05 201 chars → `error-title` "size must be between 1 and 200"; shot-06 2001 chars → `error-description`; API count stayed 0 |
| U4 | AC-US1-4 | pass | ui-results U4: `task-status` = TODO/IN_PROGRESS/DONE, `task-priority` = LOW/MEDIUM/HIGH only (shot-08) |
| U5 | AC-US1-5 | pass | shot-08 `task-edit-12` form prefilled with the saved values; shot-09 row + API show the 5 edited fields, updatedAt changed |
| U6 | AC-US1-6 | pass | shot-10 `task-row-status` "Done"; API status DONE with completedAt |
| U7 | AC-US1-7 | pass | shot-11 row gone from default list; shot-12 shown with `filter-archived` checked, `task-restore-12` present; shot-13 after restore back in default list, API archived false |
| U8 | AC-US1-8 | pass | shot-14 row 13 gone after `task-delete-13`, API 404; shot-15 search "U8" → empty state; shot-16 archived view without it; shot-17 overdue view without it |
| U9 | AC-US1-9 / AC-US1-10 | pass | ui-results U9: 15 combinations (search both cases, each filter, date range, combined, 3 sorts, archived, no match): UI row ids and order = API ids and order in 15/15 (shot-18..32) |
| U10 | AC-US1-11 | pass | shot-33: `task-view-overdue` lists only row 14 (= `/api/tasks/overdue` [14]) with `task-row-overdue`; in the default list only row 14 has the badge |
| U11 | Navigation | pass | shot-34: `nav-tasks` ("Tasks") from /dashboard → `/tasks`, `page-title` is an H1 reading "Tasks" |

Console: 3 × 400 on POST /api/tasks (the U3 invalid saves, expected) and 1 × 404 on GET /api/tasks/13 (the U8
read-back after delete, expected); no other errors.

## Unit tests
Not part of this test phase: the runner didn't run unit tests into this attempt folder (no `unit/` folder, and
`task.md` has no `unit_result`). They run in `test-phase-regression`.

## Requirement coverage
| id | covered by |
|---|---|
| FR-01.1 task fields | A1 (all Task fields returned), U2 |
| FR-01.2 create/read/update/archive/restore/delete | A1, A5, A7, A8, U2, U5, U7, U8 |
| FR-01.3 defaults TODO / MEDIUM | A1 |
| FR-01.4 completion time set / cleared | A6, U6 |
| FR-01.5 title required, ≤ 200 | A2, U3 |
| FR-01.6 archived excluded from default list | A7, U7 |
| FR-01.7 overdue | A11, A8, U10 |
| FR-02.1 search | A9, U9 |
| FR-02.2 filters | A10, U9 |
| FR-02.3 sort | A10, U9 |
| BR-1 | A2, U3 |
| BR-2 | A3, U3 |
| BR-3 | A4, U4 |
| BR-4 | A7, U7 |
| BR-5 | A6, U6 |
| BR-14 | A8, U8 |
| NFR-5 empty state | U1, U8 (search), U9 (`search zzz`) |

All ids of this phase are covered.

## Questions
None.
