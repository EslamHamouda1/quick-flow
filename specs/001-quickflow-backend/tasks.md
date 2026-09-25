---

description: "Task list for QuickFlow backend"
---

# Tasks: QuickFlow (backend layer)

**Input**: Design documents from `specs/001-quickflow-backend/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Tests**: REQUESTED (Constitution III). In every story phase the tests come first and must fail before the
code exists. Every business rule and functional requirement has at least one named test, e.g.
`@DisplayName("BR-11: EndDateTime must be after StartDateTime")`. Three kinds: domain unit (plain JUnit 5 +
AssertJ, `Clock.fixed(Instant.parse("2026-09-24T09:00:00Z"), ZoneId.of("Africa/Cairo"))`, Mockito for
repositories, no Spring context); web slice (`@WebMvcTest` + `MockMvcTester`, services `@MockitoBean`);
persistence slice (`@DataJpaTest` on H2).

**Organization**: grouped by user story; each phase is one loop milestone.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1..US6 from spec.md
- Paths: `backend/src/main/java/com/quickflow/...` (main), `backend/src/test/java/com/quickflow/...` (tests).
  `backend/` is scaffolded by the runner (start.spring.io, Boot 4.1.1, Java 25, `web, data-jpa, h2, validation`)
  before the first implement session; no task creates it.
- Controller method names equal the `operationId`s in `contracts/openapi.yaml`, so the generated
  `target/openapi.json` matches the contract.
- Clarification-dependent tasks (Q1 BR-13 scope, Q2 plan Completed rule, Q3 settings content, Q4 plugin
  versions) follow the answers written in `spec.md` / the phase-00 review.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: build configuration, profiles, OpenAPI generation and coverage gate

- [X] T001 Check the scaffolded `backend/pom.xml` (starter artifact ids, Boot 4.1.1, `java.version` 25) and add `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1` plus test-scoped `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-jpa-test` (Boot-managed versions, keep `spring-boot-starter-test`) in backend/pom.xml (research R-1)
- [X] T002 Add `jacoco-maven-plugin` at the exact version answered for Q4 in backend/pom.xml with executions `prepare-agent`, `report` (phase `verify`) and `check` (phase `verify`, rule: BUNDLE limited to `com/quickflow/domain/**` classes, LINE COVEREDRATIO minimum 0.80)
- [X] T003 Add OpenAPI generation in backend/pom.xml: `spring-boot-maven-plugin` executions `start` (pre-integration-test, `<profiles>openapi</profiles>`) and `stop` (post-integration-test); `springdoc-openapi-maven-plugin` at the exact version answered for Q4, goal `generate` at `integration-test`, `apiDocsUrl` `http://localhost:18080/v3/api-docs`, `outputDir` `${project.build.directory}`, `outputFileName` `openapi.json` (research R-3; check the plugin parameter names in the pinned version)
- [X] T004 [P] Write backend/src/main/resources/application.properties: `spring.application.name=quickflow`, `spring.datasource.url=jdbc:h2:file:./data/quickflow`, `spring.jpa.hibernate.ddl-auto=update`, `spring.jpa.open-in-view=false`, `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`, `app.timezone=Africa/Cairo`, `springdoc.api-docs.path=/v3/api-docs`, `springdoc.swagger-ui.path=/swagger-ui`
- [X] T005 [P] Write backend/src/main/resources/application-test.properties: `spring.datasource.url=jdbc:h2:file:./data/test`, `spring.jpa.hibernate.ddl-auto=update`
- [X] T006 [P] Write backend/src/main/resources/application-openapi.properties: `server.port=18080`, `spring.datasource.url=jdbc:h2:mem:openapi;DB_CLOSE_DELAY=-1`, `spring.jpa.hibernate.ddl-auto=create-drop`
- [X] T007 [P] Add `data/` to backend/.gitignore so the H2 files are never committed

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: clock and time zone, error model, app info, OpenAPI metadata

**⚠️ CRITICAL**: no user story work can begin until this phase is complete

### Tests (write first)

- [X] T008 [P] Domain test backend/src/test/java/com/quickflow/domain/common/TimeServiceTest.java: with a fixed clock in `Africa/Cairo`, `today()` returns the Cairo date (also at 23:30 UTC, when Cairo is already on the next day), `now()` returns an `OffsetDateTime` with the Cairo offset, `zone()` is `Africa/Cairo` (Constitution IV)
- [X] T009 [P] Web-slice test backend/src/test/java/com/quickflow/web/ApiExceptionHandlerTest.java with a test-only controller: `ValidationException` → 400 `application/problem+json` with `title`, `status`, `errors[{field,message}]`; `NotFoundException` → 404; `ConflictException` → 409; malformed JSON and an unknown enum value → 400 (research R-9)
- [X] T010 [P] Web-slice test backend/src/test/java/com/quickflow/web/info/AppInfoControllerTest.java: `GET /api/app-info` → 200 `{timeZone: "Africa/Cairo", now: <ISO offset date-time>}` (TimeService mocked)

### Implementation

- [X] T011 Create `AppProperties` record (`@ConfigurationProperties("app")`, field `timezone`) in backend/src/main/java/com/quickflow/config/AppProperties.java and enable it (`@ConfigurationPropertiesScan`) in backend/src/main/java/com/quickflow/QuickflowApplication.java
- [X] T012 Create `ClockConfig` exposing `@Bean Clock clock(AppProperties p)` = `Clock.system(ZoneId.of(p.timezone()))` in backend/src/main/java/com/quickflow/config/ClockConfig.java
- [X] T013 [P] Create `NotFoundException`, `ConflictException` and `ValidationException` (list of `FieldError(field, message)`) in backend/src/main/java/com/quickflow/domain/common/
- [X] T014 Create `TimeService` (`now()`, `today()`, `zone()`, `toOffset(Instant)`, all from the injected `Clock`) in backend/src/main/java/com/quickflow/domain/common/TimeService.java
- [X] T015 Create `ApiExceptionHandler` (`@RestControllerAdvice`, RFC 9457 `ProblemDetail`, property `errors`) in backend/src/main/java/com/quickflow/web/ApiExceptionHandler.java (check the Jackson 3 exception types for unknown enum values in the resolved jars)
- [X] T016 Create `AppInfoResponse` record and `AppInfoController` (`getAppInfo`, `GET /api/app-info`) in backend/src/main/java/com/quickflow/web/info/
- [X] T017 [P] Create `OpenApiConfig` (title "QuickFlow API", version "0.1.0") in backend/src/main/java/com/quickflow/config/OpenApiConfig.java

**Checkpoint**: foundation ready; `./mvnw verify` writes `target/openapi.json` containing `/api/app-info`

---

## Phase 3: User Story 1 - Manage tasks (Priority: P1) 🎯 MVP

**Goal**: create, edit, complete, archive, restore, delete, search, filter, sort tasks; list overdue tasks

**Independent Test**: via `/api/tasks*` only: create → 201; invalid title → 400; complete → DONE; archive hides from the default list; search/filters/sort; overdue list; delete → 404 afterwards

### Tests for User Story 1 (write first) ⚠️

- [X] T018 [P] [US1] Domain test backend/src/test/java/com/quickflow/domain/task/TaskTest.java: `BR-1` (blank title and 201 chars rejected with field `title`, 200 accepted), `BR-2` (2,001-char description rejected with field `description`, null accepted), `BR-3` (null status or priority rejected), `BR-5` (complete → DONE with `completedAt`; leaving DONE clears `completedAt`, FR-01.4), `FR-01.3` (defaults TODO / MEDIUM / not archived), `FR-01.7` overdue (due yesterday & not DONE & not archived → true; due today, DONE, archived, no due date → false) with the fixed clock
- [X] T019 [P] [US1] Domain test backend/src/test/java/com/quickflow/domain/task/TaskServiceTest.java (Mockito `TaskRepository`, fixed clock): `FR-01.2` create sets `createdAt`/`updatedAt` from the clock, update changes `updatedAt`, `BR-4` archive/restore toggle `archived`, `FR-01.2` unknown id → `NotFoundException` for get/update/delete/complete/archive/restore
- [X] T020 [P] [US1] Persistence test backend/src/test/java/com/quickflow/persistence/TaskRepositoryTest.java (`@DataJpaTest`): `FR-02.1` title search ignores case, `FR-02.2` status/priority/dueFrom–dueTo (inclusive)/archived filters combine, `FR-02.3` sort by dueDate (nulls last) and createdAt both directions, `BR-4` archived excluded by default, `FR-01.7` overdue query, `BR-14` a deleted task is not returned by any query
- [X] T021 [P] [US1] Web-slice test backend/src/test/java/com/quickflow/web/task/TaskControllerTest.java (`@MockitoBean TaskService`): `createTask` 201 body shape, `BR-1` 400 with `errors[].field == "title"`, `BR-3` 400 for `status: "FOO"`, `getTask` 404, `listTasks` binds `q,status,priority,dueFrom,dueTo,archived,sort,direction` into `TaskQuery` (defaults `archived=false, sort=createdAt, direction=desc`), `listOverdueTasks`, `completeTask`/`archiveTask`/`restoreTask` 200, `deleteTask` 204

### Implementation for User Story 1

- [X] T022 [P] [US1] Create enums `TaskStatus {TODO, IN_PROGRESS, DONE}` and `TaskPriority {LOW, MEDIUM, HIGH}` in backend/src/main/java/com/quickflow/domain/task/
- [X] T023 [US1] Create JPA entity `Task` in backend/src/main/java/com/quickflow/domain/task/Task.java: `id` Long identity; `title` "required, trimmed non-blank, ≤ 200 (BR-1)"; `description` "≤ 2,000 (BR-2)"; `status` "required, default TODO (BR-3)"; `priority` "required, default MEDIUM (BR-3)"; `dueDate` LocalDate optional; `createdAt`, `updatedAt`, `completedAt` Instant; `archived` default false; methods `complete(now)`, `changeStatus(status, now)`, `archive(now)`, `restore(now)`, `isOverdue(today)`; rule violations throw `ValidationException`
- [X] T024 [P] [US1] Create `TaskQuery` record (q, status, priority, dueFrom, dueTo, archived, sort, direction) in backend/src/main/java/com/quickflow/domain/task/TaskQuery.java
- [X] T025 [US1] Create `TaskRepository` (`JpaRepository<Task, Long>`, `JpaSpecificationExecutor<Task>`, overdue query `dueDate < :today and status <> DONE and archived = false` ordered by dueDate) and `TaskSpecifications` building the `TaskQuery` filters and sort (nulls last on dueDate) in backend/src/main/java/com/quickflow/domain/task/
- [X] T026 [US1] Create `TaskService` (`create, get, list(TaskQuery), overdue, update, complete, archive, restore, delete`, time from `TimeService`) in backend/src/main/java/com/quickflow/domain/task/TaskService.java
- [X] T027 [P] [US1] Create records `TaskCreateRequest`, `TaskUpdateRequest`, `TaskResponse` (fields per contract `TaskCreate`, `TaskUpdate`, `Task`, including computed `overdue`) in backend/src/main/java/com/quickflow/web/task/
- [X] T028 [US1] Create `TaskController` with methods `listTasks, createTask, listOverdueTasks, getTask, updateTask, deleteTask, completeTask, archiveTask, restoreTask` mapped as in contracts/openapi.yaml in backend/src/main/java/com/quickflow/web/task/TaskController.java

**Checkpoint**: US1 works on its own

---

## Phase 4: User Story 2 - Track recurring habits (Priority: P1)

**Goal**: daily/weekly habits, one completion per date, progress and streak, deactivate/activate, delete

**Independent Test**: via `/api/habits*` only: create DAILY and WEEKLY, complete today → 201, again → 409, streak and `completedToday`, deactivate then complete → 409, delete → 404

### Tests for User Story 2 (write first) ⚠️

- [X] T029 [P] [US2] Domain test backend/src/test/java/com/quickflow/domain/habit/HabitTest.java: `BR-6` (blank name and 151 chars rejected with field `name`, 150 accepted), `FR-03.3` null frequency rejected, `FR-03.1` description > 2,000 rejected, `FR-03.2` new habit active, `deactivate`/`activate`
- [X] T030 [P] [US2] Domain test backend/src/test/java/com/quickflow/domain/habit/HabitProgressCalculatorTest.java (fixed clock): `FR-04.5` DAILY `completedToday`, `doneForCurrentPeriod`, streak counting back from today, or from yesterday when today is not done, broken by a gap; WEEKLY period = Monday–Sunday week (spec A-12), streak over consecutive weeks, done for the current week by any date in it
- [X] T031 [P] [US2] Domain test backend/src/test/java/com/quickflow/domain/habit/HabitServiceTest.java (Mockito repositories, fixed clock): `FR-04.1` completion defaults to today, future date → `ValidationException` (field `date`); `BR-7` existing (habit, date) → `ConflictException`, and a `DataIntegrityViolationException` from save → `ConflictException`; `FR-04.4` inactive habit → `ConflictException`; `FR-04.3` undo missing completion → `NotFoundException`; `FR-03.2` unknown habit → `NotFoundException`
- [X] T032 [P] [US2] Persistence test backend/src/test/java/com/quickflow/persistence/HabitRepositoryTest.java: `BR-7` unique constraint rejects a second (habit, date) row, `BR-14` deleting a habit deletes its completions and neither is returned, `FR-03.2` active filter
- [X] T033 [P] [US2] Web-slice test backend/src/test/java/com/quickflow/web/habit/HabitControllerTest.java: `createHabit` 201, `BR-6` 400 `errors[].field == "name"`, unknown frequency 400, `listHabits?active=true`, `completeHabit` 201 with and without body, `BR-7` 409 problem body, `undoHabitCompletion` 204, `deactivateHabit`/`activateHabit` 200, `deleteHabit` 204, `getHabit` 404

### Implementation for User Story 2

- [X] T034 [P] [US2] Create enum `HabitFrequency {DAILY, WEEKLY}` in backend/src/main/java/com/quickflow/domain/habit/HabitFrequency.java
- [X] T035 [US2] Create entity `Habit` in backend/src/main/java/com/quickflow/domain/habit/Habit.java: `name` "required, non-blank, ≤ 150 (BR-6)"; `description` "≤ 2,000"; `frequency` required; `createdAt` Instant; `active` default true; `@OneToMany(mappedBy="habit", cascade=ALL, orphanRemoval=true)` completions; `activate()`/`deactivate()`
- [X] T036 [US2] Create entity `HabitCompletion` (`habit` ManyToOne not null, `completionDate` LocalDate, `createdAt`; `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"habit_id", "completion_date"}))`) in backend/src/main/java/com/quickflow/domain/habit/HabitCompletion.java
- [X] T037 [US2] Create `HabitRepository` (find by active) and `HabitCompletionRepository` (exists by habit and date, find by habit ordered by date desc, find dates by habit) in backend/src/main/java/com/quickflow/domain/habit/
- [X] T038 [US2] Create pure `HabitProgressCalculator` (`progress(frequency, completionDates, today)` → `HabitProgress(completedToday, doneForCurrentPeriod, currentStreak)`) in backend/src/main/java/com/quickflow/domain/habit/HabitProgressCalculator.java
- [X] T039 [US2] Create `HabitService` (`create, get, list(active), update, activate, deactivate, delete, complete(id, date), undo(id, date), completions(id)`) in backend/src/main/java/com/quickflow/domain/habit/HabitService.java
- [X] T040 [P] [US2] Create records `HabitWriteRequest`, `HabitResponse`, `HabitCompletionCreateRequest`, `HabitCompletionResponse` (per contract) in backend/src/main/java/com/quickflow/web/habit/
- [X] T041 [US2] Create `HabitController` with `listHabits, createHabit, getHabit, updateHabit, deleteHabit, deactivateHabit, activateHabit, listHabitCompletions, completeHabit, undoHabitCompletion` in backend/src/main/java/com/quickflow/web/habit/HabitController.java

**Checkpoint**: US1 and US2 work independently

---

## Phase 5: User Story 3 - Track learning resources with milestones and notes (Priority: P2)

**Goal**: learning cards with status, milestones (done flag, target date) and notes

**Independent Test**: via `/api/learning-cards*` only: create card, empty title → 400, add/complete/remove milestones, add/remove notes, delete card → 404 for everything under it

### Tests for User Story 3 (write first) ⚠️

- [X] T042 [P] [US3] Domain test backend/src/test/java/com/quickflow/domain/learning/LearningCardTest.java: `BR-8` blank title rejected (field `title`), title > 200 rejected, `FR-05.1` description > 2,000 rejected, default status NOT_STARTED, null status rejected; `FR-06.1` milestone title required and ≤ 200, new milestone not done; `FR-06.2` note text required and ≤ 5,000
- [X] T043 [P] [US3] Domain test backend/src/test/java/com/quickflow/domain/learning/LearningServiceTest.java (Mockito, fixed clock): `FR-05.2` create/update/get/delete, unknown card → `NotFoundException`; `BR-9` updating or deleting a milestone/note through another card's id → `NotFoundException`; `FR-06.3` milestone done/un-done, note `createdAt` from the clock
- [X] T044 [P] [US3] Persistence test backend/src/test/java/com/quickflow/persistence/LearningCardRepositoryTest.java: `BR-9` milestone row requires a card, `BR-14` deleting a card deletes its milestones and notes and none is returned, milestone done/total counts
- [X] T045 [P] [US3] Web-slice test backend/src/test/java/com/quickflow/web/learning/LearningCardControllerTest.java: `createLearningCard` 201, `BR-8` 400 `errors[].field == "title"`, unknown status 400, `listLearningCards` includes milestones and notes, `addMilestone` 201, `updateMilestone` 200, `deleteMilestone` 204, `addNote` 201, `deleteNote` 204, `getLearningCard` 404, `deleteLearningCard` 204

### Implementation for User Story 3

- [X] T046 [P] [US3] Create enum `LearningStatus {NOT_STARTED, IN_PROGRESS, COMPLETED}` in backend/src/main/java/com/quickflow/domain/learning/LearningStatus.java
- [X] T047 [US3] Create entity `LearningCard` in backend/src/main/java/com/quickflow/domain/learning/LearningCard.java: `title` "required, non-blank (BR-8), ≤ 200"; `description` "≤ 2,000"; `status` default NOT_STARTED; `createdAt`; `@OneToMany(mappedBy="card", cascade=ALL, orphanRemoval=true)` milestones (ordered by id) and notes (ordered by createdAt)
- [X] T048 [P] [US3] Create entity `LearningMilestone` (`card` ManyToOne `nullable=false` (BR-9), `title` "required, ≤ 200", `done` default false, `targetDate` LocalDate optional) in backend/src/main/java/com/quickflow/domain/learning/LearningMilestone.java
- [X] T049 [P] [US3] Create entity `LearningNote` (`card` ManyToOne not null, `text` "required, non-blank, ≤ 5,000 (A-9)", `createdAt`) in backend/src/main/java/com/quickflow/domain/learning/LearningNote.java
- [X] T050 [US3] Create `LearningCardRepository`, `LearningMilestoneRepository` (find by id and card id), `LearningNoteRepository` (find by id and card id) in backend/src/main/java/com/quickflow/domain/learning/
- [X] T051 [US3] Create `LearningService` (`create, get, list, update, delete, addMilestone, updateMilestone, deleteMilestone, addNote, deleteNote`) in backend/src/main/java/com/quickflow/domain/learning/LearningService.java
- [X] T052 [P] [US3] Create records `LearningCardCreateRequest`, `LearningCardUpdateRequest`, `LearningCardResponse` (with `milestonesDone`, `milestonesTotal`), `MilestoneCreateRequest`, `MilestoneUpdateRequest`, `MilestoneResponse`, `NoteCreateRequest`, `NoteResponse` in backend/src/main/java/com/quickflow/web/learning/
- [X] T053 [US3] Create `LearningCardController` with `listLearningCards, createLearningCard, getLearningCard, updateLearningCard, deleteLearningCard, addMilestone, updateMilestone, deleteMilestone, addNote, deleteNote` in backend/src/main/java/com/quickflow/web/learning/LearningCardController.java

**Checkpoint**: US1–US3 work independently

---

## Phase 6: User Story 4 - Build and follow Todo Plans (Priority: P2)

**Goal**: plans built from existing tasks/habits/cards, item done toggles, computed status, progress and rest time

**Independent Test**: with one task, habit and card present: create plan → 201; no items / unknown source / end ≤ start → 400; tick items → progress and (TASK item) task DONE; status and `restSeconds` before, during and after the window; delete → 204

### Tests for User Story 4 (write first) ⚠️

- [X] T054 [P] [US4] Domain test backend/src/test/java/com/quickflow/domain/plan/PlanStatusPolicyTest.java (fixed clocks before, at and after start/end): `FR-08.4` NOT_STARTED before start, IN_PROGRESS inside the window, COMPLETED per the Q2 answer (recommended: at/after end, or all items done once started; never before start); `BR-12` `restSeconds` = seconds to end only when start ≤ now < end, else null; `FR-08.3` progress = floor(100·done/total) (0/3 → 0, 1/3 → 33, 3/3 → 100); `NFR-4` the same stored values and time always give the same status
- [X] T055 [P] [US4] Domain test backend/src/test/java/com/quickflow/domain/plan/PlanTest.java: `BR-10` no items rejected (field `items`), `BR-11` end equal to or before start rejected (field `endDateTime`), `FR-07.1` blank or > 200-char title, duration outside 1..100,000, priorityOrder < 1 rejected, `FR-07.2` the same (type, id) twice rejected, new items not done
- [X] T056 [P] [US4] Domain test backend/src/test/java/com/quickflow/domain/plan/PlanServiceTest.java (Mockito repositories and `PlanSourceResolver`, fixed clock): `BR-10` unknown/deleted source → `ValidationException` (field `items[i].sourceId`); `BR-13` per the Q1 answer (recommended: TASK item done → `TaskService.complete`; HABIT and LEARNING_RESOURCE items and un-ticking change no source); `FR-08.5` toggling returns the new progress; `FR-07.6` item whose source was deleted → `sourceRemoved=true`, keeps `sourceTitle`, still counted; `BR-14` deleting a plan leaves its sources untouched; `FR-07.3` update edits fields and re-checks BR-11; list grouping/ordering (active by priorityOrder then start; completed by end desc)
- [X] T057 [P] [US4] Persistence test backend/src/test/java/com/quickflow/persistence/PlanRepositoryTest.java: items saved and loaded with the plan, `FR-07.2` unique (plan, sourceType, sourceId), `BR-14` deleting a plan deletes its items and neither is returned
- [X] T058 [P] [US4] Web-slice test backend/src/test/java/com/quickflow/web/plan/PlanControllerTest.java: `createPlan` 201 body shape (status, progressPercent, restSeconds, items), `BR-10` 400 empty items, `BR-11` 400 `errors[].field == "endDateTime"`, offset date-time parsing, `listPlans?group=active|completed|all`, `getPlan` 404, `updatePlan` 200, `setPlanItemDone` 200 returns the plan, `deletePlan` 204

### Implementation for User Story 4

- [X] T059 [P] [US4] Create enums `PlanStatus {NOT_STARTED, IN_PROGRESS, COMPLETED}` and `PlanSourceType {TASK, HABIT, LEARNING_RESOURCE}` in backend/src/main/java/com/quickflow/domain/plan/
- [X] T060 [US4] Create entity `PlanItem` (`plan` ManyToOne not null, `sourceType`, `sourceId`, `sourceTitle`, `done` default false; unique `(plan_id, source_type, source_id)`) in backend/src/main/java/com/quickflow/domain/plan/PlanItem.java
- [X] T061 [US4] Create entity `Plan` in backend/src/main/java/com/quickflow/domain/plan/Plan.java: `title` "required, ≤ 200"; `estimatedDurationMinutes` "1..100,000"; `startDateTime`, `endDateTime` Instant, "end after start (BR-11)"; `priorityOrder` "≥ 1, 1 = highest, not unique"; `createdAt`; items "≥ 1 (BR-10), cascade ALL, orphan removal"; no stored status
- [X] T062 [US4] Create pure `PlanStatusPolicy` (`statusAt`, `restSeconds`, `progressPercent`) and `PlanProgress` record in backend/src/main/java/com/quickflow/domain/plan/ (research R-6, Q2)
- [X] T063 [US4] Create `PlanRepository` in backend/src/main/java/com/quickflow/domain/plan/PlanRepository.java
- [X] T064 [US4] Create `PlanSourceResolver` (looks up TASK/HABIT/LEARNING_RESOURCE by id through their repositories: exists + current title) in backend/src/main/java/com/quickflow/domain/plan/PlanSourceResolver.java
- [X] T065 [US4] Create `PlanService` (`create, get, list(group), update, delete, setItemDone` with BR-13 propagation per Q1 via `TaskService.complete`; `PlanView` with computed status/progress/rest time and refreshed `sourceTitle`/`sourceRemoved`) in backend/src/main/java/com/quickflow/domain/plan/PlanService.java
- [X] T066 [P] [US4] Create records `PlanCreateRequest`, `PlanUpdateRequest`, `PlanItemRefRequest`, `PlanItemUpdateRequest`, `PlanResponse`, `PlanItemResponse` (date-times as `OffsetDateTime` in the app zone) in backend/src/main/java/com/quickflow/web/plan/
- [X] T067 [US4] Create `PlanController` with `listPlans, createPlan, getPlan, updatePlan, deletePlan, setPlanItemDone` in backend/src/main/java/com/quickflow/web/plan/PlanController.java

**Checkpoint**: US1–US4 work; plans reflect their items

---

## Phase 7: User Story 5 - See everything on the Dashboard (Priority: P3)

**Goal**: one `GET /api/dashboard` computed from current data

**Independent Test**: with known data, every Dashboard list and number matches a manual count

### Tests for User Story 5 (write first) ⚠️

- [X] T068 [P] [US5] Domain test backend/src/test/java/com/quickflow/domain/dashboard/DashboardServiceTest.java (Mockito services/repositories, fixed clock): `FR-09.1` dueToday, overdue, completedToday (completedAt on today in Cairo, non-archived), `AC-US5-3` taskCompletionPercent floor(100·DONE/non-archived) and 0 with no tasks, habit counts (active, completedToday), activePlans only IN_PROGRESS ordered by priority, plan counts, learning snapshot (cards per status, milestones done/total); `FR-08.5` after a plan item toggle the next summary changes; `FR-09.2` figures computed on each call
- [X] T069 [P] [US5] Persistence test backend/src/test/java/com/quickflow/persistence/DashboardQueriesTest.java: task due-today and completed-between queries, archived tasks excluded (`BR-4`), deleted rows never counted (`BR-14`), card counts per status and milestone done/total counts
- [X] T070 [P] [US5] Web-slice test backend/src/test/java/com/quickflow/web/dashboard/DashboardControllerTest.java: `getDashboard` 200 with every field of contract schema `Dashboard`

### Implementation for User Story 5

- [X] T071 [US5] Add the count/list queries the Dashboard needs (tasks due on a date, completed between two instants, non-archived totals by status; cards per status; milestone done/total) to backend/src/main/java/com/quickflow/domain/task/TaskRepository.java and backend/src/main/java/com/quickflow/domain/learning/LearningMilestoneRepository.java / LearningCardRepository.java
- [X] T072 [P] [US5] Create `DashboardSummary` record (and nested count records) in backend/src/main/java/com/quickflow/domain/dashboard/DashboardSummary.java
- [X] T073 [US5] Create `DashboardService` in backend/src/main/java/com/quickflow/domain/dashboard/DashboardService.java
- [X] T074 [US5] Create `DashboardResponse` record and `DashboardController` (`getDashboard`, `GET /api/dashboard`) in backend/src/main/java/com/quickflow/web/dashboard/

**Checkpoint**: Dashboard matches the data of US1–US4

---

## Phase 8: User Story 6 - Adjust settings (Priority: P3)

**Goal**: profile information and preferences kept between sessions (content per the Q3 answer)

**Independent Test**: `PUT /api/settings`, restart, `GET /api/settings` returns the saved values

### Tests for User Story 6 (write first) ⚠️

- [X] T075 [P] [US6] Domain test backend/src/test/java/com/quickflow/domain/settings/SettingsServiceTest.java: `FR-10.2` first read returns defaults (recommended Q3: `displayName` null, `planStartNotifications` true, `defaultPage` DASHBOARD) and stores them; update saves; `displayName` > 100 rejected (field `displayName`); null `defaultPage` rejected
- [X] T076 [P] [US6] Persistence test backend/src/test/java/com/quickflow/persistence/SettingsRepositoryTest.java: `NFR-3` the single settings row (id 1) is saved and read back
- [X] T077 [P] [US6] Web-slice test backend/src/test/java/com/quickflow/web/settings/SettingsControllerTest.java: `getSettings` 200 with exactly the contract `Settings` fields (`displayName`, `planStartNotifications`, `defaultPage`), `updateSettings` 200, unknown `defaultPage` 400, too-long `displayName` 400

### Implementation for User Story 6

- [X] T078 [P] [US6] Create enum `DefaultPage {DASHBOARD, TASKS, HABITS, LEARNING, PLANS, SETTINGS}` and entity `AppSettings` (`id` always 1, `displayName` "≤ 100", `planStartNotifications` default true, `defaultPage` default DASHBOARD) in backend/src/main/java/com/quickflow/domain/settings/
- [X] T079 [US6] Create `SettingsRepository` in backend/src/main/java/com/quickflow/domain/settings/SettingsRepository.java
- [X] T080 [US6] Create `SettingsService` (`get()` transactional read-or-create of the default row, so it works on an empty database; `update(...)`) in backend/src/main/java/com/quickflow/domain/settings/SettingsService.java
- [X] T081 [US6] Create `SettingsDto` record and `SettingsController` (`getSettings`, `updateSettings`) in backend/src/main/java/com/quickflow/web/settings/

**Checkpoint**: all six stories work

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: contract match, performance, coverage, traceability

- [ ] T083 Compare backend/target/openapi.json (from `./mvnw verify`) with specs/001-quickflow-backend/contracts/openapi.yaml (paths, methods, operationIds, status codes, schema fields) and fix the code (annotations, record fields, method names) where they differ; list any deliberate difference in specs/001-quickflow-backend/research.md
- [ ] T084 [P] Add indexes for NFR-1 (`task(archived, due_date)`, `task(status)`, `habit_completion(habit_id, completion_date)` is covered by the unique constraint, `plan_item(plan_id)`) via `@Table(indexes=...)` in backend/src/main/java/com/quickflow/domain/task/Task.java and backend/src/main/java/com/quickflow/domain/plan/PlanItem.java
- [ ] T085 Bring line coverage of `com.quickflow.domain` to ≥ 80% (`jacoco:check`) by adding missing domain tests under backend/src/test/java/com/quickflow/domain/
- [ ] T086 [P] Check that every BR-1..BR-14 and every backend FR id (all FR-01.x..FR-10.x except the UI-only FR-08.1 notification and FR-10.1 navigation) appears in at least one `@DisplayName` under backend/src/test/java/com/quickflow/ and add the missing tests
- [ ] T087 [P] Update specs/001-quickflow-backend/quickstart.md if any command, profile or path changed during implementation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none (the runner scaffolds `backend/` first)
- **Foundational (Phase 2)**: after Setup; blocks all stories
- **US1 (Phase 3)**, **US2 (Phase 4)**, **US3 (Phase 5)**, **US6 (Phase 8)**: after Foundational, independent of each other
- **US4 (Phase 6)**: after Foundational + US1, US2, US3 (plan items reference their entities; BR-13 calls `TaskService`)
- **US5 (Phase 7)**: after Foundational + US1–US4 (summarises all of them)
- **Polish (Phase 9)**: after every story phase

### Within Each User Story

- Tests first (they must fail), then enums/entities, repositories, pure domain calculators, services, DTOs, controllers
- US6 touches no other story's files (the Dashboard greeting reads `GET /api/settings` directly)

### Parallel Opportunities

- Setup T004–T007; Foundational tests T008–T010 and T013, T017
- In every story: all test tasks [P]; enums and request/response records [P]
- Entities of the same story that are separate files (T048, T049)

---

## Parallel Example: User Story 4

```text
Task: "Domain test PlanStatusPolicyTest in backend/src/test/java/com/quickflow/domain/plan/PlanStatusPolicyTest.java"
Task: "Domain test PlanTest in backend/src/test/java/com/quickflow/domain/plan/PlanTest.java"
Task: "Domain test PlanServiceTest in backend/src/test/java/com/quickflow/domain/plan/PlanServiceTest.java"
Task: "Persistence test PlanRepositoryTest in backend/src/test/java/com/quickflow/persistence/PlanRepositoryTest.java"
Task: "Web-slice test PlanControllerTest in backend/src/test/java/com/quickflow/web/plan/PlanControllerTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup → Phase 2 Foundational → Phase 3 US1
2. The testing loop verifies US1 (curl + unit-test run) before the next phase

### Incremental Delivery

Setup → Foundational → US1 → US2 → US3 → US4 → US5 → US6 → Polish; each phase is one milestone
(review → implement → test → close) and leaves the API working.

---

## Notes

- [P] tasks = different files, no dependencies
- Tests carry the rule/requirement id in `@DisplayName`
- Dev sessions never verify their own work; the testing loop does
