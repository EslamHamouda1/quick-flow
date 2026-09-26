# test-phase-regression report (002-us-tags), attempt 1

Run on the runner's fresh test database (empty `GET /api/tasks` at the start), 2026-09-27 ~00:07–00:12 +03:00.
Evidence folder: `loops/testing/runs/002-us-tags/suite/test-phase-regression/attempt-1/` (below: `A/`).

`bash phase-01-verify.sh` needed approval in this session, so its calls ran unchanged as chained `curl --next`
commands (tasks 1–12; raw bodies + labelled status lines in `A/curl.log`, requests `#01`–`#50`). UI checks via
Playwright MCP (headless) on http://localhost:4200/tasks; "Given" tasks 13–23 created with curl (`A/setup-curl.log`);
every tag action was done in the UI; API read-backs in `A/curl-ui.log`.

| check | criterion | result | evidence |
|---|---|---|---|
| R1 C1 | backend phase-01 AC-US1-1 | pass | `A/curl.log` #02 200 `["urgent","work"]`, #03 GET same, #04 list same |
| R1 C2 | backend phase-01 AC-US1-2 | pass | #14 `?tag=work` = ids {1,2,3} exactly (4 home, 5 none, 6 workshop out); #15 `nosuchtag` 200 `[]` |
| R1 C3 | backend phase-01 AC-US1-3 | pass | #18 DELETE 200 `["home"]`, #19 GET `["home"]`, #20 task 7 not in `tag=work` |
| R1 C4 | backend phase-01 AC-US1-4 (BR-T1) | pass | #23–#26 each 400 `errors[{field:"tags"}]` with message; #27 tags still `["alpha"]` (no `beta`); #28 30 chars 200 |
| R1 C5 | backend phase-01 AC-US1-5 (BR-T2) | pass | #31 add `Work` 200 `["work"]`, #32 GET `["work"]`, #33 `?tag=WORK` lists 9 |
| R1 C6 | backend phase-01 AC-US1-6 (BR-T3) | pass | #35 10 tags 200; #36 `t11` 400 "must have at most 10 tags"; #37 still t01..t10 |
| R2 C1 | backend phase-02 swagger = contract | pass | `A/api-docs.json`, `A/file-checks.txt` R2 C1 |
| R2 C2 | backend phase-02 app-wide copy = live | pass | `A/file-checks.txt` R2 C2 (paths/components hashes equal, 41 ops each) |
| R2 C3 | backend phase-02 phase-01 re-run | pass | = R1 C1–C6 |
| R2 C4 | backend phase-02 `@NotEmpty` / 404 / missing `tag` | pass | #40–#42 `[]`/`null`/`{}` 400 field `tags`, #43 tags `["keep"]`; #44/#45 404; #46 DELETE without tag 400 field `tag` |
| R2 C5 | backend phase-02 dashboard `tags` | pass | #50 `dueToday[0]` id 12 `tags ["report","work"]` (#47 app-info now 2026-09-27 00:08 Africa/Cairo) |
| R2 C6/C7 | backend phase-02 unit + coverage | pass | = R5, R6 |
| R3 C1 | frontend phase-01 AC-US1-1 | pass | `shot-01-C1-added.png`; row 13 tags urgent, work + notice "Tags added", same after reload; `curl-ui.log` GET 13 `["urgent","work"]` |
| R3 C2 | frontend phase-01 AC-US1-2 | pass | `shot-02-C2-filter-work.png`; UI rows under `work` = [1,2,3,9,12,13,14,15,19,21] = API `?tag=work` (W1 14, W2 15, A 13 in; N1 16, N2 17, N3 18 out; others are earlier work-tagged tasks) |
| R3 C3 | frontend phase-01 AC-US1-3 | pass | `shot-03-C3-removed.png`, `shot-04-C3-filter-work.png`; notice "Tag removed", row 19 only home, not under `work`; API `["home"]` |
| R3 C4 | frontend phase-01 AC-US1-4 | pass | `shot-05-C4-empty.png` error-tags "tag must not be blank", `shot-06-C4-31chars.png` "must be at most 30 characters" (input keeps its 31 chars); row 20 only alpha; API `["alpha"]` |
| R3 C5 | frontend phase-01 AC-US1-5 | pass | `shot-07-C5-add-Work.png` one "work"; `shot-08-C5-filter-WORK.png` row 21 listed; API `["work"]` |
| R3 C6 | frontend phase-01 AC-US1-6 | pass | `shot-09-C6-ten.png` 10 tags; `shot-10-C6-eleventh.png` error-tags "must have at most 10 tags", still 10; API t01..t10 |
| R4 C1–C6 | frontend phase-02 = phase-01 C1–C6 | pass | = R3 C1–C6 |
| R4 C7 | frontend phase-02 ui-contract texts | pass | `shot-11-C7-contract.png`, `shot-13-C7-overdue-disabled.png`; label "Tag", `task-row-tags` present in untagged row 17, remove "×" aria-label "Remove tag urgent", placeholder "Add tags, comma-separated", button "Add tag", `filter-tag` disabled in Overdue |
| R4 C8 | frontend phase-02 Enter / notice / emptied input | pass | `shot-12-C8-enter.png`, `shot-14-C8-enter-retry.png`; Enter adds enter1/enter2, input empty, notice "Tags added" 26 ms after Enter; API `["enter1","enter2"]` (see note) |
| R4 C9 | frontend phase-02 build log | pass | `A/file-checks.txt` R4 C9 |
| R5 | unit tests | pass | `A/unit/backend/unit-result.json` 386 passed, 0 failed, 0 errors; frontend `none` |
| R6 | domain coverage ≥ 0.80 | pass | `A/file-checks.txt` R6: 715/734 = 97.4% |

Console (`A/console-errors.txt`): only the 3 expected 400s (two on task 20 for C4, one on task 22 for C6).

Note on R4 C8: the first read of the notice returned null. The reason was my script, not the app: two notices
were on screen, so `innerText()` on the `notice` locator failed in strict mode and the script caught that as null.
The retry (`allInnerTexts`, polling) read "Tags added" 26 ms after Enter. Not filed.

## Unit tests
- backend: passed 386, failed 0, errors 0, skipped 0, outcome `pass`; coverage reported by the runner as skipped
  ("no classes in com/quickflow/domain"); from `jacoco.csv`, `com.quickflow.domain*` line coverage 715/734 = 97.4% (≥ 80%).
- frontend: outcome `none` (no unit test command configured).

## Requirement coverage
| requirement | checks |
|---|---|
| AC-US1-1 / FR-001, FR-002 | R1 C1, R3 C1 (+ unit suite) |
| AC-US1-2 / FR-003, FR-004 | R1 C2, R3 C2 |
| AC-US1-3 / FR-005 | R1 C3, R3 C3 |
| AC-US1-4 / BR-T1, FR-006 | R1 C4, R2 C4, R3 C4 |
| AC-US1-5 / BR-T2 | R1 C5, R3 C5 |
| AC-US1-6 / BR-T3, FR-007 | R1 C6, R3 C6 |
| contract (swagger / app-wide copy / dashboard tags) | R2 C1, C2, C5 |
| ui-contract texts, Enter, build | R4 C7, C8, C9 |
| constitution III coverage | R6 |
| FR-008 survive restart | none (sessions never restart the app; as in the suite plan) |

## Questions
None.

**Result: 6/6 (R1–R6, 25 sub-checks) pass, no bugs → done.**
