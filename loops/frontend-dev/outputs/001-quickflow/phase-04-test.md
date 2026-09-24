# phase-04-test: US2 Track recurring habits (frontend-dev)

Criteria listed in `loops/frontend-dev/outputs/001-quickflow/phase-04.md` (compare on later attempts):
AC-US2-1 … AC-US2-9, text as in `specs/001-quickflow-frontend/spec.md` lines 67–75.

Selectors from the approved review (`phase-04-review.md`, Files + Planned checks) and
`specs/001-quickflow-frontend/contracts/ui-contract.md` (Habits). Visible texts only where the approved review states
them (FA-21 card texts, FA-23 notices, FA-24 empty state). Server-side values (active, createdAt, completions) are read
back through the API on `http://localhost:8080` as supporting evidence. "Today" = the app time zone (`Africa/Cairo`)
date at run time.

All steps use Playwright MCP (headless) on `http://localhost:4200`. Screenshots go to
`<run_dir>/shot-<NN>-<check id>.png`.

| check | criterion | Playwright MCP steps | expected |
|---|---|---|---|
| C1 | AC-US2-9 | fresh DB; navigate `/habits`; read `empty-state`; click `empty-state-action`; also `/habits?add=1` | `empty-state` "No habits yet" with action "Add Habit" (FA-24) that opens `habit-form`; `?add=1` opens `habit-form` |
| C2 | AC-US2-1 | `habit-add` → fill `habit-name`, `habit-description`, `habit-frequency` Weekly → `habit-save`; read card; API `GET /api/habits` | `habit-card-<id>` with `habit-frequency-label` "Weekly", `habit-period-done` "Not done", `habit-streak` "0", no `habit-inactive`; `notice` "Habit created"; form closes; API `active: true`, `createdAt` set |
| C3 | AC-US2-2 | open form; save with empty name; then with a 151-char name | form stays open; `error-name` shows a message naming the name field; no habit added |
| C4 | AC-US2-3, AC-US2-5 | create a Daily habit; click `habit-toggle-<id>`; API `GET /api/habits/{id}/completions`; toggle the Weekly habit | toggle checked, `habit-period-done` "Done today", `habit-streak` "1"; exactly one completion with today's date; Weekly card → "Done this week" |
| C5 | AC-US2-4 | on an unchecked Daily habit, complete it for today via the API while the page is open; click the still-unchecked toggle | `notice-error` tells it is already complete for that date; card re-reads to checked; API still one record for that date |
| C6 | FR-04.3 (undo; review planned checks) | uncheck `habit-toggle-<id>` | unchecked, "Not done", streak back to 0; API has no record for today |
| C7 | AC-US2-6 | `habit-edit-<id>` → prefilled → change name, description, frequency → `habit-save` | card shows new name and frequency label; `notice` "Habit updated"; API has the new description |
| C8 | AC-US2-7 | complete a habit; `habit-deactivate-<id>`; API `listHabits?active=true` and completions; then `habit-activate-<id>` | `habit-inactive` badge, toggle disabled, `habit-activate-<id>` shown; API active list omits it; completions still listed; after activate badge gone and toggle enabled ("today's habits" on the Dashboard is US5) |
| C9 | AC-US2-8 | `habit-delete-<id>` | card gone (no confirm, FA-7); `notice` "Habit deleted"; API `GET /api/habits/{id}` → 404, not in `GET /api/habits` |

Unit tests: the frontend layer has no `test` command and no `coverage` (`unit-result.json` outcome `none`).
