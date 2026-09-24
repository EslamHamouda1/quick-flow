# phase-03-test: US1 Manage tasks (frontend-dev)

Criteria listed in `loops/frontend-dev/outputs/001-quickflow/phase-03.md` (compare on later attempts):
AC-US1-1 … AC-US1-12, text as in `specs/001-quickflow-frontend/spec.md` lines 40–51 (AC-US1-4 UI part per the
approved review: FA-11, only valid values offered).

Selectors from `specs/001-quickflow-frontend/contracts/ui-contract.md` (Shell + Tasks). Visible texts only where the
approved review states them (FA-17 notices, FA-18 empty state, FA-19 row labels). Server-side values (status,
archived, createdAt/updatedAt) are read back through `GET /api/tasks` on `http://localhost:8080` as supporting
evidence. "Today" = the app time zone (`Africa/Cairo`) date at run time.

All steps use Playwright MCP (headless) on `http://localhost:4200`. Screenshots go to
`<run_dir>/shot-<NN>-<check id>.png`.

| check | criterion | Playwright MCP steps | expected |
|---|---|---|---|
| C1 | AC-US1-12 | fresh DB; navigate `/tasks`; read `empty-state`; click `empty-state-action` | `empty-state` shown with an Add Task action (FA-18 "No tasks yet" / "Add Task"); the action opens `task-form` |
| C2 | AC-US1-1 | `task-add` → fill `task-title`, `task-description`, `task-priority` High, `task-due-date` → `task-save`; read row; API `GET /api/tasks` | row appears with `task-row-status` "Todo"; `notice` shown; form closes; API: `status TODO`, `archived false`, `createdAt`/`updatedAt` set. `/tasks?add=1` opens `task-form` |
| C3 | AC-US1-2 | open form; save with empty title; then with a 201-char title | form stays open; `error-title` shows a message naming the title field; no task created |
| C4 | AC-US1-3 | open form; valid title + 2,001-char description; save | form stays open; `error-description` shows a message naming the description field; no task created |
| C5 | AC-US1-4 (FA-11) | edit form: read options of `task-status` and `task-priority` | exactly Todo / In Progress / Done and Low / Medium / High |
| C6 | AC-US1-5 | `task-edit-<id>` → form prefilled → change title, description, status, priority, due date → `task-save` | row shows new values; API `updatedAt` later than before |
| C7 | AC-US1-6 | `task-complete-<id>` | `task-row-status` "Done" (API `status DONE`) |
| C8 | AC-US1-7 | `task-archive-<id>` → default list; check `filter-archived` → list; `task-restore-<id>` → uncheck | row leaves default list; listed with `task-restore-<id>` when archived asked; back in default list after restore |
| C9 | AC-US1-8 | `task-delete-<id>`; then default list, `task-search` on its title, archived view, overdue view | row gone everywhere |
| C10 | AC-US1-9 | tasks "Alpha abc", "xABCy", "Other"; `task-search` "abc" | only the two titles containing "abc" in any case |
| C11 | AC-US1-10 | set `filter-status`, `filter-priority`, `filter-due-from`/`filter-due-to`, `sort-field` dueDate/createdAt, `sort-direction` asc/desc | only matching rows, in the requested order |
| C12 | AC-US1-11 | tasks: due yesterday (Todo), due today, due yesterday Done, due yesterday archived; `task-view-overdue` | only the first listed, with `task-row-overdue`; the others absent; in default list badge only on overdue row |
| C13 | AC-US1-12 | search text matching nothing | `empty-state` ("No tasks match") with `empty-state-action` opening `task-form` |

Unit tests: the frontend layer has no `test` command and no `coverage` (`unit-result.json` outcome `none`).
