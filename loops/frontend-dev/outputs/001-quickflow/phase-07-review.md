# phase-07 review: US5 See everything on the Dashboard
layer: frontend
story_id: US5
spec_phase: 7
depends_on: [phase-02, phase-03, phase-04, phase-05, phase-06, backend-dev:US5]

## Goal
The Dashboard page with every figure from `getDashboard`, live in-progress plans and quick-add actions.

## Tasks
- [ ] T036 [US5] Check that the generated `DashboardService.getDashboard` returns every `Dashboard` field of the contract (dueToday, overdue, completedToday, taskCompletionPercent, taskCounts, habits, habitCounts, activePlans, planCounts, learning) and that the Dashboard section of contracts/ui-contract.md covers AC-US5-1..7; raise a question for anything missing
- [ ] T037 [US5] Implement the Dashboard page: greeting (`Hello, <displayName>` / `Hello`, from `getSettings()` on every load), metric cards, task completion percent, due today / overdue / completed today lists, today's habits with done marks and count, learning snapshot, in frontend/src/app/pages/dashboard/dashboard.page.ts (FR-09.1, FR-09.2, AC-US5-1..3, AC-US5-5)
- [ ] T038 [US5] Add the in-progress plans section (reuse `plan-card` from US4: progress, live rest time, item toggles; re-read the dashboard after a toggle and when a shown plan's end is reached) in frontend/src/app/pages/dashboard/dashboard.page.ts (AC-US5-4, FR-08.5)
- [ ] T039 [US5] Add quick-add actions navigating to `/tasks?add=1`, `/habits?add=1`, `/learning?add=1`, `/plans?add=1` in frontend/src/app/pages/dashboard/dashboard.page.ts (AC-US5-6, research R-7)

Order: first `generate_client` (the loop's app-wide step, run at the start of implement so `DashboardService` and
the `Dashboard`, `TaskCounts`, `HabitCounts`, `PlanCounts`, `LearningSnapshot` models exist; checked now:
`frontend/src/app/api/api` has only `InfoService`, `TasksService`, `HabitsService`, `LearningService` and
`PlansService`), then T036, then T037, T038, T039 one after another (same file, no `[P]`).

## Acceptance criteria
- AC-US5-1: Given tasks due today, overdue tasks and tasks completed today, When the user opens the Dashboard, Then each group lists exactly those tasks (non-archived only).
- AC-US5-2: Given active and inactive habits, When the user opens the Dashboard, Then today's habits list the active habits and marks those completed today, with counts matching the data.
- AC-US5-3: Given non-archived tasks, When the Dashboard shows the task completion percentage, Then it equals (non-archived tasks with status Done) / (all non-archived tasks), and 0% when there are none.
- AC-US5-4: Given plans that are In Progress, When the user opens the Dashboard, Then each is shown with its progress percentage and live rest time; ticking an item in a plan changes the Dashboard figures on the next view without other action.
- AC-US5-5: Given learning cards and milestones, When the user opens the Dashboard, Then the learning snapshot shows the number of cards per status and milestones done out of total, matching the data.
- AC-US5-6: Given the Dashboard, When the user uses a quick-add action (task, habit, learning card, plan), Then the matching add form opens.
- AC-US5-7: Given any page, When the user uses the persistent navigation, Then Dashboard, Tasks, Habits, Learning Resources, Todo Plans and Settings are each reachable.

## Files
- app-wide: re-run `generate_client` → `frontend/src/app/api/**` (adds `DashboardService` and the dashboard models;
  the other files are regenerated unchanged); `loops/frontend-dev/outputs/ui-url.md` rewritten unchanged.
- edit `specs/001-quickflow-frontend/research.md` R-2: a "Checked at implement (T036)" entry with the generated
  `DashboardService` name, `getDashboard()`'s return type and the dashboard model field names/types found.
- rewrite `frontend/src/app/pages/dashboard/dashboard.page.ts` (T037–T039), today a placeholder with only
  `page-title` "Dashboard". Keeps `page-title`. Adds, all fed by one `getDashboard()` read (FR-09.2; the page never
  computes a figure itself):
  - `dash-greeting` (FA-34).
  - `metric-tasks` ("<done>/<total> tasks done", `taskCounts`), `metric-habits` ("<completedToday>/<active> habits
    today", `habitCounts`), `metric-plans` ("<inProgress> in progress · <notStarted> upcoming · <completed>
    completed", `planCounts`), `metric-learning` ("<inProgress> in progress · <completed> completed", `learning`).
  - `dash-task-percent` "<taskCompletionPercent>%".
  - `dash-due-today`, `dash-overdue`, `dash-completed-today` lists in the server's order; rows `dash-task-<id>` with
    the title, priority label and due date (`formatDate`, existing `core/date-format.ts`).
  - `dash-habits` with `dash-habit-count` "<completedToday>/<active>" and rows `dash-habit-<id>` (name, frequency
    label) plus a `dash-habit-done-<id>` badge "Done today" when `completedToday`.
  - `dash-plans` with one `dash-plan-<id>` wrapper per `activePlans` entry (server order) around `app-plan-card`, which
    brings `plan-progress`, `plan-rest-time` and the `plan-item-done-<itemId>` toggles (FA-35).
  - `dash-learning` with `dash-learning-not-started`, `dash-learning-in-progress`, `dash-learning-completed` (counts)
    and `dash-milestones` "<milestonesDone>/<milestonesTotal>".
  - `quick-add-task` "Add Task", `quick-add-habit` "Add Habit", `quick-add-learning` "Add Learning Card",
    `quick-add-plan` "Create Plan": `routerLink` with `queryParams: {add: 1}` to `/tasks`, `/habits`, `/learning`,
    `/plans` (R-7; all four pages already open their form on `add=1`, checked now in their `queryParamMap`
    subscriptions).
- edit `frontend/src/app/pages/plans/plan-card.ts` (T038): one new input `actions` (boolean, default `true`); when
  false the Edit/Delete buttons are not rendered (FA-35). The Todo Plans page doesn't pass it, so it is unchanged.
- endpoints used: `GET /api/dashboard` (`getDashboard`), `PUT /api/plans/{id}/items/{itemId}` (`setPlanItemDone`).
  `getSettings` is not called (FA-34).
- no other file changes (routes, shell, nav and the other pages stay as they are; AC-US5-7's nav exists since
  phase-02).

## Planned checks
- Implement session (quick feedback only, not verification): `cd frontend && npm run build` succeeds; output in
  `loops/frontend-dev/runs/001-quickflow/phase-07-build.log`.
- No unit tests: the frontend layer has no `coverage` and `test` is empty (FA-1, approved).
- Testing loop (Playwright MCP, headless) can check, on `http://localhost:4200/dashboard` (data created through the
  API first; every figure compared with `GET /api/dashboard` and with a count from the list endpoints, SC-003):
  - tasks due today (one of them DONE), due yesterday (TODO), due yesterday but archived, completed today with no due
    date → `dash-due-today` lists the two due today, `dash-overdue` only the non-archived one, `dash-completed-today`
    the DONE ones; the archived task appears nowhere (AC-US5-1).
  - an active habit completed today, an active habit not completed, an inactive habit → `dash-habits` lists the two
    active ones, `dash-habit-done-<id>` only on the completed one, `dash-habit-count` "1/2", `metric-habits` agrees
    (AC-US5-2).
  - `dash-task-percent` = floor(100 × done / total) over non-archived tasks, as the API returns it; with no tasks
    "0%" (AC-US5-3).
  - a plan with start in the past and end in the future → `dash-plan-<id>` with `plan-progress` and `plan-rest-time`
    `H:MM:SS` decreasing between two reads a few seconds apart; a NOT_STARTED and a COMPLETED plan are not in
    `dash-plans` but are counted in `metric-plans`. Tick `plan-item-done-<itemId>` on the Dashboard →
    `plan-progress` and (for a TASK item) `dash-task-percent` / `metric-tasks` change without a reload; ticking an
    item on `/plans` and then opening the Dashboard shows the new figures (AC-US5-4, FR-08.5). Ticking the last
    item → the plan leaves `dash-plans`. No `plan-edit-<id>` / `plan-delete-<id>` on the Dashboard (FA-35).
  - a plan whose end is ~1 minute ahead, page open → when the end is reached it leaves `dash-plans` without a manual
    reload (R-4).
  - cards NOT_STARTED / IN_PROGRESS / COMPLETED with milestones → `dash-learning-*` counts and `dash-milestones`
    "<done>/<total>" match (AC-US5-5).
  - each `quick-add-*` → the matching page with its form open (`task-form`, `habit-form`, `card-form`,
    `plan-builder`) (AC-US5-6).
  - `nav-*` from the Dashboard (and from any page) reaches all six pages (AC-US5-7).
  - `dash-greeting` reads "Hello" (FA-34, no Settings API yet).

## Risks
- **Client not regenerated yet**: the swagger has `/api/dashboard` with `operationId: getDashboard` (checked now), so
  the implement session runs `generate_client` first; T036 then checks the generated names before code uses them. A
  generated file that doesn't compile under Angular 22.2.0 becomes a question (generated files are never patched).
- **`/api/settings` does not exist yet** (checked now: no `settings` path in `loops/backend-dev/outputs/openapi.json`;
  backend US6 is a later story), so T037's "from `getSettings()` on every load" can't be done in this phase. See FA-34.
- **Backend semantics (checked now in `backend/.../domain/dashboard/DashboardService.java`)**: `dueToday` is every
  non-archived task due today, DONE ones included, so a task can be in both `dueToday` and `completedToday` (its
  `dash-task-<id>` then appears in both lists, each scoped by its list selector); `overdue` = `findOverdue(today)`
  (not DONE, not archived); `taskCompletionPercent` = `100 * done / total` in integer arithmetic (floor), 0 with no
  tasks; `activePlans` = IN_PROGRESS plans by priority order, then start, then id. The page shows what it gets.
- **Plans starting while the Dashboard is open**: a NOT_STARTED plan isn't in `activePlans`, so the page has no start
  time to watch. `PlanStartWatcher` (phase-06) does watch it and re-reads at the start; FA-36 uses that.
- **Boundary vs. clock skew**: same approach as the Todo Plans page (a re-read at the first tick where `now ≥ end +
  1 s`, next boundary chosen among the times still ahead of the last read), so a stale answer never loops.
- **Toggle while a re-read runs**: a newer `getDashboard` cancels the pending one; a card's controls are disabled
  while its item call runs; after an error the dashboard is re-read (the checkbox snaps back, as on `/plans`).

## Assumptions
- **FA-34 (new) Greeting before Settings exist**: until US6 the greeting is "Hello" (the "no name" form the contract
  and T037 already define). The name sits behind one private method (`displayName(): Promise<string | null>`,
  returning `null` now), called on every Dashboard load; T037 is recorded in tasks.md as done "with no name until
  US6", as FA-30 did for T035. Phase-08 (US6) replaces it with `getSettings().displayName` (a blank or missing name
  → "Hello"); phase-08's review adds that as a task on `pages/dashboard/dashboard.page.ts`. The testing loop can't
  check "Hello, <name>" before US6.
- **FA-35 (new) Plans on the Dashboard**: each in-progress plan is the US4 `app-plan-card` (status, progress, rest
  time, window, priority, items with done toggles, removed badges, the `plan-started-<id>` highlight from
  `PlanStartWatcher`), inside a `dash-plan-<id>` wrapper, with a new `actions` input set to `false` so Edit/Delete are
  not shown there (editing and removing stay on the Todo Plans page; FR-09.1 lists only progress and rest time). A
  toggle calls `setPlanItemDone`, shows the same notices as `/plans` ("Item marked done" / "Item marked not done"),
  then re-reads the whole dashboard, so every figure (plan progress, task lists, percent, counts) comes from the
  server (FR-08.5, FR-09.2).
- **FA-36 (new) Re-reads while the page is open**: the dashboard is read on every visit (FR-09.2), and re-read (a)
  after an item toggle, (b) when `now` passes a shown plan's `endDateTime` (+1 s), (c) when `PlanStartWatcher`'s
  `highlighted` set gains a plan (a plan just started, so it now belongs in `dash-plans`), and (d) when
  `clock.today()` changes (app-zone midnight: due today / overdue / completed today / habits done today change). No
  polling otherwise. (c) only fires when a plan-start notice is shown (notifications on, FR-10.2); with them off a
  started plan appears on the next visit or re-read.
- **FA-37 (new) Page texts and layout**: headings "Due today", "Overdue", "Completed today", "Today's habits",
  "Plans in progress", "Learning", "Task completion"; empty lists show "Nothing due today", "No overdue tasks",
  "Nothing completed today", "No active habits", "No plans in progress" (text only, no `empty-state` component: the
  Dashboard always has content). Metric cards use the existing `card` styles in a grid; the quick-add buttons sit in
  a row under the greeting. A failed `getDashboard` shows `notice-error` with the server's message and keeps the last
  figures (none on first load).
- FA-1, FA-2, FA-7, FA-10 (no extra dependencies, plain CSS), FA-11 and FA-30..FA-33 apply as approved.

## Open questions
None.

## Swagger input
`loops/backend-dev/outputs/openapi.json` (from task.md).

## Contract differences
Compared with `specs/001-quickflow-backend/contracts/openapi.yaml` for the US5 endpoint (checked now):
- `GET /api/dashboard`, tag `dashboard`, `operationId: getDashboard`, no parameters, 200 with `Dashboard`: match.
- `Dashboard`: same 11 required fields (`now`, `dueToday`, `overdue`, `completedToday`, `taskCompletionPercent`,
  `taskCounts`, `habits`, `habitCounts`, `activePlans`, `planCounts`, `learning`), same item types (`Task`, `Habit`,
  `Plan`), `taskCompletionPercent` 0–100, same descriptions ("Active habits", "IN_PROGRESS plans by priority").
- `TaskCounts {total, done}`, `HabitCounts {active, completedToday}`, `PlanCounts {notStarted, inProgress,
  completed}`, `LearningSnapshot {notStarted, inProgress, completed, milestonesDone, milestonesTotal}`: same fields and
  required lists. Extras in the generated swagger only: `format: int32` on the integers. No effect on the client
  (`number`).
- Wording only: the contract's 200 description "Summary computed from current data" is the generated `summary`; the
  generated response says "OK". No effect.
- `/api/settings` (`getSettings`, used by T037's greeting) is in the contract but not yet in the generated swagger:
  it belongs to backend US6 (FA-34).
