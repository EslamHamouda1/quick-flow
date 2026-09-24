# phase-02 review: Foundational
layer: frontend
story_id: none
spec_phase: 2
depends_on: [phase-01]

## Goal
API client wiring, app time zone and clock, error mapping, notices, shell with persistent navigation, routes.

## Tasks
- [ ] T005 Check the generated client in frontend/src/app/api/ (service class names, `getAppInfo`, `getSettings`, the provider function or `Configuration` class, enum shapes) and write the names found into specs/001-quickflow-frontend/research.md R-2
- [ ] T006 Provide `HttpClient` (`provideHttpClient(withFetch())`) and the generated API client with base path `''` in frontend/src/app/app.config.ts
- [ ] T007 [P] Create `ClockService` in frontend/src/app/core/clock.service.ts: loads `getAppInfo()` via `provideAppInitializer`, stores `timeZone` and server offset, `now` signal ticking every 1 s, `today()` = app-zone `YYYY-MM-DD` (research R-3)
- [ ] T008 [P] Create frontend/src/app/core/date-format.ts: `formatDate(isoDate)`, `formatDateTime(iso, zone)`, `toZonedIso(datetimeLocal, zone)` (offset from `timeZoneName: 'longOffset'`), `toDatetimeLocal(iso, zone)`, `formatDuration(ms)` → `H:MM:SS` (research R-3)
- [ ] T009 [P] Create frontend/src/app/core/problem.ts: `readProblem(err: HttpErrorResponse)` → `{message, fieldErrors: Record<string,string>}` from the contract `Problem`/`FieldError` shape (research R-5)
- [ ] T010 [P] Create `NoticeService` (signal list, `success()`, `error()`, `info()`, auto-dismiss after 5 s) in frontend/src/app/core/notice.service.ts
- [ ] T011 [P] Create shared components frontend/src/app/shared/empty-state.ts (`empty-state`, `empty-state-action`, emits `add`) and frontend/src/app/shared/field-error.ts (`error-<field>`)
- [ ] T012 [P] Create placeholder standalone pages (each with `<h1 data-testid="page-title">` = its nav label) in frontend/src/app/pages/{dashboard/dashboard.page.ts,tasks/tasks.page.ts,habits/habits.page.ts,learning/learning.page.ts,plans/plans.page.ts,settings/settings.page.ts}
- [ ] T013 Define lazy routes `/dashboard`, `/tasks`, `/habits`, `/learning`, `/plans`, `/settings`, `''` → `/dashboard` (replaced by the default-page guard in US6) and `**` → `''` in frontend/src/app/app.routes.ts (data-model Routes)
- [ ] T014 Build the shell in frontend/src/app/app.ts, frontend/src/app/app.html, frontend/src/app/app.css: `nav-main` with the six `nav-*` links (`routerLinkActive`), notice area rendering `NoticeService` items (`notice`, `notice-error`), `<router-outlet />` (FR-10.1, AC-US5-7)

Order: T005 → T006 (both non-[P]), then the [P] group T007–T012 in parallel, then T013 → T014.

## Acceptance criteria
- AC-US5-7: Given any page, When the user uses the persistent navigation, Then Dashboard, Tasks, Habits, Learning Resources, Todo Plans and Settings are each reachable. (shell part; the pages are placeholders until their story phase)

## Files
- edit `specs/001-quickflow-frontend/research.md` R-2: names found in the generated client (T005)
- edit `frontend/src/app/app.config.ts`: add `provideHttpClient(withFetch())`, `provideApi('')`, the `ClockService` app initializer (T006, T007)
- create `frontend/src/app/core/clock.service.ts` (T007), `frontend/src/app/core/date-format.ts` (T008), `frontend/src/app/core/problem.ts` (T009), `frontend/src/app/core/notice.service.ts` (T010)
- create `frontend/src/app/shared/empty-state.ts`, `frontend/src/app/shared/field-error.ts` (T011)
- create six placeholder pages under `frontend/src/app/pages/*/` (T012)
- edit `frontend/src/app/app.routes.ts` (T013); edit `frontend/src/app/app.ts`, `app.html`, `app.css` (T014)
- endpoint used: `GET /api/app-info` (`InfoService.getAppInfo`) only
- app-wide outputs of the implement step: re-run `generate_client` (`frontend/src/app/api/**`), `loops/frontend-dev/outputs/ui-url.md`
- `frontend/src/app/app.spec.ts` (scaffold test referencing `title`): left as is unless it breaks `npm run build` (it is not part of the build target); no test runner is configured (`test` is empty)

## Planned checks
- Implement session (quick feedback only, not verification): `cd frontend && npm run build` succeeds; output in `loops/frontend-dev/runs/001-quickflow/phase-02-build.log`.
- No unit tests: the frontend layer has no `coverage` and `test` is empty (FA-1, approved).
- Testing loop (Playwright MCP, headless) can check:
  - `http://localhost:4200/` redirects to `/dashboard`; an unknown path (e.g. `/nope`) ends on `/dashboard`.
  - `nav-main` is present on every page; each of `nav-dashboard`, `nav-tasks`, `nav-habits`, `nav-learning`, `nav-plans`, `nav-settings` has the label from ui-contract and, when clicked, lands on its route with `page-title` equal to that label (AC-US5-7).
  - the active link carries the `routerLinkActive` class.
  - the page loads with a `GET /api/app-info` request (via the proxy) and no console errors.

## Risks
- **Generated client is partial**: the current swagger (`loops/backend-dev/outputs/openapi.json`) has only `/api/app-info`, so the client has only `InfoService` + `AppInfo`. `getSettings`, the other tag services and the enum models named in T005 do not exist yet. T005 records what exists now and marks the rest "not generated yet (backend story pending)"; later story phases re-check after `generate_client`. Nothing in this phase calls `getSettings` (the default-page guard is US6).
- **`Problem`/`FieldError` are not in the generated swagger** (no error responses declared yet), so no generated model exists. T009 defines a local `Problem`/`FieldError` interface from the contract (`specs/001-quickflow-backend/contracts/openapi.yaml`: `title`, `status` required; `type`, `detail`, `instance`, `errors[{field, message}]`).
- **Base path**: checked now: `api.base.service.ts` defaults `basePath` to `http://localhost:18080` (the swagger's server URL); `provideApi('')` sets `BASE_PATH` to `''`, and the base service accepts it (`typeof '' === 'string'`), so requests go to same-origin `/api/...` through the proxy.
- **App start depends on `/api/app-info`**: `provideAppInitializer` blocks bootstrap on the request; if the backend is down the app would not render. See assumption FA-12.
- `longOffset` / `formatToParts` behaviour is browser-dependent; the checks run in chromium (R-3).

## Assumptions
- **FA-12 (new)**: if `GET /api/app-info` fails at start-up, the initializer does not block the app: `ClockService` keeps `offsetMs = 0`, uses the browser's zone (`Intl.DateTimeFormat().resolvedOptions().timeZone`) as a fallback, and `NoticeService.error()` shows "Could not load app time zone from the server". Needs approval; the alternative is letting bootstrap fail (blank page).
- FA-1, FA-3 (server-side validation messages only), FA-10 (no extra dependencies, plain CSS) apply as approved at phase-00.

## Open questions
None.

## Swagger input
`loops/backend-dev/outputs/openapi.json` (from task.md).

## Contract differences
Compared with `specs/001-quickflow-backend/contracts/openapi.yaml` for what this phase uses:
- `GET /api/app-info` / `AppInfo {timeZone, now}` (both required): matches.
- `servers`: generated `http://localhost:18080` vs contract `http://localhost:8080`. No effect: the client is given base path `''`.
- `Problem` / `FieldError` schemas and `application/problem+json` error responses: in the contract, absent from the generated swagger (backend has not added error responses yet). Handled locally in T009 (see Risks).
- `GET /api/settings` (`getSettings`, named in T005): in the contract, absent from the generated swagger (backend US6 not built). Not used in this phase.
