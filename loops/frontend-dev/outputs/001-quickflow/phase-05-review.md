# phase-05 review: US3 Track learning resources with milestones and notes
layer: frontend
story_id: US3
spec_phase: 5
depends_on: [phase-02, backend-dev:US3]

## Goal
The Learning Resources page: card grid, expandable milestones and notes, status, remove.

## Tasks
- [ ] T026 [US3] Check that the generated `LearningService` has `listLearningCards`, `createLearningCard`, `updateLearningCard`, `deleteLearningCard`, `addMilestone`, `updateMilestone`, `deleteMilestone`, `addNote`, `deleteNote`; check the Learning section of contracts/ui-contract.md covers AC-US3-1..9; raise a question for anything missing
- [ ] T027 [P] [US3] Create the card form component (title, description/source, field errors; edit also saves status) in frontend/src/app/pages/learning/card-form.ts (AC-US3-1, AC-US3-2)
- [ ] T028 [P] [US3] Create the learning card component (title, status select saving via `updateLearningCard`, milestone count, expand toggle, milestone list with done checkbox/remove and add row, note list with time in the app zone/remove and add row, field errors per sub-form) in frontend/src/app/pages/learning/learning-card.ts (AC-US3-3..6, AC-US3-8)
- [ ] T029 [US3] Implement the Learning Resources page (card grid, add flow also from `?add=1`, card delete, re-read after every change, empty state) in frontend/src/app/pages/learning/learning.page.ts (AC-US3-1, AC-US3-7, AC-US3-9)

Order: first `generate_client` (the loop's app-wide step, run at the start of implement so `LearningService` exists;
`frontend/src/app/api/api` has only `InfoService`, `TasksService` and `HabitsService` now), then T026, then T027 and
T028 (the `[P]` group: one subagent each, they write different files), then T029 (uses both components).

## Acceptance criteria
- AC-US3-1: Given no cards, When the user adds a card with a title and optional description/source, Then it is stored with status Not Started and its creation time set.
- AC-US3-2: Given the card form, When the user submits an empty title, Then the card is rejected with a message naming the title field (BR-8).
- AC-US3-3: Given a card, When the user changes its status to Not Started, In Progress or Completed, Then the new status is saved; any other value is rejected.
- AC-US3-4: Given a card, When the user adds a milestone with a title and an optional target date, Then the milestone is stored under that card only, not done (BR-9).
- AC-US3-5: Given a milestone, When the user marks it done (or not done again), Then its done flag is saved; When the user removes it, Then it is no longer returned.
- AC-US3-6: Given a card, When the user adds a note with text, Then it is stored under that card with its timestamp; When the user removes the note, Then it is no longer returned.
- AC-US3-7: Given a card with milestones and notes, When the user removes the card, Then the card, its milestones and its notes are never returned by normal queries again (BR-14).
- AC-US3-8: Given a card, When the user expands it, Then its milestones and notes are shown.
- AC-US3-9: Given no cards exist, When the page shows the grid, Then an empty state guides the user to the Add Learning Card action.

## Files
- app-wide: re-run `generate_client` → `frontend/src/app/api/**` (adds `LearningService` and the `LearningCard`,
  `LearningCardCreate`, `LearningCardUpdate`, `LearningStatus`, `Milestone`, `MilestoneCreate`, `MilestoneUpdate`,
  `Note`, `NoteCreate` models; the Tasks/Habits/Info files are regenerated unchanged);
  `loops/frontend-dev/outputs/ui-url.md` rewritten unchanged.
- edit `specs/001-quickflow-frontend/research.md` R-2: the Learning names and signatures found in the generated client
  (T026), in particular the argument order of `updateMilestone(id, milestoneId, body)` / `deleteNote(id, noteId)` and
  the `LearningStatus` constant names.
- create `frontend/src/app/pages/learning/card-form.ts` (T027): standalone `app-card-form`, same shape as
  `habit-form.ts`; inputs `card` (`LearningCard | null`, null = create) and `errors` (`Record<string,string>`);
  output `save` (`{title, description}`) and `cancel`; selectors `card-form`, `card-title`, `card-description`
  (textarea, label "Description / source"), `card-save`, `card-cancel`, and `app-field-error` for `title`,
  `description`, `status`. No `maxlength`/`required` attributes (FA-3). The page turns `save` into
  `createLearningCard({title, description})` or `updateLearningCard(id, {title, description, status: card.status})`
  (FA-25).
- create `frontend/src/app/pages/learning/learning-card.ts` (T028): standalone `app-learning-card`; input `card`
  (`LearningCard`), `expanded` (boolean), `busy` (boolean); it calls `LearningService` itself for status, milestones
  and notes and emits `changed` (the page re-reads), `edit`, `remove`, `toggle`. Selectors: `card-<id>` (root),
  `card-title-text`, `card-description-text` (when set; extra, FA-27), `card-status` (select Not Started / In
  Progress / Completed = `NOT_STARTED` / `IN_PROGRESS` / `COMPLETED`), `card-milestone-count` ("<milestonesDone>/
  <milestonesTotal>" from the server), `card-expand-<id>`, `card-edit-<id>`, `card-delete-<id>`; when expanded
  `card-details-<id>` with `milestone-<id>` rows (title, target date via `formatDate`, `milestone-done-<id>`
  checkbox, `milestone-delete-<id>`), `milestone-title-input-<cardId>`, `milestone-date-input-<cardId>`
  (`type="date"`), `milestone-add-<cardId>`, `note-<id>` rows (text and `formatDateTime(createdAt, clock zone)`,
  `note-delete-<id>`), `note-text-input-<cardId>` (textarea), `note-add-<cardId>`; field errors of the milestone and
  note sub-forms as `app-field-error` for `title`, `targetDate`, `text` inside `card-details-<id>` (FA-28).
- rewrite `frontend/src/app/pages/learning/learning.page.ts` (T029): keeps `page-title` "Learning Resources";
  `card-add`, the `app-card-form` above the grid, `card-grid` with one `app-learning-card` per card in the server's
  order (newest first), `app-empty-state`; `expanded` kept as a `Set<id>` signal in the page so a re-read keeps cards
  open (data-model.md "Learning page"); `?add=1` handled like the Habits page. Uses `NoticeService`, `readProblem`,
  `ClockService` (phase-02, unchanged).
- endpoints used: `GET /api/learning-cards` (`listLearningCards`), `POST /api/learning-cards`
  (`createLearningCard`), `PUT /api/learning-cards/{id}` (`updateLearningCard`), `DELETE /api/learning-cards/{id}`
  (`deleteLearningCard`), `POST /api/learning-cards/{id}/milestones` (`addMilestone`),
  `PUT /api/learning-cards/{id}/milestones/{milestoneId}` (`updateMilestone`),
  `DELETE /api/learning-cards/{id}/milestones/{milestoneId}` (`deleteMilestone`), `POST /api/learning-cards/{id}/notes`
  (`addNote`), `DELETE /api/learning-cards/{id}/notes/{noteId}` (`deleteNote`). `getLearningCard` is not used (the
  list already carries milestones and notes).
- no other file changes (routes, shell and shared components stay as in phase-02).

## Planned checks
- Implement session (quick feedback only, not verification): `cd frontend && npm run build` succeeds; output in
  `loops/frontend-dev/runs/001-quickflow/phase-05-build.log`.
- No unit tests: the frontend layer has no `coverage` and `test` is empty (FA-1, approved).
- Testing loop (Playwright MCP, headless) can check, on `http://localhost:4200/learning`:
  - no cards → `empty-state` "No learning cards yet" with `empty-state-action` "Add Learning Card", which opens
    `card-form` (AC-US3-9). `/learning?add=1` opens `card-form` directly.
  - `card-add` → title, description → `card-save` → `card-<id>` appears first in `card-grid` with
    `card-title-text`, `card-status` "Not Started", `card-milestone-count` "0/0"; a `notice` shows; the form closes;
    API `status: NOT_STARTED`, `createdAt` set (AC-US3-1).
  - empty (or blank) title → form stays open, `error-title` inside `card-form` shows the server's message; no card
    added (AC-US3-2).
  - `card-status` → "In Progress", then "Completed" → API status follows each change; the select offers only the three
    values (AC-US3-3; "any other value is rejected" is a server rule, checked by curl in backend US3).
  - `card-expand-<id>` → `card-details-<id>` visible; again → hidden (AC-US3-8).
  - in `card-details-<id>`: milestone title + date → `milestone-add-<cardId>` → `milestone-<id>` row with the title and
    the date, `milestone-done-<id>` unchecked, `card-milestone-count` "0/1"; API: the milestone is under that card
    only, `done: false`, same `targetDate` (AC-US3-4). Empty milestone title → `error-title` inside
    `card-details-<id>`, nothing added.
  - `milestone-done-<id>` → checked, count "1/1", API `done: true`; again → unchecked, "0/1" (AC-US3-5);
    `milestone-delete-<id>` → row gone, count "0/0", API card has no such milestone (AC-US3-5).
  - note text → `note-add-<cardId>` → `note-<id>` shows the text and its time in the app zone; API note has
    `createdAt` (AC-US3-6). Empty text → `error-text`, nothing added. `note-delete-<id>` → row gone, API card has no
    such note (AC-US3-6).
  - `card-edit-<id>` → form prefilled → change title and description → `card-save` → card shows them; status
    unchanged (FA-25).
  - card with milestones and notes → `card-delete-<id>` → card gone, no confirm dialog (FA-7);
    `GET /api/learning-cards/{id}` → 404 and its milestone/note sub-paths → 404 (AC-US3-7).

## Risks
- **Client not regenerated yet**: the swagger has all ten learning operations (checked now), so the implement session
  runs `generate_client` first; T026 then checks the generated method signatures (positional arguments, as with
  `TasksService` / `HabitsService`) before code uses them. If the generated code does not compile under Angular
  22.2.0, that becomes a question (generated files are never patched).
- **Error responses are not declared in the swagger** (see Contract differences), so still no `Problem` model;
  `core/problem.ts` is used. Checked now in `backend/.../web/learning/*Request.java` and
  `domain/learning/LearningService.java`: bean-validation 400s name the fields `title`, `description`, `status`,
  `targetDate`, `done`, `text` (record field names); 404 `detail`s are "Learning card <id> not found", "Milestone <m>
  not found on learning card <id>", "Note <n> not found on learning card <id>".
- **Duplicate `error-title` testid**: the card form and each card's milestone add row both use field `title`. They
  are scoped by parent (`card-form` vs `card-details-<id>`), and a sub-form's errors are cleared when another form
  is submitted (FA-28), so at most one is shown at a time in practice; checks should still scope by parent.
- **Status and done need the full body**: `updateLearningCard` requires `title` and `status`, `updateMilestone`
  requires `title` and `done` (PUT replaces). A status change sends the card's current title/description; a done
  toggle sends the milestone's current title/targetDate. Values come from the last list read, so an edit made in
  another tab in between would be overwritten (single-user app; accepted, FA-26).
- **Checkbox / select state after a failed call**: the browser changes the control before the request ends. Both are
  bound to the server values and the list is re-read after every call, success or error, so a failure snaps them back
  (same as FA-22).
- **Milestone target date**: `type="date"` gives `YYYY-MM-DD` (the swagger's `format: date`); empty is sent as `null`.
  Shown with `formatDate` (no zone shift). The server does not restrict past dates (none in spec.md).
- Note order is oldest first and milestone order is by id, as the server returns them (`@OrderBy`); the UI does not
  re-sort.

## Assumptions
- **FA-25 (new) Card form and edit**: the form has title and "Description / source" only (the ui-contract lists no
  status field in `card-form`); on edit it sends `updateLearningCard(id, {title, description, status: <current>})`,
  so "edit also saves status" in T027 means the current status is kept; status is changed only by the card's
  `card-status` select. The form starts empty; an empty description is sent as `null`; on edit, clearing it clears
  the stored value. Only one form is open at a time, shown above the grid.
- **FA-26 (new) Card actions**: `card-status` change calls `updateLearningCard` with the card's title, description and
  the new status; `milestone-done-<id>` calls `updateMilestone(cardId, id, {title, targetDate, done: <new>})`. While a
  call for a card runs, that card's controls are disabled (no double submit). After every call (success or error) the
  list is re-read; expanded cards stay expanded. A new card starts collapsed.
- **FA-27 (new) Card content**: each card shows `card-title-text`, the description when set (`card-description-text`,
  extra selector not in the ui-contract; nothing listed there is dropped), `card-status`, `card-milestone-count`, and
  the expand / edit / delete buttons. Expanded: a "Milestones" list with its add row, then a "Notes" list with its
  add row; an empty list shows "No milestones yet" / "No notes yet" (text only, no `empty-state` testid, which is the
  page's grid empty state). The add inputs are cleared after a successful add and kept after an error.
- **FA-28 (new) Notices and errors**: success notices "Learning card created", "Learning card updated", "Status
  updated", "Learning card deleted", "Milestone added", "Milestone updated", "Milestone removed", "Note added", "Note
  removed". A 400 on a form keeps it open with the field messages under that form (card form: `title`,
  `description`, `status`; milestone row: `title`, `targetDate`; note row: `text`); a 400 whose field has no input
  there, and every other error (404, network), goes to `notice-error` with the server's `detail`. Submitting any form
  clears the errors of the others. `?add=1` is removed with `replaceUrl` when the form closes.
- **FA-29 (new) Empty state text**: "No learning cards yet" with action "Add Learning Card" (opens the create form);
  the page's add button `card-add` reads "Add Learning Card" (PRD "Add Learning Card action").
- FA-1, FA-3 (server-side validation messages only), FA-7 (deletes without confirmation), FA-10 (no extra
  dependencies, plain CSS) and FA-11 (only valid enum values offered) apply as approved at phase-00.

## Open questions
None.

## Swagger input
`loops/backend-dev/outputs/openapi.json` (from task.md).

## Contract differences
Compared with `specs/001-quickflow-backend/contracts/openapi.yaml` for the US3 endpoints (checked now):
- Paths, methods, operationIds (`listLearningCards`, `createLearningCard`, `getLearningCard`, `updateLearningCard`,
  `deleteLearningCard`, `addMilestone`, `updateMilestone`, `deleteMilestone`, `addNote`, `deleteNote`), path
  parameters (`id`, `milestoneId`, `noteId`: `integer` `int64`), request bodies (all required) and success responses
  (200 / 201 / 204): match.
- Schemas `LearningStatus`, `LearningCardCreate`, `LearningCardUpdate`, `LearningCard`, `MilestoneCreate`,
  `MilestoneUpdate`, `Milestone`, `NoteCreate`, `Note`: match (same required lists, types, enums, length limits).
  Extras in the generated swagger only: `description` has `minLength: 0` in `LearningCardCreate` /
  `LearningCardUpdate`, and `milestonesDone` / `milestonesTotal` have `format: int32`. No effect on the client (both
  give `number` / `string | null`).
- Error responses: the contract declares `400 BadRequest` (create/update card, add/update milestone, add note) and
  `404 NotFound` (every `{id}` operation) as `application/problem+json` `Problem`; the generated swagger declares none.
  The backend does return them (see Risks); the frontend uses its local `Problem` type.
- Response descriptions differ in wording only (e.g. "Created (NOT_STARTED)", "Added (not done)", "Deleted",
  "Removed" vs "Created", "OK", "No Content"). No effect.
