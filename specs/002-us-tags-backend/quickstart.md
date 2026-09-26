# Quickstart: Task tags

## Run
- Build: `cd backend && ./mvnw -q -DskipTests package`
- Unit tests and the generated swagger: `cd backend && ./mvnw -q verify` (writes
  `backend/target/openapi.json`, JaCoCo report in `backend/target/site/jacoco`)
- App (test profile, used by the testing loop): `cd backend && ./mvnw -q spring-boot:run -Dspring-boot.run.profiles=test`
- API docs: http://localhost:8080/v3/api-docs, Swagger UI: http://localhost:8080/swagger-ui

## Scenarios to check (testing loop; see `contracts/openapi.yaml` for shapes)
| AC | Steps | Expected |
|---|---|---|
| AC-US1-1 | create a task; `POST /api/tasks/{id}/tags` `{"tags":["work","urgent"]}`; `GET /api/tasks/{id}` | 200 both times; `tags` = `["urgent","work"]` |
| AC-US1-2 | tag task A "work", leave task B untagged; `GET /api/tasks?tag=work` | only A |
| AC-US1-3 | `DELETE /api/tasks/{A}/tags?tag=work`; `GET /api/tasks/{A}`; `GET /api/tasks?tag=work` | `tags` without "work"; A not listed |
| AC-US1-4 | `POST .../tags` with `[""]`, `["   "]`, `["a"×31]` | 400, `errors[].field` = `tags`; task's `tags` unchanged |
| AC-US1-5 | task tagged "work": add `["Work"]`; `GET /api/tasks?tag=WORK` | one "work" tag (Q1/Q2); task listed |
| AC-US1-6 | task with 10 tags: add a new one | 400 on `tags`; still 10 tags |
| FR-008 | delete a tagged task; `GET /api/tasks?tag=<its tag>` | not listed |
