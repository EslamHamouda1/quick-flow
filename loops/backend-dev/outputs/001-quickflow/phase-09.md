# phase-09: Polish
layer: backend
story_id: none
spec_phase: 9
depends_on: [phase-03, phase-04, phase-05, phase-06, phase-07, phase-08]
## Goal
Generated swagger matches the contract, NFR-1 indexes, domain coverage ≥ 80%, every rule and backend FR has a named test.
## Acceptance criteria
## Tasks
- [x] T083 Compare backend/target/openapi.json (from `./mvnw verify`) with specs/001-quickflow-backend/contracts/openapi.yaml (paths, methods, operationIds, status codes, schema fields) and fix the code (annotations, record fields, method names) where they differ; list any deliberate difference in specs/001-quickflow-backend/research.md
- [x] T084 [P] Add indexes for NFR-1 (`task(archived, due_date)`, `task(status)`, `habit_completion(habit_id, completion_date)` is covered by the unique constraint, `plan_item(plan_id)`) via `@Table(indexes=...)` in backend/src/main/java/com/quickflow/domain/task/Task.java and backend/src/main/java/com/quickflow/domain/plan/PlanItem.java
- [x] T085 Bring line coverage of `com.quickflow.domain` to ≥ 80% (`jacoco:check`) by adding missing domain tests under backend/src/test/java/com/quickflow/domain/
- [x] T086 [P] Check that every BR-1..BR-14 and every backend FR id (all FR-01.x..FR-10.x except the UI-only FR-08.1 notification and FR-10.1 navigation) appears in at least one `@DisplayName` under backend/src/test/java/com/quickflow/ and add the missing tests
- [x] T087 [P] Update specs/001-quickflow-backend/quickstart.md if any command, profile or path changed during implementation
