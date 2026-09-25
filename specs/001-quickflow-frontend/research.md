# Research: QuickFlow (frontend layer)

Decisions made while planning. Anything that could not be checked now is marked "check at implement" and
becomes a `## Questions` entry if the check fails.

## R-1 Scaffold and versions
- **Decision**: use the runner's scaffold (`@angular/cli@22.2.0 new frontend --defaults --skip-git --routing
  --style=css --ssr=false --zoneless --ai-config=none`). Setup checks `package.json` pins `@angular/*` at 22.2.0
  and adds nothing but `proxy.conf.json` and the `proxyConfig` serve option.
- **Rationale**: Constitution I; the stack table pins only Angular, Node and the generator CLI.
- **Alternatives**: adding Angular Material / a date library: rejected (no pinned version).
- **Checked at implement (T001, 2026-09-24)**: differences from the above, versions left unchanged:
  - `package.json` lists `@angular/*` (incl. `@angular/cli`, `@angular/build`) as caret ranges `^22.2.0`, not exact
    pins; the installed packages (`node_modules/@angular/core`, `@angular/cli`) are 22.2.0 and `package-lock.json`
    locks them. Scripts match: `start` = `ng serve`, `build` = `ng build`.
  - `app.config.ts` has no zoneless provider call: it provides `provideBrowserGlobalErrorListeners()` and
    `provideRouter(routes)` only. The app is zoneless because Angular 22 needs an explicit
    `provideZoneChangeDetection()` plus `zone.js` for zone-based change detection, and neither is present
    (no `zone.js` package, no `polyfills` in `angular.json`).

## R-2 Generated API client
- **Decision**: `generate_client` (from `project.config.yaml`) writes `frontend/src/app/api` from
  `loops/backend-dev/outputs/openapi.json`. Components inject the generated tag services (expected names
  `TasksService`, `HabitsService`, `LearningService`, `PlansService`, `DashboardService`, `SettingsService`,
  `InfoService`, methods named by `operationId`) and use the generated model types. The base path is `''`
  (same origin, proxied). **Check at implement**: the exact service/model/enum names and the provider API
  (`provideApi(...)` or a `Configuration` provider) in the generated sources before code uses them.
- **Rationale**: one source of truth for the API; the contract's operationIds are stable.
- **Alternatives**: hand-written `HttpClient` calls: rejected (drift from the swagger).
- **Risk**: the generator CLI downloads its jar on first run (network) and needs Java (JDK 25 present). The
  generated code must compile with Angular 22.2.0; a compile error there is reported as a question, not patched in
  the generated files.
- **Checked at implement (T005, 2026-09-24)**, client generated from the current
  `loops/backend-dev/outputs/openapi.json` (only `/api/app-info` so far):
  - services: `InfoService` (`api/info.service.ts`, `providedIn: 'root'`) with `getAppInfo()` →
    `Observable<AppInfo>`. Exported from `frontend/src/app/api` (`index.ts`).
  - models: `AppInfo { timeZone: string; now: string }` (both required). No enum models yet.
  - provider: `provideApi(configOrBasePath: string | ConfigurationParameters)` (`provide-api.ts`); a string sets
    `BASE_PATH`. Without it the base path defaults to the swagger server URL (`http://localhost:18080`), so the app
    calls `provideApi('')`. A `Configuration` class also exists (`configuration.ts`).
  - not generated yet (backend stories pending): `getSettings` / `SettingsService`, the other tag services
    (`TasksService`, `HabitsService`, `LearningService`, `PlansService`, `DashboardService`), the enum models and the
    `Problem` / `FieldError` models. Each story phase re-checks its names after `generate_client`.
- **Checked at implement (T015, 2026-09-24)**, client regenerated after backend US1:
  - `TasksService` (`api/tasks.service.ts`, `providedIn: 'root'`), positional arguments (no request-parameter
    objects): `listTasks(q?, status?: TaskStatus, priority?: TaskPriority, dueFrom?: string, dueTo?: string,
    archived?: boolean, sort?: 'createdAt' | 'dueDate', direction?: 'asc' | 'desc')` → `Observable<Task[]>`;
    `listOverdueTasks()` → `Observable<Task[]>`; `createTask(TaskCreate)`, `updateTask(id, TaskUpdate)`,
    `completeTask(id)`, `archiveTask(id)`, `restoreTask(id)` → `Observable<Task>`; `deleteTask(id)` →
    `Observable<any>`; `getTask(id)` (unused).
  - models: `Task { id: number; title; description?: string | null; status; priority; dueDate?: string | null;
    createdAt; updatedAt; completedAt?: string | null; archived: boolean; overdue: boolean }`, `TaskCreate { title;
    description?; status?; priority?; dueDate? }`, `TaskUpdate { title; description?; status; priority; dueDate? }`.
  - enums are `as const` objects plus a union type: `TaskStatus = {Todo: 'TODO', InProgress: 'IN_PROGRESS', Done:
    'DONE'}`, `TaskPriority = {Low: 'LOW', Medium: 'MEDIUM', High: 'HIGH'}`.
  - still no `Problem` / `FieldError` model (errors not declared in the swagger): `core/problem.ts` stays.
  - ui-contract "Tasks `/tasks` (US1)" lists a selector for every AC-US1-1..12 action (form, search, filters, sort,
    overdue toggle, rows, row actions, empty state from the Shell section): nothing missing.
- **Checked at implement (T021, 2026-09-24)**, client regenerated after backend US2:
  - `HabitsService` (`api/habits.service.ts`, `providedIn: 'root'`), positional arguments: `listHabits(active?:
    boolean)` → `Observable<Habit[]>`; `createHabit(HabitWrite)`, `updateHabit(id, HabitWrite)`, `deactivateHabit(id)`,
    `activateHabit(id)` → `Observable<Habit>`; `completeHabit(id, habitCompletionCreate?: HabitCompletionCreate)` →
    `Observable<HabitCompletion>` (body optional; the page passes none); `undoHabitCompletion(id, date: string)` and
    `deleteHabit(id)` → `Observable<any>`; `getHabit(id)`, `listHabitCompletions(id)` (unused).
  - models: `Habit { id: number; name; description?: string | null; frequency: HabitFrequency; createdAt; active:
    boolean; completedToday: boolean; doneForCurrentPeriod: boolean; currentStreak: number }`, `HabitWrite { name;
    description?: string | null; frequency }`, `HabitFrequency = {Daily: 'DAILY', Weekly: 'WEEKLY'}` (`as const`).
  - still no `Problem` model: `core/problem.ts` stays.
  - ui-contract "Habits `/habits` (US2)" has selectors for the form, cards (frequency label, period done, streak,
    inactive badge), the today toggle and every card action; the empty state comes from the Shell section: AC-US2-1..9
    covered, nothing missing.
- **Checked at implement (T026, 2026-09-24)**, client regenerated after backend US3:
  - `LearningService` (`api/learning.service.ts`, `providedIn: 'root'`), positional arguments: `listLearningCards()` →
    `Observable<LearningCard[]>`; `createLearningCard(LearningCardCreate)`, `updateLearningCard(id,
    LearningCardUpdate)` → `Observable<LearningCard>`; `addMilestone(id, MilestoneCreate)`, `updateMilestone(id,
    milestoneId, MilestoneUpdate)` → `Observable<Milestone>`; `addNote(id, NoteCreate)` → `Observable<Note>`;
    `deleteLearningCard(id)`, `deleteMilestone(id, milestoneId)`, `deleteNote(id, noteId)` → `Observable<any>`;
    `getLearningCard(id)` (unused).
  - models: `LearningCard { id: number; title; description?: string | null; status: LearningStatus; createdAt;
    milestones: Milestone[]; notes: Note[]; milestonesDone: number; milestonesTotal: number }`, `LearningCardCreate
    { title; description? }`, `LearningCardUpdate { title; description?; status }`, `Milestone { id; cardId; title;
    done: boolean; targetDate?: string | null }`, `MilestoneCreate { title; targetDate? }`, `MilestoneUpdate { title;
    targetDate?; done }`, `Note { id; cardId; text; createdAt }`, `NoteCreate { text }`, `LearningStatus =
    {NotStarted: 'NOT_STARTED', InProgress: 'IN_PROGRESS', Completed: 'COMPLETED'}` (`as const`).
  - still no `Problem` model: `core/problem.ts` stays.
  - ui-contract "Learning Resources `/learning` (US3)" has selectors for the form, cards (title, status select,
    milestone count), expand, edit/delete, the milestone and note add rows and rows; the empty state comes from the
    Shell section: AC-US3-1..9 covered, nothing missing.
- **Checked at implement (T030, 2026-09-25)**, client regenerated after backend US4:
  - `PlansService` (`api/plans.service.ts`, `providedIn: 'root'`), positional arguments: `listPlans(group?: 'active' |
    'completed' | 'all')` → `Observable<Plan[]>`; `createPlan(PlanCreate)`, `getPlan(id)`, `updatePlan(id, PlanUpdate)`
    → `Observable<Plan>`; `setPlanItemDone(id, itemId, PlanItemUpdate)` → `Observable<Plan>`; `deletePlan(id)` →
    `Observable<any>`. `getPlan` is unused.
  - models: `Plan { id: number; title; items: PlanItem[]; estimatedDurationMinutes: number; startDateTime;
    endDateTime; priorityOrder: number; status: PlanStatus; createdAt; doneItems: number; totalItems: number;
    progressPercent: number; restSeconds?: number | null }`, `PlanItem { id; sourceType: PlanSourceType; sourceId;
    sourceTitle; sourceRemoved: boolean; done: boolean }`, `PlanCreate { title; items: PlanItemRef[];
    estimatedDurationMinutes; startDateTime; endDateTime; priorityOrder }`, `PlanUpdate` (the same without `items`),
    `PlanItemRef { sourceType; sourceId }`, `PlanItemUpdate { done }`, `PlanStatus = {NotStarted: 'NOT_STARTED',
    InProgress: 'IN_PROGRESS', Completed: 'COMPLETED'}`, `PlanSourceType = {Task: 'TASK', Habit: 'HABIT',
    LearningResource: 'LEARNING_RESOURCE'}` (`as const`).
  - still no `Problem` model and no `SettingsService` (backend US6): `core/problem.ts` stays; FA-30 applies.
  - ui-contract "Todo Plans `/plans` (US4)" has selectors for the create button, builder, source checkboxes,
    `error-items`, both groups, plan cards (status, progress, rest time, window, priority), items, actions and the
    started highlight; the empty state comes from the Shell section: AC-US4-1..13 covered, nothing missing.
- **Checked at implement (T036, 2026-09-26)**, client regenerated after backend US5:
  - `DashboardService` (`api/dashboard.service.ts`, `providedIn: 'root'`): `getDashboard()` (no parameters, `GET
    /api/dashboard`) → `Observable<Dashboard>`. The other services regenerate unchanged in their API.
  - models: `Dashboard { now: string; dueToday: Task[]; overdue: Task[]; completedToday: Task[];
    taskCompletionPercent: number; taskCounts: TaskCounts; habits: Habit[]; habitCounts: HabitCounts; activePlans:
    Plan[]; planCounts: PlanCounts; learning: LearningSnapshot }` (all required), `TaskCounts { total; done }`,
    `HabitCounts { active; completedToday }`, `PlanCounts { notStarted; inProgress; completed }`, `LearningSnapshot
    { notStarted; inProgress; completed; milestonesDone; milestonesTotal }` (all `number`, required): every field the
    contract lists is there.
  - still no `SettingsService` (backend US6): FA-34 applies to the greeting.
  - ui-contract "Dashboard `/dashboard` (US5)" has `dash-greeting`, the four `metric-*` cards, `dash-task-percent`, the
    three task lists with `dash-task-<id>` rows, `dash-habits` / `dash-habit-<id>` / `dash-habit-done-<id>` /
    `dash-habit-count`, `dash-plans` / `dash-plan-<id>` (with the plan card's `plan-progress`, `plan-rest-time`),
    `dash-learning` with its counts and `dash-milestones`, and the four `quick-add-*` actions; navigation (AC-US5-7)
    is the Shell's `nav-*`: AC-US5-1..7 covered, nothing missing.

## R-3 Time zone handling
- **Decision**: `ClockService` loads `/api/app-info` at start-up (`provideAppInitializer`), stores `timeZone` and
  `offsetMs = Date.parse(now) − Date.now()`, and ticks a `now` signal every 1 s. Display uses
  `Intl.DateTimeFormat(undefined, {timeZone, ...})`. `today()` is the app-zone calendar date of `now`
  (`formatToParts`, `en-CA` gives `YYYY-MM-DD`). A `datetime-local` value (wall time in the app zone) becomes an
  ISO string with offset: compute the zone offset for that wall time with `Intl.DateTimeFormat` option
  `timeZoneName: 'longOffset'` (`GMT+03:00`) and append it; editing does the reverse with `formatToParts`.
- **Rationale**: Constitution IV (frontend displays date-times in the app zone); Africa/Cairo has DST, so a
  fixed offset is wrong. `longOffset` is supported by chromium (the checks' browser).
- **Alternatives**: browser zone: rejected (midnight mismatch). A tz library: rejected (not pinned).

## R-4 Rest time and status changes while a page is open
- **Decision**: rest time text = `endDateTime − clock.now()` formatted `H:MM:SS`, shown only while the plan's
  API `status` is `IN_PROGRESS` and `restSeconds` is not null (BR-12). A page showing plans re-reads them
  (`listPlans` / `getDashboard`) when `now` passes any shown plan's `startDateTime` or `endDateTime`.
- **Rationale**: status is computed by the server (FR-08.4, Constitution IV); the UI never decides it.
- **Alternatives**: polling every N seconds: more requests, same result; computing status in the UI: rejected.

## R-5 Errors
- **Decision**: `problem.ts` turns an `HttpErrorResponse` with an `application/problem+json` body into
  `{message: detail ?? title, fieldErrors: Record<field, message>}`. Forms show `fieldErrors[f]` under field `f`;
  other errors go to the notice area. 409 on habit completion shows the server's `detail` (AC-US2-4).
- **Rationale**: contract `Problem` / `FieldError` schemas; AC-US1-2/3, AC-US2-2, AC-US3-2, AC-US4-3 need a
  message naming the field.

## R-6 Plan-start notification (FR-08.1, AC-US4-9, A-10; assumption FA-2)
- **Decision**: a root-level `PlanStartWatcher` (runs on every page) loads `listPlans(group=active)`
  at start-up and after any plan change, and when `now` reaches a `NOT_STARTED` plan's `startDateTime` shows a
  notice "Plan “<title>” has started" and highlights that plan on the Todo Plans page. On app open, plans that are
  `IN_PROGRESS` and whose id is not in `localStorage['quickflow.notifiedPlanIds']` are notified once (A-10). Only
  when `Settings.planStartNotifications` is true (FR-10.2).
- **Alternatives**: only on the Todo Plans page (the AC's minimum). Browser `Notification` API: rejected (A-10
  says in-app only).

## R-7 Quick-add from the Dashboard (AC-US5-6)
- **Decision**: quick-add buttons navigate to `/tasks?add=1`, `/habits?add=1`, `/learning?add=1`, `/plans?add=1`;
  each page opens its add form when `add=1` is present.

## R-8 Default page (FR-10.2)
- **Decision**: route `''` uses a guard that reads `getSettings()` and returns a `UrlTree` for the mapped route
  (`DASHBOARD→/dashboard, TASKS→/tasks, HABITS→/habits, LEARNING→/learning, PLANS→/plans, SETTINGS→/settings`).
  Deep links are not redirected. Unknown routes redirect to `''`.

## R-9 Habit completion toggle
- **Decision**: the toggle shows `completedToday`; checking calls `completeHabit(id)` with no body (server uses
  today, app zone); unchecking calls `undoHabitCompletion(id, clock.today())`. The card also shows
  `doneForCurrentPeriod` ("Done this week" for weekly) and `currentStreak`. Inactive habits show the toggle disabled.

## R-10 Change detection
- **Decision**: zoneless; component state in `signal`/`computed`; async results written into signals so views
  update without zone.js. The 1 s ticker is a `setInterval` writing the `now` signal.
