# phase-06-review: US4 Build and follow Todo Plans

## Goal
Plans built from existing tasks, habits and learning cards, with item done toggles (a TASK item marked done
completes its task), and status, progress and rest time computed on read (`/api/plans*`).

## Tasks
- [ ] T054 [P] [US4] Domain test backend/src/test/java/com/quickflow/domain/plan/PlanStatusPolicyTest.java (fixed clocks before, at and after start/end): `FR-08.4` NOT_STARTED before start, IN_PROGRESS inside the window, COMPLETED per the Q2 answer (recommended: at/after end, or all items done once started; never before start); `BR-12` `restSeconds` = seconds to end only when start ≤ now < end, else null; `FR-08.3` progress = floor(100·done/total) (0/3 → 0, 1/3 → 33, 3/3 → 100); `NFR-4` the same stored values and time always give the same status
- [ ] T055 [P] [US4] Domain test backend/src/test/java/com/quickflow/domain/plan/PlanTest.java: `BR-10` no items rejected (field `items`), `BR-11` end equal to or before start rejected (field `endDateTime`), `FR-07.1` blank or > 200-char title, duration outside 1..100,000, priorityOrder < 1 rejected, `FR-07.2` the same (type, id) twice rejected, new items not done
- [ ] T056 [P] [US4] Domain test backend/src/test/java/com/quickflow/domain/plan/PlanServiceTest.java (Mockito repositories and `PlanSourceResolver`, fixed clock): `BR-10` unknown/deleted source → `ValidationException` (field `items[i].sourceId`); `BR-13` per the Q1 answer (recommended: TASK item done → `TaskService.complete`; HABIT and LEARNING_RESOURCE items and un-ticking change no source); `FR-08.5` toggling returns the new progress; `FR-07.6` item whose source was deleted → `sourceRemoved=true`, keeps `sourceTitle`, still counted; `BR-14` deleting a plan leaves its sources untouched; `FR-07.3` update edits fields and re-checks BR-11; list grouping/ordering (active by priorityOrder then start; completed by end desc)
- [ ] T057 [P] [US4] Persistence test backend/src/test/java/com/quickflow/persistence/PlanRepositoryTest.java: items saved and loaded with the plan, `FR-07.2` unique (plan, sourceType, sourceId), `BR-14` deleting a plan deletes its items and neither is returned
- [ ] T058 [P] [US4] Web-slice test backend/src/test/java/com/quickflow/web/plan/PlanControllerTest.java: `createPlan` 201 body shape (status, progressPercent, restSeconds, items), `BR-10` 400 empty items, `BR-11` 400 `errors[].field == "endDateTime"`, offset date-time parsing, `listPlans?group=active|completed|all`, `getPlan` 404, `updatePlan` 200, `setPlanItemDone` 200 returns the plan, `deletePlan` 204
- [ ] T059 [P] [US4] Create enums `PlanStatus {NOT_STARTED, IN_PROGRESS, COMPLETED}` and `PlanSourceType {TASK, HABIT, LEARNING_RESOURCE}` in backend/src/main/java/com/quickflow/domain/plan/
- [ ] T060 [US4] Create entity `PlanItem` (`plan` ManyToOne not null, `sourceType`, `sourceId`, `sourceTitle`, `done` default false; unique `(plan_id, source_type, source_id)`) in backend/src/main/java/com/quickflow/domain/plan/PlanItem.java
- [ ] T061 [US4] Create entity `Plan` in backend/src/main/java/com/quickflow/domain/plan/Plan.java: `title` "required, ≤ 200"; `estimatedDurationMinutes` "1..100,000"; `startDateTime`, `endDateTime` Instant, "end after start (BR-11)"; `priorityOrder` "≥ 1, 1 = highest, not unique"; `createdAt`; items "≥ 1 (BR-10), cascade ALL, orphan removal"; no stored status
- [ ] T062 [US4] Create pure `PlanStatusPolicy` (`statusAt`, `restSeconds`, `progressPercent`) and `PlanProgress` record in backend/src/main/java/com/quickflow/domain/plan/ (research R-6, Q2)
- [ ] T063 [US4] Create `PlanRepository` in backend/src/main/java/com/quickflow/domain/plan/PlanRepository.java
- [ ] T064 [US4] Create `PlanSourceResolver` (looks up TASK/HABIT/LEARNING_RESOURCE by id through their repositories: exists + current title) in backend/src/main/java/com/quickflow/domain/plan/PlanSourceResolver.java
- [ ] T065 [US4] Create `PlanService` (`create, get, list(group), update, delete, setItemDone` with BR-13 propagation per Q1 via `TaskService.complete`; `PlanView` with computed status/progress/rest time and refreshed `sourceTitle`/`sourceRemoved`) in backend/src/main/java/com/quickflow/domain/plan/PlanService.java
- [ ] T066 [P] [US4] Create records `PlanCreateRequest`, `PlanUpdateRequest`, `PlanItemRefRequest`, `PlanItemUpdateRequest`, `PlanResponse`, `PlanItemResponse` (date-times as `OffsetDateTime` in the app zone) in backend/src/main/java/com/quickflow/web/plan/
- [ ] T067 [US4] Create `PlanController` with `listPlans, createPlan, getPlan, updatePlan, deletePlan, setPlanItemDone` in backend/src/main/java/com/quickflow/web/plan/PlanController.java

tasks.md `## Phase 6` matches `phase-06.md` (T054–T067, same wording).

The wording "per the Q1/Q2 answer (recommended: ...)" in T054/T056 and "(pending Q1/Q2)" in AC-US4-5/7 of
`phase-06.md` predate plan-apply. Both questions were answered **as recommended** (phase-00 approval
notes, spec.md Clarifications, FR-07.5, FR-08.4, research R-6/R-7), so the code and tests follow those
answers exactly: the recommended text is the rule. The wording is left as it is (a review session doesn't
edit tasks.md); the reviewer may reword it here and implement will sync it.

Order: the five tests T054–T058 first (parallel, different files), then T059, T060+T061 together (the two
entities reference each other), T062, T063, T064, T065, then T066 (needs the enums) → T067.

## Acceptance criteria
Copied from `phase-06.md` (spec.md US4); Q1/Q2 now answered (FR-07.5, FR-08.4):
- AC-US4-1: Given existing tasks, habits and learning cards, When the user creates a plan selecting one or more of them with a title, estimated duration, start date-time, end date-time and priority order, Then the plan is stored with one item per selected entity (each not done) and its creation time set.
- AC-US4-2: Given the plan builder, When the user selects no items, or an item that does not exist (or was deleted), Then the plan is rejected (BR-10).
- AC-US4-3: Given the plan builder, When the end date-time is equal to or before the start date-time, Then the plan is rejected with a message naming the end date-time (BR-11).
- AC-US4-4: Given a plan, When the user marks an item done or not done, Then the item's done flag is saved and the plan's progress percentage becomes (items done) / (total items), shown immediately on the plan and on the Dashboard (FR-08). (Dashboard part: phase-07)
- AC-US4-5: Given a plan item that references a task, habit or learning card, When the user marks it done, Then the original entity's completion state changes only as stated in FR-07.5 (BR-13): a TASK item marked done sets the task to DONE with `completedAt`; un-ticking and HABIT / LEARNING_RESOURCE items change no source.
- AC-US4-6: Given a plan whose start date-time is in the future, When the plan is read, Then its status is Not Started and no rest time is shown (BR-12).
- AC-US4-7: Given a plan whose start date-time has passed and whose end date-time has not, When the plan is read, Then its status is In Progress (Completed if all items are done, FR-08.4) and its rest time is the time left until its end date-time.
- AC-US4-8: Given a plan whose status follows FR-08.4, When the app is closed and reopened, Then the same status is shown, computed from the stored times, the item flags and the current time (NFR-4).
- AC-US4-9: (frontend layer; backend supplies `startDateTime` and `status`.)
- AC-US4-10: Given several plans, When the user opens the Todo Plans page, Then plans are grouped into active/upcoming and completed, each showing its progress, rest time (when active), items with done toggles and a remove action; active/upcoming plans are ordered by priority order.
- AC-US4-11: Given past (completed) plans, When the user views the history, Then each shows how much of it was completed (percentage of items done).
- AC-US4-12: Given a plan, When the user removes it, Then the plan and its items are never returned again, and the tasks, habits and learning cards it referenced are unchanged (BR-14).
- AC-US4-13: (frontend layer.)

What the testing loop can observe after this phase (all under `http://localhost:8080`, with one task,
one habit and one learning card created first):
- `POST /api/plans` `{"title", "items": [{"sourceType": "TASK", "sourceId": t}, {"sourceType": "HABIT", "sourceId": h}, {"sourceType": "LEARNING_RESOURCE", "sourceId": c}], "estimatedDurationMinutes", "startDateTime", "endDateTime", "priorityOrder"}` → 201 `Plan`: 3 items with `done: false`, `sourceRemoved: false`, `sourceTitle` = the task title / habit name / card title; `createdAt` set; `doneItems: 0`, `totalItems: 3`, `progressPercent: 0`; date-times returned with the app-zone offset (`+03:00`/`+02:00` for Africa/Cairo).
- Status/rest time by window (FR-08.4, BR-12): start in the future → `NOT_STARTED`, `restSeconds: null`; start past and end future → `IN_PROGRESS`, `restSeconds` ≈ seconds to end (decreasing between two GETs); start and end both past (allowed on create) → `COMPLETED`, `restSeconds: null`.
- 400 `application/problem+json` with `errors[].field`: `items` (missing or `[]`), `items[i].sourceId` (unknown or deleted id), `items[i]` (same type+id twice), `endDateTime` (end = start, end < start), `title` (missing, blank, 201 chars), `estimatedDurationMinutes` (0, 100001), `priorityOrder` (0), `items[i].sourceType` / `sourceType` (unknown value, as the handler reports enum errors).
- `PUT /api/plans/{id}/items/{itemId}` `{"done": true}` on the TASK item → 200 whole plan, `doneItems: 1`, `progressPercent: 33`; then `GET /api/tasks/{t}` → `status: DONE`, `completedAt` set. `{"done": false}` → 200, `progressPercent: 0`, the task stays DONE. HABIT item done → `GET /api/habits/{h}` unchanged (no completion added); LEARNING_RESOURCE item done → card status unchanged. All three done while in the window → `COMPLETED`, `restSeconds` still the time to end (BR-12 is independent of status, R-6); one un-ticked again → `IN_PROGRESS`. Missing `done` → 400 field `done`; unknown plan → 404; an `itemId` of another plan → 404.
- `PUT /api/plans/{id}` `{"title", "estimatedDurationMinutes", "startDateTime", "endDateTime", "priorityOrder"}` → 200, items and their flags unchanged; end ≤ start → 400 field `endDateTime`; unknown id → 404.
- `GET /api/plans?group=active` → NOT_STARTED and IN_PROGRESS plans by `priorityOrder` asc, then `startDateTime` asc; `group=completed` → COMPLETED by `endDateTime` desc; `group=all` or no param → active then completed; `group=foo` → 400 field `group`.
- `DELETE /api/tasks/{t}` while a plan uses it → 204; then `GET /api/plans/{id}` keeps the item with `sourceRemoved: true`, its last `sourceTitle`, its `done` flag, still counted in `totalItems` (FR-07.6). Same for a deleted habit or card.
- `DELETE /api/plans/{id}` → 204; `GET /api/plans/{id}` → 404, absent from the list; the task, habit and card are unchanged (BR-14); second `DELETE` → 404.
- `/v3/api-docs` and `backend/target/openapi.json` list the 6 operationIds with schemas `Plan`, `PlanItem`, `PlanCreate`, `PlanUpdate`, `PlanItemRef`, `PlanItemUpdate`, `PlanStatus`, `PlanSourceType`.

## Files
| File | Change |
|---|---|
| `backend/src/main/java/com/quickflow/domain/plan/PlanStatus.java` | new enum, `@Schema(enumAsRef = true)` as for the other enums |
| `backend/src/main/java/com/quickflow/domain/plan/PlanSourceType.java` | new enum, `@Schema(enumAsRef = true)` |
| `backend/src/main/java/com/quickflow/domain/plan/PlanItem.java` | new `@Entity` (table `plan_item`, FK `plan_id` not null, `source_type` `@Enumerated(STRING)`, `source_id`, `source_title` ≤ 200, `done`), `@Table(uniqueConstraints = (plan_id, source_type, source_id))`; `setDone(boolean)`, `refreshTitle(String)`; no FK to the source tables (sources can be deleted, FR-07.6) |
| `backend/src/main/java/com/quickflow/domain/plan/Plan.java` | new `@Entity` (table `plan`), FR-07.1 / BR-10 / BR-11 / FR-07.2 checks collected into one `ValidationException` (fields `title`, `estimatedDurationMinutes`, `endDateTime`, `priorityOrder`, `items`, `items[i]`); `update(...)` (no item change); `items` `@OneToMany(mappedBy="plan", cascade=ALL, orphanRemoval=true) @OrderBy("id")`; `findItem(itemId)` |
| `backend/src/main/java/com/quickflow/domain/plan/PlanStatusPolicy.java` | new pure class: `statusAt(start, end, done, total, now)`, `restSeconds(start, end, now)`, `progressPercent(done, total)` |
| `backend/src/main/java/com/quickflow/domain/plan/PlanProgress.java` | new record (`status`, `doneItems`, `totalItems`, `progressPercent`, `restSeconds`) |
| `backend/src/main/java/com/quickflow/domain/plan/PlanRepository.java` | new, `JpaRepository<Plan, Long>` |
| `backend/src/main/java/com/quickflow/domain/plan/PlanSourceResolver.java` | new `@Component`, `Optional<String> title(PlanSourceType, long id)` via `TaskRepository`, `HabitRepository`, `LearningCardRepository` |
| `backend/src/main/java/com/quickflow/domain/plan/PlanView.java` | new record (plan fields, item views with `sourceRemoved`, `PlanProgress`), built inside the service transaction |
| `backend/src/main/java/com/quickflow/domain/plan/PlanService.java` | new `@Service @Transactional`, uses `PlanRepository`, `PlanSourceResolver`, `TaskService`, `TimeService` |
| `backend/src/main/java/com/quickflow/web/plan/PlanCreateRequest.java` | new record, `@Schema(name = "PlanCreate")`, bean validation matching the contract (`@NotBlank @Size(max=200)`, `@NotEmpty @Valid` items, `@Min(1) @Max(100000)`, `@NotNull` date-times, `@Min(1)` priorityOrder) |
| `backend/src/main/java/com/quickflow/web/plan/PlanUpdateRequest.java` | new record, `@Schema(name = "PlanUpdate")` |
| `backend/src/main/java/com/quickflow/web/plan/PlanItemRefRequest.java` | new record, `@Schema(name = "PlanItemRef")`, `@NotNull` sourceType and sourceId |
| `backend/src/main/java/com/quickflow/web/plan/PlanItemUpdateRequest.java` | new record, `@Schema(name = "PlanItemUpdate")`, `@NotNull Boolean done` |
| `backend/src/main/java/com/quickflow/web/plan/PlanResponse.java` | new record, `@Schema(name = "Plan")`, date-times via `TimeService.toOffset`, nullable `Long restSeconds` |
| `backend/src/main/java/com/quickflow/web/plan/PlanItemResponse.java` | new record, `@Schema(name = "PlanItem")` |
| `backend/src/main/java/com/quickflow/web/plan/PlanController.java` | new, `@RequestMapping("/api/plans")`, `@Tag(name = "plans")`, 6 methods |
| `backend/src/test/java/com/quickflow/domain/plan/PlanStatusPolicyTest.java` | new |
| `backend/src/test/java/com/quickflow/domain/plan/PlanTest.java` | new |
| `backend/src/test/java/com/quickflow/domain/plan/PlanServiceTest.java` | new |
| `backend/src/test/java/com/quickflow/persistence/PlanRepositoryTest.java` | new |
| `backend/src/test/java/com/quickflow/web/plan/PlanControllerTest.java` | new |

Endpoints added (contract `paths./api/plans*`): `listPlans`, `createPlan`, `getPlan`, `updatePlan`,
`deletePlan`, `setPlanItemDone`. No change to task/habit/learning code, `pom.xml`, properties or
`ApiExceptionHandler` (validation 400 with `errors[]`, unknown enum 400, `NotFoundException` 404 exist).
`TaskService.complete` is reused as is (checked: `Task.changeStatus` keeps an existing `completedAt` when the
task is already DONE, so ticking twice doesn't move it).

## Planned checks
- **Tests written before the code they test** (layer has `coverage`): T054 before T062, T055 before T060/T061, T056 before T064/T065, T057 before T063, T058 before T066/T067.
- Check before relying on it (in the resolved jars / existing code, not from memory):
  - Jackson 3 (Boot 4) deserialising `OffsetDateTime` request fields with any offset (`Z`, `+03:00`) and the error it raises for a malformed date-time (how `ApiExceptionHandler` maps it to a field);
  - `@Valid` on `List<PlanItemRefRequest>` producing field paths `items[0].sourceId` in bean-validation errors (same shape the domain uses);
  - `@OneToMany` with `@OrderBy("id")` + cascade persist of new items and cascade delete on `repository.delete(plan)` in Hibernate 7;
  - that `open-in-view=false` (checked: `application.properties`) means `PlanView` must be built inside the service transaction;
  - how springdoc 3.1.1 renders the nullable `Long restSeconds` and `@RequestParam` `group` enum with default `all`;
  - the test annotations used by phase-03/04/05 (`@DataJpaTest`, `@WebMvcTest`, `@MockitoBean`, `MockMvcTester`).
- Run `L.build` and `L.test`; save output to `loops/backend-dev/runs/001-quickflow/phase-06-build.log`.
- Copy `backend/target/openapi.json` to `loops/backend-dev/outputs/openapi.json` and compare the 6 plan operations (paths, operationIds, `id`/`itemId` params, `group` param, 201/204 codes, the 8 schemas' properties, required lists, `restSeconds` nullable) with `contracts/openapi.yaml`; earlier operations must stay unchanged. Known differences (no generated 4xx responses, int32 formats, servers url) are logged, not changed.
- JaCoCo: domain line coverage ≥ 0.80 over `com/quickflow/domain/**`.
- **Unit tests, one per business rule** (named with the rule id):
  - `PlanStatusPolicyTest`: `fr08_4_beforeStartNotStarted`, `fr08_4_beforeStartAllDoneStillNotStarted`, `fr08_4_atStartInProgress`, `fr08_4_insideWindowInProgress`, `fr08_4_insideWindowAllDoneCompleted`, `fr08_4_atEndCompleted`, `fr08_4_afterEndCompleted`, `fr08_4_untickBeforeEndBackToInProgress`, `br12_restSecondsNullBeforeStart`, `br12_restSecondsAtStartIsFullWindow`, `br12_restSecondsInsideWindow`, `br12_restSecondsNullAtEnd`, `br12_restSecondsNullAfterEnd`, `fr08_3_progressZeroOfThree`, `fr08_3_progressOneOfThreeIs33`, `fr08_3_progressTwoOfThreeIs66`, `fr08_3_progressAllIs100`, `nfr4_sameInputsSameResult`.
  - `PlanTest`: `br10_noItemsRejectedWithFieldItems`, `br10_nullItemsRejected`, `br11_endEqualStartRejectedWithFieldEndDateTime`, `br11_endBeforeStartRejected`, `br11_updateReChecksEndAfterStart`, `fr07_1_blankTitleRejected`, `fr07_1_title201CharsRejected`, `fr07_1_title200CharsAccepted`, `fr07_1_durationZeroRejected`, `fr07_1_duration100001Rejected`, `fr07_1_durationBoundsAccepted`, `fr07_1_priorityOrderZeroRejected`, `fr07_1_createdAtSet`, `fr07_2_duplicateSourceRejected`, `fr07_2_sameIdDifferentTypeAccepted`, `fr07_2_newItemsNotDone`, `fr07_3_updateKeepsItems`.
  - `PlanServiceTest`: `br10_unknownSourceRejectedWithItemField`, `br10_eachSourceTypeResolved`, `fr07_1_createSetsCreatedAtFromClock`, `fr07_2_sourceTitleSnapshotOnCreate`, `br13_taskItemDoneCompletesTask`, `br13_taskItemUndoneLeavesTask`, `br13_habitItemDoneChangesNoSource`, `br13_learningItemDoneChangesNoSource`, `br13_taskItemOfDeletedTaskDoneNoPropagation`, `fr08_5_setItemDoneReturnsNewProgress`, `fr07_6_deletedSourceMarkedRemovedKeepsTitleAndCounts`, `fr07_6_existingSourceTitleRefreshed`, `br14_deletePlanTouchesNoSource`, `fr07_3_updateEditsFields`, `fr07_3_updateEndBeforeStartRejected`, `unknownPlanNotFound` (parameterized over get/update/delete/setItemDone), `itemOfOtherPlanNotFound`, `listActiveOrderedByPriorityThenStart`, `listCompletedOrderedByEndDesc`, `listAllActiveThenCompleted`.
  - `PlanRepositoryTest`: `fr07_2_itemsSavedAndLoadedWithPlan`, `fr07_2_duplicateSourceInPlanRejectedByConstraint`, `fr07_2_sameSourceInTwoPlansAllowed`, `br14_deletingPlanDeletesItems`, `fr07_6_itemSurvivesSourceDeletion`.
  - `PlanControllerTest`: `createPlanReturns201WithComputedFields`, `br10_emptyItemsIs400WithItemsField`, `br10_unknownSourceIs400`, `br11_endNotAfterStartIs400WithEndDateTimeField`, `offsetDateTimeParsedAndReturnedInAppZone`, `listPlansGroupActive`, `listPlansGroupCompleted`, `listPlansDefaultAll`, `listPlansUnknownGroupIs400`, `getPlanUnknownIs404`, `updatePlanReturns200`, `setPlanItemDoneReturnsPlan`, `setPlanItemDoneMissingDoneIs400`, `deletePlanIs204`.

## Risks
- **Coverage gate reads only package `com/quickflow/domain`** (phase-02–05 finding, `scripts/lib/loopctl.py`): coverage for `com/quickflow/domain/plan` may be reported as "skipped"; the tests aim at ≥ 0.80 either way. I can't change `scripts/`.
- **Lazy items with open-in-view off**: `PlanView` (including resolver lookups for `sourceRemoved`/title) is built inside the `@Transactional` service method; the controller only maps views.
- **N+1 reads on list**: each item's source is looked up by id on read (FR-07.6 `sourceRemoved`, title refresh). For a single-user app with small plans this is acceptable (plan: no pagination); noted, not optimised.
- **`TaskService.complete` inside `PlanService`'s transaction**: both are `@Transactional` (REQUIRED), so the task change and the item flag commit together; a failure rolls both back.
- **Unique constraint vs. in-memory duplicate check**: duplicates are rejected by the domain before insert (400 `items[i]`); the DB constraint is a safety net and a violation would surface as 500. Covered by the domain check so it can't be reached through the API.
- **Rest time rounding**: `restSeconds` = whole seconds from now to end, truncated (see A-5); a GET within the last second of the window returns `0` while still `IN_PROGRESS`/`COMPLETED`.
- **Clock at the boundary**: `now == start` → IN_PROGRESS and rest time shown; `now == end` → COMPLETED and no rest time (FR-08.4 "at or after", BR-12 `start <= now < end`).
- **Archived/inactive sources and title-change race**: see A-2 and A-4.
- **springdoc output vs. contract**: as in phase-03/04/05, 400/404 responses aren't generated from the advice and integers get `format: int32`; logged, not changed. `restSeconds` may render as `type: integer` + `nullable` differently from the contract's `[integer, 'null']`; checked after build and logged.

## Assumptions
- A-1: plan titles are **trimmed** and the ≤ 200 check applies to the trimmed value (as for task, habit and card titles).
- A-2: BR-10 "existing, non-deleted" is checked only as "exists" (hard delete, R-5). **Archived tasks and inactive habits are accepted by the API**: spec A-7 says the *builder* offers non-archived tasks and active habits and rejects only deleted or missing entities, so filtering is the frontend's job. Ticking a TASK item whose task is archived still completes it (FR-07.5 names no exception).
- A-3: error fields: unknown/deleted source → `items[<i>].sourceId`; a repeated (type, id) → `items[<i>]` at the second occurrence; all field errors of one request are returned together in `errors[]` (as the other entities do).
- A-4: `sourceTitle` is stored at creation. On every read the response shows the source's current title while it exists (and the stored snapshot is updated when the plan is written: create, update, item toggle); once the source is deleted the last stored title is shown with `sourceRemoved: true`. Reads don't write to the database.
- A-5: `restSeconds` = `Duration.between(now, end).getSeconds()` (whole seconds, truncated) when `start <= now < end`, else `null`; it's shown whatever the status (a plan Completed early by all items done still shows its rest time until the end), per research R-6 "independent of status".
- A-6: `PUT /api/plans/{id}` takes the full `PlanUpdate` (all five fields required, contract) and never changes items, their flags or `createdAt`; start/end may be moved into the past or future freely (only BR-11 applies).
- A-7: ticking a TASK item whose task was deleted saves the item flag and propagates nothing; ticking a TASK item done that is already done calls `complete` again, which keeps the first `completedAt` (checked in `Task.changeStatus`).
- A-8: an `itemId` that exists but belongs to another plan → 404 "Plan item <itemId> not found on plan <id>" (same message as a missing one); an unknown plan is checked first → 404 "Plan <id> not found".
- A-9: ordering ties: active plans by `priorityOrder` asc, `startDateTime` asc, then `id` asc; completed by `endDateTime` desc, then `id` desc; items by `id` asc (the order they were given on create).
- A-10: request date-times accept any ISO-8601 offset and are stored as `Instant`; responses use the app zone (Africa/Cairo) offset, like `createdAt` elsewhere. A date-time without an offset is rejected with 400 (the contract says `format: date-time`).
- A-11: `group` with an unknown value → 400 with field `group`; no `group` → `all`.
- A-12: the plan has no `updatedAt` (FR-07.1 and the contract don't list one).

## Open questions
None.
