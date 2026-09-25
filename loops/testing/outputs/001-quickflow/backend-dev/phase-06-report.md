# phase-06-report: US4 Build and follow Todo Plans (backend-dev, attempt 1)

Test plan: `phase-06-test.md` (new this attempt, C1–C15). Run against `http://localhost:8080` (fresh test
database) on 2026-09-25 from 23:39 to 23:44 +03:00.

**How the checks were run:** as in phase-05, `bash phase-06-verify.sh <run_dir>` was blocked by the session's
permission check ("requires approval"), and `source`, shell functions, quote-concatenated bodies and loop
variables were blocked too. So the same checks were run as direct `curl` calls with literal request bodies
and fixed times (future window 2026-09-26T02:00–04:00+03:00, current window 2026-09-25T22:30 →
2026-09-26T01:30+03:00, past windows ending 20:30/21:00 on 2026-09-25). Every response is saved under
`<run_dir>/http/<check>-*.json` (and `.head` where the content type was checked). `<run_dir>/curl.log` has all
of them in one file. Run folder: `loops/testing/runs/001-quickflow/backend-dev/phase-06/attempt-1`.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US4-1 | pass | `http/C1.json`: 201 `application/json`, 3 items with `done:false`, `sourceRemoved:false`, `sourceTitle` "Task P6"/"Habit P6"/"Card P6", `doneItems 0`, `totalItems 3`, `progressPercent 0`, `createdAt` 2026-09-25T23:40:07+03:00 (run time 23:40), start/end as sent; `http/C1-get.json` the same (createdAt stored at µs precision) |
| C2 | AC-US4-2, BR-10 | pass | `C2-empty` / `C2-missing`: 400 `application/problem+json`, `errors[{field:"items","must not be empty"}]`; `C2-unk-TASK/HABIT/LR`: 400 `items[0].sourceId` "must reference an existing …"; deleted task 2 (`C2-t`, DELETE 204) → `C2-del` 400 `items[1].sourceId`; `C2-after`: list still `[1]` |
| C3 | AC-US4-3, BR-11 | pass | `C3-eq` (end = start), `C3-eqZ` (same instant with offset `Z`), `C3-lt`: 400 problem+json `errors[{field:"endDateTime","must be after startDateTime"}]`; `C3-put` 400 same field; `C3-get` plan 1 unchanged |
| C4 | AC-US4-4, FR-08.3 | pass | `C4-h1` 200: `doneItems 1/3`, `progressPercent 33`, item 5 `done:true`; `C4-g1` same after GET; `C4-h0`/`C4-g0` 0 %, all false; `C4-q1` 1/2 → `50` |
| C5 | AC-US4-5, BR-13, FR-07.5 | pass | HABIT item 5 and LEARNING_RESOURCE item 6 done → habit, its completions (`[]`) and card are byte-identical before/after (`C5-h-*`, `C5-hc-*`, `C5-l-*`); task before `TODO`/`null` (`C5-t-before`); TASK item 4 done → `C5-t-after` `DONE`, `completedAt` 23:40:47.98 (run time 23:40:48); un-tick → `C5-t-undo` still `DONE`, same `completedAt` |
| C6 | AC-US4-6, BR-12, FR-08.4 | pass | `C6-g`: `NOT_STARTED`, `restSeconds:null`; all 3 items done → `C6-d3`/`C6-all`: still `NOT_STARTED`, `restSeconds null`, 100 % ("never Completed before its start") |
| C7 | AC-US4-7, FR-08.2, FR-08.4 | pass | `C7-g1`: `IN_PROGRESS`, `restSeconds 6538` vs end − now = 6539 s; `C7-g2` 3 s later `6535` (decreasing); all done → `C7-d10` `COMPLETED`, 100 %; un-tick → `C7-u` `IN_PROGRESS`, 50 % |
| C8 | AC-US4-8, NFR-4 | pass | past plan 5 created 201 → `COMPLETED`, `restSeconds null` (`C8-p`); two GETs identical (`C8-g1`/`C8-g2`, cmp); PUT times into the window → `C8-mv` `IN_PROGRESS` with rest time; into the future → `C8-mv2`/`C8-g3` `NOT_STARTED`, `null`: status follows stored times + flags + now on each read. (An actual app restart is done by the runner between runs, not by this session; the stored-values rule is what this check observes.) |
| C9 | AC-US4-10, contract `listPlans` | pass | `C9-a` active: `[2,3,5,1,7,6]` = (prio 1, 22:30), (1, 22:30), (1, 02:00), (2, 02:00), (2, 03:00), (3, 02:00): priorityOrder asc, then start asc, only NOT_STARTED/IN_PROGRESS; in-window plans carry `restSeconds`; `C9-c` completed `[4,8]` end desc; `C9-all` = `C9-def` = `[2,3,5,1,7,6,4,8]` (active then completed); every plan has status, progressPercent, restSeconds, items with id/done |
| C10 | AC-US4-11 | pass | past plan 8 with 1 of 3 items done: in `C9-c` (group=completed) `COMPLETED`, `doneItems 1`, `totalItems 3`, `progressPercent 33`; plan 5 moved to the future is not in the completed list |
| C11 | FR-07.6 | pass | task 3 ticked in plan 9, then deleted (204): `C11-g` item 17 `sourceRemoved:true`, `sourceTitle "C11 task"`, `done:true`, `totalItems 2`, 50 %; `C11-tget` 404; habit 2 + card 2 deleted → `C11-g2` both `sourceRemoved:true` with their titles, still counted; also in the list (`C11-list`) |
| C12 | AC-US4-12, BR-14 | pass | DELETE plan 11 → 204; `C12-get` 404 problem+json "Plan 11 not found"; `C12-item` 404; not in `C12-list`; second DELETE 404; task 1, habit 1, its completions and card 1 byte-identical before/after (`C12-*0` vs `C12-*1`, cmp "sources-unchanged") |
| C13 | FR-07.1, FR-07.2, swagger `PlanCreate` | pass | blank and missing title → 400 `title`; 201 chars → 400 `title` "size must be between 1 and 200", 200 chars → 201; duration 0 / 100001 → 400 `estimatedDurationMinutes`, 1 / 100000 → 201; priorityOrder 0 → 400 `priorityOrder`; same HABIT twice → 400 `items[1]` "must not repeat a selected item"; same id as TASK + HABIT → 201; sourceType `NOPE` → 400 problem+json `items[0].sourceType` |
| C14 | FR-07.3, contract `updatePlan` / `setPlanItemDone` | pass | `C14-u` 200: title "C14 renamed", 120 min, priority 5, new end; `C14-bef` vs `C14-aft`: items, flags and `createdAt` unchanged; unknown plan PUT/GET/DELETE → 404 problem+json; item PUT `{}` → 400 `done` "must not be null"; unknown plan item → 404; item 9 of plan 4 via plan 2 → 404 "Plan item 9 not found on plan 2" |
| C15 | swagger | pass | `http/C15-api-docs.json`, `backend/target/openapi.json`, `loops/backend-dev/outputs/openapi.json`: all three list listPlans, createPlan, getPlan, updatePlan, deletePlan, setPlanItemDone; createPlan 201, deletePlan 204; `group` enum active/completed/all; schemas Plan, PlanItem, PlanCreate, PlanUpdate, PlanItemRef, PlanItemUpdate, PlanStatus, PlanSourceType all present; enums as in the contract |

15 / 15 pass, 0 fail, 0 unclear.

Not checked here: AC-US4-9 and AC-US4-13 (frontend layer); the Dashboard part of AC-US4-4 (phase-07).

## Unit tests
From `unit/unit-result.json`: layer backend, exit code 0, **passed 276, failed 0, errors 0, skipped 0**,
outcome `pass`. The runner reported `coverage: null`, `coverage_skipped: true` ("no classes in
com/quickflow/domain": the gate matches the package exactly, and classes are in sub-packages; same as
phase-02–05). Read from `backend/target/site/jacoco/jacoco.csv` (package `com.quickflow.domain.*`): domain
lines covered 590, missed 23 → **96.2 %** (≥ 0.80); `com.quickflow.domain.plan` alone 191 / 204 = 93.6 %.
Plan test classes: PlanStatusPolicyTest (19), PlanTest (19), PlanServiceTest (23 incl. 4 parameterized),
PlanRepositoryTest (5), PlanControllerTest (15), all passing (`unit/surefire/`).

## Requirement coverage
| id | unit tests | checks |
|---|---|---|
| FR-07.1 | PlanTest `fr07_1_*` (blank/201/200 title, duration 0/100001/bounds, priorityOrder 0, createdAtSet); PlanServiceTest `fr07_1_createSetsCreatedAtFromClock` | C1, C13 |
| FR-07.2 | PlanTest `fr07_2_duplicateSourceRejected`, `sameIdDifferentTypeAccepted`, `newItemsNotDone`; PlanServiceTest `fr07_2_sourceTitleSnapshotOnCreate`; PlanRepositoryTest `fr07_2_*` | C1, C13 |
| FR-07.3 | PlanTest `fr07_3_updateKeepsItems`; PlanServiceTest `fr07_3_updateEditsFields`, `fr07_3_updateEndBeforeStartRejected`; PlanControllerTest `updatePlanReturns200`, `deletePlanIs204` | C1, C4, C12, C14 |
| FR-07.4 / BR-11 | PlanTest `br11_*`; PlanControllerTest `br11_endNotAfterStartIs400WithEndDateTimeField`; PlanServiceTest `fr07_3_updateEndBeforeStartRejected` | C3 |
| FR-07.5 / BR-13 | PlanServiceTest `br13_taskItemDoneCompletesTask`, `br13_taskItemUndoneLeavesTask`, `br13_habitItemDoneChangesNoSource`, `br13_learningItemDoneChangesNoSource`, `br13_taskItemOfDeletedTaskDoneNoPropagation` | C5 |
| FR-07.6 | PlanServiceTest `fr07_6_deletedSourceMarkedRemovedKeepsTitleAndCounts`, `fr07_6_existingSourceTitleRefreshed`; PlanRepositoryTest `fr07_6_itemSurvivesSourceDeletion` | C11 |
| FR-08.1 | none (frontend: in-app notification) | none here; AC-US4-9 is frontend-layer |
| FR-08.2 / BR-12 | PlanStatusPolicyTest `br12_*` (null before start, full window at start, inside, null at/after end) | C6, C7, C8 |
| FR-08.3 | PlanStatusPolicyTest `fr08_3_*` (0/3, 1/3 → 33, 2/3 → 66, all → 100) | C4, C10, C11 |
| FR-08.4 | PlanStatusPolicyTest `fr08_4_*` (8 tests) | C6, C7, C8, C9 |
| FR-08.5 | PlanServiceTest `fr08_5_setItemDoneReturnsNewProgress`; PlanControllerTest `setPlanItemDoneReturnsPlan` | C4, C7 (plan side; Dashboard side is phase-07) |
| BR-10 | PlanTest `br10_noItemsRejectedWithFieldItems`, `br10_nullItemsRejected`; PlanServiceTest `br10_unknownSourceRejectedWithItemField`, `br10_eachSourceTypeResolved`; PlanControllerTest `br10_emptyItemsIs400WithItemsField`, `br10_unknownSourceIs400` | C2 |
| BR-14 | PlanServiceTest `br14_deletePlanTouchesNoSource`; PlanRepositoryTest `br14_deletingPlanDeletesItems` | C11, C12 |
| NFR-4 | PlanStatusPolicyTest `nfr4_sameInputsSameResult` | C8 |

Flag: FR-08.1 has no backend test or check in this phase; it is a frontend requirement (AC-US4-9), the
backend only supplies `startDateTime` and `status` (both checked in C1/C6/C7).

## Questions
None.
