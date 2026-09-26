# phase-02: Polish & Cross-Cutting Concerns
layer: backend
story_id: none
spec_phase: 2
depends_on: [phase-01]
## Goal
The generated swagger matches the tags contract, and the whole backend test suite and the coverage gate still pass.
## Acceptance criteria
## Tasks
- [x] T011 Compare the generated backend/target/openapi.json with specs/002-us-tags-backend/contracts/openapi.yaml for `listTasks` (`tag` parameter), `addTaskTags`, `removeTaskTag`, schemas `TaskTags` and `Task.tags` (required, array of string); fix the `@Schema`/`@Parameter` annotations in backend/src/main/java/com/quickflow/web/task/ where they differ
- [x] T012 Run `cd backend && ./mvnw -q verify`: all existing tests (dashboard, plans, tasks) still pass with the eager `tags` collection, and JaCoCo line coverage on `com.quickflow.domain` stays ≥ 80% (Constitution III); save the output to loops/backend-dev/runs/002-us-tags/phase-02-build.log
