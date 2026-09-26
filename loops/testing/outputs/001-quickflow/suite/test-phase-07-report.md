# test-phase-07 report: US6 Adjust settings (attempt 1)

Run 2026-09-26 13:24–14:00 (Africa/Cairo) against a fresh test database. Evidence folder:
`loops/testing/runs/001-quickflow/suite/test-phase-07/attempt-1/`: `curl.log` (API checks, raw responses with an
`== <check> ... HTTP <code>` line after each), `body-name100.json` / `body-name101.json` (jq-generated boundary bodies),
`ui-results.json` (Playwright MCP return values), `shot-01..10-*.png` (screenshots), `page-*.yml` / `console-*.log`
(Playwright MCP).

API checks ran as direct curl calls (`curl --next`, appended to `curl.log`). UI checks ran through Playwright MCP,
headless, with the `data-testid` selectors from `specs/001-quickflow-frontend/contracts/ui-contract.md`; UI
read-backs used `fetch('/api/settings')` from the page, through the dev-server proxy. `localStorage` was cleared before
the UI checks. Plan-start notices were recorded with a `MutationObserver` on the page, because they are transient.

**Result: 10/10 pass, 0 fail, 0 unclear. No bugs filed.**

| check | criterion | result | evidence |
|---|---|---|---|
| A1 | FR-10.2 | pass | curl.log `== A1 GET /api/settings (fresh DB)`: 200 `{"displayName":null,"planStartNotifications":true,"defaultPage":"DASHBOARD"}`; `== A1 GET /api/app-info`: 200 `timeZone` "Africa/Cairo" |
| A2 | AC-US6-2 / NFR-3 | pass | curl.log `== A2 …`: PUT then GET for each of TASKS, HABITS, LEARNING, PLANS, SETTINGS, DASHBOARD: every PUT 200 and the next GET returns exactly the values sent (name "Eslam"/"Tester Q", notifications false/true); `displayName` null clears the name (PUT and GET null) |
| A3 | FR-10.2 | pass | `== A3 PUT displayName 100 chars` 200, GET returns the 100-char name; `== A3 PUT displayName 101 chars` 400 `errors[0].field` "displayName" ("size must be between 0 and 100"); the next GET still has the 100-char name |
| A4 | FR-10.2 | pass | 400 naming `defaultPage` for "HOME", "dashboard" (lower case) and missing; 400 naming `planStartNotifications` for "maybe", missing and null; `== A4 GET after invalid PUTs` equals the last saved settings |
| U1 | AC-US6-1 | pass | shot-01; ui-results `U1`: `page-title` "Settings", `settings-display-name` = the API's 100-char name, `settings-notifications` unchecked (= false), `settings-default-page` PLANS "Todo Plans" (six options, one per FR-10.1 page), `settings-time-zone` "Africa/Cairo" = `GET /api/app-info` `timeZone`, read-only (a `SPAN`) |
| U2 | AC-US6-2 / NFR-3 | pass | shot-02 / shot-03; ui-results `U2`: fill "Mona Test", check notifications, select Habits, `settings-save` → PUT 200, `notice` "Settings saved" (no `notice-error`), API GET = the new values; after reload and in a new browser context (no shared storage) the form shows "Mona Test" / checked / HABITS |
| U3 | FR-10.2 (takes effect) | pass | shot-04 / shot-05; ui-results `U3`: `dash-greeting` "Hello, Mona Test"; `/` → `/habits` after the UI save; for each of the six `defaultPage` values `/` lands on its route with the matching `page-title`; name cleared through the form (sent `null`) → `/` opens `/dashboard` with "Hello" |
| U4 | FR-10.2 / FR-08.1 (takes effect) | pass | ui-results `U4`: notifications off (saved through the form) → plan 1 starting 13:56:25 with `/plans` open, observed until 13:56:45: no `notice`, no `plan-started-1`, the card turned "In Progress" (API IN_PROGRESS) (shot-06); notifications on → plan 2 starting 13:58:00: `notice` "Plan “U4 notifications ON” has started" at 13:58:00.09 and `plan-started-2` shown (shot-07, after the notice had gone); plan 3 at 13:59:14: notice at 13:59:14.38, screenshot with the notice (shot-08) |
| U5 | FR-10.2 | pass | shot-09; ui-results `U5`: 101-char name + Tasks, `settings-save` → PUT 400, `error-displayName` "size must be between 0 and 100", no "saved" notice, API unchanged ("Mona Test", DASHBOARD) and the reload shows the saved values |
| U6 | FR-10.1 | pass | shot-10; ui-results `U6`: from each of the six pages `nav-settings` (text "Settings") → `/settings`, `page-title` an H1 "Settings", 0 bad |

Notes (recorded, not filed):
- The `settings-display-name` input accepts more than 100 characters; the limit is enforced by the API and shown in
  `error-displayName`, which is what the checklist asks for.
- shot-08: at the moment of the plan 3 notice the card still read "Not Started" (the status label follows on the page's
  next refresh; in the notifications-off run plan 1 read "In Progress" 20 s after its start without a reload). Status
  timing on the page isn't a US6 criterion; plan status was checked in test-phase-05.
- After a reload of `/plans`, plan 2 no longer showed its Started badge. AC-US4-9 asks for the highlight when the start
  is reached while the app is open, so this was not asserted.
- The run paused about 27 minutes between A4 (13:25) and U1 (13:53) while waiting for tool approval; no check spans that gap.

Console: one error, the expected 400 from U5 (`/api/settings`); otherwise only Angular dev-mode and vite reconnect logs.

## Unit tests
Not part of this test phase: the runner didn't run unit tests into this attempt folder (no `unit/` folder, and
`task.md` has no `unit_result`). They run in `test-phase-regression`.

## Requirement coverage
| id | covered by |
|---|---|
| FR-10.2 settings shown (profile + preferences, time zone read-only from app-info) | A1, U1 |
| FR-10.2 kept between sessions / stored on the server | A2, U2 |
| FR-10.2 `displayName` optional, ≤ 100, used in the greeting | A2 (null), A3, U3, U5 |
| FR-10.2 `planStartNotifications` default on, notices only when on | A1, A4, U4 |
| FR-10.2 `defaultPage` one of six, default Dashboard, the page the app opens on | A1, A2, A4, U3 |
| FR-10.1 navigation to Settings | U6 |
| NFR-3 state persists between sessions | A2, U2 |
| AC-US6-1 | U1 |
| AC-US6-2 | A2, U2, U3, U4 |

All ids of this phase are covered.

## Questions
None.
