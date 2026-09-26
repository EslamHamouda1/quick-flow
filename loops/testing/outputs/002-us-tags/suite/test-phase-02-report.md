# test-phase-02 report: US1 Tag tasks and filter by tag (002-us-tags), attempt 1

Result: **10/10 API checks and 8/8 UI checks pass. No bugs filed.**

Evidence folder: `loops/testing/runs/002-us-tags/suite/test-phase-02/attempt-1/`.
- API: `curl.log` (every request, status, `time_total`, and the response fields each check reads). The checks
  are also written as `test-phase-02-verify.sh`, but running `bash` on a script needed approval in this session
  (as in earlier sessions), so the same calls were run as chained `curl --next` commands against a fresh test DB.
- UI: Playwright MCP (headless) on `http://localhost:4200`, screenshots `shot-NN-<check>.png`, page snapshots
  `page-*.yml`, console logs `console-*.log`. Tasks for the Given parts (ids 17–22, "S2 UI-*") were created with
  `fetch('/api/tasks')` from the page's own origin; every tag action (including the Given tags) was done in the UI.

| check | criterion | result | evidence |
|---|---|---|---|
| A1 | AC-US1-1 / FR-001 / FR-002 | pass | curl.log §A1: POST tags `["work","urgent"]` → 200 `["urgent","work"]`; GET /api/tasks/1 and the list carry the same |
| A2 | AC-US1-2 / FR-004 / SC-004 | pass | curl.log §A2: `?tag=work` → ids [3,2,1] exactly (N1 home, N2 untagged, N3 workshop absent); `?tag=nosuchtag` → 200 `[]` |
| A3 | AC-US1-3 / FR-005 | pass | curl.log §A3: DELETE `?tag=work` → 200 `["home"]`; GET agrees; 7 not in `?tag=work`; absent tag → 200 unchanged (updatedAt same); unknown id → 404 |
| A4 | AC-US1-4 / FR-006 / BR-T1 / SC-003 | pass | curl.log §A4: `[""]`, `["   "]`, 31-char, `["beta",31-char]` → 400 `errors[{field:"tags", message:"tag must not be blank" / "must be at most 30 characters"}]`, tags stay `["alpha"]` after each; 30-char → 200; `"  gamma "` → `gamma` |
| A5 | AC-US1-5 / FR-003 / BR-T2 | pass | curl.log §A5: `["Work"]` on [work] → 200 `["work"]`; `?tag=WORK` lists 9; `["Home"]` → `home` |
| A6 | AC-US1-6 / FR-007 / BR-T3 / SC-003 | pass | curl.log §A6: t01..t10 → 200 (10); `t11` → 400 "must have at most 10 tags", still 10; `T05` → 200, still 10 |
| A7 | FR-004 + other filters, BR-4 | pass | curl.log §A7: `tag=work&priority=HIGH` → [11]; `tag=work&status=TODO` → [12,9,3,2,1]; `tag=work&q=alpha` → [11]; archived 14 absent from `tag=work` |
| A8 | BR-T4 | pass | curl.log §A8: PUT, complete, archive, restore each return tags `["keep","work"]`; GET agrees |
| A9 | FR-008 / BR-T4 delete | pass | curl.log §A9: DELETE 16 → 204; `?tag=gone` → `[]`; POST/DELETE tags on 16 → 404 |
| A10 | SC-002 | pass | curl.log: all addTaskTags / removeTaskTag / listTasks?tag= calls ≤ 0.026 s (max = first add, A1) < 0.500 s |
| U1 | reachability, ui-contract | pass | shot-01-U1.png: "Tasks" nav link (/tasks) clicked from /dashboard; `filter-tag` label "Tag"; 20/20 rows have `task-row-tags` (also untagged row 22); `task-tag-input-17` placeholder "Add tags, comma-separated"; `task-tag-add-17` "Add tag" |
| U2 | AC-US1-1 | pass | shot-02-U2-added.png; page-…21-02-26 line 18 "Tags added"; input emptied; row 17 tags "urgent", "work"; same after reload (page-…21-02-40); API `["urgent","work"]` |
| U3 | AC-US1-2 | pass | shot-03-U3-filter-work.png: `filter-tag`=work → rows {1,2,3,9,11,12,15,17,18,20} = API `?tag=work` = every task tagged work, each row shows "work"; shot-04-U3-empty.png: `nosuchtag` → 0 rows, `empty-state` "No tasks match" |
| U4 | AC-US1-3 | pass | `task-tag-remove` "×" aria-label "Remove tag work"; page-…21-03-47 line 18 "Tag removed"; shot-05-U4-removed.png: row 18 shows only "home" (API `["home"]`); shot-06-U4-filter-work.png: row 18 not listed with `filter-tag`=work |
| U5 | AC-US1-4 | pass | shot-07-U5-empty.png: blank value "   " → `error-tags` "tag must not be blank"; shot-08-U5-31char.png: 31-char → "must be at most 30 characters"; row tags stay "alpha", input keeps its text, API `["alpha"]` |
| U6 | AC-US1-5 | pass | add "Work" to row 20 [work] → still one "work", no error, API `["work"]`; shot-09-U6-filter-WORK.png: `filter-tag`=WORK lists row 20 (same 9 rows as work after U4) |
| U7 | AC-US1-6 | pass | shot-10-U7-eleventh.png: row 21 (t01..t10, added in the UI) + `t11` → `error-tags` "must have at most 10 tags", still 10 `task-row-tag`, input keeps "t11", API unchanged |
| U8 | ui-contract | pass | Enter in `task-tag-input-18/19/20/21` added the Given tags ("Tags added" in page-…21-02-50 line 18, input emptied); shot-11-U8-overdue.png: after "Overdue" `filter-tag` disabled = true; console (all): only the 3 expected 400s on /api/tasks/19/tags (×2, U5) and /api/tasks/21/tags (U7) |

Note on U5: the "empty value" was entered as blank spaces ("   "), which the server rejects as empty after trimming
(`tag must not be blank`); the API side also covers `[""]` (A4).

## Unit tests
Not part of this test phase. The runner runs them into `unit/` for `test-phase-regression`.

## Requirement coverage
| id | covered by (this phase) |
|---|---|
| AC-US1-1 | A1, U2 |
| AC-US1-2 | A2, U3 |
| AC-US1-3 | A3, U4 |
| AC-US1-4 | A4, U5 |
| AC-US1-5 | A5, U6 |
| AC-US1-6 | A6, U7 |
| FR-001 add tags | A1, U2 |
| FR-002 tags returned on read | A1, U2 (reload) |
| FR-003 case-insensitive | A5, U6 |
| FR-004 filter by tag (with other filters) | A2, A7, U3 |
| FR-005 remove tag | A3, U4 |
| FR-006 validation 1–30 | A4, U5 |
| FR-007 max 10 | A6, U7 |
| FR-008 tags kept / removed with the task | A8, A9 — **"survive an app restart" has no suite check** (sessions never restart the app) |
| BR-T1 | A4, U5 |
| BR-T2 | A5, U6 |
| BR-T3 | A6, U7 |
| BR-T4 | A8, A9 |
| SC-002 (< 500 ms) | A10 |
| SC-003 (validation message) | A4, A6, U5, U7 |
| SC-004 (filter exact) | A2, U3 |

## Questions
None.
