# phase-09-report: Polish (backend-dev), attempt 1

Test plan: `phase-09-test.md` (new this attempt, C1–C7). Run against `http://localhost:8080` (fresh test
database) on 2026-09-26 around 11:42 +03:00. Evidence folder:
`loops/testing/runs/001-quickflow/backend-dev/phase-09/attempt-1/`.

**How the checks were run:** `phase-09-verify.sh` was written, but running it with `bash` was blocked by the
session's permission check ("requires approval"), as in phases 05–08. So C1/C2 were run as direct
`curl` / `jq` / `cmp` calls (results in `file-checks.txt`, live swagger in `C1-api-docs.json`). For C3 the
earlier phase scripts couldn't be run either. Instead, every one of the 39 operations was called directly with
literal bodies against the fresh database, comparing results with the behaviour the phase-03..08 reports
recorded. Each response is in `reg/NN-*.json`. There is no `curl.log` this attempt.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | R2: `/v3/api-docs` operationIds = `contracts/openapi.yaml` | pass | `C1-api-docs.json` (200); `file-checks.txt` §C1: both sorted lists identical, **39** each (the review's "38" is a miscount; the sets match) |
| C2 | R2: `outputs/openapi.json` equals `backend/target/openapi.json` | pass | `file-checks.txt` §C2: `cmp` identical; same 39 operationIds and 200/201/204 success codes |
| C3 | R1: endpoints of phases 03–08 unchanged | pass | `reg/`: **tasks**: create 201 with the 11 `Task` fields (`02`), overdue flag (`03`), blank title → 400 problem `errors[title]` (`04`), get 200 / 404 (`05`,`06`), default list excludes archived (`07`,`12`), overdue list (`08`), PUT 200 (`09`), complete sets DONE + `completedAt` (`10`), archive/restore (`11`,`13`), delete 204 → 404 (`15`,`16`). **habits**: create 201 (`20`), blank → 400 (`21`), complete 201 then 409 (BR-7, `22`,`23`), completions list (`24`), streak 1 / completedToday (`25`), undo 204 (`26`), PUT (`27`), deactivate/list/activate (`28`–`30`), delete 204 → 404 (`32`,`33`). **learning**: card create 201 (`40`), milestone add/update/delete (`41`,`42`,`48`), note add/delete (`44`,`47`), get with counts 1/2 (`45`), PUT status (`46`), list 1/1 after deletes (`49`), blank → 400 (`50`), delete 204 → 404 (`52`,`53`). **plans**: create IN_PROGRESS 0/3, restSeconds 40641 = 23:00 − 11:42:38 (`60`), end = start → 400 `endDateTime` (BR-11, `61`), missing source → 400 `items[0].sourceId` (BR-10, `62`), tick TASK item → 33% and task 2 DONE (BR-13, `63`,`64`), PUT (`65`), `group=active` (`66`), get (`67`), delete 204 → 404 with task unchanged (`74`–`76`), list empty (`77`). **dashboard**: empty DB zeros (`00`), filled: dueToday/completedToday/100%/habits/activePlans/planCounts/learning consistent with the data (`70`). **settings**: defaults (`01`), PUT + read-back (`71`,`73`), `FOO` → 400 `errors[defaultPage]` (`72`). **app-info** 200 Africa/Cairo +03:00 (`79`) |
| C4 | G1 (NFR-1 indexes), R3 index part | pass | `file-checks.txt` §C4: `SchemaIndexesTest` 4/4 (three `idx_…` indexes + habit-completion unique constraint), 0 failures/errors |
| C5 | R3: restart on an existing file DB | not run | The testing loop never starts or stops processes, and the runner started the app on a freshly deleted test DB. Same handling as phase-08: left to the unit run (`QuickflowApplicationTests` 0 failures; NFR-3 repository tests). Not counted as a pass |
| C6 | G2: domain coverage ≥ 80% | pass | `file-checks.txt` §C6: `jacoco.csv` `com.quickflow.domain.*` 676/695 = **97.3%**; `PlanSourceResolver` now fully covered |
| C7 | G3: every BR and backend FR named in a test | pass | `file-checks.txt` §C7: every BR-1..14 and FR id of `spec.md` is in a `@DisplayName`, except the excluded FR-08.1 / FR-10.1; new `fr01_1_taskHasExactlyContractFields` and 4 `PlanSourceResolverTest` tests pass |

Plan note: C7's steps were corrected before the run. Surefire XML holds method names, not display names, so the
check reads the `@DisplayName` strings in `backend/src/test/java`. The criteria didn't change.

## Unit tests
From `unit/unit-result.json`: passed 338, failed 0, errors 0, skipped 0; exit code 0; outcome **pass**.
Coverage was reported as skipped ("no classes in com/quickflow/domain"). As in phases 02–08, the runner
matches only the exact package `com/quickflow/domain` and ignores sub-packages. From `unit/coverage/jacoco.csv`
the domain line coverage is 676/695 = 97.3% ≥ `min_line` 0.80, so the unit rule passes either way.

## Requirement coverage
Polish phase: `story_id: none`, no FR/BR of its own. What it changes or claims:

| rule | unit tests | checks |
|---|---|---|
| NFR-1 indexes (`task(archived,due_date)`, `task(status)`, `plan_item(plan_id)`) | `SchemaIndexesTest` nfr1_* (3) | C4 |
| BR-7 unique `habit_completion(habit_id, completion_date)` | `SchemaIndexesTest` br7_* ; earlier BR-7 tests | C4, C3 (`23` 409) |
| BR-10 source resolution | `PlanSourceResolverTest` br10_* (4) | C3 (`60`, `62`) |
| FR-01.1 task fields | `TaskControllerTest` fr01_1_taskHasExactlyContractFields | C3 (`02`: exactly the 11 fields) |
| FR-01.5, 01.6, 04.2, 07.4, 07.5, 08.2 (names added to existing tests) | tests named in `file-checks.txt` §C7 | C3 (`04`, `07`/`12`, `23`, `61`, `63`/`64`, `60` restSeconds) |
| Contract match (T083) | none (build-time diff) | C1, C2 |
| NFR-3 persistence across restart | `QuickflowApplicationTests`, NFR-3 repository tests | C5 not run (no restart by the testing loop) |

Everything is covered by a unit test or a check. NFR-3 restart has unit tests only.

## Questions
None.
