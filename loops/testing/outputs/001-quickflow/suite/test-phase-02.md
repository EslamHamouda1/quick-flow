# test-phase-02: US1 Manage tasks

## Goal
US1's acceptance criteria (AC-US1-1..12) and rules BR-1..BR-5, BR-14 hold through the API, then the same
behaviour holds on the Tasks page (`/tasks`) through Playwright MCP, headless, using the selectors in
`specs/001-quickflow-frontend/contracts/ui-contract.md`.

Depends on: test-phase-01. Layers: backend (API checks) → frontend (UI checks).

## API checks (curl, `http://localhost:8080`)
- [ ] A1 AC-US1-1 / FR-01.3: create with title, description, priority, due date → 201, status `TODO`, not archived, createdAt and updatedAt set; without priority → `MEDIUM`.
- [ ] A2 AC-US1-2 / BR-1: empty title and 201-char title → 400 naming field `title`; 200 chars → 201.
- [ ] A3 AC-US1-3 / BR-2: 2001-char description → 400 naming field `description`; 2000 → 201.
- [ ] A4 AC-US1-4 / BR-3: invalid status / invalid priority → 400.
- [ ] A5 AC-US1-5: edit title, description, status, priority, due date → saved, updatedAt changes.
- [ ] A6 AC-US1-6 / BR-5 / FR-01.4: complete → status `DONE` with completedAt; set back to `TODO` → completedAt cleared.
- [ ] A7 AC-US1-7 / BR-4: archive → gone from default list, present with `archived=true`; restore → back in default list.
- [ ] A8 AC-US1-8 / BR-14: delete → 204; 404 on get; absent from list, search, archived list, overdue and dashboard.
- [ ] A9 AC-US1-9 / FR-02.1: search text matches title substrings ignoring case, and only those.
- [ ] A10 AC-US1-10 / FR-02.2 / FR-02.3: filters status, priority, due from/to (inclusive, either end), combined; sort dueDate / createdAt asc / desc; default createdAt newest first.
- [ ] A11 AC-US1-11 / FR-01.7: overdue lists a task due yesterday, not Done, not archived; not one due today, a Done one or an archived one.

## UI checks (Playwright MCP, `http://localhost:4200/tasks`)
- [ ] U1 AC-US1-12 / NFR-5: with no tasks, `empty-state` shown with `empty-state-action` opening `task-form`; `/tasks?add=1` opens the form.
- [ ] U2 AC-US1-1: create through `task-form` → row appears with status Todo; API read-back matches.
- [ ] U3 AC-US1-2 / AC-US1-3: empty title / 201-char title → `error-title`; 2001-char description → `error-description`; nothing saved.
- [ ] U4 AC-US1-4: `task-status` and `task-priority` offer only the listed values.
- [ ] U5 AC-US1-5: `task-edit-<id>` prefilled, save → row and API updated.
- [ ] U6 AC-US1-6: `task-complete-<id>` → `task-row-status` Done.
- [ ] U7 AC-US1-7: `task-archive-<id>` → row leaves default list, shown with `filter-archived`; `task-restore-<id>` brings it back.
- [ ] U8 AC-US1-8: `task-delete-<id>` → row gone from list, search, archived and overdue views.
- [ ] U9 AC-US1-9 / AC-US1-10: `task-search`, filters and sort show exactly the API's rows in the API's order.
- [ ] U10 AC-US1-11: `task-view-overdue` lists only the overdue task with `task-row-overdue`.
- [ ] U11 Navigation: `nav-tasks` reaches `/tasks` with `page-title` "Tasks".
