# phase-07-test: US5 See everything on the Dashboard (frontend-dev)

Criteria listed in `loops/frontend-dev/outputs/001-quickflow/phase-07.md` (compare on later attempts):
AC-US5-1 … AC-US5-7, text as in `specs/001-quickflow-frontend/spec.md` lines 149–155. Also covered: FR-09.1,
FR-09.2, FR-08.5 (the Dashboard half of AC-US4-4, left over from phase-06).

Selectors come from `specs/001-quickflow-frontend/contracts/ui-contract.md` (Shell, Dashboard `/dashboard`, and the
add forms of Tasks/Habits/Learning/Plans). Visible texts are used only where the criteria, the spec or the contract
state them. Each figure is compared with `GET /api/dashboard` (FR-09.2) and with a count taken from the list
endpoints. The data is set up through the API on `http://localhost:8080`, called from the page through the
dev-server proxy (`/api/...`), and the read-backs are saved as JSON in the attempt folder.

The greeting's "Hello, <displayName>" form can't be checked before US6 (no `/api/settings`; FA-34, approved).
C9 checks only the "Hello" form.

All steps use Playwright MCP (headless) on `http://localhost:4200`. Screenshots go to
`<run_dir>/shot-<NN>-<check id>.png`.

| check | criterion | Playwright MCP steps | expected |
|---|---|---|---|
| C1 | AC-US5-3 (empty), FR-09.1 | fresh DB; navigate `/dashboard`; read every `dash-*` / `metric-*` selector | `dash-task-percent` "0%" ("0% when there are none"); the lists are empty; the counts are 0, matching `GET /api/dashboard` |
| C2 | AC-US5-1 | API: A due today (TODO), E due today then DONE, B due yesterday (TODO), C due yesterday DONE, D no due date completed, F due today archived, G due yesterday archived, H completed + archived; reload `/dashboard` | `dash-due-today` lists exactly `dashboard.dueToday` (A, E); `dash-overdue` exactly `overdue` (B); `dash-completed-today` exactly `completedToday` (tasks completed today, non-archived); F, G, H appear in no list ("non-archived only") |
| C3 | AC-US5-3 | same data; compare `dash-task-percent` and `metric-tasks` with `GET /api/tasks` (non-archived total / DONE) | percent = (non-archived DONE)/(all non-archived) as the API returns it (floor); `metric-tasks` shows done and total matching the list |
| C4 | AC-US5-2 | API: H1 active, completed today; H2 active; H3 inactive; reload | `dash-habits` lists H1, H2 only; `dash-habit-done-<H1>` present, none for H2; `dash-habit-count` "1/2"; `metric-habits` agrees with `habitCounts` |
| C5 | AC-US5-5 | API: 4 cards (NOT_STARTED, IN_PROGRESS ×2, COMPLETED), 5 milestones with 3 done; reload | `dash-learning-not-started` 1, `-in-progress` 2, `-completed` 1; `dash-milestones` "3/5"; the same as `learning` and the list endpoint |
| C6 | AC-US5-4, FR-08.5 | API: P1 in progress (TASK item + HABIT item), P2 in progress, P3 not started, P4 past; reload; read `plan-rest-time` twice a few seconds apart; tick P1's TASK item on the Dashboard; then tick an item of P2 on `/plans` and go back to `/dashboard` | `dash-plans` has `dash-plan-P1`/`P2` only, each with `plan-progress` and `plan-rest-time` `H:MM:SS` ≈ end − now and going down; `metric-plans` = `planCounts`. After the tick, `plan-progress` and the task figures (`dash-task-percent`, `metric-tasks`, `dash-completed-today`) change with no reload. The tick made on `/plans` shows on the next view. Ticking every item of a plan removes it from `dash-plans` |
| C7 | AC-US5-6 | click each `quick-add-task`, `-habit`, `-learning`, `-plan` from `/dashboard` | the matching page opens with its add form: `task-form`, `habit-form`, `card-form`, `plan-builder` |
| C8 | AC-US5-7, FR-10.1 | from `/dashboard` and from another page, click each `nav-*` | `nav-main` is present on every page; each link reaches its route with `page-title` "Dashboard", "Tasks", "Habits", "Learning Resources", "Todo Plans", "Settings" |
| C9 | FR-09.1 (greeting) | read `dash-greeting` | "Hello" (no name before US6, FA-34) |

Unit tests: the frontend layer has no `test` command and no `coverage` (`unit-result.json` outcome `none`).
