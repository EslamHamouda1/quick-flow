# phase-06-test: US4 Build and follow Todo Plans (backend-dev)

Layer: backend (API at `http://localhost:8080`). Script: `phase-06-verify.sh <attempt-folder>`
(bash + curl + jq only; raw requests/responses appended to `<attempt-folder>/curl.log`).

Criteria this plan was written against (from `loops/backend-dev/outputs/001-quickflow/phase-06.md`):
AC-US4-1 … AC-US4-13 (AC-US4-9 and AC-US4-13 are frontend layer, not checked here; the Dashboard part
of AC-US4-4 is phase-07). Q1/Q2 answers from spec.md Clarifications, FR-07.5 and FR-08.4. Limits from
FR-07.1 / FR-07.2 and the swagger `PlanCreate` (title 1..200, duration 1..100000, priorityOrder ≥ 1,
items minItems 1). Codes from the contract (`contracts/openapi.yaml`: 201, 204, 400 BadRequest, 404
NotFound) and ordering from its `listPlans.group` description. Setup: one task, one habit (DAILY) and
one learning card created first. Time windows relative to now: future (+1 h..+3 h), window
(−1 h..+1 h), past (−3 h..−1 h).

| check | criterion | steps | expected (quoted) |
|---|---|---|---|
| C1 | AC-US4-1 | POST /api/plans with the 3 sources, title, 90 min, future window, priority 2; GET it | 201; "one item per selected entity (each not done)"; `sourceTitle` = source title, `sourceRemoved:false`; `doneItems 0`, `totalItems 3`, `progressPercent 0`; "its creation time set" (createdAt ≈ now, with offset); same start/end instants; GET equal |
| C2 | AC-US4-2, BR-10 | POST with `items: []`, without `items`, with unknown TASK/HABIT/LEARNING_RESOURCE id, with a deleted task | "the plan is rejected": 400 problem+json (`items` field for no items); plan count unchanged |
| C3 | AC-US4-3, BR-11 | POST end = start, end < start; PUT end < start | 400 "with a message naming the end date-time": `errors[].field == endDateTime` with a message; rejected PUT leaves the plan unchanged |
| C4 | AC-US4-4, FR-08.3 | window plan with 3 items: tick HABIT, GET, un-tick, GET; 2-item plan tick one | "the item's done flag is saved and the plan's progress percentage becomes (items done) / (total items)": 1/3 → 33, 0/3 → 0, 1/2 → 50; flags persisted on GET |
| C5 | AC-US4-5, BR-13, FR-07.5 | tick HABIT and LEARNING_RESOURCE items; tick TASK item; un-tick TASK item | habit (and completions) and card unchanged; "marking a task-type item done MUST set the underlying task's status to Done … with its completion time recorded"; "setting an item back to not done never changes the source" (task stays DONE, same completedAt) |
| C6 | AC-US4-6, BR-12, FR-08.4 | future plan GET; tick all items; GET | `NOT_STARTED`, `restSeconds: null` ("no rest time is shown"); "A plan is never Completed before its start date-time" (still NOT_STARTED at 100%) |
| C7 | AC-US4-7, FR-08.2, FR-08.4 | window plan GET, wait 3 s, GET; tick all; un-tick one | `IN_PROGRESS`, `restSeconds` ≈ end − now (±3 s) and decreasing; all done → `COMPLETED`; "setting an item back to not done before the end date-time moves the plan back to In Progress" |
| C8 | AC-US4-8, NFR-4 | create past plan (spec: "allowed; it is read as Completed"); GET twice; PUT times into the window, then into the future; GET | `COMPLETED`, `restSeconds null`; two reads identical; status follows the stored times on the next read (IN_PROGRESS, then NOT_STARTED) — computed on read, not stored |
| C9 | AC-US4-10, contract listPlans | GET ?group=active / completed / all / none; two future plans priority 3 and 2 | active = NOT_STARTED/IN_PROGRESS "(priorityOrder asc, then start asc)"; completed = COMPLETED "(end desc)"; "all = active then completed"; default all; each plan has status, progressPercent, items with done; in-window plan has restSeconds; priority 2 before 3 |
| C10 | AC-US4-11 | past plan with 3 items, tick one; GET ?group=completed | entry `COMPLETED` with `doneItems 1`, `totalItems 3`, `progressPercent 33` ("how much of it was completed"); plan moved to the future not listed |
| C11 | FR-07.6 | plan with a task (ticked) and habit; delete the task; GET plan; same for a habit and a card | item stays "with its done flag, marked as referring to a removed item, and still count toward the plan's progress" (`sourceRemoved true`, last title, done kept, totalItems 2, 50%); deleted task 404 |
| C12 | AC-US4-12, BR-14 | DELETE plan; GET, item PUT, list, second DELETE; re-read sources | 204; "never returned again": 404, not listed, 404; task, habit and card "unchanged" (identical JSON) |
| C13 | FR-07.1, FR-07.2, swagger PlanCreate | blank / 201-char / missing title; duration 0, 100001; priorityOrder 0; same (HABIT,id) twice; unknown sourceType; boundaries title 200, duration 1 and 100000 | 400 with field `title` / `estimatedDurationMinutes` / `priorityOrder`; duplicate "an entity appears at most once in the same plan" → 400; bad enum → 400; boundaries 201 |
| C14 | FR-07.3, contract updatePlan / setPlanItemDone | PUT plan fields; GET; PUT unknown id; GET unknown; item PUT `{}`; item of unknown plan; item of another plan | 200 with new title/duration/priority, "items are unchanged" (ids, flags); 404 NotFound; missing `done` (required) → 400 field `done`; 404 for unknown plan / foreign item |
| C15 | swagger | /v3/api-docs, backend/target/openapi.json, loops/backend-dev/outputs/openapi.json | operationIds listPlans, createPlan, getPlan, updatePlan, deletePlan, setPlanItemDone; 201 / 204; group enum; schemas Plan, PlanItem, PlanCreate, PlanUpdate, PlanItemRef, PlanItemUpdate, PlanStatus, PlanSourceType |
