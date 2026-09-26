# phase-09-test: Polish (backend-dev)

Source of the checks: `loops/backend-dev/outputs/001-quickflow/phase-09.md` has an empty
`## Acceptance criteria` section (Polish phase, `story_id: none`). The approved review file
`phase-09-review.md` lists what the testing loop can observe after this phase; each statement is one
criterion, plus the phase goal's two file-level items (coverage, rule/FR naming). Criteria listed
(compare on later attempts):

- R1: "Every endpoint of phases 03–08 behaves exactly as before (regression): same paths, status codes and bodies."
- R2: "`/v3/api-docs` lists the same 38 operationIds as `contracts/openapi.yaml` [...], and `loops/backend-dev/outputs/openapi.json` equals `backend/target/openapi.json`."
- R3: "After a restart on the existing file database, the app still starts (Hibernate `ddl-auto=update` adds the new indexes) and the data created before is still there (NFR-3)."
- G1 (goal): "NFR-1 indexes" exist (`task(archived, due_date)`, `task(status)`, `plan_item(plan_id)`; `habit_completion(habit_id, completion_date)` via the unique constraint).
- G2 (goal): "domain coverage ≥ 80%".
- G3 (goal): "every rule and backend FR has a named test" (BR-1..BR-14, all FR-01.x..FR-10.x except FR-08.1 and FR-10.1).

| check | criterion | steps | expected |
|---|---|---|---|
| C1 | R2 | `curl GET {base_url}/v3/api-docs`; list `paths.*.*.operationId`; list `operationId:` in `specs/001-quickflow-backend/contracts/openapi.yaml` | both sets identical, 38 each |
| C2 | R2 | compare `loops/backend-dev/outputs/openapi.json` with `backend/target/openapi.json` (written by the runner's `./mvnw verify`) | byte-identical; its operationId set equals C1's |
| C3 | R1 | re-run the earlier phase checks unchanged against the running app, one sub-folder each: `phase-08`, `phase-07` (both expect an empty database, so first), then `phase-03`..`phase-06` | every check `PASS` as in its earlier verdict; a `FAIL` caused only by data left by another phase's run (not by the app) is reported as such |
| C4 | G1, R3 (index part) | runner's unit run: `unit/surefire/TEST-com.quickflow.persistence.SchemaIndexesTest.xml` | present, tests for the three indexes and the habit-completion unique constraint, 0 failures / errors |
| C5 | R3 | the testing loop never restarts processes | not run here: restart persistence left to the unit tests (`QuickflowApplicationTests` on the file DB, NFR-3 repository tests), as in phase-08 |
| C6 | G2 | `unit/coverage/jacoco.csv`, package `com.quickflow.domain*` | line coverage ≥ 0.80 (`min_line`) |
| C7 | G3 | ids named in `@DisplayName` strings under `backend/src/test/java` (surefire XML holds method names only, not display names), compared with the ids in `spec.md`; plus the new tests' surefire results | every BR-1..BR-14 and every FR-01.x..FR-10.x id of `specs/001-quickflow-backend/spec.md` except FR-08.1 / FR-10.1 appears at least once |

C1–C3 run in `phase-09-verify.sh`; C4, C6, C7 are file checks saved to `<run_dir>/file-checks.txt`.
