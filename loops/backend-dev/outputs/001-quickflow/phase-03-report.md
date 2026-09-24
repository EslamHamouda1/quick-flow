# phase-03-report: US1 Manage tasks (backend-dev), attempt 1

Plan: `phase-03-test.md` (new on this attempt). Evidence folder:
`loops/testing/runs/001-quickflow/backend-dev/phase-03/attempt-1/`.

Note: `phase-03-verify.sh` could not be executed (the session's permission check asked for
approval, as in phases 01 and 02), so its checks were run as direct curl calls against the same
URLs with the same inputs. Every request is labelled in `curl.log` by its URL fragment
(`#C<n>-...`), followed by the full response and `=== <method> <url> -> status <code> (sent <n> bytes)`.
Long request bodies are the `req-*.json` files (lengths in `file-checks.txt`). Today (app time zone,
`GET /api/app-info`) was 2026-09-24 for the whole run (21:53–21:57 +03:00). Task ids: C1 = 1,
C5 = 5, C7 = 6, C8 = 7, C9 = 8–11, C10 a–f = 12–17, C11 o1, o2, n1–n5 = 18–24.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US1-1, FR-01.1, FR-01.3 | pass | `curl.log` first entry: `POST /api/tasks` → 201 `application/json`, `status TODO`, `archived false`, `createdAt` = `updatedAt` = `2026-09-24T21:53:41.56…+03:00`, fields echoed, `completedAt null`; `#C1-get` → 200 same values; `#C1-def` `{title}` only → 201 `status TODO`, `priority MEDIUM`, `archived false` |
| C2 | AC-US1-2, BR-1 | pass | `#C2-empty`, `#C2-blank`, `#C2-missing-title`, `#C2-title-201-chars` (213 bytes), `#C2-put-empty-title`, `#C2-put-title-201-chars` → each 400 `application/problem+json` with `errors[{field:"title", message:"must not be blank" / "size must be between 1 and 200"}]`; `#C2-title-200-chars` (212 bytes) → 201, full 200-char title stored |
| C3 | AC-US1-3, BR-2 | pass | `#C3-desc-2001-chars` (2032 bytes), `#C3-put-desc-2001-chars` → 400 problem, `errors[{field:"description", message:"size must be between 0 and 2000"}]`; `#C3-desc-2000-chars` (2031 bytes) → 201 |
| C4 | AC-US1-4, BR-3 | pass | `#C4-post-status-FOO`, `#C4-post-priority-FOO`, `#C4-put-status-FOO`, `#C4-put-priority-URGENT`, `#C4-put-status-null` → each 400 `application/problem+json` (`errors` names `status` / `priority`); `#C4-get-unchanged` → task 1 still `C1 Buy milk`, `TODO`, `HIGH`, `updatedAt` unchanged |
| C5 | AC-US1-5, FR-01.2 | pass | `#C5-create` / `#C5-get-before` `updatedAt 21:56:07.03798`; `#C5-put` (6 s later) → 200 with new title, description, `IN_PROGRESS`, `HIGH`, `2026-10-01`; `#C5-get-after` same values, `updatedAt 21:56:13.720895`, `createdAt 21:56:07.03798` unchanged |
| C6 | AC-US1-6, BR-5, FR-01.4 | pass | `#C6-complete` → 200 `status DONE`, `completedAt 21:56:13.738…`; `#C6-get` DONE; `#C6-back-to-TODO` → 200 `status TODO`, `completedAt null`; `#C6-complete-unknown` → 404 problem |
| C7 | AC-US1-7, BR-4, FR-01.6 | pass | `c7` block: `#C7-archive` → 200 `archived true`; `#C7-default-list-after-archive` ids 5,4,3,2,1 (no 6); `#C7-archived-list-after-archive` = [6]; `#C7-restore` → 200 `archived false`; `#C7-default-list-after-restore` includes 6; `#C7-archived-list-after-restore` = [] |
| C8 | AC-US1-8, BR-14, FR-01.2 | pass | `#C8-precondition-overdue` / `#C8-precondition-search` list task 7; `#C8-delete` → 204; `#C8-get-after-delete` → 404 `application/problem+json` "Task 7 not found"; `#C8-search-after-delete`, `#C8-overdue-after-delete`, `#C8-archived-after-delete` = [], `#C8-default-list-after-delete` ids 6,5,4,3,2,1; PUT / complete / archive / restore / DELETE on 7 → 404 each; `#C8-get-unknown` → 404. "Summary" has no endpoint in this phase (dashboard phase). |
| C9 | AC-US1-9, FR-02.1 | pass | `c9` block: `q=zyxq42`, `q=ZYXQ42`, `q=yXq4` each → exactly ids 10, 9, 8 ("Mixed ZyXq42 case", "zyxq42 lower", "Report ZYXQ42 alpha"); 11 ("Other 42", token only in its description) never listed |
| C10 | AC-US1-10, FR-02.2, FR-02.3 | pass | `c10b` block (with `q=flt42`): `status=TODO` → f,e,c,a; `priority=HIGH` → e,d,b,a; both → e,a; `dueFrom=09-26&dueTo=09-27` → f,d,b (both ends inclusive); `dueFrom=09-27` → f,c,b; `dueTo=09-26` → d,a; `priority=HIGH&dueFrom=09-26` → d,b; `createdAt asc` a..f; `createdAt desc` f..a; no sort → f..a (newest first); `dueDate asc` 25,26,27,27,29 (then e); `dueDate desc` 29,27,27,26,25 (then e); `status=TODO&sort=dueDate&direction=asc` → a,f,c (then e) |
| C11 | AC-US1-11, FR-01.7 | pass | `c11b` block: `#C11-overdue-list` → exactly 19 (IN_PROGRESS, due 09-20) and 18 (TODO, due 09-23), both `overdue true`; not listed: 20 (due today), 21 (DONE), 22 (archived), 23 (no due date), 24 (tomorrow); `GET` each: `overdue` true for 18, 19 and false for 20–24; `#C11-today-check` still 2026-09-24 |
| C12 | swagger / contract | pass | `file-checks.txt` §C12, `C12-api-docs.json`: 9 task operationIds at the contract's paths/methods, `createTask` 201, `deleteTask` 204, schemas `Task`, `TaskCreate`, `TaskUpdate`, `TaskStatus`, `TaskPriority` with the contract's enums; `backend/target/openapi.json` and the copy byte-identical |

AC-US1-12 is frontend layer: not checked here.

Observations, not checks (no criterion of this phase covers them):
- The generated swagger lists only success responses; the contract's `400` (`BadRequest`) and
  `404` (`NotFound`) responses on the task operations are not in `openapi.json` (the review's risk
  list already names this). The live API does return them as the contract describes.
- `servers[0].url` in the generated file is still `http://localhost:18080` (phase-02 observation).
- The response of a create/update carries nanosecond timestamps (`…41.562222553`), a later `GET`
  the stored microsecond value (`…41.562223`). Same instant to within 1 µs; a client comparing
  strings between the two would see a difference.

## Unit tests
From `unit/unit-result.json`: passed 62, failed 0, errors 0, skipped 0; exit code 0; outcome **pass**;
coverage: reported as skipped ("no classes in com/quickflow/domain").

The skip is wrong again: `com/quickflow/domain/task` and `com/quickflow/domain/common` hold 12
classes. The runner only matches a JaCoCo package named exactly `com/quickflow/domain`
(`scripts/lib/loopctl.py`, noted in phase-02 and in this phase's review risks). From
`unit/coverage/jacoco.csv`, domain line coverage is **155/160 = 96.9%**, above `min_line` 0.80
(`file-checks.txt`). So the unit rule passes either way. The runner check needs fixing before a
phase whose domain coverage could be under the threshold.

## Requirement coverage
| id | unit tests | checks |
|---|---|---|
| FR-01.1 task fields | `TaskTest.fr01_3_defaultsTodoMediumNotArchived`, `TaskControllerTest.createTaskReturns201Task` | C1, C12 |
| FR-01.2 create/read/update/archive/restore/delete | `TaskServiceTest.fr01_2_*` (create, update, delete, unknown id ×6), `TaskControllerTest` (create, update, complete/archive/restore, delete, get 404) | C1, C5, C7, C8 |
| FR-01.3 defaults | `TaskTest.fr01_3_defaultsTodoMediumNotArchived` | C1 |
| FR-01.4 completion time | `TaskTest.br5_completeSetsDoneAndCompletedAt`, `TaskTest.fr01_4_leavingDoneClearsCompletedAt` | C6 |
| FR-01.5 / BR-1 title | `TaskTest.br1_*` (3), `TaskControllerTest.br1_blankTitleIs400WithTitleField` | C2 |
| FR-01.5 / BR-2 description | `TaskTest.br2_*` (2) | C3 |
| FR-01.5 / BR-3 status, priority | `TaskTest.br3_*` (2), `TaskControllerTest.br3_unknownStatusIs400`, `br3_unknownStatusQueryParamIs400` | C4 |
| FR-01.6 / BR-4 archived | `TaskServiceTest.br4_*` (2), `TaskRepositoryTest.br4_*` (2) | C7 |
| FR-01.7 overdue | `TaskTest.fr01_7_*` (5), `TaskServiceTest.fr01_7_overdueUsesToday`, `TaskRepositoryTest.fr01_7_overdueQuery`, `TaskControllerTest.listOverdueTasks` | C8, C11 |
| BR-5 completed = Done | `TaskTest.br5_completeSetsDoneAndCompletedAt` | C6 |
| BR-14 deleted not returned | `TaskRepositoryTest.br14_deletedTaskNotReturned`, `TaskServiceTest.fr01_2_deleteRemovesExistingTask` | C8 |
| FR-02.1 title search | `TaskRepositoryTest.fr02_1_titleSearchIgnoresCase` | C9 |
| FR-02.2 filters | `TaskRepositoryTest.fr02_2_filtersCombine`, `fr02_2_dueRangeInclusive`, `TaskControllerTest.listTasksBinds*` (2) | C10 |
| FR-02.3 sort | `TaskRepositoryTest.fr02_3_*` (3), `TaskControllerTest.listTasksBindsQueryWithDefaults`, `unknownSortIs400` | C10 |

None uncovered.

## Questions
None.
