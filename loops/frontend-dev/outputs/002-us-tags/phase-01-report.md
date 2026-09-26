# phase-01-report: US1 Tag tasks and filter by tag (frontend-dev), attempt 1

Plan: `phase-01-test.md` (written this attempt, C1–C6 from AC-US1-1..6). Run with Playwright MCP, headless,
against http://localhost:4200/tasks on the runner's fresh test database. Evidence folder:
`loops/testing/runs/002-us-tags/frontend-dev/phase-01/attempt-1/` (below: `E/`).

Setup: the "Given" tasks A, W1, W2, N1, N2, N3, R, V, K, F (ids 1–10) were created with
`POST /api/tasks` from the page (`fetch`, the shell refused a loop of curl calls), result in `E/setup-tasks.json`
(all 201, `tags: []`); the plan's `setup-curl.log` is therefore `setup-tasks.json`. All tagging, removing and
filtering was done in the UI; "reading back" is `GET /api/tasks/<id>` after the UI action.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US1-1 | pass | `E/C1-after-add.json`: after typing `work, urgent` + "Add tag", row 1 `task-row-tag` = [urgent, work], remove buttons "×" / "Remove tag urgent", "Remove tag work", input emptied, `GET /api/tasks/1` tags [urgent, work]; `E/C1-after-reload.json` still [urgent, work] after reload; `shot-01-C1.png`, `shot-02-C1-reload.png` |
| C2 | AC-US1-2 | pass | `E/C2-filter-work.json`: `filter-tag` = work lists exactly rows 9, 7, 3, 2, 1, the tasks whose API tags include work; N1 [home], N2 [], N3 [workshop], V [alpha], F [] not listed; `shot-03-C2.png` |
| C3 | AC-US1-3 | pass | `E/C3-after-remove.json`: clicking "Remove tag work" in row 7 → row 7 [home], API [home]; `E/C3-filter-work.json`: under work rows 9, 3, 2, 1 (7 gone); `shot-04-C3-remove.png`, `shot-05-C3-filter.png` |
| C4 | AC-US1-4 | pass | `E/C4-empty.json`: empty input + "Add tag" → `error-tags` "tag must not be blank", row 8 and API still [alpha]; `E/C4-31chars.json`: 31-char tag → `error-tags` "must be at most 30 characters", row 8 and API [alpha], input kept its text; `shot-06-C4-empty.png`, `shot-07-C4-31chars.png` |
| C5 | AC-US1-5 | pass | `E/C5-add-Work.json`: adding `Work` to row 9 (tagged work) → no error, row 9 [work] (one tag), API [work]; `E/C5-filter-WORK.json`: `filter-tag` = WORK lists rows 9, 3, 2, 1 (row 9 included); `shot-08-C5-add.png`, `shot-09-C5-filter.png` |
| C6 | AC-US1-6 | pass | `E/C6-after10.json`: t01..t10 added in one entry → 10 `task-row-tag`; `E/C6-eleventh.json`: `t11` → `error-tags` "must have at most 10 tags", still the 10 tags in the row and in the API; `shot-10-C6-ten.png`, `shot-11-C6-eleventh.png` |

Console (`E/console-errors.txt`): only the 3 expected 400 responses of the rejected adds (C4 ×2, C6).
Notes (outside the ACs): the "Tags added" notice was seen after an add (row 2 setup); it expires, so it
wasn't caught when read about half a second or more later (C1, C3).

## Unit tests
From `unit-result.json`: `{"layer":"frontend","outcome":"none","reason":"no unit test command configured"}`:
no frontend unit tests configured; passed 0, failed 0, no coverage threshold for this layer.

## Requirement coverage
| id | unit tests | checks |
|---|---|---|
| FR-001 add one or more tags in one action | none (frontend has none) | C1, C6 |
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
| BR-T4 deleted with the task | none | **none** (no AC of this phase covers deleting; covered by backend-dev phase-01 unit tests) |

## Questions
None.
