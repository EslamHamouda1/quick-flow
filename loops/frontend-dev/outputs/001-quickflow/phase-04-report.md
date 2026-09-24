# phase-04-report: US2 Track recurring habits (frontend-dev), attempt 1

Plan: `phase-04-test.md`, written this attempt. Evidence folder:
`loops/testing/runs/001-quickflow/frontend-dev/phase-04/attempt-1/` (`checks-state.json` holds the values read
per check; `shot-NN-Cn.png` per check; `console-warnings-errors.txt`; page snapshots `page-*.yml`).
Playwright MCP, headless chromium, against `http://localhost:4200` (backend `http://localhost:8080`, profile `test`,
fresh DB). App date (Africa/Cairo): 2026-09-24. API reads were made from the page through the dev-server proxy
(`/api`); the one direct call to `http://localhost:8080` from the page origin was blocked by CORS (a test-side read,
not an app call; the app uses the proxy). Notices auto-dismiss after 5 s, so they were recorded by an in-page
observer as they appeared.

**Verdict: pass** (9/9).

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US2-9: no habits → empty state leading to Add Habit | pass | `checks-state.json` C1: API `[]`, `empty-state` "No habits yet" + `empty-state-action` "Add Habit"; click opens `habit-form` (frequency options Daily / Weekly); `/habits?add=1` opens `habit-form`; `shot-01-C1.png` |
| C2 | AC-US2-1: create → active, creation time set | pass | C2: form closes; `habit-card-1` "Weekly review" / `habit-frequency-label` "Weekly" / "Not done" / streak "0", no `habit-inactive`; API `active true`, `createdAt` set; second create (Daily "Drink water", empty description → `null`) shows `notice` "Habit created"; `shot-02-C2.png` |
| C3 | AC-US2-2 / BR-6: empty or 151-char name rejected, message on name field | pass | C3: both submits keep the form open, `error-name` "size must be between 1 and 150" under the Name field; API still 2 habits; console 400s; `shot-03-C3.png` |
| C4 | AC-US2-3, AC-US2-5: complete today → one record for today; label, period status, streak shown | pass | C4: Daily card "Done today", streak "1", toggle checked; Weekly card "Done this week", streak "1"; each habit has exactly one completion dated 2026-09-24; `notice` "Habit completed" ×2; `shot-04-C4.png` |
| C5 | AC-US2-4 / BR-7: second completion for the same date refused, user told | pass | C5: with the page open, API completion → 201 (toggle still unchecked); clicking the toggle → `notice-error` "Habit 2 is already complete for 2026-09-24"; card re-read to checked / "Done today"; API still one record for that date; `shot-05-C5.png` (taken after the notice closed) |
| C6 | FR-04.3: undo a completion | pass | C6: unchecking → "Not done", streak "0", `notice` "Habit completion undone"; API completions for habit 2 `[]`; `shot-06-C6.png` |
| C7 | AC-US2-6: edit name, description, frequency → saved | pass | C7: form prefilled ("Weekly review", "Review the week", WEEKLY); saved "Morning stretch" / "Ten minutes" / Daily; card shows new name and "Daily"; `notice` "Habit updated"; API has the new values; `shot-07-C7.png` |
| C8 | AC-US2-7: deactivate → not among active habits, history kept, cannot be completed; activate again | pass | C8: `habit-inactive` "Inactive", toggle disabled, `habit-activate-2` shown instead of deactivate; API `?active=true` → `[1]`; completion id 3 still listed; completing while inactive → 409 "Habit 2 is inactive"; after activate badge gone, toggle enabled, `?active=true` → `[1,2]`; `shot-08-C8-inactive.png`, `shot-08-C8.png` ("today's habits" on the Dashboard is US5) |
| C9 | AC-US2-8 / BR-14: removed habit and completions never returned | pass | C9: no confirm dialog (FA-7), `notice` "Habit deleted"; card gone; API list and `?active=true` → `[1]`; `GET /api/habits/2` → 404, `GET /api/habits/2/completions` → 404; `shot-09-C9.png` |

Console (`console-warnings-errors.txt`): the CORS error from the test's own direct read, the two 400s from C3, the
409s from C5 (UI click) and C8 (test probe), and the two 404s from C9 (test probes). No app errors.

## Unit tests
From `unit/unit-result.json`: outcome **none** ("no unit test command configured"). The frontend layer has no
`test` command and no `coverage`, so the unit rule does not apply.

## Requirement coverage
| id | unit tests | checks |
|---|---|---|
| AC-US2-1 / FR-03 (create) | none (no frontend test runner, FA-1) | C2 |
| AC-US2-2 / BR-6, FR-03.3 | none | C3 |
| AC-US2-3 / FR-04.1 | none | C4 |
| AC-US2-4 / BR-7, FR-04.2 | none | C5 |
| AC-US2-5 / FR-04.5 | none | C2, C4, C6 |
| AC-US2-6 | none | C7 |
| AC-US2-7 / FR-04.4 | none | C8 |
| AC-US2-8 / BR-14 | none | C9 |
| AC-US2-9 / NFR-5 | none | C1 |
| FR-04.3 (undo) | none | C6 |

No id of this phase is without a check. None has frontend unit tests (the layer has none configured, FA-1).

## Questions
None.
