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
