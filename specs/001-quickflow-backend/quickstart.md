# Quickstart: QuickFlow backend

Commands run from the repo root. Verification of each phase is done by the testing loop (curl against
the running API), not by the dev loop.

## Prerequisites
- JDK 25 (full JDK with `javac`), network access for the first Maven resolve.
- `backend/` scaffolded by the runner (see `project.config.yaml` → `layers.backend.scaffold`).

## Build and unit tests
- Build: `cd backend && ./mvnw -q -DskipTests package`
- Tests + coverage + OpenAPI: `cd backend && ./mvnw -q verify`
  - Surefire reports: `backend/target/surefire-reports/TEST-*.xml`
  - JaCoCo: `backend/target/site/jacoco/jacoco.xml` (line coverage ≥ 80% on `com.quickflow.domain`)
  - Generated swagger: `backend/target/openapi.json` (copied to `loops/backend-dev/outputs/openapi.json`).
    During `verify`, `spring-boot-maven-plugin` starts the app with profile `openapi` (port 18080, in-memory
    H2 `openapi`, `ddl-auto=create-drop`), `springdoc-openapi-maven-plugin` reads
    `http://localhost:18080/v3/api-docs`, then the app is stopped. Its known differences from
    `contracts/openapi.yaml` are listed in [research.md](research.md) (R-contract).

## Run
- Default profile (file DB `backend/data/quickflow`): `cd backend && ./mvnw spring-boot:run`
- Test profile (file DB `backend/data/test`, wiped by the runner): `cd backend && ./mvnw -q spring-boot:run -Dspring-boot.run.profiles=test`
- Ready when `http://localhost:8080/v3/api-docs` answers; Swagger UI at `http://localhost:8080/swagger-ui`.

## Validation scenarios (what the testing loop checks; see [contracts/openapi.yaml](contracts/openapi.yaml))
1. **Info**: `GET /api/app-info` → `timeZone` = `Africa/Cairo`.
2. **Tasks (US1)**: create → 201 with `status TODO`, `archived false`; empty or 201-char title → 400 with
   `errors[].field = title`; complete → `DONE`; archive → gone from `GET /api/tasks`, present with
   `?archived=true`; restore; `q`, `status`, `priority`, `dueFrom/dueTo`, `sort` filter correctly; a task due
   yesterday appears in `/api/tasks/overdue`; delete → 204 then 404.
3. **Habits (US2)**: create DAILY and WEEKLY; `POST /completions` → 201, again for the same date → 409;
   `completedToday`/`currentStreak` reflect it; deactivate → excluded from `?active=true`, completing → 409;
   delete → 404 afterwards.
4. **Learning (US3)**: create card (empty title → 400); add milestones and notes; mark a milestone done;
   remove a note; delete the card → 404 for card, milestones and notes.
5. **Plans (US4)**: create from one task, habit and card with end after start → 201, `progressPercent 0`;
   no items or unknown source → 400; end ≤ start → 400 (`errors[].field = endDateTime`); tick an item →
   `progressPercent` changes and (TASK item) the task is `DONE`; a future plan is `NOT_STARTED` with
   `restSeconds null`; a running plan is `IN_PROGRESS` with `restSeconds` ≈ end − now; delete → 204.
6. **Dashboard (US5)**: numbers and lists match the data created above.
7. **Settings (US6)**: `PUT /api/settings` then `GET` returns the saved values after a restart.
