# phase-03 review: US1 Manage tasks
layer: frontend
story_id: US1
spec_phase: 3
depends_on: [phase-02, backend-dev:US1]

## Goal
The Tasks page: add, edit, complete, archive, restore, delete, search, filter, sort and the overdue view.

## Tasks
- [ ] T015 [US1] Check that the generated `TasksService` has `listTasks` (all 8 query parameters), `listOverdueTasks`, `createTask`, `updateTask`, `completeTask`, `archiveTask`, `restoreTask`, `deleteTask` and that the Tasks section of specs/001-quickflow-frontend/contracts/ui-contract.md covers AC-US1-1..12; raise a question for anything missing
- [ ] T016 [P] [US1] Create the task form component (create + edit, fields title/description/priority/due date, status when editing, no client-side length checks, shows `fieldErrors` via `field-error`) in frontend/src/app/pages/tasks/task-form.ts (AC-US1-1..5)
- [ ] T017 [US1] Implement the Tasks page list with search box, status/priority/due-from/due-to/archived filters and sort field/direction bound to `listTasks` parameters (defaults as in the contract), rows with overdue badge, in frontend/src/app/pages/tasks/tasks.page.ts (FR-02, AC-US1-9, AC-US1-10)
- [ ] T018 [US1] Add create/edit flow (open `task-form` from `task-add`, from `task-edit-<id>` and from `?add=1`; on 400 keep the form open with field errors; on success re-read the list and show a notice) in frontend/src/app/pages/tasks/tasks.page.ts (AC-US1-1..5)
- [ ] T019 [US1] Add row actions complete, archive, restore (archived rows), delete, each re-reading the list, in frontend/src/app/pages/tasks/tasks.page.ts (AC-US1-6, AC-US1-7, AC-US1-8)
- [ ] T020 [US1] Add the overdue view toggle (`task-view-overdue` → `listOverdueTasks`) and the empty state pointing to Add Task, in frontend/src/app/pages/tasks/tasks.page.ts (AC-US1-11, AC-US1-12)

Order: first `generate_client` (the loop's app-wide step, run at the start of implement so `TasksService` exists), then
T015, then T016 (the only `[P]` task, so one subagent or done inline), then T017 → T018 → T019 → T020 (all edit
`tasks.page.ts`, so one after another).

## Acceptance criteria
- AC-US1-1: Given no tasks, When the user creates a task with a title, optional description, priority and due date, Then the task is stored with status Todo, not archived, and with its creation and update times set.
- AC-US1-2: Given the task form, When the user submits an empty title or a title longer than 200 characters, Then the task is rejected with a message naming the title field (BR-1).
- AC-US1-3: Given the task form, When the user submits a description longer than 2,000 characters, Then the task is rejected with a message naming the description field (BR-2).
- AC-US1-4: Given a task, When a status other than Todo / In Progress / Done or a priority other than Low / Medium / High is submitted, Then the change is rejected (BR-3). (UI part per FA-11: only valid values are offered.)
- AC-US1-5: Given an existing task, When the user edits its title, description, status, priority or due date, Then the changes are saved and its update time changes.
- AC-US1-6: Given a task, When the user marks it completed, Then its status is Done (BR-5).
- AC-US1-7: Given a task, When the user archives it, Then it no longer appears in the default task list but can still be listed by asking for archived tasks, and restoring it brings it back to the default list (BR-4).
- AC-US1-8: Given a task, When the user deletes it, Then it is never returned by any normal list, search, lookup or summary again (BR-14).
- AC-US1-9: Given several tasks, When the user searches by text, Then only tasks whose title contains that text (ignoring case) are listed.
- AC-US1-10: Given several tasks, When the user filters by status, priority and/or due date and chooses a sort (due date or creation date), Then only matching tasks are listed, in that order.
- AC-US1-11: Given a task with a due date before today that is not Done and not archived, When the user asks for overdue tasks, Then that task is listed as overdue; a task due today, a Done task or an archived task is not.
- AC-US1-12: Given no tasks match (or none exist), When the Tasks page shows the list, Then an empty state guides the user to the Add Task action.

## Files
- app-wide: re-run `generate_client` → `frontend/src/app/api/**` (adds `TasksService` and the `Task`, `TaskCreate`,
  `TaskUpdate`, `TaskStatus`, `TaskPriority` models); `loops/frontend-dev/outputs/ui-url.md` rewritten unchanged.
- edit `specs/001-quickflow-frontend/research.md` R-2: the Tasks names found in the generated client (T015).
- create `frontend/src/app/pages/tasks/task-form.ts` (T016): standalone `app-task-form`; inputs `task` (`Task | null`,
  null = create) and `errors` (`Record<string,string>`); outputs `save` (`TaskCreate` or `TaskUpdate`) and `cancel`;
  selectors `task-form`, `task-title`, `task-description`, `task-priority`, `task-due-date` (`<input type="date">`),
  `task-status` (edit only), `task-save`, `task-cancel`, and `app-field-error` for `title`, `description`,
  `status`, `priority`, `dueDate`. No `maxlength`/`required` attributes (FA-3).
- rewrite `frontend/src/app/pages/tasks/tasks.page.ts` (T017–T020): keeps `page-title` "Tasks"; `task-add`,
  `task-search`, `filter-status`, `filter-priority`, `filter-due-from`, `filter-due-to`, `filter-archived`,
  `sort-field`, `sort-direction`, `task-view-overdue`, `task-list`, `task-row-<id>` (with `task-row-title`,
  `task-row-status`, `task-row-priority`, `task-row-due`, `task-row-overdue`), row actions `task-edit-<id>`,
  `task-complete-<id>`, `task-archive-<id>`, `task-restore-<id>`, `task-delete-<id>`; `app-empty-state`.
  Uses `NoticeService`, `readProblem`, `formatDate` (phase-02, unchanged).
- endpoints used: `GET /api/tasks` (`listTasks`), `GET /api/tasks/overdue` (`listOverdueTasks`), `POST /api/tasks`
  (`createTask`), `PUT /api/tasks/{id}` (`updateTask`), `POST /api/tasks/{id}/complete|archive|restore`,
  `DELETE /api/tasks/{id}` (`deleteTask`). `getTask` is not used (the edit form is filled from the listed row).
- no other file changes (routes, shell and shared components stay as in phase-02).

## Planned checks
- Implement session (quick feedback only, not verification): `cd frontend && npm run build` succeeds; output in
  `loops/frontend-dev/runs/001-quickflow/phase-03-build.log`.
- No unit tests: the frontend layer has no `coverage` and `test` is empty (FA-1, approved).
- Testing loop (Playwright MCP, headless) can check, on `http://localhost:4200/tasks`:
  - no tasks → `empty-state` shown; `empty-state-action` opens `task-form` (AC-US1-12).
  - `task-add` → fill title/description/priority/due date → `task-save` → row appears with status "Todo", a
    `notice` shows, the form closes (AC-US1-1). `/tasks?add=1` opens `task-form` directly.
  - empty title, or a 201-character title → form stays open, `error-title` shows the server's message; a
    2,001-character description → `error-description` (AC-US1-2, AC-US1-3).
  - `task-status` / `task-priority` offer exactly Todo / In Progress / Done and Low / Medium / High (AC-US1-4, FA-11).
  - `task-edit-<id>` → form prefilled, including `task-status` → change fields → `task-save` → row shows the new
    values (AC-US1-5; the update time is checked through the API).
  - `task-complete-<id>` → `task-row-status` "Done" (AC-US1-6).
  - `task-archive-<id>` → row leaves the default list; `filter-archived` on → row listed with `task-restore-<id>`;
    restore → back in the default list (AC-US1-7).
  - `task-delete-<id>` → row gone from the list, search and archived view (AC-US1-8, FA-7: no confirm dialog).
  - `task-search` "abc" → only titles containing "abc" in any case (AC-US1-9).
  - `filter-status`, `filter-priority`, `filter-due-from`, `filter-due-to`, `sort-field`, `sort-direction` → only
    matching rows, in the server's order (AC-US1-10).
  - `task-view-overdue` → only overdue tasks, each with `task-row-overdue`; a task due today, a Done task and an
    archived one are absent (AC-US1-11). In the default list, the badge shows only where `overdue` is true.

## Risks
- **Client not regenerated yet**: `frontend/src/app/api` still has only `InfoService`. The swagger now has all nine
  task operations (checked now), so the implement session runs `generate_client` first; T015 then checks the
  generated method signatures (param object vs positional arguments, enum model shape) before code uses them. If
  the generated code does not compile under Angular 22.2.0, that becomes a question (generated files are never
  patched).
- **Error responses are not declared in the swagger** (see Contract differences), so no `Problem` model is
  generated. `core/problem.ts` (phase-02) already defines it locally. Checked now in
  `backend/src/main/java/com/quickflow/web/ApiExceptionHandler.java`: 400s carry `errors: [{field, message}]` as a
  top-level property (bean validation uses the Java field name, e.g. `title`, `description`), which is what
  `readProblem` reads, so `error-title` / `error-description` work.
- **`archived=true` lists only archived tasks** (checked now: `TaskSpecifications` filters `archived = <value>`), so
  `filter-archived` switches the list to the archived ones; restore buttons appear there.
- **Out-of-order responses** while typing in `task-search`: each change re-reads the list; the page cancels a
  pending request when a newer one starts (FA-14) so a slow old response cannot overwrite a newer one.
- `?add=1` must open the form also when the page is already open (Dashboard quick-add arrives later in US5).

## Assumptions
- **FA-13 (new) Empty optional fields**: an empty description or due date is sent as `null` (both are
  `[string, null]` in the contract). On edit, clearing them clears the stored value (PUT replaces the editable
  fields). The create form has no status field, so the server's default Todo applies (AC-US1-1); priority defaults
  to Medium in the form (the contract's default).
- **FA-14 (new) Filters and search**: `filter-status` / `filter-priority` have an "Any" first option (omits the
  parameter); `filter-due-from` / `filter-due-to` are `<input type="date">` (empty = omitted); `filter-archived` is a
  checkbox (checked = `archived=true`); `sort-field` / `sort-direction` start at `createdAt` / `desc`. Every change,
  including each keystroke in `task-search`, re-reads the list at once (no debounce), cancelling the previous request.
- **FA-15 (new) Overdue view**: `task-view-overdue` is a toggle button (`aria-pressed`); while on, the list shows
  `listOverdueTasks()` and the search/filter/sort controls are disabled (that endpoint takes no parameters);
  turning it off returns to the filtered list. Row actions work the same in both views, each re-reading the view
  that is shown.
- **FA-16 (new) Row actions shown**: `task-complete-<id>` only on rows not Done and not archived; `task-archive-<id>`
  only on non-archived rows; `task-restore-<id>` only on archived rows; `task-edit-<id>` and `task-delete-<id>` on
  every row.
- **FA-17 (new) Notices and form**: success notices "Task created", "Task updated", "Task completed", "Task
  archived", "Task restored", "Task deleted"; any other error (e.g. 404, network) goes to `notice-error` with the
  server's `detail`. A 400 on save keeps the form open with the field messages; a 400 whose field has no input on
  the form is shown as `notice-error`. Opening `?add=1` removes the parameter from the URL when the form closes
  (`replaceUrl`), so a reload does not reopen it. Only one form is open at a time, shown above the list.
- **FA-18 (new) Empty state text**: "No tasks yet" when the default list has no filter set, "No tasks match" when a
  search/filter/overdue view is active; the action is "Add Task" in both cases and opens the create form.
- **FA-19 (new) Row text**: `task-row-status` / `task-row-priority` use the labels in `data-model.md` (Todo /
  In Progress / Done, Low / Medium / High); `task-row-due` shows `formatDate(dueDate)` or "No due date"; the overdue
  badge text is "Overdue".
- FA-1, FA-3 (server-side validation messages only), FA-7 (deletes without confirmation), FA-10 (no extra
  dependencies, plain CSS) and FA-11 apply as approved at phase-00.

## Open questions
None.

## Swagger input
`loops/backend-dev/outputs/openapi.json` (from task.md).

## Contract differences
Compared with `specs/001-quickflow-backend/contracts/openapi.yaml` for the US1 endpoints (checked now):
- Paths, methods, operationIds (`listTasks`, `createTask`, `listOverdueTasks`, `getTask`, `updateTask`, `deleteTask`,
  `completeTask`, `archiveTask`, `restoreTask`), the 8 `listTasks` query parameters (names, types, enums, defaults
  `archived=false`, `sort=createdAt`, `direction=desc`), request bodies and success responses (200 / 201 / 204):
  match.
- Schemas `Task`, `TaskCreate`, `TaskUpdate`, `TaskStatus`, `TaskPriority`: match (same required lists and types).
  Only extra in the generated swagger: `description` has `minLength: 0` in `TaskCreate` / `TaskUpdate`. No effect.
- Error responses: the contract declares `400 BadRequest` (listTasks, createTask, updateTask) and `404 NotFound`
  (getTask, updateTask, deleteTask, complete/archive/restore) as `application/problem+json` `Problem`; the generated
  swagger declares none, and has no `Problem` / `FieldError` schema. The backend does return them (see Risks); the
  frontend uses its local `Problem` type.
- Response descriptions differ in wording only (e.g. "Matching tasks, in the requested order (null due dates last)"
  vs "OK"). No effect on the client.
- `servers`: generated `http://localhost:18080` vs contract `http://localhost:8080`. No effect: base path is `''`.
