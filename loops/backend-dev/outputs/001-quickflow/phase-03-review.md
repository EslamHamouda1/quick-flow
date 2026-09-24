# phase-03-review: US1 Manage tasks

## Goal
Create, edit, complete, archive, restore, delete, search, filter and sort tasks; list overdue tasks (`/api/tasks*`).

## Tasks
- [ ] T018 [P] [US1] Domain test backend/src/test/java/com/quickflow/domain/task/TaskTest.java: `BR-1` (blank title and 201 chars rejected with field `title`, 200 accepted), `BR-2` (2,001-char description rejected with field `description`, null accepted), `BR-3` (null status or priority rejected), `BR-5` (complete → DONE with `completedAt`; leaving DONE clears `completedAt`, FR-01.4), `FR-01.3` (defaults TODO / MEDIUM / not archived), `FR-01.7` overdue (due yesterday & not DONE & not archived → true; due today, DONE, archived, no due date → false) with the fixed clock
- [ ] T019 [P] [US1] Domain test backend/src/test/java/com/quickflow/domain/task/TaskServiceTest.java (Mockito `TaskRepository`, fixed clock): `FR-01.2` create sets `createdAt`/`updatedAt` from the clock, update changes `updatedAt`, `BR-4` archive/restore toggle `archived`, `FR-01.2` unknown id → `NotFoundException` for get/update/delete/complete/archive/restore
- [ ] T020 [P] [US1] Persistence test backend/src/test/java/com/quickflow/persistence/TaskRepositoryTest.java (`@DataJpaTest`): `FR-02.1` title search ignores case, `FR-02.2` status/priority/dueFrom–dueTo (inclusive)/archived filters combine, `FR-02.3` sort by dueDate (nulls last) and createdAt both directions, `BR-4` archived excluded by default, `FR-01.7` overdue query, `BR-14` a deleted task is not returned by any query
- [ ] T021 [P] [US1] Web-slice test backend/src/test/java/com/quickflow/web/task/TaskControllerTest.java (`@MockitoBean TaskService`): `createTask` 201 body shape, `BR-1` 400 with `errors[].field == "title"`, `BR-3` 400 for `status: "FOO"`, `getTask` 404, `listTasks` binds `q,status,priority,dueFrom,dueTo,archived,sort,direction` into `TaskQuery` (defaults `archived=false, sort=createdAt, direction=desc`), `listOverdueTasks`, `completeTask`/`archiveTask`/`restoreTask` 200, `deleteTask` 204
- [ ] T022 [P] [US1] Create enums `TaskStatus {TODO, IN_PROGRESS, DONE}` and `TaskPriority {LOW, MEDIUM, HIGH}` in backend/src/main/java/com/quickflow/domain/task/
- [ ] T023 [US1] Create JPA entity `Task` in backend/src/main/java/com/quickflow/domain/task/Task.java: `id` Long identity; `title` "required, trimmed non-blank, ≤ 200 (BR-1)"; `description` "≤ 2,000 (BR-2)"; `status` "required, default TODO (BR-3)"; `priority` "required, default MEDIUM (BR-3)"; `dueDate` LocalDate optional; `createdAt`, `updatedAt`, `completedAt` Instant; `archived` default false; methods `complete(now)`, `changeStatus(status, now)`, `archive(now)`, `restore(now)`, `isOverdue(today)`; rule violations throw `ValidationException`
- [ ] T024 [P] [US1] Create `TaskQuery` record (q, status, priority, dueFrom, dueTo, archived, sort, direction) in backend/src/main/java/com/quickflow/domain/task/TaskQuery.java
- [ ] T025 [US1] Create `TaskRepository` (`JpaRepository<Task, Long>`, `JpaSpecificationExecutor<Task>`, overdue query `dueDate < :today and status <> DONE and archived = false` ordered by dueDate) and `TaskSpecifications` building the `TaskQuery` filters and sort (nulls last on dueDate) in backend/src/main/java/com/quickflow/domain/task/
- [ ] T026 [US1] Create `TaskService` (`create, get, list(TaskQuery), overdue, update, complete, archive, restore, delete`, time from `TimeService`) in backend/src/main/java/com/quickflow/domain/task/TaskService.java
- [ ] T027 [P] [US1] Create records `TaskCreateRequest`, `TaskUpdateRequest`, `TaskResponse` (fields per contract `TaskCreate`, `TaskUpdate`, `Task`, including computed `overdue`) in backend/src/main/java/com/quickflow/web/task/
- [ ] T028 [US1] Create `TaskController` with methods `listTasks, createTask, listOverdueTasks, getTask, updateTask, deleteTask, completeTask, archiveTask, restoreTask` mapped as in contracts/openapi.yaml in backend/src/main/java/com/quickflow/web/task/TaskController.java

Order: the four tests T018–T021 first (in parallel, different files), then T022, T024 and T027 in
parallel (T027 needs the enums from T022, so T027 starts after T022 if run by subagents), then
T023 → T025 → T026 → T028 (each needs the previous).

## Acceptance criteria
Copied from `phase-03.md` (spec.md US1):
- AC-US1-1: Given no tasks, When the user creates a task with a title, optional description, priority and due date, Then the task is stored with status Todo, not archived, and with its creation and update times set.
- AC-US1-2: Given the task form, When the user submits an empty title or a title longer than 200 characters, Then the task is rejected with a message naming the title field (BR-1).
- AC-US1-3: Given the task form, When the user submits a description longer than 2,000 characters, Then the task is rejected with a message naming the description field (BR-2).
- AC-US1-4: Given a task, When a status other than Todo / In Progress / Done or a priority other than Low / Medium / High is submitted, Then the change is rejected (BR-3).
- AC-US1-5: Given an existing task, When the user edits its title, description, status, priority or due date, Then the changes are saved and its update time changes.
- AC-US1-6: Given a task, When the user marks it completed, Then its status is Done (BR-5).
- AC-US1-7: Given a task, When the user archives it, Then it no longer appears in the default task list but can still be listed by asking for archived tasks, and restoring it brings it back to the default list (BR-4).
- AC-US1-8: Given a task, When the user deletes it, Then it is never returned by any normal list, search, lookup or summary again (BR-14).
- AC-US1-9: Given several tasks, When the user searches by text, Then only tasks whose title contains that text (ignoring case) are listed.
- AC-US1-10: Given several tasks, When the user filters by status, priority and/or due date and chooses a sort (due date or creation date), Then only matching tasks are listed, in that order.
- AC-US1-11: Given a task with a due date before today that is not Done and not archived, When the user asks for overdue tasks, Then that task is listed as overdue; a task due today, a Done task or an archived task is not.
- AC-US1-12: (frontend layer, not built or observable here.)

What the testing loop can observe after this phase (all under `http://localhost:8080`):
- `POST /api/tasks` → 201 `Task`; 400 `application/problem+json` with `errors[].field` `title` / `description` / `status` / `priority` for AC-US1-2..4.
- `GET /api/tasks` (with `q,status,priority,dueFrom,dueTo,archived,sort,direction`), `GET /api/tasks/overdue`, `GET/PUT/DELETE /api/tasks/{id}`, `POST /api/tasks/{id}/complete|archive|restore`, with the status codes of `contracts/openapi.yaml`; 404 after delete.
- `/v3/api-docs` and `backend/target/openapi.json` list these 9 operationIds with schemas `Task`, `TaskCreate`, `TaskUpdate`, `TaskStatus`, `TaskPriority`.

## Files
| File | Change |
|---|---|
| `backend/src/main/java/com/quickflow/domain/task/TaskStatus.java` | new enum |
| `backend/src/main/java/com/quickflow/domain/task/TaskPriority.java` | new enum |
| `backend/src/main/java/com/quickflow/domain/task/Task.java` | new `@Entity` (table `task`), rules BR-1/2/3/5, `isOverdue(today)` |
| `backend/src/main/java/com/quickflow/domain/task/TaskQuery.java` | new record; `sort` / `direction` as small enums (`TaskSort {CREATED_AT, DUE_DATE}`, `Sort.Direction`) — see A-9 |
| `backend/src/main/java/com/quickflow/domain/task/TaskRepository.java` | new, `JpaRepository<Task, Long>` + `JpaSpecificationExecutor<Task>` + `@Query` overdue |
| `backend/src/main/java/com/quickflow/domain/task/TaskSpecifications.java` | new, `Specification<Task>` from `TaskQuery` + `Sort` (dueDate nulls last) |
| `backend/src/main/java/com/quickflow/domain/task/TaskService.java` | new `@Service`, `@Transactional`, uses `TaskRepository` + `TimeService` |
| `backend/src/main/java/com/quickflow/web/task/TaskCreateRequest.java` | new record (+ `@NotBlank`/`@Size` so the generated swagger has the contract's `minLength`/`maxLength`, A-4) |
| `backend/src/main/java/com/quickflow/web/task/TaskUpdateRequest.java` | new record (`title`, `status`, `priority` required) |
| `backend/src/main/java/com/quickflow/web/task/TaskResponse.java` | new record, `OffsetDateTime` via `TimeService.toOffset`, `overdue` from `isOverdue(today)` |
| `backend/src/main/java/com/quickflow/web/task/TaskController.java` | new, `@RequestMapping("/api/tasks")`, 9 methods |
| `backend/src/main/java/com/quickflow/web/ApiExceptionHandler.java` | + handler for `MethodArgumentTypeMismatchException` (bad enum/date/boolean in a **query or path** param → 400 problem with `errors[{field: <param name>}]`), A-10 — not named by any task, needs approval |
| `backend/src/test/java/com/quickflow/domain/task/TaskTest.java` | new |
| `backend/src/test/java/com/quickflow/domain/task/TaskServiceTest.java` | new |
| `backend/src/test/java/com/quickflow/persistence/TaskRepositoryTest.java` | new |
| `backend/src/test/java/com/quickflow/web/task/TaskControllerTest.java` | new |

Endpoints added (contract `paths./api/tasks*`): `listTasks`, `createTask`, `listOverdueTasks`,
`getTask`, `updateTask`, `deleteTask`, `completeTask`, `archiveTask`, `restoreTask`.
No change to `pom.xml` (`spring-boot-starter-data-jpa-test`, `-webmvc-test`, `-validation` already there) or the properties files.

## Planned checks
- **Tests written before the code they test** (layer has `coverage`): T018 before T023, T019 before T026, T020 before T025, T021 before T027/T028.
- Check before relying on it (in the resolved jars, not from memory):
  - how Spring Data JPA (Boot 4.1.1's version) applies `Sort.Order.nullsLast()` with `findAll(Specification, Sort)` — whether `QueryUtils.toOrders` passes null precedence to the Criteria query on Hibernate 7, or the spec must add the order itself (`cb.asc/desc` + `Nulls.LAST` / a `case when dueDate is null` expression);
  - the `Specification` API shape in that Spring Data version (`toPredicate` signature, `Specification.where`/`and` / `allOf`, since it changed in Spring Data 4);
  - the `@DataJpaTest` / `@WebMvcTest` / `@MockitoBean` / `MockMvcTester` packages already used by phase-02 tests (reuse the same imports);
  - the exception MVC throws for a bad enum in a query param (`MethodArgumentTypeMismatchException`) and its `getName()`.
- Run `L.build` and `L.test`; save output to `loops/backend-dev/runs/001-quickflow/phase-03-build.log`.
- Copy `backend/target/openapi.json` to `loops/backend-dev/outputs/openapi.json` and compare the 9 task operations (paths, operationIds, parameters with enums/defaults, `Task`/`TaskCreate`/`TaskUpdate` properties, required lists, nullable types, 201/204 codes) with `contracts/openapi.yaml`; differences are logged, and any that can't be closed without unapproved annotations are raised as a question.
- JaCoCo: domain line coverage ≥ 0.80 over `com/quickflow/domain/**` (Task, TaskService, TaskSpecifications, enums, TaskQuery). `TaskSpecifications` and the overdue `@Query` are covered by T020 (`@DataJpaTest`, runs in `verify`).
- **Unit tests, one per business rule** (named with the rule id):
  - `TaskTest`: `br1_blankTitleRejected`, `br1_title201CharsRejected`, `br1_title200CharsAccepted`, `br2_description2001CharsRejected`, `br2_nullDescriptionAccepted`, `br3_nullStatusRejected`, `br3_nullPriorityRejected`, `br5_completeSetsDoneAndCompletedAt`, `fr01_4_leavingDoneClearsCompletedAt`, `fr01_3_defaultsTodoMediumNotArchived`, `fr01_7_overdueWhenDueYesterdayOpenNotArchived`, `fr01_7_notOverdueWhenDueToday`, `fr01_7_notOverdueWhenDone`, `fr01_7_notOverdueWhenArchived`, `fr01_7_notOverdueWithoutDueDate`.
  - `TaskServiceTest`: `fr01_2_createSetsCreatedAndUpdatedFromClock`, `fr01_2_updateChangesUpdatedAt`, `br4_archiveSetsArchived`, `br4_restoreClearsArchived`, `fr01_2_unknownIdNotFound` (parameterized over get/update/delete/complete/archive/restore).
  - `TaskRepositoryTest`: `fr02_1_titleSearchIgnoresCase`, `fr02_2_filtersCombine`, `fr02_2_dueRangeInclusive`, `fr02_3_sortDueDateNullsLastAsc`, `fr02_3_sortDueDateNullsLastDesc`, `fr02_3_sortCreatedAtBothDirections`, `br4_archivedExcludedByDefault`, `br4_archivedTrueListsArchived`, `fr01_7_overdueQuery`, `br14_deletedTaskNotReturned`.
  - `TaskControllerTest`: `createTaskReturns201Task`, `br1_blankTitleIs400WithTitleField`, `br3_unknownStatusIs400`, `getTaskUnknownIs404`, `listTasksBindsQueryWithDefaults`, `listTasksBindsAllParams`, `listOverdueTasks`, `completeArchiveRestoreReturn200`, `deleteTaskReturns204`.

## Risks
- **Coverage gate reads only package `com/quickflow/domain`**: the phase-02 report found the runner matches that exact JaCoCo package and ignores sub-packages (`scripts/lib/loopctl.py:656`), so domain coverage for `com/quickflow/domain/task` may again be reported as "skipped". I can't change `scripts/`; the tests above aim at ≥ 0.80 either way.
- **Nulls-last sort**: if Spring Data's `Sort` null handling isn't honoured by Hibernate/H2 through Specifications, the dueDate order would put nulls first in `desc` (H2 default). Caught by T020; fixed in `TaskSpecifications` (checked in the jars before coding).
- **Two validation paths for the same rule**: bean validation on the request records (for the swagger's `minLength`/`maxLength`) and the entity rules (BR-1/BR-2, the ones the domain tests cover). Both must name the same field (`title`, `description`); messages may differ. A blank-after-trim title (`"   "`) is caught by `@NotBlank` and by the entity.
- **Query-param type errors**: without A-10, `GET /api/tasks?status=FOO` gets Spring's default 400 body (not `application/problem+json`), which the contract's `BadRequest` response doesn't match.
- **springdoc output vs. contract**: nullable fields (`type: [string, 'null']`) and `format: date` on `LocalDate` / `date-time` on `OffsetDateTime` depend on springdoc 3.1.1's OpenAPI 3.1 mapping; 400/404 responses are not added by springdoc (phase-02 log: "no controller-advice error responses were added"), so the generated file lists only the success codes unless `@ApiResponse` annotations are added (not planned; logged as a difference).
- **Delete-while-referenced**: plan items (US4) reference tasks later; in this phase delete is a plain hard delete (R-5). The `sourceRemoved` handling belongs to phase-06.
- **`@DataJpaTest` in `com.quickflow.persistence`**: the test sits outside the entity's package; it finds `@SpringBootApplication` by walking up to `com.quickflow`, so entity scan still covers `domain.task`. It uses H2 in-memory (Boot's test DB replacement), not `./data/test`.

## Assumptions
- A-1: the stored title is the **trimmed** title ("trimmed non-blank"); the length check (≤ 200) applies to the trimmed value. Description is stored as given; an empty-string description is stored as `null`.
- A-2: `POST /api/tasks` accepts optional `status`/`priority` (contract `TaskCreate`); missing → TODO / MEDIUM. Creating with `status: DONE` sets `completedAt` (same rule as update).
- A-3: `PUT /api/tasks/{id}` replaces title, description, status, priority, dueDate (missing `description`/`dueDate` → cleared to null); it never changes `archived`; editing an archived task is allowed.
- A-4: request records carry `@NotBlank @Size(max = 200)` on `title` and `@Size(max = 2000)` on `description`, `@NotNull` on `status`/`priority` in `TaskUpdateRequest`, so the generated swagger matches the contract's constraints and invalid bodies are rejected before the service; the entity keeps its own checks (the domain tests are the BR tests).
- A-5: `complete` on a task that is already DONE keeps its original `completedAt` and still sets `updatedAt`; `archive`/`restore` on an already archived/restored task is a no-op apart from `updatedAt`. All return 200.
- A-6: `complete`, `archive`, `restore` and status changes all set `updatedAt` (data-model: "set on create and every change").
- A-7: `archived=true` lists **only** archived tasks (AC-US1-7 "listed by asking for archived tasks"); `archived=false` (default) only non-archived ones.
- A-8: `dueFrom`/`dueTo` exclude tasks without a due date; `q` blank or missing means no text filter; `%` and `_` in `q` are matched literally (escaped in the `like`). The `overdue` flag in list responses is computed with `TimeService.today()`.
- A-9: `sort=createdAt|dueDate` and `direction=asc|desc` bind as lower-case strings exactly as the contract's enums; an unknown value → 400 problem with `errors[{field: "sort"|"direction"}]`. Ties are broken by `id` in the same direction, so the order is stable. Null due dates are last in both directions.
- A-10: `ApiExceptionHandler` gets one more handler, `MethodArgumentTypeMismatchException` → 400 problem with `errors[{field: <parameter name>, message}]` (R-9: "unknown enum values → 400 with the offending field when known"), covered by `br3_unknownStatusQueryParamIs400` in `TaskControllerTest`. This touches a phase-02 file that no phase-03 task names.
- A-11: `GET /api/tasks/overdue` orders by `dueDate` ascending, ties by `id` ascending.
- A-12: `id` path values that aren't numbers get the same A-10 400; a missing id gives 404 `NotFoundException("Task <id> not found")`.

## Open questions
None.
