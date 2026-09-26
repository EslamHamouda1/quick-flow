# test-phase-01: API contract

## Goal
Every operation in the swagger (`loops/backend-dev/outputs/openapi.json`, 39 operations) is called with curl
against `http://localhost:8080`; its status codes (2xx from the swagger, 400/404/409 from
`specs/001-quickflow-backend/contracts/openapi.yaml`) and response schema are checked, and every
create/update/delete/filter call is timed with `curl -w '%{time_total}'` against NFR-1 (under 500 ms).

Sources: swagger, planning contract, Task_PRD.md §10 (NFR-1). Stories: none (cross-cutting). Evidence:
`runs/001-quickflow/suite/test-phase-01/attempt-<n>/curl.log` (request, status, body, time_total).

## Checks
- [x] C1 Swagger: `GET /v3/api-docs` 200; its 39 operationIds equal the ones in `loops/backend-dev/outputs/openapi.json`.
- [x] C2 App info: `getAppInfo` 200, body matches `AppInfo` (time zone `Africa/Cairo` per config).
- [x] C3 Tasks: `createTask` 201 (`Task` schema), `listTasks` 200 (array of `Task`; query params accepted), `listOverdueTasks` 200, `getTask` 200 / 404, `updateTask` 200 / 400 / 404, `completeTask` 200 / 404, `archiveTask` 200 / 404, `restoreTask` 200 / 404, `deleteTask` 204 / 404; invalid body → 400 with the error body shape (`errors[].field`).
- [x] C4 Habits: `createHabit` 201 / 400, `listHabits` 200, `getHabit` 200 / 404, `updateHabit` 200 / 400 / 404, `deactivateHabit` 200 / 404, `activateHabit` 200 / 404, `completeHabit` 201 / 400 (future date) / 404 / 409 (same date again), `listHabitCompletions` 200 / 404, `undoHabitCompletion` 204 / 404, `deleteHabit` 204 / 404.
- [x] C5 Learning cards: `createLearningCard` 201 / 400, `listLearningCards` 200, `getLearningCard` 200 / 404, `updateLearningCard` 200 / 400 / 404, `addMilestone` 201 / 400 / 404, `updateMilestone` 200 / 400 / 404, `deleteMilestone` 204 / 404, `addNote` 201 / 400 / 404, `deleteNote` 204 / 404, `deleteLearningCard` 204 / 404.
- [x] C6 Plans: `createPlan` 201 / 400 (no items, end ≤ start), `listPlans` 200, `getPlan` 200 / 404, `updatePlan` 200 / 400 / 404, `setPlanItemDone` 200 / 400 / 404, `deletePlan` 204 / 404.
- [x] C7 Dashboard and settings: `getDashboard` 200 (`Dashboard` schema), `getSettings` 200 (`Settings` schema), `updateSettings` 200 / 400.
- [x] C8 Schemas: each 2xx body carries exactly the required properties of its swagger schema, with the stated types and enum values.
- [x] C9 NFR-1: every create/update/delete/filter call above has `time_total` < 0.500 s.
