# phase-08-report: US6 Adjust settings (frontend-dev), attempt 1

The test plan `phase-08-test.md` was written in this attempt (AC-US6-1, AC-US6-2). All evidence is in
`loops/testing/runs/001-quickflow/frontend-dev/phase-08/attempt-1/`: `C1-state.json`, `C2-C8-results.json`,
`shot-01`..`shot-21`, and the console log. Playwright MCP ran headless on `http://localhost:4200`, with a fresh test
database. Data was set up and read back through the dev-server proxy (`/api/...`). Time zone: Africa/Cairo.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US6-1, FR-10.2 | pass | `C1-state.json`: `/settings` shows `settings-form` with `settings-display-name` "" and `settings-notifications` checked. `settings-default-page` shows "Dashboard", with six options in nav order and nav labels (`DASHBOARD`..`SETTINGS`). These match `GET /api/settings` `{null, true, DASHBOARD}`. `settings-time-zone` is a read-only `<span>` showing "Africa/Cairo", which matches `GET /api/app-info` `timeZone`. Labels: "Display name", "Plan-start notifications", "Default page". `shot-01-C1.png` |
| C2 | AC-US6-1 | pass | `C2-C8-results.json` C2: `PUT /api/settings` `{Ada, false, TASKS}` returned 200. After a reload the form shows "Ada", unchecked, "Tasks". `shot-02-C2.png` |
| C3 | AC-US6-2 (kept) | pass | C3: the form was set to "Grace", checked, "Habits", then `settings-save` was clicked. The `notice` said "Settings saved", and `GET /api/settings` returned `{Grace, true, HABITS}`. After a new page load the form shows the same values. `shot-03-C3-saved.png`, `shot-04-C3-reload.png` |
| C4 | AC-US6-2 (takes effect: default page) | pass | C4: with HABITS set, `/` opens `/habits`. Each of the six values was saved through the form and `/` was opened: it lands on `/dashboard`, `/tasks`, `/habits`, `/learning`, `/plans`, `/settings`, each with the matching `page-title`. With HABITS set, the deep link `/tasks` still opens Tasks and `/nope` opens `/habits` (R-8). `shot-05`..`shot-13` |
| C5 | AC-US6-2 (takes effect: greeting) | pass | C5: `dash-greeting` shows "Hello, Grace". After the name was cleared and saved (the API stores `displayName` null), it shows "Hello". `shot-14-C5-named.png`, `shot-15-C5-cleared.png` |
| C6 | AC-US6-2 (takes effect: notifications), FR-10.2, FR-08.1 | pass | C6 "off": with `planStartNotifications` false, P-off (id 1) started at 08:21:41Z while `/plans` stayed open with no reload. Polling every 200 ms until 08:22:06Z saw no `notice` and no `plan-started-1`, although the card turned In Progress with its rest time. C6 "on": notifications were turned on through the Settings form. P-on2 (id 3) started at 08:25:37Z, and at 08:25:37.183Z the `notice` "Plan “P-on2” has started" and `plan-started-3` "Started" appeared. `shot-18-C6-off.png`, `shot-20`/`shot-21` (the notice is transient; the timestamps come from the 200 ms poll). See the note below about the first "on" try |
| C7 | FR-10.2 (displayName ≤ 100) | pass | C7: a 101-character name was saved. `error-displayName` showed "size must be between 0 and 100" (the server's message; the PUT got a 400), no success notice appeared, and `GET /api/settings` was unchanged (`Grace`). `shot-16-C7.png` |
| C8 | FR-10.1 | pass | C8: from `/dashboard`, `nav-settings` ("Settings") opens `/settings` with `page-title` "Settings". `shot-17-C8.png` |

Console: only Angular dev-mode log lines and the expected 400 from C7. No errors.

**Note on C6 (the first "on" try; test environment, not counted against the phase):** the first "on" try used P-on
(id 2, start 08:23:00Z) and showed no notice. The reason was the browser's `localStorage`
`quickflow.notifiedPlanIds`, which was `[2,3,5,7,8,1]`. Ids 2, 3, 5, 7 and 8 were left there by earlier test runs
on an older test database, in the same Playwright MCP profile. The runner deletes the test database but not the
browser storage, so ids that come back on a fresh database already count as notified. I removed only those
stale ids (kept `[1,2]`) and ran the case again with a new plan (id 3); it passed. Evidence is in
`C2-C8-results.json` C6 `on_first_try_stale_storage` and in `shot-19-C6-on.png`. This affects later UI checks of
plan-start notices (the suite's E2E phase too). The fix is to clear the Playwright profile's storage when the test
database is reset.

## Unit tests
From `unit/unit-result.json`: layer `frontend`, outcome `none` ("no unit test command configured"). Passed: n/a,
failed: n/a, coverage: n/a (the frontend layer has no `coverage` rule).

## Requirement coverage
| requirement | unit tests | checks |
|---|---|---|
| AC-US6-1 | none (frontend) | C1, C2 |
| AC-US6-2 | none (frontend) | C3, C4, C5, C6 |
| FR-10.2 displayName (≤ 100, greeting) | none (frontend; backend phase-08 unit tests cover the limit) | C1, C3, C5, C7 |
| FR-10.2 planStartNotifications | none (frontend) | C1, C3, C6 |
| FR-10.2 defaultPage | none (frontend) | C1, C3, C4 |
| FR-10.2 time zone read-only | none (frontend) | C1 |
| FR-10.1 | none (frontend) | C4, C8 |
| FR-08.1 | none (frontend) | C6 |

Frontend unit tests are optional (constitution III).

## Questions
None.
