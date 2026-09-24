# phase-03-report: US1 Manage tasks (frontend-dev), attempt 1

Plan: `phase-03-test.md`, written this attempt. Evidence folder:
`loops/testing/runs/001-quickflow/frontend-dev/phase-03/attempt-1/` (`checks-state.json` holds the values read
per check; `shot-NN-Cn.png` per check; `console-warnings-errors.txt`).
Playwright MCP, headless chromium, against `http://localhost:4200` (backend `http://localhost:8080`, profile `test`,
fresh DB). App date (Africa/Cairo): 2026-09-24. Tasks 2–9 for C9–C13 were seeded through `POST /api/tasks`
(+ `/complete`, `/archive`) since task creation through the UI was already checked in C2; every checked
behaviour was then driven through the UI.

**Verdict: pass** (13/13).

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US1-12: no tasks → empty state leading to Add Task | pass | `checks-state.json` C1: API `[]`, `empty-state` "No tasks yet" + `empty-state-action` "Add Task", click opens `task-form` (no `task-status` in create); `shot-01-C1.png` |
| C2 | AC-US1-1: create → Todo, not archived, created/updated set | pass | C2: form closes, `notice` "Task created", `task-row-1` "First task" / Todo / High / Oct 1, 2026; API `status TODO`, `archived false`, `createdAt`=`updatedAt` set; `/tasks?add=1` opens `task-form`, cancel drops `?add=1`; `shot-02-C2.png` |
| C3 | AC-US1-2 / BR-1: empty or 201-char title rejected, message on title field | pass | C3: both submits keep form open, `error-title` "size must be between 1 and 200" under the Title field, API still 1 task; console 400s; `shot-03-C3.png` |
| C4 | AC-US1-3 / BR-2: 2,001-char description rejected, message on description field | pass | C4: form open, `error-description` "size must be between 0 and 2000" under the Description label, no `error-title`, API still 1 task; `shot-04-C4.png` |
| C5 | AC-US1-4 / BR-3 (FA-11: only valid values offered) | pass | C5: `task-status` options exactly Todo / In Progress / Done (`TODO`,`IN_PROGRESS`,`DONE`); `task-priority` exactly Low / Medium / High; edit form prefilled from the row; `shot-05-C5.png` |
| C6 | AC-US1-5: edit all fields → saved, update time changes | pass | C6: `notice` "Task updated"; row "First task edited" / In Progress / Low / Oct 5, 2026; API description "Changed description", `updatedAt` 22:06:31.54 → 22:07:17.60; `shot-06-C6.png` |
| C7 | AC-US1-6 / BR-5: complete → Done | pass | C7: `notice` "Task completed", `task-row-status` "Done", complete button gone; API `DONE`, `completedAt` set; `shot-07-C7.png` |
| C8 | AC-US1-7 / BR-4: archive hides from default, listed when archived asked, restore brings back | pass | C8: after archive default list empty; `filter-archived` → `task-row-1` with `task-restore-1`; restore → archived view empty, default list has `task-row-1`, API `archived false`; `shot-08-C8-archived.png`, `shot-08-C8.png` |
| C9 | AC-US1-8 / BR-14: deleted task never returned again | pass | C9: no confirm dialog (FA-7), `notice` "Task deleted"; `task-row-2` gone from default list, search "Delete me" ("No tasks match"), archived view (`[9]`), overdue view (`[6]`); API `GET /api/tasks/2` → 404; `shot-09-C9.png` |
| C10 | AC-US1-9: case-insensitive title search | pass | C10: "abc" and "ABC" (fill and typed key by key) → only "xABCy", "Alpha abc"; "Other" and the rest absent; `shot-10-C10.png` |
| C11 | AC-US1-10: filters + sort, only matching rows in that order | pass | C11: High → [5,3]; Done → [8,1]; due 2026-10-01..10-10 → [4,3,1] (inclusive, createdAt desc); + sort dueDate asc → [4,1,3]; all dueDate asc [6,8,7,4,1,3,5] and desc [5,3,1,4,7,8,6] equal the API; createdAt asc [1,3,…,8]; Todo+Medium [6,7] = API; `shot-11-C11.png` |
| C12 | AC-US1-11: overdue lists due-before-today, not Done, not archived only | pass | C12: `task-view-overdue` (aria-pressed true) → only id 6 "Overdue yesterday" with `task-row-overdue` "Overdue"; due today (7), Done (8), archived (9) absent; API overdue `[6]`; default list shows the badge only on 6; `shot-12-C12.png` |
| C13 | AC-US1-12: no matches → empty state leading to Add Task | pass | C13: search "zzz-no-match" → 0 rows, `empty-state` "No tasks match" + "Add Task"; action opens `task-form` "Add Task"; `shot-13-C13.png` |

Console: the only errors are the three `400 (Bad Request)` resource loads from the rejected submits in C3/C4
(`console-warnings-errors.txt`).

## Unit tests
From `unit/unit-result.json`: outcome **none** ("no unit test command configured"). The frontend layer has no
`test` command and no `coverage`, so the unit rule does not apply.

## Requirement coverage
| id | unit tests | checks |
|---|---|---|
| AC-US1-1 / FR-01.2, FR-01.3 | none (no frontend test runner, FA-1) | C2 |
| AC-US1-2 / BR-1, FR-01.5 | none | C3 |
| AC-US1-3 / BR-2, FR-01.5 | none | C4 |
| AC-US1-4 / BR-3, FR-01.5 | none | C5 (UI offers only valid values; rejection itself checked in backend-dev phase-03) |
| AC-US1-5 / FR-01.2 | none | C6 |
| AC-US1-6 / BR-5, FR-01.4 | none | C7 |
| AC-US1-7 / BR-4, FR-01.6 | none | C8 |
| AC-US1-8 / BR-14 | none | C9 |
| AC-US1-9 / FR-02.1 | none | C10 |
| AC-US1-10 / FR-02.2, FR-02.3 | none | C11 |
| AC-US1-11 / FR-01.7 | none | C12 |
| AC-US1-12 / NFR-5 | none | C1, C13 |

No id of this phase is without a check. None has frontend unit tests (the layer has none configured, FA-1).

## Questions
None.
