# test-phase-01: API contract

## Goal
Every operation in the swagger (`loops/backend-dev/outputs/openapi.json`, 41 operations, including the two this
feature adds, `addTaskTags` and `removeTaskTag`, and the new `tag` query parameter of `listTasks`) is called with curl
against `http://localhost:8080`; its status codes (2xx from the swagger, 400/404/409 from
`specs/001-quickflow-backend/contracts/openapi.yaml` and `specs/002-us-tags-backend/contracts/openapi.yaml`) and
response schema are checked, and every call is timed with `curl -w '%{time_total}'`: the tag operations against
SC-002 (under 500 ms) and every create/update/delete/filter call against the app-wide limit SC-002 cites (PRD §10,
constitution "Performance": under 500 ms).

Sources: swagger, both planning contracts, `specs/002-us-tags-backend/spec.md` (SC-002), constitution. Stories: none
(cross-cutting). Evidence: `runs/002-us-tags/suite/test-phase-01/attempt-<n>/curl.log` (request, status, body,
time_total).

## Checks
- [x] C1 Swagger: `GET /v3/api-docs` 200; its 41 operationIds equal the ones in `loops/backend-dev/outputs/openapi.json`.
- [x] C2 App info: `getAppInfo` 200, body matches `AppInfo` (time zone `Africa/Cairo` per config).
- [x] C3 Tasks: `createTask` 201 (`Task` schema, `tags` = `[]` on a new task: spec Assumptions "Existing tasks start with no tags", `Task.required` has `tags`), `listTasks` 200 (array of `Task`; query params accepted, including `tag`), `listOverdueTasks` 200, `getTask` 200 / 404, `updateTask` 200 / 400 / 404, `completeTask` 200 / 404, `archiveTask` 200 / 404, `restoreTask` 200 / 404, `deleteTask` 204 / 404; invalid body → 400 with the error body shape (`errors[].field`).
- [x] C4 Task tags: `addTaskTags` 200 (`Task`) / 400 (`{"tags":[]}`, `errors[].field` = `tags`) / 404 (unknown id); `removeTaskTag` 200 (`Task`) / 400 (no `tag` query) / 404 (unknown id); `listTasks?tag=<t>` 200.
- [x] C5 Habits: `createHabit` 201 / 400, `listHabits` 200, `getHabit` 200 / 404, `updateHabit` 200 / 400 / 404, `deactivateHabit` 200 / 404, `activateHabit` 200 / 404, `completeHabit` 201 / 400 (future date) / 404 / 409 (same date again), `listHabitCompletions` 200 / 404, `undoHabitCompletion` 204 / 404, `deleteHabit` 204 / 404.
- [x] C6 Learning cards: `createLearningCard` 201 / 400, `listLearningCards` 200, `getLearningCard` 200 / 404, `updateLearningCard` 200 / 400 / 404, `addMilestone` 201 / 400 / 404, `updateMilestone` 200 / 400 / 404, `deleteMilestone` 204 / 404, `addNote` 201 / 400 / 404, `deleteNote` 204 / 404, `deleteLearningCard` 204 / 404.
- [x] C7 Plans: `createPlan` 201 / 400 (no items, end ≤ start), `listPlans` 200, `getPlan` 200 / 404, `updatePlan` 200 / 400 / 404, `setPlanItemDone` 200 / 400 / 404, `deletePlan` 204 / 404.
- [x] C8 Dashboard and settings: `getDashboard` 200 (`Dashboard` schema; its task entries carry `tags`), `getSettings` 200 (`Settings` schema), `updateSettings` 200 / 400.
- [x] C9 Schemas: each 2xx body carries exactly the required properties of its swagger schema, with the stated types and enum values; every `Task` body's `tags` is an array of unique strings of 1–30 characters, at most 10, lower case and alphabetical (swagger `Task.tags`).
- [x] C10 Response time: `addTaskTags`, `removeTaskTag` and `listTasks?tag=` (SC-002) and every other create/update/delete/filter call above have `time_total` < 0.500 s.
