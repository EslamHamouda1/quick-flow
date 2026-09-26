# phase-09-test: Polish (frontend-dev)

Source of the checks: `loops/frontend-dev/outputs/001-quickflow/phase-09.md` has an empty
`## Acceptance criteria` section (Polish, `story_id: none`). The approved review `phase-09-review.md` says:
"None of its own (Polish). The story ACs of phases 03–08 must still hold after any change." and lists what the
testing loop can check (`## Planned checks`). Each listed item is one criterion (compare on later attempts):

- R1: "every selector of `ui-contract.md` is present on its route once there is data (a task, a habit, a card with a
  milestone and a note, a plan with items, settings), including `empty-state` / `empty-state-action` on an empty
  list, `notice` / `notice-error`, and `error-title` after an invalid save."
- R2: "a smoke run of each page (`/dashboard`, `/tasks`, `/habits`, `/learning`, `/plans`, `/settings`) via `nav-*`,
  `page-title` text equal to the nav label, no console errors."
- R3: "quickstart steps 1–3 work as written."

Selectors: `specs/001-quickflow-frontend/contracts/ui-contract.md` (sections Shell, Tasks, Habits, Learning,
Plans, Dashboard, Settings). Data is set up through the API (`/api/*` via the page's origin) and each selector is
looked up in the DOM with `browser_evaluate` (`document.querySelector('[data-testid="…"]')`), results saved as
JSON in `run_dir`.

| check | criterion | Playwright MCP steps | expected |
|---|---|---|---|
| C1 | R1 (Shell, empty) | fresh DB; open `/tasks`, `/habits`, `/learning`, `/plans` | `nav-main`, the six `nav-*`, `page-title` present; `empty-state` + `empty-state-action` on each empty list |
| C2 | R1 (Tasks) | API: one task (with due date in the past), one archived task; open `/tasks`; `task-add` → form; save with empty title; check selectors; toggle `filter-archived`; toggle `task-view-overdue` | every Tasks selector present (form fields incl. `task-status` when editing, filters, sort, row fields, `task-row-overdue`, row actions, `task-restore-<id>` on the archived row); `error-title` after the empty save; a `notice` after a successful action |
| C3 | R1 (Habits) | API: an active daily habit, an inactive weekly habit; open `/habits`; `habit-add` → form; toggle today twice / re-complete to trigger an error | every Habits selector present (`habit-activate-<id>` on the inactive card, `habit-deactivate-<id>` on the active one, `habit-inactive` badge); `notice-error` shown for an API error |
| C4 | R1 (Learning) | API: a card with one milestone and one note; open `/learning`; `card-add` → form; `card-expand-<id>` | every Learning selector present, including milestone and note rows and inputs |
| C5 | R1 (Plans) | API: plan In Progress (start past, end ahead) with a task, habit and card item; a completed plan; delete the task source; open `/plans`; `plan-create` → builder, save with nothing ticked; API plan starting ~1 min ahead, wait with page open | every Plans selector present (`plans-active`/`plans-completed`, `plan-<id>` fields, `plan-rest-time` on In Progress only, items, `plan-item-removed-<itemId>`, `plan-source-*`, `error-items` after empty save, `plan-started-<id>` after the start is reached) |
| C6 | R1 (Dashboard) | with the data above plus a task due today and a done task; open `/dashboard` | every Dashboard selector present (`dash-greeting`, four `metric-*`, `dash-task-percent`, three task lists with `dash-task-<id>`, habits with `dash-habit-count`, `dash-plans`/`dash-plan-<id>` with `plan-progress`/`plan-rest-time`, learning snapshot, four `quick-add-*`) |
| C7 | R1 (Settings) | open `/settings` | `settings-form`, `settings-display-name`, `settings-notifications`, `settings-default-page`, `settings-save`, `settings-time-zone` present |
| C8 | R2 | from `/settings` click `nav-dashboard`, `nav-tasks`, `nav-habits`, `nav-learning`, `nav-plans`, `nav-settings` in turn; read URL and `page-title`; `browser_console_messages` level error | each click lands on its route with `page-title` = nav label; no console errors |
| C9 | R3 | step 3: app at `http://localhost:4200` answers; `GET /api/app-info` through the dev-server proxy → 200. Steps 1–2 (generate client, `npm run build`) write under `frontend/`, which the testing loop may not do | step 3 as written; steps 1–2 not run here (the runner started the app with `npm start`, which compiles the same sources) |

Screenshots: `<run_dir>/shot-NN-<check>.png` for every check.
Unit tests: the frontend layer has no `test` command and no `coverage` (`unit-result.json` outcome `none`).
