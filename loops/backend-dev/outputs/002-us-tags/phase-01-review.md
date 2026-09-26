# phase-01 review: US1 Tag tasks and filter by tag
layer: backend
story_id: US1
spec_phase: 1
depends_on: []

## Goal
Add tags to a task, read them back, filter the task list by tag and remove a tag, with validation
(1–30 characters after trim, case-insensitive and stored lower case, at most 10 per task).

## Tasks
Tests first (T001–T004, parallel `[P]`), then code (T005–T010). Full text in `phase-01.md` and
`specs/002-us-tags-backend/tasks.md` `## Phase 1`.
| Task | What | Kind |
|---|---|---|
| T001 [P] | `TaskTest`: add/remove/validation/no-op/limit/BR-T4 domain tests | unit test (domain) |
| T002 [P] | `TaskServiceTest`: `addTags` / `removeTag`, 404, time from `TimeService` | unit test (domain) |
| T003 [P] | `TaskRepositoryTest`: persistence, tag filter, delete cascade; new `tag` arg in existing `TaskQuery` calls | slice test (JPA) |
| T004 [P] | `TaskControllerTest`: POST/DELETE `/tags`, `?tag=` filter, `tags` in JSON, 400/404 shapes | slice test (web) |
| T005 | `Task.tags` element collection + `addTags` / `removeTag` / `getTags` | code |
| T006 | `TaskQuery.tag` + `EXISTS` predicate in `TaskSpecifications` | code |
| T007 | `TaskService.addTags` / `removeTag` | code |
| T008 [P] | `TaskTagsRequest` record (`TaskTags` schema) | code |
| T009 | `TaskResponse.tags` | code |
| T010 | `TaskController`: `tag` query param, `addTaskTags`, `removeTaskTag`; missing-param handler only if needed | code |

## Acceptance criteria
- AC-US1-1: Given an existing task When the user adds the tags "work" and "urgent" to it Then the task shows both tags, and reading the task back returns both tags
- AC-US1-2: Given tasks tagged "work" and tasks without that tag When the user filters the task list by "work" Then only the tasks tagged "work" are listed
- AC-US1-3: Given a task tagged "work" When the user removes the tag "work" Then the task no longer shows it and no longer appears when filtering by "work"
- AC-US1-4: Given a task When the user tries to add an empty tag or a tag longer than 30 characters Then the tag is rejected with a validation message and the task's tags are unchanged (BR-T1)
- AC-US1-5: Given a task tagged "work" When the user adds "Work" or filters by "WORK" Then "Work" and "work" are treated as the same tag: the task still has one such tag, and it is listed by the filter (BR-T2)
- AC-US1-6: Given a task with 10 tags When the user adds another, different tag Then the tag is rejected with a validation message and the task's tags are unchanged (BR-T3)

## Files
Changed (main):
- `backend/src/main/java/com/quickflow/domain/task/Task.java`: `tags` (`@ElementCollection(fetch = EAGER)`, table `task_tag`, unique `(task_id, tag)`, index `idx_task_tag_tag`), `TAG_MAX`, `TAGS_MAX`, `addTags`, `removeTag`, `getTags`
- `backend/src/main/java/com/quickflow/domain/task/TaskQuery.java`: new component `tag` after `archived`
- `backend/src/main/java/com/quickflow/domain/task/TaskSpecifications.java`: tag `EXISTS` predicate
- `backend/src/main/java/com/quickflow/domain/task/TaskService.java`: `addTags(long, List<String>)`, `removeTag(long, String)`
- `backend/src/main/java/com/quickflow/web/task/TaskResponse.java`: `tags`
- `backend/src/main/java/com/quickflow/web/task/TaskController.java`: `tag` param on `listTasks`, two new endpoints
- `backend/src/main/java/com/quickflow/web/ApiExceptionHandler.java`: only if T004 shows a missing required `tag` isn't mapped to the problem shape

New (main):
- `backend/src/main/java/com/quickflow/web/task/TaskTagsRequest.java`

Changed (tests): `TaskTest.java`, `TaskServiceTest.java`, `TaskRepositoryTest.java`, `TaskControllerTest.java`.

Database: new table `task_tag (task_id bigint FK, tag varchar(30))`, created by Hibernate `ddl-auto=update`
(default and test profiles, checked in `application.properties` / `application-test.properties`) and
`create-drop` (openapi profile).

Endpoints (contract `specs/002-us-tags-backend/contracts/openapi.yaml`):
- `POST /api/tasks/{id}/tags` `{"tags":[...]}` → 200 `Task` | 400 | 404 (`addTaskTags`)
- `DELETE /api/tasks/{id}/tags?tag=...` → 200 `Task` | 400 | 404 (`removeTaskTag`)
- `GET /api/tasks?tag=...` (new optional query parameter on `listTasks`)
- Every endpoint returning a `Task` (incl. `GET /api/tasks/{id}` and the dashboard) now returns `tags`.

App-wide output: the regenerated `backend/target/openapi.json` copied to `loops/backend-dev/outputs/openapi.json`.

## Planned checks
Build and tests: `cd backend && ./mvnw -q -DskipTests package`, then
`cd backend && ./mvnw -q verify -Dmaven.test.failure.ignore=true` (log in `RUNS/phase-01-build.log`).
Coverage of `com/quickflow/domain` stays ≥ 0.80 lines.

Unit tests, one or more per business rule, named with the rule id (existing convention:
`@DisplayName("<rule>: ...")`, method `<rule>_<what>`):
| Rule | Test (in `TaskTest` unless noted) |
|---|---|
| FR-001 / FR-002 | `fr001_addTagsAddsAllSortedAlphabetically` (`["work","urgent"]` → `[urgent, work]`) |
| BR-T1 | `brT1_tagIsTrimmed`, `brT1_emptyTagRejected`, `brT1_blankTagRejected`, `brT1_nullTagRejected`, `brT1_tag31CharsRejected`, `brT1_tag30CharsAccepted`, `brT1_batchWithOneInvalidTagAddsNone` (field `tags`, tags unchanged) |
| BR-T2 / Q1 | `brT2_tagStoredLowerCase`, `brT2_addingSameTagOtherCaseKeepsOne` (and `updatedAt` unchanged, Q2), `brT2_sameTagTwiceInOneCallAddsOne`, `brT2_removeTagIgnoresCase` |
| BR-T3 | `brT3_eleventhTagRejected` (field `tags`, 10 kept), `brT3_existingTagOnFullTaskAccepted` |
| BR-T4 | `brT4_updateKeepsTags`, `brT4_changeStatusKeepsTags`, `brT4_completeKeepsTags`, `brT4_archiveKeepsTags`, `brT4_restoreKeepsTags` |
| FR-005 / Q3 | `fr005_removeTagRemovesAndSetsUpdatedAt`, `fr005_removeAbsentTagIsNoOp`, `fr005_blankRemoveValueRejectedOnTag` |
| R-8 (A5) | `updatedAt` set on add/remove when the set changes, unchanged on a no-op (in the tests above) |
| FR-001 / FR-005 (`TaskServiceTest`) | `fr001_addTagsAddsToFoundTask`, `fr005_removeTagRemovesFromFoundTask`, `fr001_addTagsUnknownIdNotFound`, `fr005_removeTagUnknownIdNotFound`, time taken from `TimeService` |

Slice tests:
- `TaskRepositoryTest` (FR-004, FR-008, BR-T2, BR-T4, BR-4, BR-14): reload after flush + clear; filter `work` / `WORK` / ` work `; `workshop` not matched; a task with several tags listed once; AND with status; archived excluded by default; removed tag no longer matches; delete removes `task_tag` rows.
- `TaskControllerTest` (AC-US1-1, -3, -4, -6, FR-002, FR-004): 200 bodies with `tags`; 400 problem with `errors[].field` = `tags` for `{}`, `{"tags":[]}` and a service `ValidationException`; missing `tag` → 400; 404 for unknown ids; `?tag=work` reaches `TaskQuery.tag`.

No curl or Playwright here: the testing loop verifies the phase.

## Risks
- **Spring Data JPA `findAll(Specification)` and `CriteriaQuery`** (research R-7): the existing code already guards `cq != null` for ordering; the `EXISTS` subquery needs `cq.subquery(...)`. T006 checks first (small compile/test) that the Boot 4.1.1-managed version passes a non-null `CriteriaQuery`; if it can't be checked, the session stops with a question.
- **Missing required `tag` parameter**: `ApiExceptionHandler` does not extend `ResponseEntityExceptionHandler` and has no handler for `MissingServletRequestParameterException` (checked just now), so Spring's default resolver answers the 400 without the project's `errors[]` shape. T010 adds a handler if T004 shows this.
- **Existing tests calling `new TaskQuery(...)`** (checked: `TaskControllerTest` lines 173, 188; `TaskRepositoryTest` lines 51, 55, 59, 85, 101, 162) and `TaskController.listTasks` (line 64) break at compile time until the new `tag` argument is added (T003, T004, T010).
- **EAGER collection on list queries**: each listed task loads its tags (possible N+1 selects on the task list and dashboard). Needed because `spring.jpa.open-in-view=false` and `TaskResponse.from` runs outside the transaction (checked: `TaskController` line 127, `DashboardResponse` line 74). Acceptable for this app's data size; no NFR on list latency is changed by this feature.
- **Existing H2 file databases** (`./data/quickflow`, `./data/test`): `ddl-auto=update` adds the new table; existing tasks start with no tags (A6, A7).
- **Unique constraint vs. case**: safe only because values are lower-cased before storing (Q1); the domain dedupes before insert, so the constraint never fires in normal use.
- **Generated swagger**: the `TaskTags` schema name and `tags` required on `Task` must match the contract, as the frontend generates its client from `loops/backend-dev/outputs/openapi.json`.

## Assumptions
All already approved at the phase-00 review (A1–A7 in `phase-00-review.md`; Q1–Q3 answered). None new
for this phase.

## Open questions
None.

## Reviewer notes
