# phase-03: US1 Manage tasks
layer: backend
story_id: US1
spec_phase: 3
depends_on: [phase-02]
## Goal
Create, edit, complete, archive, restore, delete, search, filter and sort tasks; list overdue tasks.
## Acceptance criteria
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
- AC-US1-12: Given no tasks match (or none exist), When the Tasks page shows the list, Then an empty state guides the user to the Add Task action. (frontend layer)
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
