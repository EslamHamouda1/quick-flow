# phase-08-test: US6 Adjust settings (backend-dev)

Layer `backend` (API at `http://localhost:8080`), checks run by `phase-08-verify.sh <attempt-folder>` on a
fresh test database. Sources: `phase-08.md` acceptance criteria, `specs/001-quickflow-backend/spec.md`
FR-10.2 / NFR-3, the generated swagger `loops/backend-dev/outputs/openapi.json` (`getSettings`,
`updateSettings`, schemas `Settings`, `DefaultPage`) and the contract `contracts/openapi.yaml`
(`updateSettings` 400 → `BadRequest`, `application/problem+json` `Problem` with `errors[].field`).

Acceptance criteria this plan was written for:
- AC-US6-1: Given the Settings page, When the user opens it, Then the profile information and current preferences are shown.
- AC-US6-2: Given the Settings page, When the user changes a preference and saves, Then it is kept between sessions and takes effect (FR-10).

The "takes effect" part (default page opened, plan-start notifications shown, greeting) is frontend behaviour
and is not checked here. A backend restart is not done by the testing loop (it never starts or stops
processes); persistence across requests is checked, restart persistence is left to the unit test
`nfr3_*` and the suite.

| check | criterion | steps | expected (quoted) |
|---|---|---|---|
| C1 | AC-US6-1, FR-10.2, swagger | `GET /v3/api-docs`; `GET /api/settings` twice on the empty database | api-docs: `getSettings` (GET) and `updateSettings` (PUT) on `/api/settings`, tag `settings`, schemas `Settings` (required `defaultPage`, `planStartNotifications`; `displayName` `[string, null]` maxLength 100) and `DefaultPage` (the six values). GET: 200 `application/json`, exactly the keys `displayName`, `planStartNotifications`, `defaultPage`; defaults "`displayName` (optional …)" → `null`, "`planStartNotifications` (on/off, default on)" → `true`, "`defaultPage` (… default Dashboard)" → `"DASHBOARD"`; second GET identical |
| C2 | AC-US6-2, FR-10.2, NFR-3 | `PUT /api/settings` `{"displayName":"Eve","planStartNotifications":false,"defaultPage":"HABITS"}`; `GET`; `PUT` with each of the six `defaultPage` values, `GET` after each; `PUT` `displayName: null`; `GET`; `PUT` without `displayName`; `GET` | "keep changed preferences between sessions", swagger "Replace the settings": 200 with the saved body; the next GET returns the same three values; each of `DASHBOARD, TASKS, HABITS, LEARNING, PLANS, SETTINGS` accepted and read back; `displayName` null (and omitted, `displayName` is not in `required`) → GET shows `null` |
| C3 | FR-10.2 "`displayName` (optional, at most 100 characters)", swagger maxLength 100 | set a known state; `PUT` with a 100-char `displayName`; `GET`; `PUT` with a 101-char `displayName`; `GET` | 100 chars → 200, stored whole; 101 chars → 400 `application/problem+json`, `status 400`, an `errors[]` entry with `field == "displayName"` and a non-empty message; the stored settings unchanged |
| C4 | FR-10.2 "`defaultPage` (one of the six pages)", swagger `DefaultPage` enum, `required` | `PUT` with `defaultPage` `"FOO"`, missing, `null`, `"dashboard"` (lower case); `GET` after | each → 400 `application/problem+json`; for `"FOO"`, missing and `null` an `errors[]` entry with `field == "defaultPage"`; stored settings unchanged |
| C5 | FR-10.2 "`planStartNotifications` (on/off)", swagger `type: boolean`, `required` | `PUT` with `planStartNotifications` missing, `null`, `"yes"`; `GET` after | each → 400 `application/problem+json`; for missing and `null` an `errors[]` entry with `field == "planStartNotifications"`; stored settings unchanged |
