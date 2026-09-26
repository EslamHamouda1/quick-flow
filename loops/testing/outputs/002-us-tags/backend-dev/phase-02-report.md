# phase-02-report: Polish & Cross-Cutting Concerns (backend-dev), attempt 1

Test plan: `phase-02-test.md` (C1–C7, written this attempt; the phase has no acceptance criteria of its own,
so the checks come from its Goal, T011/T012 and the review's planned checks).
Evidence folder: `loops/testing/runs/002-us-tags/backend-dev/phase-02/attempt-1/` (`RUN`).

`phase-02-verify.sh` was written but running it (`bash <script>`) needed approval in this session, so the same
checks ran as direct commands: one `jq` over the live swagger (C1), `jq -S` + `cmp` (C2), and one `curl --next`
chain of 49 calls on the fresh test DB (C3–C5), each body saved as `RUN/http/NN.json`, status lines in
`RUN/http/status.txt`. There is no `curl.log` for this attempt.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | Goal + T011: swagger matches the tags contract | pass | `RUN/api-docs.json` (live `/v3/api-docs`); jq over listTasks `tag` (query, not required, string), addTaskTags (POST, `id` path int64 required, body required `$ref TaskTags`, 200 → `Task`), removeTaskTag (DELETE, `tag` query required string minLength 1, 200 → `Task`), `TaskTags` (required [tags], array of string, minItems 1, no maxItems), `Task.tags` (in required, maxItems 10, uniqueItems, items 1–30) → `true,true,true,true,true` |
| C2 | copy at `loops/backend-dev/outputs/openapi.json` = generated swagger | pass | `RUN/c2-live.json` vs `RUN/c2-copy.json` (`paths` + `components`, sorted): `cmp` identical |
| C3 | phase-01 C1–C6 (AC-US1-1..6) still hold | pass | C1: 12/13 `[urgent,work]`, 14 task 1 same; C2: 19 `tag=work` ids [1,2,3] (task 1 is tagged work too; 4 home, 5 none, 6 workshop out), 20 `[]`; C3: 22/23 `[home]`, 24 task 7 not listed; C4: 26/27 400 `tags` "tag must not be blank", 28/29 400 `tags` "must be at most 30 characters", 30 still `[alpha]` (no beta), 31 30-char → 200; C5: 33/34 `[work]`, 35 `tag=WORK` lists 9; C6: 36 ten tags 200, 37 400 `tags` "must have at most 10 tags", 38 ten kept |
| C4 | T011 `@NotEmpty` change keeps null/empty → 400 field `tags`; contract 404 / required `tag` | pass | 40/41/42 (`[]`, `null`, `{}`) 400 `tags` "must not be empty", 43 still `[alpha]`; 44/45 unknown id → 404 on POST and DELETE; 46 DELETE without `tag` → 400 field `tag` |
| C5 | contract: Task-returning endpoints incl. dashboard return `tags` | pass | 47 task 12 due 2026-09-26, 48 tags `[board,dash]`; 49 `/api/dashboard` `dueToday` entry for 12 has `tags` `[board,dash]` |
| C6 | T012: all existing tests still pass | pass | `RUN/unit/unit-result.json`: 386 passed, 0 failed, 0 errors, 0 skipped, outcome pass |
| C7 | T012: domain line coverage ≥ 80% (Constitution III) | pass | `RUN/unit/coverage/jacoco.csv`, LINE over `com.quickflow.domain.*`: covered 715, missed 19 → 97.4% (learning 126/1, plan 195/9, common 18/0, task 176/5, settings 41/0, dashboard 41/0, habit 118/4) |

Notes:
- The runner reported coverage `skipped` ("no classes in com/quickflow/domain": it matches the package exactly,
  and all domain classes are in sub-packages). C7 reads the same JaCoCo report from `RUN/unit/coverage/`, as in
  earlier phases.
- Differences between the live swagger and the contract that the dev loop listed and didn't fix (build log
  "springdoc presentation, same as the 001 feature's D-1..D-5"): no 400/404 responses declared on the
  operations, 200 description "OK", `servers` url, `TaskTags` `examples`. They aren't counted as failures: the
  400/404 behaviour the contract describes was checked on the running API (C3, C4) and holds.
- C4's messages changed with `@NotEmpty` ("must not be empty"); no spec or test states the message text.

## Unit tests
From `RUN/unit/unit-result.json` (layer backend): passed 386, failed 0, errors 0, skipped 0, exit code 0,
outcome `pass`. Coverage: runner `null` / `coverage_skipped: true` ("no classes in com/quickflow/domain");
computed from `jacoco.csv` 715/734 = 97.4% line on `com.quickflow.domain.*` (≥ 0.80).

## Requirement coverage
This phase adds no FR or business rule; it re-checks the ones of phase-01.

| id | unit tests | checks |
|---|---|---|
| FR-001 add tags | phase-01 rule-named tests (in the 386 passed) | C3 (phase-01 C1), C4 |
| FR-002 tags shown on task and list | phase-01 tests | C3 (C1), C5 |
| FR-003 / BR-T2 case-insensitive, duplicate is a no-op | phase-01 tests | C3 (C5) |
| FR-004 filter by tag | phase-01 tests | C3 (C2, C5) |
| FR-005 remove tag | phase-01 tests | C3 (C3), C4 (404, missing `tag`) |
| FR-006 / BR-T1 1–30 chars, field `tags` | phase-01 tests | C3 (C4), C4 |
| FR-007 / BR-T3 max 10 | phase-01 tests | C3 (C6) |
| FR-008 / BR-T4 kept with the task, deleted with it | phase-01 tests | **none in this phase** (not an AC-US1 check; restart and delete-with-task not re-run here) |
| Constitution III coverage gate | JaCoCo | C7 |
| Contract (T011) | — | C1, C2 |

## Questions
None.
