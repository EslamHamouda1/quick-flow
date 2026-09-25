# phase-07-review: US5 See everything on the Dashboard

## Goal
One read-only `GET /api/dashboard` (operationId `getDashboard`, contract schema `Dashboard`) whose every list and
number is computed from the current data on each call (FR-09.1, FR-09.2), reusing the task, habit, plan and
learning services and repositories of phases 03–06.

## Tasks
- [ ] T068 [P] [US5] Domain test backend/src/test/java/com/quickflow/domain/dashboard/DashboardServiceTest.java (Mockito services/repositories, fixed clock): `FR-09.1` dueToday, overdue, completedToday (completedAt on today in Cairo, non-archived), `AC-US5-3` taskCompletionPercent floor(100·DONE/non-archived) and 0 with no tasks, habit counts (active, completedToday), activePlans only IN_PROGRESS ordered by priority, plan counts, learning snapshot (cards per status, milestones done/total); `FR-08.5` after a plan item toggle the next summary changes; `FR-09.2` figures computed on each call
- [ ] T069 [P] [US5] Persistence test backend/src/test/java/com/quickflow/persistence/DashboardQueriesTest.java: task due-today and completed-between queries, archived tasks excluded (`BR-4`), deleted rows never counted (`BR-14`), card counts per status and milestone done/total counts
- [ ] T070 [P] [US5] Web-slice test backend/src/test/java/com/quickflow/web/dashboard/DashboardControllerTest.java: `getDashboard` 200 with every field of contract schema `Dashboard`
- [ ] T071 [US5] Add the count/list queries the Dashboard needs (tasks due on a date, completed between two instants, non-archived totals by status; cards per status; milestone done/total) to backend/src/main/java/com/quickflow/domain/task/TaskRepository.java and backend/src/main/java/com/quickflow/domain/learning/LearningMilestoneRepository.java / LearningCardRepository.java
- [ ] T072 [P] [US5] Create `DashboardSummary` record (and nested count records) in backend/src/main/java/com/quickflow/domain/dashboard/DashboardSummary.java
- [ ] T073 [US5] Create `DashboardService` in backend/src/main/java/com/quickflow/domain/dashboard/DashboardService.java
- [ ] T074 [US5] Create `DashboardResponse` record and `DashboardController` (`getDashboard`, `GET /api/dashboard`) in backend/src/main/java/com/quickflow/web/dashboard/

tasks.md `## Phase 7` matches `phase-07.md` (T068–T074, same wording).

Order: the three tests T068–T070 first (parallel, different files), then T071 and T072 (parallel in effect:
different files, T072 is `[P]`), T073 (needs both), T074 (needs T072/T073).

## Acceptance criteria
Copied from `phase-07.md` (spec.md US5):
- AC-US5-1: Given tasks due today, overdue tasks and tasks completed today, When the user opens the Dashboard, Then each group lists exactly those tasks (non-archived only).
- AC-US5-2: Given active and inactive habits, When the user opens the Dashboard, Then today's habits list the active habits and marks those completed today, with counts matching the data.
- AC-US5-3: Given non-archived tasks, When the Dashboard shows the task completion percentage, Then it equals (non-archived tasks with status Done) / (all non-archived tasks), and 0% when there are none.
- AC-US5-4: Given plans that are In Progress, When the user opens the Dashboard, Then each is shown with its progress percentage and live rest time; ticking an item in a plan changes the Dashboard figures on the next view without other action.
- AC-US5-5: Given learning cards and milestones, When the user opens the Dashboard, Then the learning snapshot shows the number of cards per status and milestones done out of total, matching the data.
- AC-US5-6: (frontend layer: quick-add actions.)
- AC-US5-7: (frontend layer: persistent navigation.)

What the testing loop can observe after this phase (`GET http://localhost:8080/api/dashboard`, data created
first through the phase-03–06 endpoints):
- 200 `application/json` with all 11 required fields: `now` (date-time with the app-zone offset), `dueToday`,
  `overdue`, `completedToday` (arrays of `Task`, same shape as `GET /api/tasks/{id}`), `taskCompletionPercent`,
  `taskCounts {total, done}`, `habits` (array of `Habit`), `habitCounts {active, completedToday}`, `activePlans`
  (array of `Plan`), `planCounts {notStarted, inProgress, completed}`, `learning {notStarted, inProgress,
  completed, milestonesDone, milestonesTotal}`. Empty database → empty arrays, all counts 0,
  `taskCompletionPercent: 0`.
- AC-US5-1: a task with `dueDate` = today appears in `dueToday` (whatever its status, A-1); a TODO task due
  yesterday appears in `overdue` with `overdue: true` and not in `dueToday`; completing a task
  (`PUT /api/tasks/{id}` status DONE) puts it in `completedToday`; archiving any of them removes it from every
  list and from `taskCounts`; deleting it removes it everywhere (BR-4, BR-14).
- AC-US5-3: 3 non-archived tasks with 1 DONE → `taskCounts {total: 3, done: 1}`, `taskCompletionPercent: 33`;
  an archived DONE task changes neither number.
- AC-US5-2: one active habit completed today, one active not completed, one inactive → `habits` has the two
  active ones (with `completedToday` true/false), `habitCounts {active: 2, completedToday: 1}`.
- AC-US5-4: plans in the window, in the future and in the past → `activePlans` holds only `IN_PROGRESS` ones,
  ordered by `priorityOrder` asc (then `startDateTime`, then `id`), each with `progressPercent` and a
  `restSeconds` that decreases between two GETs; `planCounts` counts all three statuses. Ticking an item
  (`PUT /api/plans/{id}/items/{itemId}`) changes that plan's `progressPercent` on the next GET; ticking the last
  item moves it out of `activePlans` and from `inProgress` to `completed` (FR-08.4).
- AC-US5-5: cards in each status and milestones with some `done` → `learning` counts match; deleting a card
  removes it and its milestones from the counts.
- `/v3/api-docs` and `backend/target/openapi.json` list `getDashboard` under tag `dashboard` with schemas
  `Dashboard`, `TaskCounts`, `HabitCounts`, `PlanCounts`, `LearningSnapshot`.

## Files
| File | Change |
|---|---|
| `backend/src/main/java/com/quickflow/domain/task/TaskRepository.java` | add `findByDueDateAndArchivedFalseOrderByIdAsc(LocalDate)`, `findCompletedBetween(Instant from, Instant to)` (non-archived, DONE, `from <= completedAt < to`, `completedAt desc, id desc`), `countByArchivedFalse()`, `countByArchivedFalseAndStatus(TaskStatus)`; `findOverdue` reused as is |
| `backend/src/main/java/com/quickflow/domain/learning/LearningCardRepository.java` | add `countByStatus(LearningStatus)` |
| `backend/src/main/java/com/quickflow/domain/learning/LearningMilestoneRepository.java` | add `countByDoneTrue()` (explicit `@Query` if the derived name clashes, as the existing comment warns); total via `count()` |
| `backend/src/main/java/com/quickflow/domain/dashboard/DashboardSummary.java` | new record (`now`, `dueToday`, `overdue`, `completedToday` as `List<Task>`, `taskCompletionPercent`, `TaskCounts`, `List<HabitView> habits`, `HabitCounts`, `List<PlanView> activePlans`, `PlanCounts`, `LearningSnapshot`) with nested count records |
| `backend/src/main/java/com/quickflow/domain/dashboard/DashboardService.java` | new `@Service @Transactional(readOnly = true)`, `summary()`; uses `TaskRepository`, `HabitService.list(true)`, `PlanService.list(PlanGroup.ALL)`, `LearningCardRepository`, `LearningMilestoneRepository`, `TimeService` |
| `backend/src/main/java/com/quickflow/web/dashboard/DashboardResponse.java` | new record `@Schema(name = "Dashboard")` + nested records `@Schema(name = "TaskCounts" / "HabitCounts" / "PlanCounts" / "LearningSnapshot")`, all fields `RequiredMode.REQUIRED` |
| `backend/src/main/java/com/quickflow/web/dashboard/DashboardController.java` | new, `@RestController @RequestMapping("/api/dashboard") @Tag(name = "dashboard")`, `@GetMapping` `getDashboard` |
| `backend/src/main/java/com/quickflow/web/task/TaskResponse.java`, `web/habit/HabitResponse.java`, `web/plan/PlanResponse.java` | the static `from(...)` factories are package-private today; make them `public` so `web.dashboard` maps `Task`/`Habit`/`Plan` the same way (no other change; `PlanItemResponse.from` stays as is, it's only called inside `PlanResponse`) |
| `backend/src/test/java/com/quickflow/domain/dashboard/DashboardServiceTest.java` | new |
| `backend/src/test/java/com/quickflow/persistence/DashboardQueriesTest.java` | new |
| `backend/src/test/java/com/quickflow/web/dashboard/DashboardControllerTest.java` | new |

Endpoint added: `GET /api/dashboard` (`getDashboard`). No change to `pom.xml`, properties, entities or
`ApiExceptionHandler`. Dashboard has no request input, so no 400/404 paths.

## Planned checks
- **Tests written before the code they test** (layer has `coverage`): T068 before T073, T069 before T071,
  T070 before T074.
- Check before relying on it (in the existing code / resolved jars, not from memory):
  - that Spring Data derives `countByArchivedFalseAndStatus`, `countByStatus` and `countByDoneTrue` on these
    entities (the milestone repository notes a derived-path clash for `CardId`; `Done` is a plain field, but
    if derivation fails an explicit `@Query` is used);
  - how the phase-03–06 web-slice tests import the other controllers' mapping (`@WebMvcTest(DashboardController.class)`
    with `@MockitoBean DashboardService` and `TimeService`) and that `MockMvcTester` asserts JSON paths the same way;
  - that `HabitView.progress()` carries `completedToday` (used for `habitCounts.completedToday`);
  - that `PlanService.list(ALL)` builds the views inside its own read-only transaction, so calling it from a
    read-only `DashboardService` transaction joins it (REQUIRED) with `open-in-view=false`.
- Run `L.build` and `L.test`; save output to `loops/backend-dev/runs/001-quickflow/phase-07-build.log`.
- Copy `backend/target/openapi.json` to `loops/backend-dev/outputs/openapi.json`; compare `getDashboard` and the
  5 schemas (properties, required lists, nested `$ref`s to `Task`, `Habit`, `Plan`) with `contracts/openapi.yaml`;
  earlier operations must stay unchanged. Known differences (int32 formats, servers url) are logged, not changed.
- JaCoCo: domain line coverage ≥ 0.80 over `com/quickflow/domain/**`.
- **Unit tests, one per business rule** (named with the rule id):
  - `DashboardServiceTest` (fixed clock in Africa/Cairo, mocks): `fr09_1_dueTodayFromRepositoryForToday`,
    `fr09_1_overdueFromFindOverdue`, `fr09_1_completedTodayUsesCairoMidnightBounds` (clock just after 00:00
    Cairo = the previous UTC day; bounds are `today 00:00 Cairo` .. `tomorrow 00:00 Cairo`),
    `acUs5_3_completionPercentFloors` (1/3 → 33, 2/3 → 66), `acUs5_3_completionPercentAllDoneIs100`,
    `acUs5_3_completionPercentZeroWithNoTasks`, `fr09_1_taskCountsNonArchived`, `acUs5_2_habitsAreActiveOnly`,
    `acUs5_2_habitCountsCompletedToday`, `acUs5_4_activePlansOnlyInProgress`,
    `acUs5_4_activePlansOrderedByPriorityThenStart`, `fr09_1_planCountsPerStatus`,
    `acUs5_5_learningCardsPerStatus`, `acUs5_5_milestonesDoneOfTotal`, `fr09_1_nowFromClock`,
    `fr08_5_planToggleChangesNextSummary` (two calls, the mocked plan view changes between them),
    `fr09_2_figuresRecomputedOnEachCall` (repositories called again on the second call, nothing cached).
  - `DashboardQueriesTest` (`@DataJpaTest`): `fr09_1_dueOnDateReturnsOnlyThatDate`,
    `br4_dueOnDateExcludesArchived`, `fr09_1_completedBetweenIncludesFromExcludesTo`,
    `br4_completedBetweenExcludesArchived`, `fr09_1_completedBetweenOnlyDone`,
    `acUs5_3_countNonArchivedAndDone`, `br14_deletedTaskNeverCounted`, `acUs5_5_cardsCountedPerStatus`,
    `acUs5_5_milestonesDoneAndTotal`, `br14_deletedCardMilestonesNeverCounted`.
  - `DashboardControllerTest` (`@WebMvcTest`): `getDashboardReturns200WithEveryContractField` (the 11 required
    fields and the nested count fields present), `getDashboardEmptyIsZerosAndEmptyArrays`,
    `getDashboardDateTimesInAppZone`.

## Risks
- **Coverage gate reads only package `com/quickflow/domain`** (phase-02–06 finding, `scripts/lib/loopctl.py`):
  `com/quickflow/domain/dashboard` may be reported as "skipped"; the tests aim at ≥ 0.80 either way. I can't
  change `scripts/`.
- **One "now" per summary**: `DashboardService` reads `TimeService` once for `now`/`today`, but
  `PlanService` and `HabitService` read the clock themselves, a few ms apart. At a midnight or plan-boundary
  instant the lists could straddle it. Accepted (single user, ms window); not worked around by changing the
  phase-04/06 services.
- **Reusing `PlanService.list(ALL)`** loads every plan and looks up every item's source (the phase-06 N+1 note);
  fine for a single-user app, not optimised.
- **Making three `from(...)` factories public** touches phase-03/04/06 files; the change is visibility only
  and their tests still run in `L.test`.
- **springdoc output vs. contract**: nested records must be named with `@Schema(name = ...)` so the generated
  schemas are `TaskCounts` etc. and not `DashboardResponse.TaskCounts`; checked after build and logged.
- **Completed-today window** uses `completedAt` (an `Instant`) against Cairo midnight bounds converted with
  `TimeService.zone()`; DST days in Cairo (23 h / 25 h) are handled by `ZonedDateTime` start-of-day, not by
  adding 24 h.

## Assumptions
- A-1: `dueToday` = non-archived tasks with `dueDate` = today, **any status** (data-model: "non-archived tasks
  with dueDate = today"); a DONE task due today is listed in both `dueToday` and (if completed today)
  `completedToday`. Order: `id` asc (creation order).
- A-2: `overdue` = the existing FR-01.7 query (`TaskService.overdue()` / `findOverdue(today)`: due before today,
  not DONE, non-archived, oldest due date first) — the same list as `GET /api/tasks/overdue`.
- A-3: `completedToday` = non-archived tasks with status DONE and `completedAt` in
  `[today 00:00, tomorrow 00:00)` of the app zone; order `completedAt` desc, then `id` desc. A task completed
  today and then moved back to TODO is not listed (status check).
- A-4: `taskCounts.total` = non-archived tasks, `taskCounts.done` = non-archived DONE tasks;
  `taskCompletionPercent = floor(100 · done / total)`, 0 when `total = 0` (same floor rule as plan progress).
- A-5: `habits` = active habits in the habit list order (oldest first, A-8 of phase-04), each as the full `Habit`
  schema; `habitCounts.active` = their number, `habitCounts.completedToday` = those with `completedToday = true`
  (a completion dated today; for a WEEKLY habit, a completion earlier in the week doesn't count as "today").
- A-6: `activePlans` = plans whose computed status is `IN_PROGRESS`, ordered as the active plan list
  (`priorityOrder` asc, `startDateTime` asc, `id` asc); NOT_STARTED plans are only counted.
  `planCounts` counts every plan by computed status.
- A-7: `learning` counts every card by status (cards have no archive) and every milestone of every card;
  `milestonesDone` / `milestonesTotal` over all cards together.
- A-8: `now` = `TimeService.now()` (app-zone offset); the greeting's display name is not part of this endpoint
  (it comes from `GET /api/settings`, phase-08).
- A-9: no caching of any kind; each GET queries again (FR-09.2).

## Open questions
None.
