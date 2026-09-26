# phase-09-review: Polish

## Goal
Close the backend: the generated swagger matches `contracts/openapi.yaml` (differences fixed or listed as
deliberate in `research.md`), the NFR-1 indexes exist, `com.quickflow.domain` line coverage stays ≥ 80%, every
BR and every backend FR id is named in at least one `@DisplayName`, and `quickstart.md` matches what was built.
No new endpoint, no behaviour change.

## Tasks
- [ ] T083 Compare backend/target/openapi.json (from `./mvnw verify`) with specs/001-quickflow-backend/contracts/openapi.yaml (paths, methods, operationIds, status codes, schema fields) and fix the code (annotations, record fields, method names) where they differ; list any deliberate difference in specs/001-quickflow-backend/research.md
- [ ] T084 [P] Add indexes for NFR-1 (`task(archived, due_date)`, `task(status)`, `habit_completion(habit_id, completion_date)` is covered by the unique constraint, `plan_item(plan_id)`) via `@Table(indexes=...)` in backend/src/main/java/com/quickflow/domain/task/Task.java and backend/src/main/java/com/quickflow/domain/plan/PlanItem.java
- [ ] T085 Bring line coverage of `com.quickflow.domain` to ≥ 80% (`jacoco:check`) by adding missing domain tests under backend/src/test/java/com/quickflow/domain/
- [ ] T086 [P] Check that every BR-1..BR-14 and every backend FR id (all FR-01.x..FR-10.x except the UI-only FR-08.1 notification and FR-10.1 navigation) appears in at least one `@DisplayName` under backend/src/test/java/com/quickflow/ and add the missing tests
- [ ] T087 [P] Update specs/001-quickflow-backend/quickstart.md if any command, profile or path changed during implementation

tasks.md `## Phase 9` matches `phase-09.md` (T083–T087, same wording).

Order: T084 and T086 (different files) can run in parallel; T083 after T084 (the indexes don't change the swagger,
but T083 reads a fresh `./mvnw verify` output); T085 after T086 (new tests may already raise coverage); T087 last
(it records whatever T083/T084 changed).

## Acceptance criteria
None in `phase-09.md` (Polish has no story). What the testing loop can observe after this phase
(`http://localhost:8080`, test profile):
- Every endpoint of phases 03–08 behaves exactly as before (regression): same paths, status codes and bodies.
- `/v3/api-docs` lists the same 38 operationIds as `contracts/openapi.yaml` (checked now: both lists are identical:
  `activateHabit … updateTask`, 38 each), and `loops/backend-dev/outputs/openapi.json` equals `backend/target/openapi.json`.
- After a restart on the existing file database, the app still starts (Hibernate `ddl-auto=update` adds the new
  indexes) and the data created before is still there (NFR-3).

## Files
| File | Change |
|---|---|
| `backend/src/main/java/com/quickflow/domain/task/Task.java` | `@Table(name = "task", indexes = { @Index(name = "idx_task_archived_due_date", columnList = "archived, due_date"), @Index(name = "idx_task_status", columnList = "status") })` (T084). Column names checked now: `archived`, `dueDate` → `due_date` (Boot's default snake-case naming, no explicit `@Column(name)`), `status` |
| `backend/src/main/java/com/quickflow/domain/plan/PlanItem.java` | add `indexes = @Index(name = "idx_plan_item_plan_id", columnList = "plan_id")` next to the existing `uniqueConstraints` (T084) |
| `backend/src/main/java/com/quickflow/domain/habit/HabitCompletion.java` | unchanged: `UNIQUE(habit_id, completion_date)` already exists (checked now), per the task text |
| `backend/src/test/java/com/quickflow/persistence/SchemaIndexesTest.java` | new `@DataJpaTest`: reads H2 `INFORMATION_SCHEMA` and asserts the three new indexes exist (T084 check) |
| existing tests under `backend/src/test/java/com/quickflow/` | T086: add the missing FR id to the `@DisplayName` of the test that already proves it (see Planned checks); no assertion changes |
| `backend/src/test/java/com/quickflow/web/task/TaskControllerTest.java` | T086: new test for FR-01.1 (task JSON has exactly the contract `Task` fields) |
| `backend/src/test/java/com/quickflow/domain/plan/PlanSourceResolverTest.java` | new (T085): lowest-covered domain class (4 of 9 lines missed) |
| `backend/src/test/java/com/quickflow/domain/…` | T085: small tests for the other missed domain lines only if the gate needs them (it doesn't today, see below) |
| code touched by T083 | only if a difference is found that isn't on the "known" list below (annotations / record fields / method names) |
| `specs/001-quickflow-backend/research.md` | T083: new section `R-contract: Deliberate differences between the generated swagger and contracts/openapi.yaml` |
| `specs/001-quickflow-backend/quickstart.md` | T087: add how `backend/target/openapi.json` is produced during `verify` (profile `openapi`, port 18080, in-memory DB); everything else in it checked now and still correct |
| `loops/backend-dev/outputs/openapi.json` | re-copied from `backend/target/openapi.json` after `verify` |

Endpoints added or changed: none. No change to `pom.xml`, properties or `ApiExceptionHandler`.

## Planned checks
- **Tests first** (layer has `coverage`): `SchemaIndexesTest` before the `@Table` edits (it fails until T084 is done),
  the FR-01.1 test and `PlanSourceResolverTest` before any code change they might reveal.
- **T083 contract diff**, done on the fresh `backend/target/openapi.json`, per operation: path, method, operationId,
  tags, parameters (name, in, required, schema), requestBody presence/required, success status code and response
  schema; per schema: property set, `required` list, types (incl. `[x, null]`), enums, `$ref`s, min/max constraints.
  Differences already logged by phases 03–08 (build logs), which go to `research.md` as deliberate unless the
  reviewer says otherwise (see A-2):
  - D-1: 400/404/409 responses are not in the generated swagger (they come from `ApiExceptionHandler` at runtime);
  - D-2: integers carry `format: int32`/`int64` (contract: `integer` without format);
  - D-3: `minLength: 0` on optional strings with `@Size(max=…)` (e.g. `HabitWrite.description`, `Settings.displayName`);
  - D-4: `servers` url is the `openapi` profile's `http://localhost:18080`;
  - D-5: `completeHabit` requestBody has no `required` key (OpenAPI default `false` = contract's `required: false`).
  Anything else found is fixed in code (not in the contract), then `verify` is re-run and the diff repeated.
- **T084 technical checks** (before relying on them): that Hibernate 7's `ddl-auto=update` creates missing `@Index`es
  on an existing table (check in the resolved hibernate-core sources / by starting the `SchemaIndexesTest` slice and,
  in `verify`, `QuickflowApplicationTests` on the `data/test` file DB); that H2's `INFORMATION_SCHEMA.INDEXES` /
  `INDEX_COLUMNS` view names are the ones in the resolved H2 jar. If `update` doesn't add indexes to an existing
  table, it becomes a question (no manual migration, no DB deletion by me).
- **T085**: coverage checked now in `backend/target/site/jacoco/jacoco.csv` (phase-08 build): `com.quickflow.domain.*`
  misses 23 lines in total (PlanSourceResolver 4/9, Plan 4, PlanService 4, TaskService 4, HabitService 3, and 1 each
  in LearningService, PlanGroup, TaskSort, Habit); the report total was 870 covered / 30 missed, so the domain
  bundle is well above 0.80 already. T085 adds `PlanSourceResolverTest` and re-checks `jacoco:check` passes.
- **T086**: checked now with grep over `@DisplayName` in `backend/src/test/java`: every BR-1..BR-14 is present; of the
  backend FRs, missing are **FR-01.1, FR-01.5, FR-01.6, FR-04.2, FR-07.4, FR-07.5, FR-08.2** (FR-08.1 and FR-10.1 are
  excluded by the task). Each one's behaviour is already tested under its BR:
  | FR | Proved today by | Change |
  |---|---|---|
  | FR-01.1 (task fields) | nothing asserts the full field set | new `TaskControllerTest` `@DisplayName("FR-01.1: a task is returned with exactly the contract Task fields")` |
  | FR-01.5 (title/description/enum limits) | `TaskTest` BR-1/BR-2/BR-3 | add `FR-01.5` to those display names |
  | FR-01.6 (archived excluded by default) | `TaskRepositoryTest` BR-4 | add `FR-01.6` |
  | FR-04.2 (no second completion) | `HabitServiceTest` / `HabitControllerTest` BR-7 | add `FR-04.2` |
  | FR-07.4 (end after start) | `PlanTest` / `PlanControllerTest` BR-11 | add `FR-07.4` |
  | FR-07.5 (only TASK items propagate, one way) | `PlanServiceTest` BR-13 (5 tests) | add `FR-07.5` |
  | FR-08.2 (rest time only inside the window) | `PlanStatusPolicyTest` BR-12 | add `FR-08.2` |
  After the edit the same grep is repeated and must list no missing id.
- **Unit tests, one per business rule** (named with the rule id), new in this phase:
  - `SchemaIndexesTest`: `nfr1_taskArchivedDueDateIndexExists`, `nfr1_taskStatusIndexExists`,
    `nfr1_planItemPlanIdIndexExists`, `br7_habitCompletionUniqueConstraintExists`.
  - `TaskControllerTest`: `fr01_1_taskHasExactlyContractFields` (`id, title, description, status, priority, dueDate,
    createdAt, updatedAt, completedAt, archived, overdue`: the 11 properties of the contract `Task` schema, checked
    now; nullable ones present as `null`).
  - `PlanSourceResolverTest` (Mockito repositories): `br10_taskSourceTitleResolved`, `br10_habitSourceNameResolved`,
    `br10_learningSourceTitleResolved`, `br10_missingSourceIsEmpty` (one per source type).
- Run `L.build` and `L.test`; save output to `loops/backend-dev/runs/001-quickflow/phase-09-build.log` (surefire
  totals, JaCoCo domain line ratio, the T083 diff result, the T086 grep result).
- Copy `backend/target/openapi.json` to `loops/backend-dev/outputs/openapi.json`.

## Risks
- **Existing file databases** (`backend/data/quickflow`, `data/test`): the new indexes are added by
  `ddl-auto=update` on start (to be checked, see above). Index names are fixed (`idx_…`) so a restart doesn't add
  duplicates.
- **`plan_item(plan_id)` is already the leading column of `UNIQUE(plan_id, source_type, source_id)`**, and H2 also
  indexes foreign-key columns, so the extra index is redundant for reads. The task asks for it explicitly; it's
  harmless on a single-user dataset, so I add it as written (see A-3).
- **NFR-1 (< 500 ms)** can't be proven by a unit test; the indexes are the plan's only NFR-1 measure. The testing loop
  measures response times.
- **Coverage gate reads only package `com/quickflow/domain`** (phase-02–08 finding, `scripts/lib/loopctl.py`): the
  sub-packages may be reported as "skipped" by the runner; the pom's `jacoco:check` covers `com/quickflow/domain/**`
  and passes. I can't change `scripts/`.
- **Display-name edits** touch many test files owned by earlier (closed) phases; only the `@DisplayName` string
  changes, no assertion, so their verdicts stay valid.

## Assumptions
- A-1: For T086, an FR whose behaviour is already asserted by a BR test is satisfied by adding the FR id to that
  test's `@DisplayName` (e.g. `"BR-1, FR-01.5: a blank title is rejected"`); a new test is written only where no test
  asserts the FR (FR-01.1).
- A-2: The five known swagger differences D-1..D-5 are accepted as deliberate and documented in `research.md`, not
  "fixed": D-1 would mean adding `@ApiResponse` annotations to ~38 operations only for documentation (the frontend
  client doesn't use error types); D-2/D-3/D-5 are springdoc's rendering of the same constraint; D-4 is the
  generation profile's port. If the reviewer wants D-1 documented in the swagger, it becomes one more task.
- A-3: `plan_item(plan_id)` gets its own index as the task says, even though the unique constraint's leading column
  already covers it.
- A-4: The contract is never edited in this phase; where the code and the contract differ in a way not on the D-list,
  the code changes.

## Open questions
None.
