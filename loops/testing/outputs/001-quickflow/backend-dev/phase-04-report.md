# phase-04-report: US2 Track recurring habits (backend-dev), attempt 1

Plan: `phase-04-test.md` (new on this attempt). Evidence folder:
`loops/testing/runs/001-quickflow/backend-dev/phase-04/attempt-1/`.

Note: `phase-04-verify.sh` could not be executed (the session's permission check asked for
approval, as in phases 01–03), so its checks were run as direct curl calls against the same URLs
with the same inputs. Each response (status line, headers, body) is saved as `http/<name>.http`;
`curl.log` lists every request's status line in order (labelled by URL fragment `#C<n>-...`),
the C1 transcript and the habit ids. Long request bodies are `req/*.json` (sizes in `curl.log`).
Today (app time zone, `GET /api/app-info`) was 2026-09-24, a Thursday, for the whole run
(22:22–22:27 +03:00); the current Monday–Sunday week starts 2026-09-21.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US2-1, FR-03.1, FR-03.2 | pass | `curl.log` C1 block: `POST /api/habits` → 201 `application/json`, `active true`, `createdAt 2026-09-24T22:23:00.495642872+03:00`, name/description/`DAILY` echoed; `#C1-get` → 200 same values (createdAt `.495643`, same instant); `#C1-weekly` → 201 `WEEKLY`, `description null`, `active true` |
| C2 | AC-US2-2, BR-6, FR-03.3 | pass | `http/C2-empty`, `C2-blank`, `C2-missing`, `C2-151`, `C2-put-empty`, `C2-put-151` → each 400 `application/problem+json`, `errors[{field:"name", message:"must not be blank" / "size must be between 1 and 150"}]`; `http/C2-150` → 201 id 3 (request 181 bytes = 150-char name); `C2-get1-unchanged` habit 1 unchanged |
| C3 | FR-03.1, FR-03.3 | pass | `http/C3-2001` → 400 problem `errors[{field:"description"}]`; `C3-2000` → 201 id 4; `C3-nofreq` → 400 `{field:"frequency", "must not be null"}`; `C3-monthly` → 400 `{field:"frequency", "must be one of [DAILY, WEEKLY]"}` |
| C4 | AC-US2-3, FR-04.1 | pass | `http/C4-nobody` (no body), `C4-empty` (`{}`), `C4-dated` (`{"date":"2026-09-24"}`) → 201 `HabitCompletion` `completionDate 2026-09-24`, `habitId` 5/6/7, `id`, `createdAt`; `C4-list5/6/7` → exactly one record each, for today |
| C5 | AC-US2-4, BR-7, FR-04.2 | pass | `http/C5-1` → 201; `C5-2` (no body) and `C5-3` (dated today) → 409 `application/problem+json` "Habit 8 is already complete for 2026-09-24"; `C5-y1` → 201, `C5-y2` → 409 "…already complete for 2026-09-23"; `C5-list` → exactly 2 records (09-24, 09-23), newest first |
| C6 | FR-04.1 | pass | `http/C6-future` `{"date":"2026-09-25"}` → 400 problem `errors[{field:"date", "must not be in the future"}]`; `C6-list` → `[]` |
| C7 | AC-US2-5, FR-04.5 DAILY | pass | fresh (`http/C7-new`): `completedToday false`, `doneForCurrentPeriod false`, `currentStreak 0`; after 09-23 + 09-22 (`C7-g1`): false, false, **2** (streak from the previous day); after today (`C7-g2`): true, true, **3**; gap habit 09-23 + 09-21 (`C7-g3`): false, false, **1**; `frequency DAILY` shown each time; `C7-list` carries the same progress |
| C8 | AC-US2-5, FR-04.5 WEEKLY, A-12 | pass | habit 12 completed 09-21, 09-14, 09-07 (Mondays) → `http/C8-g`: `doneForCurrentPeriod true`, `currentStreak 3`, `frequency WEEKLY`; habit 13 completed only Sunday 09-20 → `C8b-g`: `doneForCurrentPeriod false` (Sunday is in last week), `currentStreak 1`, `completedToday false` |
| C9 | AC-US2-6, FR-03.2 | pass | `http/C9-put` → 200 name "C9 edited H1", description "new desc", `WEEKLY`; `C9-get` same values; `createdAt` 22:25:33.470929652 → .47093 (same instant, µs rounding) |
| C10 | AC-US2-7, FR-03.2, FR-04.4 | pass | `http/C10-deact` → 200 `active false`; `C10-act-list` (`active=true`) ids 1–14, 16, 17 (no 15), no `active:false` entry; `C10-inact-list` = [15]; `C10-all` includes 15; `C10-hist` still holds 09-23 ("keeps its history"); `C10-comp` (today) and `C10-comp-past` (09-22) → 409 problem "Habit 15 is inactive"; `C10-hist2` unchanged; `C10-act` → 200 `active true`, streak 1; `C10-act-list2` includes 15; `C10-comp2` → 201 |
| C11 | AC-US2-8, BR-14 | pass | `http/C11-pre` 2 completions; `C11-del` → 204; `C11-get` → 404 `application/problem+json` "Habit 16 not found"; `C11-comps` → 404 problem; habit 16 absent from `C11-list-all`, `C11-list-act`, `C11-list-inact`; PUT / deactivate / activate / complete / undo / DELETE on 16 → 404 each; `C11-unk` (999999) → 404 |
| C12 | FR-04.3 | pass | `http/C12-c` → 201; `C12-undo` `DELETE …/completions/2026-09-24` → 204; `C12-list` → `[]`; `C12-undo2` → 404 problem "Habit 17 has no completion for 2026-09-24" |
| C13 | swagger / contract | pass | `C13-api-docs.json` (live, 200): `listHabits` (`active` boolean query), `createHabit` (201, `HabitWrite` required), `getHabit`, `updateHabit` (200), `deleteHabit` (204) on `/api/habits/{id}`, `deactivateHabit`, `activateHabit`, `listHabitCompletions`, `completeHabit` (201, optional `HabitCompletionCreate` body), `undoHabitCompletion` (204, `date` path `format: date`); schemas `Habit`, `HabitWrite`, `HabitCompletion`, `HabitCompletionCreate`, `HabitFrequency` enum [DAILY, WEEKLY]; `backend/target/openapi.json` and the copy are byte-identical (`cmp`) |

AC-US2-9 is frontend layer: not checked here. "Today's habits" (AC-US2-7) is the dashboard's
view; no endpoint for it exists in this phase.

Observations, not checks (no criterion of this phase covers them):
- As for tasks, the generated swagger lists only success responses; the contract's `400` / `404` /
  `409` responses on the habit operations are not in `openapi.json`. The live API returns them as
  the contract describes.
- An empty name (`""`, `http/C2-empty.http`, `C2-put-empty.http`) gets two `errors[]` entries for
  `name` (`size must be between 1 and 150` and `must not be blank`); both are true and name the
  field. A UI showing every message would show both.
- `servers[0].url` in the generated file is still `http://localhost:18080` (phase-02 observation).

## Unit tests
From `unit/unit-result.json`: passed 126, failed 0, errors 0, skipped 0; exit code 0; outcome **pass**;
coverage: reported as skipped ("no classes in com/quickflow/domain").

The skip is wrong again (same runner exact-package match as phases 02–03). From
`unit/coverage/jacoco.csv`, domain line coverage (`com.quickflow.domain.*`) is **273/282 = 96.8%**
(task 137/142, habit 118/122 = 96.7%, common 18/18), above `min_line` 0.80. The unit rule passes
either way.

## Requirement coverage
| id | unit tests | checks |
|---|---|---|
| FR-03.1 habit fields, description ≤ 2,000 | `HabitTest.fr03_1_description2001CharsRejected`, `fr03_1_nullDescriptionAccepted` | C1, C3 |
| FR-03.2 create/read/update/deactivate/reactivate/delete, new = active | `HabitTest.fr03_2_*` (3), `HabitServiceTest.fr03_2_*` (createSetsCreatedAtFromClock, deactivateAndActivate, deleteRemovesHabit, listActive/listWithoutFilter, unknownHabitNotFound ×8), `HabitRepositoryTest.fr03_2_activeFilter`, `HabitControllerTest` (create, update, deactivate/activate, delete, list, get 404) | C1, C9, C10, C11 |
| FR-03.3 / BR-6 name required ≤ 150, frequency Daily/Weekly | `HabitTest.br6_*` (3), `fr03_3_nullFrequencyRejected`, `HabitControllerTest.br6_blankNameIs400WithNameField`, `unknownFrequencyIs400WithFrequencyField` | C2, C3 |
| FR-04.1 completion default today, future rejected | `HabitServiceTest.fr04_1_*` (3), `HabitControllerTest.completeHabitWithoutBodyIs201`, `completeHabitWithDateIs201` | C4, C6 |
| FR-04.2 / BR-7 one completion per date | `HabitServiceTest.br7_*` (2), `HabitRepositoryTest.br7_*` (2), `HabitControllerTest.br7_duplicateCompletionIs409Problem` | C5 |
| FR-04.3 undo | `HabitServiceTest.fr04_3_*` (3), `HabitControllerTest.undoHabitCompletionIs204`, `listHabitCompletionsNewestFirst` | C12 |
| FR-04.4 only active habits completed | `HabitServiceTest.fr04_4_inactiveHabitCompletionIsConflict` | C10 |
| FR-04.5 progress and streak (Daily, Monday–Sunday week) | `HabitProgressCalculatorTest.fr04_5_*` (13), `HabitServiceTest.fr04_5_getComputesProgressFromCompletionDates`, `HabitRepositoryTest.fr04_5_completionDatesOfOneHabitOnly` | C7, C8 |
| BR-14 deleted habit and completions not returned | `HabitRepositoryTest.br14_deletingHabitDeletesCompletions`, `HabitServiceTest.fr03_2_deleteRemovesHabit` | C11 |

None uncovered.

## Questions
None.
