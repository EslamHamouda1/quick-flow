# test-phase-regression report: every phase-mode check plus unit tests (attempt 1)

Evidence folder: `loops/testing/runs/001-quickflow/suite/test-phase-regression/attempt-1/`.

This attempt ran over three sessions:
- **Session 1** (15:05–15:12, +03:00) crashed. Its files (`api-*.json`, `curl.log`, `swagger.html`) came from a
  different app run and DB, so they are **not** used as evidence here (see the header of `file-checks.txt`).
- **Session 2** (17:25–18:07) ran R1–R17 on a fresh test DB and crashed before it wrote this report. Its evidence:
  - `curl-r2.log`, which has a `== R<n>-C<m> … HTTP <code>` line after each raw response.
  - `r2-*.json` / `b-*.json` (bodies and read-backs).
  - `file-checks.txt` (sections R1, R2, R9, R20).
  - `ui-R10-R11.json` … `ui-R17.json`.
  - `shot-01..89`.
- **Session 3** (21:47–21:51) is this session. The runner re-ran the unit tests into `unit/` and restarted the app on a
  fresh test DB at 21:46. This session:
  - ran R18 and R19 again (`ui-R18-R19.json`, `R18-console-errors.txt`, `shot-90..111`, and the third section of `file-checks.txt`);
  - re-read R20 from this run's `unit/`;
  - did not re-run R1–R17, which keep the session 2 evidence.

The session 2 and session 3 app runs have the same backend: 338 unit tests and 39 operationIds.

The test plans in `outputs/001-quickflow/{backend-dev,frontend-dev}/*-test.md` were reused unchanged. The API checks
ran as direct curl calls, because `bash <verify.sh>` needs approval in this loop. The UI checks ran through Playwright
MCP (headless) with the `ui-contract.md` selectors, and set up data through `/api/*` on the page origin.

**Result: 20/20 pass, 0 fail, 0 unclear. No bugs filed.**

| check | criterion | result | evidence |
|---|---|---|---|
| R1 | backend-dev phase-01 Setup C1–C7 | pass | `curl-r2.log`: `== R1-C1 api-docs HTTP 200 application/json`, `== R1-C2 swagger-ui HTTP 200 text/html`. `file-checks.txt` R1: C4 unit exit 0, Boot 4.1.1 / Java 25 (running on 25.0.4.1). C5: jacoco report + check executions. C6: target = copy (`cmp`). C7: `backend/data/test.mv.db`, profile `test`, `jdbc:h2:file:./data/test` |
| R2 | backend-dev phase-02 Foundational C1–C7 | pass | `curl-r2.log` `== R2-C1 app-info HTTP 200`: `{"timeZone":"Africa/Cairo","now":"2026-09-26T17:25:40.86+03:00"}`, where the offset is +03:00 and `now` = the response `Date` 14:25:40 GMT. `== R2-C3 does-not-exist HTTP 404`. `r2-api-docs.json`: title "QuickFlow API", version 0.1.0, `getAppInfo`, AppInfo requires `now`/`timeZone`. `file-checks.txt` R2: C5 copy identical. C6 ApiExceptionHandlerTest 6/0. C7 TimeServiceTest 5/0 and AppInfoControllerTest 1/0 |
| R3 | backend-dev phase-03 US1 C1–C12 | pass | `curl-r2.log` `== R3-C1..C12`: create 201 with defaults. BR-1/2: 200/2000 → 201 and 201/2001 → 400 on POST and PUT. BR-3: bad status/priority → 400. Edit 200. BR-5 complete/re-open. BR-4 archive/restore: ids leave and return. BR-14 delete → 404 on all 6 routes and gone from list, archived, search and overdue. Case-insensitive search: the same 3 titles for 3 spellings. 12 filter/sort queries in the expected order. Overdue excludes today, Done and archived |
| R4 | backend-dev phase-04 US2 C1–C13 | pass | `curl-r2.log` `== R4-C1..C12`: DAILY/WEEKLY 201. BR-6: name empty/blank/missing/151 → 400, 150 → 201. desc 2001 → 400. Frequency missing/MONTHLY → 400. Completion defaults to today (201 x3). BR-7: 409 on a repeat and the day before is 201. Future date → 400. Streaks: DAILY 3/1, WEEKLY 3/1. Edit 200. Deactivate: gone from `active=true`, history kept, 409 on complete, activate → 201. Delete → 404 x8. Undo → 204 then 404 |
| R5 | backend-dev phase-05 US3 C1–C10 | pass | `curl-r2.log` `== R5-C1..C9`: create 201. BR-8: title empty/blank/missing/201 → 400, 200 ok. desc 2001 → 400, 2000 ok. Status PUT x3 read back, DONE/missing → 400, unknown → 404. Milestones on card A only, blank/201 → 400, unknown card → 404, done/undone saved, card B routes → 404. Notes: blank/missing/5001 → 400, 5000 ok, delete. Card delete → 404 on 8 routes. Embedded lists newest first |
| R6 | backend-dev phase-06 US4 C1–C15 | pass | `curl-r2.log` `== R6-C1..C14`: create 201. BR-10: empty/missing/unknown TASK/HABIT/LR and deleted task → 400. BR-11: end ≤ start → 400 on POST and PUT. Progress 66/50. BR-13: a TASK tick sets the task DONE and the un-tick keeps it; HABIT/LR unchanged. NOT_STARTED/IN_PROGRESS/COMPLETED follow the times and ticks. Groups ordered by priority. FR-07.6 sourceRemoved kept. Plan delete → 404. Field limits 400/201. FR-07.3 edit. C15: `r2-swagger-ops.txt` has the 6 plan operationIds with 201/204 and the Plan* schemas |
| R7 | backend-dev phase-07 US5 C1–C6 | pass | `curl-r2.log` `== R7-C1..C6`: empty dashboard. Entries = `GET /api/tasks` (6/6 true). 50% then 42% (floor). Active habits only, with 1 → 0 after undo. Learning 1/2/1 3/5 → 1/1/1 2/4. activePlans by priority with restSeconds going down (5602 → 5596). After the ticks: 50%, then the plan leaves, with counts 1/1/2 |
| R8 | backend-dev phase-08 US6 C1–C5 | pass | `curl-r2.log` `== R8-C1..C5`: defaults returned twice. PUT/GET for all six defaultPage values, and a null/omitted name clears. Name 100 → 200, 101 → 400 with settings unchanged. defaultPage FOO/missing/null/lower-case → 400. Notifications missing/null/"yes" → 400 |
| R9 | backend-dev phase-09 Polish C1–C7 | pass (C5 not run, as in the plan) | `file-checks.txt` R9: C1 live = contract operationIds, 39 each. C2 copy identical. C3 = R3..R8 above. C4 SchemaIndexesTest 4/0. C5 restart not run (the testing loop never restarts processes, as in the phase-09 plan). C6 97.3%. C7: every BR/FR id except FR-08.1/10.1 appears in a `@DisplayName` |
| R10 | frontend-dev phase-01 Setup C1–C3 | pass | `ui-R10-R11.json`: `/` → `/dashboard`, 0 console errors, `/api/app-info` via 4200 → 200. shot-01..03 |
| R11 | frontend-dev phase-02 Foundational C1–C9 | pass | `ui-R10-R11.json`: six `nav-*` clicks each give the right URL, title and `active` class. `nav-main` plus the six links are on every route. `/` and `/nope` → `/dashboard`. app-info 200. 0 console errors. shot-04..12 |
| R12 | frontend-dev phase-03 US1 C1–C13 | pass | `ui-R12.json`, shot-13..30 (empty state and form, create, errors, edit, complete, archive/restore, delete, search, filters/sort = API order, overdue view, nav) |
| R13 | frontend-dev phase-04 US2 C1–C9 | pass | `ui-R13.json`, shot-31..40 |
| R14 | frontend-dev phase-05 US3 C1–C9 | pass | `ui-R14.json`, shot-41..51 |
| R15 | frontend-dev phase-06 US4 C1–C13 | pass | `ui-R15.json`, shot-52..66. The builder offers no archived task or inactive habit. Empty/deleted-source → `error-items` "must not be empty" / "must reference an existing TASK". The live start notice showed (shot-66) |
| R16 | frontend-dev phase-07 US5 C1–C9 | pass | `ui-R16.json`, shot-67..79. Dashboard = API, rest time = restSeconds, a tick on the Dashboard updates it without reload, four quick-adds, nav moves |
| R17 | frontend-dev phase-08 US6 C1–C8 | pass | `ui-R17.json`, shot-80..89. Defaults = API. Save → "Settings saved" and the values survive a reload. `/` opens each of the six default pages. Greeting shows "Hello, Grace" then "Hello". Notifications: off gives no notice, on gives a notice at 18:06:35. 101-char name → error with nothing saved |
| R18 | frontend-dev phase-09 Polish C1–C9 | pass | `ui-R18-R19.json`. C1 (shot-90..93): fresh DB, four empty lists with `empty-state` + action, six nav texts/hrefs, H1 = label. C2 (shot-94..97): all Tasks selectors, `task-status` only when editing, `error-title` "must not be blank", `notice` "Task completed", `task-restore-2` with `filter-archived`, overdue view = API [3], `?add=1`. C3 (shot-98): all Habits selectors, activate/deactivate shown by state, toggle disabled when inactive, `notice-error` "Habit 1 is already complete for 2026-09-26". C4 (shot-99/100): Learning grid, details with milestone/note rows and inputs. C5 (shot-101..103): `plans-active`/`plans-completed`, `plan-rest-time` only on In Progress ("1:59:59"), `plan-item-removed-1`, `error-items`, `plan-started-3` plus notice at 21:49:43, the start instant. C6 (shot-104): 23 Dashboard selectors, and lists and figures = `GET /api/dashboard` (40%, due today [6], overdue [3], completed today [7,1], habits 1/2, plans 1 and 3, learning 2/0/0 0/1). C7 (shot-105): the six Settings selectors, with the time zone as read-only text. C8 (shot-106..111): six nav clicks from `/settings`, each with the right URL and `page-title`, 0 console errors during the run. C9: `file-checks.txt`: 4200 → 200, proxied app-info → 200 (steps 1–2 not run: they write under `frontend/`) |
| R19 | frontend-dev phase-polish-1 C1–C3 | pass | `file-checks.txt` R19: C1 no `app.spec.ts` and no `*.spec.ts` under `frontend/src`. C2 `server-frontend.log` of this app run shows "Application bundle generation complete" with 0 `error TS` / `✘ [ERROR]`. C3 is the same steps as R18 C8 (shot-106..111) |
| R20 | unit tests: 0 failed, domain line coverage ≥ 80% | pass | `unit/backend/unit-result.json`: 338 passed, 0 failed/errors/skipped. 30 surefire XMLs, none with failures or errors. `unit/backend/coverage/jacoco.csv` `com.quickflow.domain.*`: 676/695 lines = 97.3% ≥ 80% |

Notes (recorded, not filed):
- R18 C6: the greeting read "Hello, GG", and `GET /api/settings` returned `displayName` "GG". This session never
  wrote settings, so something outside the session set it after the 21:46 fresh start. The greeting matching the API
  value is what the contract asks for.
- R18 C5: at the instant `plan-started-3` appeared, the card still read "Not Started", and it read "In Progress" a few
  seconds later without a reload. test-phase-07 saw the same label lag. This is not a criterion of this check.
- `unit-result.json` says "coverage skipped: no classes in com/quickflow/domain". The runner's exact-package match
  misses the `domain.*` sub-packages, as noted in every earlier report. `jacoco.csv` has them.
- R9 C5 (restart persistence) is not run by design: the testing loop never starts or stops processes.

Console: the only errors in session 3 are the deliberate 400 (C2 empty save), 400 (C3 empty save), 409 (C3 stale
toggle) and 400 (C5 empty items). There are none during the C8 smoke run (`R18-console-errors.txt`).

## Unit tests
From `unit/backend/unit-result.json` and `unit/frontend/unit-result.json`:
- backend: 338 passed, 0 failed, 0 errors, 0 skipped, exit code 0, outcome pass. Domain line coverage 676/695 = 97.3%
  from `jacoco.csv` (threshold 0.80).
- frontend: outcome `none` (no unit test command configured).

## Requirement coverage
| id | unit tests (`@DisplayName`, `r2-ids-tests.txt`) | checks |
|---|---|---|
| BR-1, BR-2, BR-3, BR-4, BR-5, BR-14 (tasks) | yes | R3, R12, R18 C2 |
| BR-6, BR-7 (habits) | yes | R4, R13, R18 C3 |
| BR-8, BR-9 (learning) | yes | R5, R14, R18 C4 |
| BR-10, BR-11, BR-12, BR-13 (plans) | yes | R6, R15, R18 C5 |
| FR-01.1–01.7 (tasks) | yes | R3, R12 |
| FR-02.1–02.3 (search, filter, sort) | yes | R3 C9/C10, R12 C11 |
| FR-03.1–03.3 (overdue, archive) | yes | R3 C7/C11, R12, R18 C2 |
| FR-04.1–04.5 (habits, streaks, undo) | yes | R4, R13 |
| FR-05.1–05.2, FR-06.1–06.3 (cards, milestones, notes) | yes | R5, R14 |
| FR-07.1–07.6 (plans, status, rest time, removed source) | yes | R6, R15, R18 C5 |
| FR-08.1 (plan-start notice) | none (UI-only, allowed exception) | R15 C8, R17 C6, R18 C5 |
| FR-08.2–08.5 (dashboard) | yes | R7, R16, R18 C6 |
| FR-09.1–09.2 | yes | R7, R16 |
| FR-10.1 (navigation) | none (UI-only, allowed exception) | R11, R16 C9, R17 C8, R18 C8, R19 C3 |
| FR-10.2 (settings) | yes | R8, R17, R18 C7 |
| NFR-1 (indexes) | SchemaIndexesTest | R9 C4 |

All 52 BR/FR ids in `specs/001-quickflow-backend/spec.md` are covered by at least one check. All but FR-08.1 and
FR-10.1 are also covered by a unit test.

## Questions
None.
