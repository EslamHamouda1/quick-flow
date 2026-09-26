---

description: "Task list for Task tags (backend layer)"
---

# Tasks: Task tags (backend layer)

**Input**: Design documents from `specs/002-us-tags-backend/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Tests**: REQUESTED (Constitution III). In the story phase the tests come first and must fail before
the code exists. Every rule BR-T1..BR-T4 and requirement FR-001..FR-008 has at least one named test,
e.g. `@DisplayName("BR-T1: a tag of 31 characters is rejected")`. Three kinds: domain unit (plain
JUnit 5 + AssertJ, the fixed clock already used in `TaskTest`, Mockito for repositories, no Spring
context); web slice (`@WebMvcTest` + `MockMvcTester`, services `@MockitoBean`); persistence slice
(`@DataJpaTest` on H2).

**Organization**: one user story, one phase, plus Polish; each phase is one loop milestone.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 from spec.md (US-TAGS-1)
- Paths: `backend/src/main/java/com/quickflow/...` (main), `backend/src/test/java/com/quickflow/...` (tests).
  The backend exists already (feature `001-quickflow`); no setup or foundational work is needed.
- Controller method names equal the `operationId`s in `contracts/openapi.yaml` (`listTasks`, `getTask`,
  `addTaskTags`, `removeTaskTag`), so the generated `target/openapi.json` matches the contract.
- Tasks marked (Q1)/(Q2)/(Q3) follow the answers to the spec's questions (research R-2, R-5, R-6);
  the reviewer confirmed the recommended answers at the phase-00 review.

---

## Phase 1: User Story 1 - Tag tasks and filter by tag (Priority: P1) 🎯 MVP

**Goal**: add tags to a task, read them back, filter the task list by tag, remove a tag, with
validation (1–30 characters, case-insensitive, at most 10).

**Independent Test**: quickstart.md scenarios AC-US1-1..AC-US1-6 and FR-008 with curl against
`contracts/openapi.yaml`.

### Tests for User Story 1 (write first, they must fail)

- [X] T001 [P] [US1] Domain tests in backend/src/test/java/com/quickflow/domain/task/TaskTest.java: `addTags(List.of("work","urgent"), now)` gives `getTags()` = `[urgent, work]` (FR-001, FR-002, alphabetical); "  Work " is stored as "work" (BR-T1 trim, BR-T2 / Q1 lower case); adding "Work" to a task tagged "work" leaves one tag and doesn't change `updatedAt` (BR-T2, Q2 no-op, R-8); `["work","WORK"]` in one call adds one tag; `""`, `"   "`, `null` and a 31-character tag are each rejected with `ValidationException` on field `tags` and leave the tags unchanged, a 30-character tag is accepted (BR-T1, FR-006); a batch with one invalid tag adds none (all or nothing); an 11th distinct tag is rejected on `tags` with the 10 kept, and adding an existing tag to a task with 10 tags is accepted (BR-T3, FR-007); `removeTag("WORK", now)` removes "work" and sets `updatedAt` (FR-005, R-8); removing an absent tag changes nothing (Q3 no-op); a blank value for `removeTag` is rejected on `tag`; `update`, `changeStatus`, `complete`, `archive` and `restore` keep the tags (BR-T4)
- [X] T002 [P] [US1] Domain tests in backend/src/test/java/com/quickflow/domain/task/TaskServiceTest.java (repository mocked): `addTags(id, tags)` adds them to the found task and returns it; `removeTag(id, tag)` removes it and returns the task; both throw `NotFoundException` for an unknown id (FR-001, FR-005); the `updatedAt` comes from `TimeService` (Constitution IV)
- [X] T003 [P] [US1] Persistence tests in backend/src/test/java/com/quickflow/persistence/TaskRepositoryTest.java: tags survive flush + clear and reload (FR-008); `findAll(TaskSpecifications.matching(query))` with `tag` "work" lists only tasks tagged "work" (AC-US1-2), with "WORK" or " work " too (BR-T2), not tasks tagged "workshop" (whole tag), each task once when it has several tags, combined with a status filter by AND, archived tasks excluded by default (FR-004, BR-4); a removed tag no longer matches (AC-US1-3); deleting a tagged task removes its `task_tag` rows and it is never listed by the tag filter (BR-T4, BR-14); add the new `tag` argument (null) to the existing `new TaskQuery(...)` calls in this file
- [X] T004 [P] [US1] Web-slice tests in backend/src/test/java/com/quickflow/web/task/TaskControllerTest.java (`TaskService` mocked): `POST /api/tasks/{id}/tags` `{"tags":["work","urgent"]}` → 200 Task with `tags` (AC-US1-1); body `{}`, `{"tags":[]}` → 400 problem with `errors[].field` = `tags`; service `ValidationException("tags", ...)` → 400 with the same shape (AC-US1-4, AC-US1-6); `NotFoundException` → 404; `DELETE /api/tasks/{id}/tags?tag=work` → 200 Task (AC-US1-3), missing `tag` → 400, 404 for an unknown id; `GET /api/tasks?tag=work` passes a `TaskQuery` with `tag` "work" (FR-004); `GET /api/tasks/{id}` JSON has a `tags` array (FR-002); add the new `tag` argument to the existing expected `TaskQuery`s in this file

### Implementation for User Story 1

- [X] T005 [US1] In backend/src/main/java/com/quickflow/domain/task/Task.java add `tags`: `@ElementCollection(fetch = FetchType.EAGER)` of `String`, `@CollectionTable(name = "task_tag", joinColumns = @JoinColumn(name = "task_id"), uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "tag"}), indexes = @Index(name = "idx_task_tag_tag", columnList = "tag"))`, `@Column(name = "tag", nullable = false, length = 30)`; constants `TAG_MAX = 30`, `TAGS_MAX = 10`; `addTags(List<String>, Instant now)` (trim, lower case with `Locale.ROOT`, "tag must not be blank" / "must be at most 30 characters" / "must have at most 10 tags" on field `tags`, all or nothing, `updatedAt = now` only if the set changed); `removeTag(String, Instant now)` ("must not be blank" on field `tag`; absent tag = no-op); `getTags()` returns an unmodifiable alphabetically sorted list (data-model.md, research R-1, R-2, R-4..R-6, R-8)
- [X] T006 [US1] Add `String tag` to backend/src/main/java/com/quickflow/domain/task/TaskQuery.java (after `archived`) and in backend/src/main/java/com/quickflow/domain/task/TaskSpecifications.java add the tag predicate: when `tag` is not blank, an `EXISTS` subquery on the task's `tags` equal to the trimmed lower-case value, ANDed with the other predicates, existing order kept; first check that `findAll(Specification)` passes a non-null `CriteriaQuery` in the Boot-managed Spring Data JPA version (research R-7)
- [X] T007 [US1] Add `addTags(long id, List<String> tags)` and `removeTag(long id, String tag)` to backend/src/main/java/com/quickflow/domain/task/TaskService.java (load with `get(id)`, call the entity, time from `TimeService`)
- [X] T008 [P] [US1] Create backend/src/main/java/com/quickflow/web/task/TaskTagsRequest.java: `@Schema(name = "TaskTags") record TaskTagsRequest(@Schema(requiredMode = REQUIRED) @NotNull @Size(min = 1) List<String> tags)` (contract `TaskTags`; no per-item constraint: blank and length rules apply after trimming and stay in the domain, BR-T1)
- [X] T009 [US1] Add `@Schema(requiredMode = REQUIRED) List<String> tags` to backend/src/main/java/com/quickflow/web/task/TaskResponse.java, filled from `task.getTags()` (contract `Task.tags`)
- [X] T010 [US1] In backend/src/main/java/com/quickflow/web/task/TaskController.java: add the optional `tag` query parameter to `listTasks` (passed into `TaskQuery`); add `addTaskTags` (`POST /{id}/tags`, `@Valid @RequestBody TaskTagsRequest`, 200 Task) and `removeTaskTag` (`DELETE /{id}/tags`, required `@RequestParam String tag`, 200 Task) with `@Operation` summaries from `contracts/openapi.yaml`; a missing required `tag` must give the 400 problem shape (add a handler for `MissingServletRequestParameterException` in backend/src/main/java/com/quickflow/web/ApiExceptionHandler.java only if the web-slice test shows it isn't mapped yet)

**Checkpoint**: `cd backend && ./mvnw -q verify` passes; US1 is testable with curl.

---

## Phase 2: Polish & Cross-Cutting Concerns

**Purpose**: contract conformance and the coverage gate

- [ ] T011 Compare the generated backend/target/openapi.json with specs/002-us-tags-backend/contracts/openapi.yaml for `listTasks` (`tag` parameter), `addTaskTags`, `removeTaskTag`, schemas `TaskTags` and `Task.tags` (required, array of string); fix the `@Schema`/`@Parameter` annotations in backend/src/main/java/com/quickflow/web/task/ where they differ
- [ ] T012 Run `cd backend && ./mvnw -q verify`: all existing tests (dashboard, plans, tasks) still pass with the eager `tags` collection, and JaCoCo line coverage on `com.quickflow.domain` stays ≥ 80% (Constitution III); save the output to loops/backend-dev/runs/002-us-tags/phase-02-build.log

---

## Dependencies & Execution Order

- Phase 1 (US1): no dependency (the 001 backend is in place).
- Phase 2 (Polish): after Phase 1.
- Within Phase 1: T001–T004 first (tests, in parallel); then T005 → T006 → T007; T008 in parallel with
  T005–T007; T009 after T005; T010 after T006–T009.

## Parallel Example: User Story 1

```text
T001 TaskTest.java | T002 TaskServiceTest.java | T003 TaskRepositoryTest.java | T004 TaskControllerTest.java
then: T005 → T006 → T007, with T008 TaskTagsRequest.java alongside
```

## Implementation Strategy

MVP = Phase 1 (the whole story). Phase 2 only checks the contract and the gates.
