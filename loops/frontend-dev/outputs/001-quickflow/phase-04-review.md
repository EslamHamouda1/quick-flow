# phase-04 review: US2 Track recurring habits
layer: frontend
story_id: US2
spec_phase: 4
depends_on: [phase-02, backend-dev:US2]

## Goal
The Habits page: add, edit, complete for today / undo, streak and period status, deactivate/activate, remove.

## Tasks
- [ ] T021 [US2] Check that the generated `HabitsService` has `listHabits`, `createHabit`, `updateHabit`, `deleteHabit`, `deactivateHabit`, `activateHabit`, `completeHabit`, `undoHabitCompletion` and that `Habit` has `completedToday`, `doneForCurrentPeriod`, `currentStreak`; check the Habits section of contracts/ui-contract.md covers AC-US2-1..9; raise a question for anything missing
- [ ] T022 [P] [US2] Create the habit form component (name, description, frequency Daily/Weekly, field errors) in frontend/src/app/pages/habits/habit-form.ts (AC-US2-1, AC-US2-2, AC-US2-6)
- [ ] T023 [US2] Implement the Habits page with habit cards (frequency label, period done text, streak, inactive badge), add/edit flow (also `?add=1`) and empty state in frontend/src/app/pages/habits/habits.page.ts (AC-US2-1, AC-US2-5, AC-US2-6, AC-US2-9)
- [ ] T024 [US2] Add the completion toggle (`completeHabit(id)` / `undoHabitCompletion(id, clock.today())`, disabled when inactive, 409 `detail` shown as a notice and the list re-read) in frontend/src/app/pages/habits/habits.page.ts (AC-US2-3, AC-US2-4, research R-9)
- [ ] T025 [US2] Add deactivate/activate and delete actions, each re-reading the list, in frontend/src/app/pages/habits/habits.page.ts (AC-US2-7, AC-US2-8)

Order: first `generate_client` (the loop's app-wide step, run at the start of implement so `HabitsService` exists;
`frontend/src/app/api/api` has only `InfoService` and `TasksService` now), then T021, then T022 (the only `[P]` task,
so one subagent or done inline), then T023 → T024 → T025 (all edit `habits.page.ts`, so one after another).

## Acceptance criteria
- AC-US2-1: Given no habits, When the user creates a habit with a name, optional description and frequency Daily or Weekly, Then it is stored as active with its creation time set.
- AC-US2-2: Given the habit form, When the user submits an empty name or a name longer than 150 characters, Then the habit is rejected with a message naming the name field (BR-6).
- AC-US2-3: Given an active habit, When the user marks it complete for today, Then one completion record for that habit and today's date is stored.
- AC-US2-4: Given a habit already completed for a date, When the user tries to complete it again for the same date, Then no second record is created and the user is told it is already complete for that date (BR-7).
- AC-US2-5: Given a habit with completions, When the user views it, Then its frequency label, whether it is complete for the current period and its current streak are shown.
- AC-US2-6: Given an existing habit, When the user edits its name, description or frequency, Then the changes are saved.
- AC-US2-7: Given an active habit, When the user deactivates it, Then it is no longer listed among active habits or today's habits, keeps its history, and cannot be completed until it is active again.
- AC-US2-8: Given a habit, When the user removes it, Then the habit and its completions are never returned by normal queries again (BR-14).
- AC-US2-9: Given no habits exist, When the Habits page shows the list, Then an empty state guides the user to the Add Habit action.

## Files
- app-wide: re-run `generate_client` → `frontend/src/app/api/**` (adds `HabitsService` and the `Habit`, `HabitWrite`,
  `HabitFrequency`, `HabitCompletion`, `HabitCompletionCreate` models; the existing Tasks/Info files are regenerated
  unchanged); `loops/frontend-dev/outputs/ui-url.md` rewritten unchanged.
- edit `specs/001-quickflow-frontend/research.md` R-2: the Habits names and signatures found in the generated client
  (T021), in particular how `completeHabit` takes its optional body.
- create `frontend/src/app/pages/habits/habit-form.ts` (T022): standalone `app-habit-form`, same shape as
  `task-form.ts`; inputs `habit` (`Habit | null`, null = create) and `errors` (`Record<string,string>`); outputs
  `save` (`HabitWrite`) and `cancel`; selectors `habit-form`, `habit-name`, `habit-description` (textarea),
  `habit-frequency` (select, options Daily / Weekly = `DAILY` / `WEEKLY`), `habit-save`, `habit-cancel`, and
  `app-field-error` for `name`, `description`, `frequency`. No `maxlength`/`required` attributes (FA-3).
- rewrite `frontend/src/app/pages/habits/habits.page.ts` (T023–T025): keeps `page-title` "Habits"; `habit-add`,
  `habit-list`, `habit-card-<id>` (with `habit-name-text`, `habit-frequency-label`, `habit-period-done`,
  `habit-streak`, `habit-inactive` badge when `active` is false), `habit-toggle-<id>` (checkbox), actions
  `habit-edit-<id>`, `habit-deactivate-<id>` (active cards), `habit-activate-<id>` (inactive cards),
  `habit-delete-<id>`; `app-empty-state`. Uses `NoticeService`, `readProblem`, `ClockService.today()` (phase-02,
  unchanged). `habit-name-text` is an extra selector not in the ui-contract (see FA-21); nothing listed there is
  dropped.
- endpoints used: `GET /api/habits` (`listHabits`, `active` omitted = all, as in data-model.md), `POST /api/habits`
  (`createHabit`), `PUT /api/habits/{id}` (`updateHabit`), `DELETE /api/habits/{id}` (`deleteHabit`),
  `POST /api/habits/{id}/deactivate|activate`, `POST /api/habits/{id}/completions` (`completeHabit`, no body),
  `DELETE /api/habits/{id}/completions/{date}` (`undoHabitCompletion`). `getHabit` and `listHabitCompletions` are not
  used (the form is filled from the listed card; the card shows the server's progress fields).
- no other file changes (routes, shell and shared components stay as in phase-02).

## Planned checks
- Implement session (quick feedback only, not verification): `cd frontend && npm run build` succeeds; output in
  `loops/frontend-dev/runs/001-quickflow/phase-04-build.log`.
- No unit tests: the frontend layer has no `coverage` and `test` is empty (FA-1, approved).
- Testing loop (Playwright MCP, headless) can check, on `http://localhost:4200/habits`:
  - no habits → `empty-state` "No habits yet" with `empty-state-action` "Add Habit", which opens `habit-form`
    (AC-US2-9). `/habits?add=1` opens `habit-form` directly.
  - `habit-add` → name, description, frequency Weekly → `habit-save` → `habit-card-<id>` appears with
    `habit-frequency-label` "Weekly", `habit-period-done` "Not done", `habit-streak` "0", no `habit-inactive`; a
    `notice` shows; the form closes; API `active: true`, `createdAt` set (AC-US2-1).
  - empty name, or a 151-character name → form stays open, `error-name` shows the server's message; no habit added
    (AC-US2-2).
  - `habit-toggle-<id>` on an active Daily habit → checked, `habit-period-done` "Done today", `habit-streak` "1";
    `GET /api/habits/{id}/completions` has exactly one record with today's app-zone date (AC-US2-3, AC-US2-5).
    On a Weekly habit → "Done this week".
  - BR-7 through the UI (AC-US2-4): with the page open, complete the habit for today through the API; then click
    the still-unchecked `habit-toggle-<id>` → `notice-error` with the server's `detail` ("Habit <id> is already
    complete for <date>"), the card re-reads to checked, the API still has one record for that date.
  - unchecking `habit-toggle-<id>` → unchecked, "Not done", streak back; the API has no record for today (FR-04.3).
  - `habit-edit-<id>` → form prefilled → change name, description and frequency → `habit-save` → card shows the new
    name and frequency label; API has the new description (AC-US2-6).
  - `habit-deactivate-<id>` → card shows `habit-inactive`, `habit-toggle-<id>` disabled, `habit-activate-<id>`
    shown; API `listHabits?active=true` omits it and its completions are still listed; `habit-activate-<id>` →
    badge gone, toggle enabled again (AC-US2-7; "today's habits" on the Dashboard is US5).
  - `habit-delete-<id>` → card gone, no confirm dialog (FA-7); `GET /api/habits/{id}` → 404 (AC-US2-8).

## Risks
- **Client not regenerated yet**: the swagger now has all ten habit operations (checked now), so the implement
  session runs `generate_client` first; T021 then checks the generated method signatures (positional arguments, as
  with `TasksService`; the optional `HabitCompletionCreate` body of `completeHabit`; `undoHabitCompletion(id, date)`
  with `date` as a string) before code uses them. If the generated code does not compile under Angular 22.2.0, that
  becomes a question (generated files are never patched).
- **Error responses are not declared in the swagger** (see Contract differences), so still no `Problem` model is
  generated; `core/problem.ts` is used. Checked now in `backend/.../web/ApiExceptionHandler.java` and
  `domain/habit/HabitService.java`: a 409 carries `detail` "Habit <id> is already complete for <date>" or
  "Habit <id> is inactive"; bean-validation 400s name the field `name` / `description` / `frequency` (the record
  field names of `HabitWriteRequest`), so `error-name` works.
- **Undo date**: `undoHabitCompletion` needs the date; the page sends `clock.today()` (app zone, research R-3). The
  server's `completedToday` uses its own today; the two agree except in the second around midnight. A mismatch there
  gives a 404, shown as `notice-error` and followed by a re-read (FA-22), so the card never stays wrong.
- **Checkbox state after a failed call**: the browser flips a checkbox on click before the request ends. The toggle
  is bound to `completedToday` from the list and the list is re-read after every call, success or error, so a
  failed call snaps it back (FA-22).
- **Weekly period text** depends only on the server's `doneForCurrentPeriod` (Monday–Sunday week, FR-04.5); the UI
  computes nothing (Constitution IV). A weekly habit done earlier this week shows "Done this week" with the today
  toggle unchecked; that is expected (R-9: the toggle is for today).
- `?add=1` must open the form also when the page is already open (Dashboard quick-add arrives in US5); same handling
  as the Tasks page.

## Assumptions
- **FA-20 (new) Form defaults and empty fields**: the create form starts with frequency Daily; an empty description
  is sent as `null`; on edit, clearing it clears the stored value (PUT replaces all three fields).
- **FA-21 (new) Card content**: each card shows the name (`habit-name-text`), the description when set, the
  `habit-frequency-label` (Daily / Weekly, data-model.md labels), `habit-period-done` ("Done today" for Daily /
  "Done this week" for Weekly when `doneForCurrentPeriod`, otherwise "Not done"), `habit-streak` as the bare number
  with a visible "Streak" label beside it (not inside the testid element), and the `habit-inactive` badge "Inactive".
  The list shows all habits (active and inactive) in the server's order (oldest first); inactive cards stay listed so
  they can be activated again.
- **FA-22 (new) Toggle and actions**: checking `habit-toggle-<id>` calls `completeHabit(id)` with no body;
  unchecking calls `undoHabitCompletion(id, clock.today())`. While a call for a card is running, that card's toggle
  and actions are disabled (no double submit). After every call (success or error) the list is re-read. Only the
  deactivate or the activate action is shown, by `active`; edit and delete are on every card.
- **FA-23 (new) Notices and form**: success notices "Habit created", "Habit updated", "Habit completed", "Habit
  completion undone", "Habit deactivated", "Habit activated", "Habit deleted"; a 409 or any other error (404,
  network) goes to `notice-error` with the server's `detail`. A 400 on save keeps the form open with the field
  messages; a 400 whose field has no input on the form is shown as `notice-error`. `?add=1` is removed with
  `replaceUrl` when the form closes. Only one form is open at a time, shown above the list.
- **FA-24 (new) Empty state text**: "No habits yet" with action "Add Habit" (opens the create form).
- FA-1, FA-3 (server-side validation messages only), FA-7 (deletes without confirmation), FA-10 (no extra
  dependencies, plain CSS) and FA-11 (only valid enum values offered) apply as approved at phase-00.

## Open questions
None.

## Swagger input
`loops/backend-dev/outputs/openapi.json` (from task.md).

## Contract differences
Compared with `specs/001-quickflow-backend/contracts/openapi.yaml` for the US2 endpoints (checked now):
- Paths, methods, operationIds (`listHabits`, `createHabit`, `getHabit`, `updateHabit`, `deleteHabit`,
  `deactivateHabit`, `activateHabit`, `listHabitCompletions`, `completeHabit`, `undoHabitCompletion`), the `active`
  query parameter, the `{date}` path parameter (`string`, `format: date`), request bodies (`HabitWrite` required;
  `HabitCompletionCreate` optional on `completeHabit`) and success responses (200 / 201 / 204): match.
- Schemas `Habit`, `HabitWrite`, `HabitFrequency`, `HabitCompletion`, `HabitCompletionCreate`: match (same required
  lists, types, enums). Extras in the generated swagger only: `description` has `minLength: 0` in `HabitWrite`, and
  `currentStreak` has `format: int32`. No effect on the client (both give `number`).
- Error responses: the contract declares `400 BadRequest` (createHabit, updateHabit, completeHabit), `404 NotFound`
  (every `{id}` operation) and `409 Conflict` (completeHabit) as `application/problem+json` `Problem`; the generated
  swagger declares none. The backend does return them (see Risks); the frontend uses its local `Problem` type.
- Response descriptions differ in wording only (e.g. "Created (active)", "Recorded", "Removed" vs "Created", "OK",
  "No Content"). No effect.
