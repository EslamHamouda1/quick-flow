# phase-08 review: US6 Adjust settings
layer: frontend
story_id: US6
spec_phase: 8
depends_on: [phase-02, backend-dev:US6]

## Goal
The Settings page; preferences kept on the server and taking effect (default page here; greeting and notifications read them where used).

## Tasks
- [ ] T040 [US6] Check that the generated `SettingsService` has `getSettings`/`updateSettings` and `Settings` has `displayName`, `planStartNotifications`, `defaultPage` (enum DASHBOARD..SETTINGS); check the Settings section of contracts/ui-contract.md covers AC-US6-1..2; raise a question for anything missing
- [ ] T041 [US6] Implement the Settings page (loads `getSettings()`; display name, plan-start notifications checkbox, default page select with the six labels, save via `updateSettings` with field errors and a notice, read-only time zone from `ClockService`) in frontend/src/app/pages/settings/settings.page.ts (FR-10.2, AC-US6-1, AC-US6-2)
- [ ] T042 [US6] Add the default-page guard on route `''` (reads `getSettings()`, maps `defaultPage` to its route, research R-8) in frontend/src/app/core/default-page.guard.ts and use it in frontend/src/app/app.routes.ts
- [ ] T046 [US6] (new, from FA-34) Replace the FA-34 stub `displayName()` with `getSettings().displayName` read on every Dashboard load (blank, missing or a failed read → "Hello") in frontend/src/app/pages/dashboard/dashboard.page.ts (FR-10.2, FA-8, FA-34)
- [ ] T047 [US6] (new, from FA-30) Replace the FA-30 stub `notificationsEnabled()` with `getSettings().planStartNotifications` read right before each plan-start notice in frontend/src/app/core/plan-start-watcher.service.ts (FR-10.2, research R-6, FA-30)

T046 and T047 were promised to this phase by the approved FA-34 (phase-07 review) and FA-30 (phase-06 review). They
are added to `phase-08.md`, `phases.json` `task_ids` and `tasks.json` now; the implement session copies them into
`tasks.md` `## Phase 8` (step I.1). New T-ids follow the last one used (T045).

Order: first `generate_client` (run at the start of implement so `SettingsService`, `Settings` and `DefaultPage`
exist; checked now: `frontend/src/app/api/api` has no `settings.service.ts` and `api/model` no `settings.ts` /
`defaultPage.ts`), then T040, then T041, T042, T046, T047 one after another (no `[P]`).

## Acceptance criteria
- AC-US6-1: Given the Settings page, When the user opens it, Then the profile information and current preferences are shown.
- AC-US6-2: Given the Settings page, When the user changes a preference and saves, Then it is kept between sessions and takes effect (FR-10).

## Files
- app-wide: re-run `generate_client` → `frontend/src/app/api/**` (adds `SettingsService`, `Settings`,
  `DefaultPage`; the other files regenerate unchanged); `loops/frontend-dev/outputs/ui-url.md` rewritten unchanged.
- edit `specs/001-quickflow-frontend/research.md` R-2: a "Checked at implement (T040)" entry with the generated
  service name, method signatures and the `Settings` / `DefaultPage` model shapes found.
- rewrite `frontend/src/app/pages/settings/settings.page.ts` (T041), today a placeholder with only `page-title`
  "Settings". Keeps `page-title`. Adds:
  - `settings-form` with `settings-display-name` (text input, label "Display name"), `settings-notifications`
    (checkbox, label "Plan-start notifications"), `settings-default-page` (select, label "Default page", options in
    nav order with the nav labels: Dashboard, Tasks, Habits, Learning Resources, Todo Plans, Settings → values
    `DASHBOARD`..`SETTINGS`), `settings-save` ("Save").
  - `settings-time-zone`: `clock.timeZone()` (the app zone from `GET /api/app-info`, loaded at start-up), read-only
    text with label "Time zone".
  - field errors `error-displayName`, `error-planStartNotifications`, `error-defaultPage` from the API's
    `errors[].field` (existing `core/problem.ts` `readProblem`); other errors → `notice-error`.
  - on success a notice "Settings saved" and the form shows the returned `Settings` (the server trims the name and
    stores a blank one as null, checked now in `backend/.../domain/settings/AppSettings.java` `checkDisplayName`).
  - no `required`/`maxlength` attributes (FA-3).
- create `frontend/src/app/core/default-page.guard.ts` (T042): a functional `CanActivateFn` that reads
  `getSettings()` and returns `router.parseUrl(<route>)` for `DASHBOARD→/dashboard, TASKS→/tasks, HABITS→/habits,
  LEARNING→/learning, PLANS→/plans, SETTINGS→/settings`; a failed read or an unknown value → `/dashboard` (FA-39).
- edit `frontend/src/app/app.routes.ts` (T042): route `''` (`pathMatch: 'full'`) becomes `{ path: '', pathMatch:
  'full', canActivate: [defaultPageGuard], children: [] }` (a route needs a component, `children`, `redirectTo` or
  `loadComponent`; the exact form is checked against the installed `@angular/router` 22.2.0 typings at implement).
  The `**` → `/` redirect stays, so unknown routes also land on the default page (R-8). The comment on `''` is updated.
- edit `frontend/src/app/pages/dashboard/dashboard.page.ts` (T046): `displayName()` becomes
  `firstValueFrom(settingsApi.getSettings())` → `displayName?.trim() || null`; an error → `null` (no notice). The
  FA-34 comment is replaced. Nothing else on the page changes.
- edit `frontend/src/app/core/plan-start-watcher.service.ts` (T047): `notificationsEnabled()` becomes
  `firstValueFrom(settingsApi.getSettings())` → `planStartNotifications`; an error → `true` (FR-10.2 default, FA-40).
  The FA-30 comment is replaced. The plan id is still stored first, so a plan reached while notifications are off is
  not notified later (FA-40).
- endpoints used: `GET /api/settings` (`getSettings`), `PUT /api/settings` (`updateSettings`); `GET /api/app-info`
  is already read by `ClockService`.
- no other file changes (nav, shell and the other pages stay as they are).

## Planned checks
- Implement session (quick feedback only, not verification): `cd frontend && npm run build` succeeds; output in
  `loops/frontend-dev/runs/001-quickflow/phase-08-build.log`.
- No unit tests: the frontend layer has no `coverage` and `test` is empty (FA-1, approved).
- Testing loop (Playwright MCP, headless) can check:
  - fresh settings (or after `PUT /api/settings` with the defaults): `/settings` shows `settings-display-name`
    empty, `settings-notifications` checked, `settings-default-page` "Dashboard", `settings-time-zone` equal to
    `GET /api/app-info` `timeZone` (AC-US6-1).
  - after `PUT /api/settings` `{displayName: "Ada", planStartNotifications: false, defaultPage: "TASKS"}` through the
    API, opening `/settings` shows those three values (AC-US6-1, values come from the server).
  - change the name to "Ada", untick notifications, pick "Habits", click `settings-save` → a `notice` "Settings
    saved"; `GET /api/settings` returns the new values; reload `/settings` → the same values shown (AC-US6-2, kept).
  - takes effect: open `/` → lands on `/habits`; open `/dashboard` → `dash-greeting` "Hello, Ada"; set the name back
    to empty and save → `dash-greeting` "Hello"; deep link `/tasks` still opens Tasks (FA-5); an unknown route
    (`/nope`) → the default page.
  - notifications: with `planStartNotifications` false, a plan starting ~1 minute ahead while the app is open shows
    no "Plan “<title>” has started" notice and no `plan-started-<id>`; with it true (another plan) the notice and
    highlight appear (FR-08.1, FR-10.2).
  - a 101-character name + save → `error-displayName` with the server's message, nothing saved (FA-3).
  - `nav-settings` from any page reaches `/settings` with `page-title` "Settings".

## Risks
- **Client not regenerated yet**: the swagger has `/api/settings` with `getSettings` / `updateSettings`, tag
  `settings` (checked now), so the implement session runs `generate_client` first; T040 checks the generated names
  (expected `SettingsService`, `Settings { displayName?: string | null; planStartNotifications: boolean;
  defaultPage: DefaultPage }`, `DefaultPage` as an `as const` object like the other enums) before code uses them.
- **Guard route form**: an empty-path route with only `canActivate` must still be a valid `Route`; the implement
  session checks the router typings / a build and uses `children: []` (or an equivalent the typings accept). If no
  form is accepted it becomes a question.
- **Guard on every `''` visit**: the guard reads `getSettings()` each time `/` is opened (one small request), so a
  default page changed on Settings takes effect on the next `/` visit without a reload (FA-38).
- **Extra reads**: the watcher reads settings only when a notice is due (rare); the Dashboard reads it once per load,
  in parallel with `getDashboard`. No polling.
- **Saving while a save runs**: `settings-save` is disabled while `updateSettings` is pending.

## Assumptions
- **FA-38 (new) Default page read on every `''` visit**: plan.md says "read once per app load"; R-8 says the guard
  "reads `getSettings()`". The guard reads it on every navigation to `''` (app open at `/` and the `**` fallback),
  which covers "once per app load" and also applies a changed default page without a reload. Deep links are not
  redirected (FA-5).
- **FA-39 (new) Guard fallback**: if `getSettings()` fails, `/` opens the Dashboard (the FR-10.2 default) and a
  `notice-error` "Could not load settings" is shown, so the app still opens.
- **FA-40 (new) Notifications setting read**: the watcher reads `planStartNotifications` right before each notice
  (so a change on Settings applies at once); a failed read counts as on (FR-10.2 default). A plan whose start passes
  while notifications are off is recorded as seen and is not notified later when they are turned on (at most one
  notice per plan, A-10).
- **FA-41 (new) Settings page texts and behaviour**: labels "Display name", "Plan-start notifications", "Default
  page", "Time zone"; button "Save"; success notice "Settings saved". Nothing saves until `settings-save` is clicked
  (the three fields are sent together, the backend replaces all three). While `getSettings()` loads the form is not
  shown; if it fails a `notice-error` with the server's message is shown and the form is not shown (no values to
  edit). An empty name is sent as `null`.
- FA-1, FA-3, FA-5, FA-8, FA-10, FA-11, FA-30 and FA-34 apply as approved.

## Open questions
None.

## Swagger input
`loops/backend-dev/outputs/openapi.json` (from task.md).

## Contract differences
Compared with `specs/001-quickflow-backend/contracts/openapi.yaml` for the US6 endpoints (checked now):
- `GET /api/settings`, tag `settings`, `operationId: getSettings`, no parameters, 200 with `Settings`: match.
- `PUT /api/settings`, tag `settings`, `operationId: updateSettings`, required body `Settings`, 200 with `Settings`:
  match. The contract also lists `400` (`BadRequest`, problem details); the generated swagger declares only 200 (as
  for every other endpoint). No effect on the client: errors are read with `core/problem.ts` as elsewhere (R-5).
- `Settings`: same description, same required list (`planStartNotifications`, `defaultPage`), `displayName`
  `string | null` max 100, `planStartNotifications` boolean default true, `defaultPage` → `DefaultPage`. Extra in
  the generated swagger only: `minLength: 0` on `displayName`. No effect.
- `DefaultPage`: same six values `DASHBOARD, TASKS, HABITS, LEARNING, PLANS, SETTINGS`: match.
- Wording only: the contract's 200 descriptions ("Current settings (defaults on first read)", "Saved") appear as the
  generated `summary` ("Current settings (defaults on first read)", "Replace the settings"); the generated responses
  say "OK". No effect.
