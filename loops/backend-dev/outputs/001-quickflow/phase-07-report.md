# phase-07-report: US5 See everything on the Dashboard (backend-dev, attempt 1)

Test plan: `phase-07-test.md` (new this attempt, C1–C6). Run against `http://localhost:8080` (fresh test
database) on 2026-09-26 from 00:54 to 00:59 +03:00 (app zone Africa/Cairo, today = 2026-09-26).

**How the checks were run:** `phase-07-verify.sh` was written, but running it with `bash` was blocked by the
session's permission check ("requires approval"), as in phase-05 and phase-06. Shell loops, variables in
request bodies and `jq --slurpfile` were blocked too. So the same checks were run as direct `curl` calls with
literal bodies (plan bodies from `http/C6-p*.req`), and the results were compared with `jq`. Every response is
saved under `<run_dir>/http/<check>-*.json`; there is no `curl.log` this attempt. Run folder:
`loops/testing/runs/001-quickflow/backend-dev/phase-07/attempt-1`.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | swagger `getDashboard`, AC-US5-3 (0 %) | pass | `http/C1.head`: 200 `application/json`; `http/C1.json`: exactly the 11 required fields, all 5 arrays `[]`, `taskCompletionPercent 0`, `taskCounts {0,0}`, `habitCounts {0,0}`, `planCounts {0,0,0}`, `learning {0,0,0,0,0}`, `now` `2026-09-26T00:56:09.149+03:00` (run time 00:56:09); `http/C1-docs.json`: `getDashboard`, tag `dashboard`, schemas Dashboard, TaskCounts, HabitCounts, PlanCounts, LearningSnapshot, with the same required list |
| C2 | AC-US5-1, FR-01.7, BR-4, BR-14 | pass | tasks 1 A (due today), 2 B (due yesterday), 3 C (created DONE, due yesterday; `completedAt` 00:56:24 today), 4 D (`/complete` 200), 5 E (PUT DONE 200), 6 F / 7 G (archived 200), 8 H (completed then archived), 9 I (DELETE 204, GET 404). `http/C2.json`: `dueToday [1,5]`, `overdue [2]` with `overdue:true`, `completedToday [5,4,3]` (every non-archived task DONE today, `http/C3-done.json`); tasks 6–9 in no list; each of the 6 listed entries equals `GET /api/tasks/{id}` (`http/C2-get-*.json`, jq compare all true) |
| C3 | AC-US5-3, SC-003 | pass | `http/C3.json` `taskCounts {total 5, done 3}` = `http/C3-all.json` (5 non-archived) and `http/C3-done.json` (3 DONE); percent 60; +1 TODO → `C3-b` `{6,3}` 50; +1 TODO → `C3-c` `{7,3}` 42 (floor of 42.86, data-model.md); archived DONE task 8 never counted |
| C4 | AC-US5-2 | pass | habits 1 (completed today, 201), 2 (not completed), 3 (completed today, then deactivated 200). `http/C4.json`: `habits` ids `[1,2]`, `completedToday` true / false, `habitCounts {active 2, completedToday 1}`; both entries equal `GET /api/habits/{id}` (`C4-get-1/2`); undo completion (DELETE 204) → `http/C4-b.json` `{active 2, completedToday 0}` |
| C5 | AC-US5-5, BR-14 | pass | cards 1 NOT_STARTED, 2 IN_PROGRESS, 3 COMPLETED, 4 IN_PROGRESS (PUT 200); milestones m1 (card 1, done), m2 (card 1), m3 (card 2, done), m4 (card 3), m5 (card 4, done). `http/C5.json` `learning {1, 2, 1, milestonesDone 3, milestonesTotal 5}`; DELETE card 4 (204) → `http/C5-b.json` `{1, 1, 1, 2, 4}` |
| C6 | AC-US5-4, FR-08.5, FR-09.2 | pass | plans (items: habit 2, card 1): 1 in window prio 2, 2 in window prio 1, 3 future, 4 past (all 201). `http/C6.json`: `activePlans [2,1]` (priority asc), both `IN_PROGRESS`, `progressPercent 0`, `restSeconds` 7333 / 3733; `planCounts {1, 2, 1}`; entries equal `GET /api/plans/{id}` apart from `restSeconds`; 3 s later `http/C6-b.json` 7330 / 3730 (decreasing). Tick item 3 of plan 2 → `http/C6-c.json` plan 2 `progressPercent 50`; tick item 4 → `http/C6-d.json` `activePlans [1]`, `planCounts {1, 1, 2}` (plan 2 `COMPLETED` 100 %, `http/C6-t2.json`) |

6 / 6 pass, 0 fail, 0 unclear.

Not checked here: AC-US5-6 and AC-US5-7 (frontend layer); the greeting (display name comes from
`GET /api/settings`, phase-08).

## Unit tests
From `unit/unit-result.json`: layer backend, exit code 0, **passed 306, failed 0, errors 0, skipped 0**,
outcome `pass`. The runner reported `coverage: null`, `coverage_skipped: true` ("no classes in
com/quickflow/domain": the gate matches the package exactly and the classes are in sub-packages; same as
phase-02–06). Read from `unit/coverage/jacoco.csv` (packages `com.quickflow.domain.*`): lines covered 631,
missed 23 → **96.5 %** (≥ 0.80); `com.quickflow.domain.dashboard` alone 41 / 41 = 100 %.
Dashboard test classes (`unit/surefire/`): DashboardServiceTest 17, DashboardQueriesTest 10,
DashboardControllerTest 3, all passing.

## Requirement coverage
| id | unit tests | checks |
|---|---|---|
| FR-09.1 (task groups) | DashboardServiceTest `fr09_1_dueTodayFromRepositoryForToday`, `fr09_1_overdueFromFindOverdue`, `fr09_1_completedTodayUsesCairoMidnightBounds`, `fr09_1_taskCountsNonArchived`, `fr09_1_nowFromClock`; DashboardQueriesTest `fr09_1_dueOnDateReturnsOnlyThatDate`, `fr09_1_completedBetweenIncludesFromExcludesTo`, `fr09_1_completedBetweenOnlyDone` | C1, C2 |
| FR-09.1 (plans, learning) | DashboardServiceTest `fr09_1_planCountsPerStatus` | C5, C6 |
| FR-09.2 | DashboardServiceTest `fr09_2_figuresRecomputedOnEachCall` | C3, C4, C5, C6 (every change shows on the next GET) |
| FR-08.5 | DashboardServiceTest `fr08_5_planToggleChangesNextSummary` | C6 |
| FR-01.7 | DashboardServiceTest `fr09_1_overdueFromFindOverdue` | C2 |
| AC-US5-1 | as FR-09.1 (task groups) | C2 |
| AC-US5-2 | DashboardServiceTest `acUs5_2_habitsAreActiveOnly`, `acUs5_2_habitCountsCompletedToday` | C4 |
| AC-US5-3 | DashboardServiceTest `acUs5_3_completionPercentFloors`, `acUs5_3_completionPercentAllDoneIs100`, `acUs5_3_completionPercentZeroWithNoTasks`; DashboardQueriesTest `acUs5_3_countNonArchivedAndDone` | C1, C3 |
| AC-US5-4 | DashboardServiceTest `acUs5_4_activePlansOnlyInProgress`, `acUs5_4_activePlansOrderedByPriorityThenStart` | C6 |
| AC-US5-5 | DashboardServiceTest `acUs5_5_learningCardsPerStatus`, `acUs5_5_milestonesDoneOfTotal`; DashboardQueriesTest `acUs5_5_cardsCountedPerStatus`, `acUs5_5_milestonesDoneAndTotal` | C5 |
| BR-4 | DashboardQueriesTest `br4_dueOnDateExcludesArchived`, `br4_completedBetweenExcludesArchived` | C2, C3 |
| BR-14 | DashboardQueriesTest `br14_deletedTaskNeverCounted`, `br14_deletedCardMilestonesNeverCounted` | C2, C5 |
| SC-003 | none by name | C3 (counts vs task list) |
| contract `Dashboard` | DashboardControllerTest `getDashboardReturns200WithEveryContractField`, `getDashboardEmptyIsZerosAndEmptyArrays`, `getDashboardDateTimesInAppZone` | C1 |
| AC-US5-6, AC-US5-7 | none (frontend layer) | none here |

## Questions
None.
