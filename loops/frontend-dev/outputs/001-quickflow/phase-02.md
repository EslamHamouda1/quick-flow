# phase-02: Foundational
layer: frontend
story_id: none
spec_phase: 2
depends_on: [phase-01]
## Goal
API client wiring, app time zone and clock, error mapping, notices, shell with persistent navigation, routes.
## Acceptance criteria
- AC-US5-7: Given any page, When the user uses the persistent navigation, Then Dashboard, Tasks, Habits, Learning Resources, Todo Plans and Settings are each reachable. (shell part; the pages are placeholders until their story phase)
## Tasks
- [x] T005 Check the generated client in frontend/src/app/api/ (service class names, `getAppInfo`, `getSettings`, the provider function or `Configuration` class, enum shapes) and write the names found into specs/001-quickflow-frontend/research.md R-2
- [x] T006 Provide `HttpClient` (`provideHttpClient(withFetch())`) and the generated API client with base path `''` in frontend/src/app/app.config.ts
- [x] T007 [P] Create `ClockService` in frontend/src/app/core/clock.service.ts: loads `getAppInfo()` via `provideAppInitializer`, stores `timeZone` and server offset, `now` signal ticking every 1 s, `today()` = app-zone `YYYY-MM-DD` (research R-3)
- [x] T008 [P] Create frontend/src/app/core/date-format.ts: `formatDate(isoDate)`, `formatDateTime(iso, zone)`, `toZonedIso(datetimeLocal, zone)` (offset from `timeZoneName: 'longOffset'`), `toDatetimeLocal(iso, zone)`, `formatDuration(ms)` → `H:MM:SS` (research R-3)
- [x] T009 [P] Create frontend/src/app/core/problem.ts: `readProblem(err: HttpErrorResponse)` → `{message, fieldErrors: Record<string,string>}` from the contract `Problem`/`FieldError` shape (research R-5)
- [x] T010 [P] Create `NoticeService` (signal list, `success()`, `error()`, `info()`, auto-dismiss after 5 s) in frontend/src/app/core/notice.service.ts
- [x] T011 [P] Create shared components frontend/src/app/shared/empty-state.ts (`empty-state`, `empty-state-action`, emits `add`) and frontend/src/app/shared/field-error.ts (`error-<field>`)
- [x] T012 [P] Create placeholder standalone pages (each with `<h1 data-testid="page-title">` = its nav label) in frontend/src/app/pages/{dashboard/dashboard.page.ts,tasks/tasks.page.ts,habits/habits.page.ts,learning/learning.page.ts,plans/plans.page.ts,settings/settings.page.ts}
- [x] T013 Define lazy routes `/dashboard`, `/tasks`, `/habits`, `/learning`, `/plans`, `/settings`, `''` → `/dashboard` (replaced by the default-page guard in US6) and `**` → `''` in frontend/src/app/app.routes.ts (data-model Routes)
- [x] T014 Build the shell in frontend/src/app/app.ts, frontend/src/app/app.html, frontend/src/app/app.css: `nav-main` with the six `nav-*` links (`routerLinkActive`), notice area rendering `NoticeService` items (`notice`, `notice-error`), `<router-outlet />` (FR-10.1, AC-US5-7)
