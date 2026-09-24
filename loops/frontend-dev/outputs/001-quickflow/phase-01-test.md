# phase-01-test: Setup (frontend-dev)

Source of the checks: `loops/frontend-dev/outputs/001-quickflow/phase-01.md` has an empty
`## Acceptance criteria` section (Setup phase, no user story; the review says "None (Setup phase;
no user story)"). The approved review file `phase-01-review.md`, `## Planned checks`, states what
the testing loop can observe after this phase; each statement is one criterion. Criteria listed
(compare on later attempts):

- R1: "the app at `http://localhost:4200` loads with no scaffold welcome content and no console errors"
- R2: "a request to `/api/app-info` through the dev server reaches the backend (proxy works)"

Swagger (`loops/backend-dev/outputs/openapi.json`): `GET /api/app-info` → 200, `AppInfo`
`{timeZone: string, now: date-time}`, both required.

| check | criterion | Playwright MCP steps | expected |
|---|---|---|---|
| C1 | R1 | `browser_navigate` `http://localhost:4200`; `browser_snapshot`; screenshot `shot-01-C1.png` | page loads; no scaffold welcome content (the Angular CLI welcome page, e.g. "Hello, …" / "Congratulations! Your app is running.") |
| C2 | R1 | after C1, `browser_console_messages` (level error); screenshot `shot-02-C2.png` | no console errors |
| C3 | R2 | `browser_navigate` `http://localhost:4200/api/app-info`; `browser_evaluate` `fetch('/api/app-info')` status + body; screenshot `shot-03-C3.png` | HTTP 200 from the dev server origin, JSON body with `timeZone` (string) and `now` (date-time), per swagger `AppInfo` |

Unit tests: the frontend layer has no `test` command and no `coverage` (`unit-result.json`
outcome `none`).
