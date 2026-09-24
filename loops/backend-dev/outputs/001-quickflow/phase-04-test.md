# phase-04-test: US2 Track recurring habits (backend-dev)

Layer: backend (API at `http://localhost:8080`). Script: `phase-04-verify.sh <attempt-folder>`
(curl only; raw requests/responses to `<attempt-folder>/curl.log`).

Sources: `loops/backend-dev/outputs/001-quickflow/phase-04.md` (acceptance criteria),
`specs/001-quickflow-backend/spec.md` (FR-03.x, FR-04.x, BR-6, BR-7, BR-14, A-12), the contract
`specs/001-quickflow-backend/contracts/openapi.yaml` (status codes; `BadRequest` / `NotFound` /
`Conflict` as `application/problem+json`, Conflict = "duplicate habit completion, inactive habit")
and the generated swagger `loops/backend-dev/outputs/openapi.json`.

"Today" is read from `GET /api/app-info` (`now`, app time zone Africa/Cairo). The Monday–Sunday
week (A-12) is computed from that date.

Criteria covered by this plan: AC-US2-1 .. AC-US2-8 (AC-US2-9 is frontend layer: not checked here).

| check | criterion | steps | expected (quoted) |
|---|---|---|---|
| C1 | AC-US2-1, FR-03.1, FR-03.2 | `POST /api/habits` `{name, description, frequency: DAILY}`; `POST` `{name, frequency: WEEKLY}` (no description); `GET /api/habits/{id}` each | 201 `Habit`; "stored as active with its creation time set": `active true`, `createdAt` ISO date-time with offset near now, name/description/frequency echoed (description `null` when omitted); GET 200 same values |
| C2 | AC-US2-2, BR-6, FR-03.3 | `POST` with name `""`, `"   "`, missing, 151 chars; 150 chars; `PUT` an existing habit with `""` and 151 chars | "rejected with a message naming the name field": 400 `application/problem+json`, `errors[]` entry `field == "name"` with a non-empty `message`; 150 chars → 201 ("at most 150 characters") |
| C3 | FR-03.1, FR-03.3 | `POST` with description 2,001 chars; with 2,000 chars; frequency missing; frequency `"MONTHLY"` | 2,001 → 400 problem naming `description`; 2,000 → 201; missing / `MONTHLY` frequency → 400 problem ("frequency MUST be Daily or Weekly") |
| C4 | AC-US2-3, FR-04.1 | active habit: `POST /api/habits/{id}/completions` with no body; second habit with `{}`; third with `{"date": today}`; `GET …/completions` | 201 `HabitCompletion` with `completionDate == today` ("default: today"), `habitId` = the habit, `id`, `createdAt` set; list holds exactly one record for today |
| C5 | AC-US2-4, BR-7, FR-04.2 | complete a habit for today, then again (no body and `{"date": today}`); complete for yesterday twice; `GET …/completions` | second attempt → 409 `application/problem+json` ("told it is already complete for that date": non-empty `detail` or `title`); "no second record is created": exactly one record per date in the list |
| C6 | FR-04.1 | `POST …/completions` `{"date": today+1}`; `GET …/completions` | 400 problem ("a future date is rejected"), errors name `date`; nothing recorded |
| C7 | AC-US2-5, FR-04.5 (DAILY) | new DAILY habit: GET (no completions); complete yesterday, today−2; GET; complete today; GET; new DAILY habit completed today−1 and today−3 (gap) | fresh: `completedToday false`, `doneForCurrentPeriod false`, `currentStreak 0`; with yesterday and today−2 only: "the previous one if the current is not yet done" → `doneForCurrentPeriod false`, streak 2; after today: `completedToday true`, `doneForCurrentPeriod true`, streak 3; gap habit: streak 1; `frequency` DAILY shown |
| C8 | AC-US2-5, FR-04.5 (WEEKLY), A-12 | new WEEKLY habit: complete on this week's Monday (or today if today is Monday) and on the Monday of last week and 2 weeks ago; GET; second WEEKLY habit completed only last Sunday (today's week start − 1 day) | first: `doneForCurrentPeriod true` ("the current Monday–Sunday week"), streak 3, `frequency` WEEKLY; second: `doneForCurrentPeriod false` (Sunday belongs to the previous week), streak 1. `completedToday` for WEEKLY is not checked when the completion is not today (its meaning for WEEKLY is not stated) |
| C9 | AC-US2-6, FR-03.2 | `PUT /api/habits/{id}` new name, description, frequency WEEKLY; `GET` | 200 with the new values ("the changes are saved"); GET shows them; `createdAt` unchanged |
| C10 | AC-US2-7, FR-03.2, FR-04.4 | habit with a completion yesterday; `POST …/deactivate`; `GET /api/habits?active=true`, `GET /api/habits`; `GET …/completions`; `POST …/completions`; `POST …/activate`; `POST …/completions` | deactivate 200 `active false`; "no longer listed among active habits" (absent from `active=true`, present in `active=false` / all); "keeps its history" (yesterday's completion still listed); completing → 409 problem ("cannot be completed"); activate 200 `active true`, listed again among active; completing → 201. ("today's habits" is the dashboard: no endpoint in this phase) |
| C11 | AC-US2-8, BR-14 | habit with 2 completions; `DELETE /api/habits/{id}`; `GET` by id, `GET …/completions`, `GET /api/habits`, `?active=true`, `?active=false`; `PUT`/`deactivate`/`activate`/`POST completions`/`DELETE` again; `GET /api/habits/999999` | DELETE 204; "the habit and its completions are never returned by normal queries again": GET 404 problem, completions 404, absent from every list; each later operation 404 |
| C12 | FR-04.3 | complete a habit for today; `DELETE …/completions/{today}`; `GET …/completions`; `DELETE` again | 204 ("undo a habit's completion for a date"); record gone; second undo 404 problem |
| C13 | swagger / contract | `GET /v3/api-docs`; read `backend/target/openapi.json` and the copy | the 10 habit operations at the contract's paths/methods (`listHabits`, `createHabit`, `getHabit`, `updateHabit`, `deleteHabit`, `deactivateHabit`, `activateHabit`, `listHabitCompletions`, `completeHabit`, `undoHabitCompletion`), `createHabit`/`completeHabit` 201, `deleteHabit`/`undoHabitCompletion` 204, schemas `Habit`, `HabitWrite`, `HabitCompletion`, `HabitCompletionCreate`, `HabitFrequency` [DAILY, WEEKLY]; target file and copy carry the same habit paths and schemas |
