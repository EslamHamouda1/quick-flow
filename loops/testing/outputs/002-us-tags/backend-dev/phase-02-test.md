# phase-02-test: Polish & Cross-Cutting Concerns (backend-dev)

Layer: backend (API, `base_url` http://localhost:8080). API checks run by `phase-02-verify.sh <attempt-folder>`
(curl + jq; raw requests/responses in `<attempt-folder>/curl.log`, live swagger saved as
`<attempt-folder>/api-docs.json`). Unit and coverage checks read the runner's `unit/` folder.

Criteria this plan was written against (from `loops/backend-dev/outputs/002-us-tags/phase-02.md`):
the phase has **no acceptance criteria of its own** (`## Acceptance criteria` is empty). The checks come from
its `## Goal` ("The generated swagger matches the tags contract, and the whole backend test suite and the
coverage gate still pass"), tasks T011/T012, the review's `## Planned checks`, and the review's statement
that the phase "protects the US1 criteria already verified in phase-01 (AC-US1-1 … AC-US1-6) and the
contract the frontend layer builds its client from". Contract: `specs/002-us-tags-backend/contracts/openapi.yaml`.

| check | source | steps | expected |
|---|---|---|---|
| C1 | Goal + T011 | GET /v3/api-docs; compare with the contract | `listTasks` has query `tag`, required false, schema string; `addTaskTags` = POST /api/tasks/{id}/tags, path `id` int64 required, body required `$ref TaskTags`, 200 → `Task`; `removeTaskTag` = DELETE /api/tasks/{id}/tags, path `id` int64, query `tag` required, string minLength 1, 200 → `Task`; schema `TaskTags` exists, required [tags], `tags` array of string, minItems 1, no maxItems; `Task.required` contains `tags`, `Task.tags` array, maxItems 10, uniqueItems true, items string minLength 1 maxLength 30 |
| C2 | review "copied to the app-wide `loops/backend-dev/outputs/openapi.json`" | compare `paths` and `components` of the copy with the live swagger | identical (servers url may differ: openapi profile) |
| C3 | review "protects AC-US1-1 … AC-US1-6" | run `phase-01-verify.sh` (phase-01 C1–C6, unchanged) | PASS C1..C6 |
| C4 | T011 fix `@NotNull @Size(min=1)` → `@NotEmpty` ("same accept/reject outcome: null or empty list -> 400, field `tags`"); contract `TaskTags.required [tags]`, `minItems: 1`, 400/404 responses, `tag` required on removeTaskTag | POST `{"tags":[]}`, `{"tags":null}`, `{}`; GET the task; POST/DELETE on an unknown id; DELETE without `tag` | each invalid body → 400 with an `errors[]` entry on field `tags`, tags unchanged; unknown id → 404 on POST and DELETE; DELETE without `tag` → 400 |
| C5 | contract info: "every endpoint that returns a Task (including the dashboard) now also returns its `tags`" | create a task due today, tag it; GET /api/dashboard | the task's entry in `dueToday` has `tags` = the task's tags |
| C6 | Goal + T012 "all existing tests still pass" | read `unit/unit-result.json` | failed 0, errors 0 |
| C7 | Goal + T012 "JaCoCo line coverage on `com.quickflow.domain` stays ≥ 80% (Constitution III)" | sum LINE covered/missed over `com.quickflow.domain*` packages in `unit/coverage/jacoco.csv` | ≥ 0.80 |
