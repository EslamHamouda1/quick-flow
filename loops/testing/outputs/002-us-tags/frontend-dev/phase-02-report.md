# phase-02-report: Polish & Cross-Cutting Concerns (frontend-dev), attempt 1

Plan: `phase-02-test.md` (written this attempt). The phase has no acceptance criteria of its own; C1–C6 re-run phase-01's
AC-US1-1..6 checks unchanged, C7–C8 check the ui-contract (T006), C9 the build log (T007). Run with Playwright MCP,
headless, against http://localhost:4200/tasks on the runner's fresh test database. Evidence folder:
`loops/testing/runs/002-us-tags/frontend-dev/phase-02/attempt-1/` (below: `E/`).

Setup: the "Given" tasks A, W1, W2, N1, N2, N3, R, V, K, F, U, E (ids 1–12) were created with `POST /api/tasks` from the
page (`fetch` to the app's `/api/tasks` proxy path). Result in `E/setup-tasks.json`: all 201, `tags: []`. All tagging,
removing and filtering was done in the UI. "Reading back" is `GET /api/tasks/<id>` after the UI action.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US1-1 | pass | `E/C1-after-add.json`: after `work, urgent` + "Add tag", row 1 tags [urgent, work], input emptied, API [urgent, work]; `E/C1-after-reload.json`: still [urgent, work] after reload; `shot-01-C1.png`, `shot-02-C1-reload.png` |
| C2 | AC-US1-2 | pass | `E/C2-filter-work.json`: `filter-tag` = work lists exactly rows 9, 7, 3, 2, 1 (the tasks whose API tags include work). N1 [home], N2 [], N3 [workshop], V [alpha] and the untagged 10–12 are not listed; `shot-03-C2.png` |
| C3 | AC-US1-3 | pass | `E/C3-after-remove.json`: "Remove tag work" in row 7 → notice "Tag removed", row 7 [home], API [home]; `E/C3-filter-work.json`: under work, rows 9, 3, 2, 1 (7 gone); `shot-04-C3-remove.png`, `shot-05-C3-filter.png` |
| C4 | AC-US1-4 | pass | `E/C4-empty.json`: empty input → `error-tags` "tag must not be blank", row 8 and API still [alpha]; `E/C4-31chars.json`: 31-char tag → `error-tags` "must be at most 30 characters", row 8 and API [alpha], input kept its 31 characters; `shot-06-C4-empty.png`, `shot-07-C4-31chars.png` |
| C5 | AC-US1-5 | pass | `E/C5-add-Work.json`: `Work` on row 9 (tagged work) → no error, row 9 [work], API [work]; `E/C5-filter-WORK.json`: `filter-tag` = WORK lists rows 9, 3, 2, 1; `shot-08-C5-add.png`, `shot-09-C5-filter.png` |
| C6 | AC-US1-6 | pass | `E/C6-after10.json`: t01..t10 → 10 `task-row-tag`, API the same 10; `E/C6-eleventh.json`: `t11` → `error-tags` "must have at most 10 tags", still 10 in the row and in the API; `shot-10-C6-ten.png`, `shot-11-C6-eleventh.png` |
| C7 | T006 / ui-contract selectors | pass | `E/C7-contract.json`: `filter-tag` is a text input labelled "Tag"; untagged row 11 has `task-row-tags` with 0 tags; placeholder "Add tags, comma-separated"; `task-tag-add-11` is a button "Add tag"; `task-row-tag` elements sit inside `task-row-tags`; remove buttons are "×" with aria-label "Remove tag urgent" / "Remove tag work". `E/C7-overdue.json`: after `task-view-overdue`, `filter-tag` is disabled; `shot-12-C7-contract.png`, `shot-13-C7-overdue.png` |
| C8 | ui-contract Enter / notice / input emptied | pass | `E/C8-enter.json`: `enter1` + Enter in `task-tag-input-12` → notice "Tags added", row 12 [enter1], input empty, API [enter1]; `shot-14-C8-enter.png` |
| C9 | T007 clean build | pass | `loops/frontend-dev/runs/002-us-tags/phase-02-build.log`: exists, line 29 "Application bundle generation complete.", 0 matches for `error TS` / `✘ [ERROR]` |

Console (`E/console-errors.txt`):
- 3 × 400: the expected responses to the rejected adds (C4 × 2, C6).
- 1 CORS error plus its `ERR_FAILED`: my first setup attempt, a `fetch` straight to `:8080`. It came from the test itself, not from the app. The retry through the app's `/api` path worked.

Notes (not checks): the generated client now types `Task.tags` as `Set<string>` (see the dev loop's progress log). The UI still renders, orders and filters the tags correctly, as C1–C8 show.

## Unit tests
From `unit-result.json`: `{"layer":"frontend","outcome":"none","reason":"no unit test command configured"}`.
No frontend unit tests are configured: passed 0, failed 0, and this layer has no coverage threshold.

## Requirement coverage
| id | unit tests | checks |
|---|---|---|
| FR-001 add one or more tags in one action | none (frontend has none) | C1, C6, C8 |
| FR-002 reads return tags, alphabetical, lower case | none | C1, C2, C5 |
| FR-003 case-insensitive, duplicate add a no-op | none | C5 |
| FR-004 filter by one tag, ignoring case | none | C2, C3, C5 |
| FR-005 remove a tag | none | C3 |
| FR-006 empty / >30 rejected with message, unchanged | none | C4 |
| FR-007 more than 10 rejected, unchanged | none | C6 |
| FR-008 tags kept with the task | none | C1 (reload) |
| BR-T1 | none | C4 |
| BR-T2 | none | C5 |
| BR-T3 | none | C6 |
| BR-T4 deleted with the task | none | **none** (no criterion or task of this phase covers deleting; covered by the backend-dev phase-01 unit tests) |

## Questions
None.
