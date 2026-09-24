# phase-03: US1 Manage tasks
layer: frontend
story_id: US1
spec_phase: 3
depends_on: [phase-02, backend-dev:US1]
## Goal
The Tasks page: add, edit, complete, archive, restore, delete, search, filter, sort and the overdue view.
## Acceptance criteria
- AC-US1-1: Given no tasks, When the user creates a task with a title, optional description, priority and due date, Then the task is stored with status Todo, not archived, and with its creation and update times set.
- AC-US1-2: Given the task form, When the user submits an empty title or a title longer than 200 characters, Then the task is rejected with a message naming the title field (BR-1).
- AC-US1-3: Given the task form, When the user submits a description longer than 2,000 characters, Then the task is rejected with a message naming the description field (BR-2).
- AC-US1-4: Given a task, When a status other than Todo / In Progress / Done or a priority other than Low / Medium / High is submitted, Then the change is rejected (BR-3).
- AC-US1-5: Given an existing task, When the user edits its title, description, status, priority or due date, Then the changes are saved and its update time changes.
- AC-US1-6: Given a task, When the user marks it completed, Then its status is Done (BR-5).
- AC-US1-7: Given a task, When the user archives it, Then it no longer appears in the default task list but can still be listed by asking for archived tasks, and restoring it brings it back to the default list (BR-4).
- AC-US1-8: Given a task, When the user deletes it, Then it is never returned by any normal list, search, lookup or summary again (BR-14).
- AC-US1-9: Given several tasks, When the user searches by text, Then only tasks whose title contains that text (ignoring case) are listed.
- AC-US1-10: Given several tasks, When the user filters by status, priority and/or due date and chooses a sort (due date or creation date), Then only matching tasks are listed, in that order.
- AC-US1-11: Given a task with a due date before today that is not Done and not archived, When the user asks for overdue tasks, Then that task is listed as overdue; a task due today, a Done task or an archived task is not.
- AC-US1-12: Given no tasks match (or none exist), When the Tasks page shows the list, Then an empty state guides the user to the Add Task action.
## Tasks
- [x] T015 [US1] Check that the generated `TasksService` has `listTasks` (all 8 query parameters), `listOverdueTasks`, `createTask`, `updateTask`, `completeTask`, `archiveTask`, `restoreTask`, `deleteTask` and that the Tasks section of specs/001-quickflow-frontend/contracts/ui-contract.md covers AC-US1-1..12; raise a question for anything missing
- [x] T016 [P] [US1] Create the task form component (create + edit, fields title/description/priority/due date, status when editing, no client-side length checks, shows `fieldErrors` via `field-error`) in frontend/src/app/pages/tasks/task-form.ts (AC-US1-1..5)
- [x] T017 [US1] Implement the Tasks page list with search box, status/priority/due-from/due-to/archived filters and sort field/direction bound to `listTasks` parameters (defaults as in the contract), rows with overdue badge, in frontend/src/app/pages/tasks/tasks.page.ts (FR-02, AC-US1-9, AC-US1-10)
- [x] T018 [US1] Add create/edit flow (open `task-form` from `task-add`, from `task-edit-<id>` and from `?add=1`; on 400 keep the form open with field errors; on success re-read the list and show a notice) in frontend/src/app/pages/tasks/tasks.page.ts (AC-US1-1..5)
- [x] T019 [US1] Add row actions complete, archive, restore (archived rows), delete, each re-reading the list, in frontend/src/app/pages/tasks/tasks.page.ts (AC-US1-6, AC-US1-7, AC-US1-8)
- [x] T020 [US1] Add the overdue view toggle (`task-view-overdue` → `listOverdueTasks`) and the empty state pointing to Add Task, in frontend/src/app/pages/tasks/tasks.page.ts (AC-US1-11, AC-US1-12)
