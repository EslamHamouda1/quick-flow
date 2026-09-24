# Implementation Plan: QuickFlow (backend layer)

**Branch**: `001-quickflow` | **Date**: 2026-09-24 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-quickflow-backend/spec.md`

**Layer**: `backend` (`backend/`, loop `backend-dev`). The frontend layer (`frontend/`, `contract_from: backend`)
plans against [contracts/openapi.yaml](contracts/openapi.yaml), which this plan writes.

## Summary

A single-user REST API under `/api` for tasks, habits (with per-date completions), learning cards (with
milestones and notes), Todo Plans (items referencing tasks/habits/cards), a computed Dashboard summary,
Settings and app info (time zone + server time). All business rules live in `com.quickflow.domain`;
every time-dependent value (overdue, due today, completed today, habit period/streak, plan status,
rest time, progress) is computed on read from stored values and an injected `java.time.Clock` in the
app time zone (`Africa/Cairo`). Persistence is JPA on H2 files (default/test profiles) and in-memory
H2 for the `openapi` profile that generates `target/openapi.json` during `./mvnw verify`.

## Technical Context

**Language/Version**: Java, JDK 25 (LTS)

**Primary Dependencies**: Spring Boot 4.1.1 (scaffolded by the runner from start.spring.io with `web, data-jpa, h2, validation`),
springdoc-openapi-starter-webmvc-ui 3.1.1, springdoc-openapi-maven-plugin and jacoco-maven-plugin
(latest release at setup, pinned in `pom.xml`: exact versions are Open question Q4 in the phase-00 review)

**Storage**: H2, Boot-managed version. Profiles: default `jdbc:h2:file:./data/quickflow`; `test` `jdbc:h2:file:./data/test`;
`openapi` in-memory, server port 18080. Schema by Hibernate `ddl-auto=update` (default/test) and `create-drop` (openapi)

**Testing**: spring-boot-starter-test, spring-boot-starter-webmvc-test, spring-boot-starter-data-jpa-test; JUnit 5 + AssertJ
domain tests with a fixed `Clock`; `@WebMvcTest` + `MockMvcTester` + `@MockitoBean`; `@DataJpaTest` on H2; JaCoCo line
coverage ≥ 80% on `com.quickflow.domain`

**Target Platform**: Local JVM process on the user's machine (`./mvnw spring-boot:run`, port 8080)

**Project Type**: Web service (backend of a web application; the Angular frontend is a separate layer)

**Performance Goals**: every create/update/delete/filter request < 500 ms for one user's data (NFR-1)

**Constraints**: all time from one `Clock` bean in `app.timezone`; no background jobs; no CORS (frontend proxies `/api`);
deleted rows never returned (BR-14); no authentication (single user, spec A-1)

**Scale/Scope**: one user, hundreds to a few thousand rows per entity; no pagination

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|---|---|---|
| I. Pinned Stack | Only versions from the stack table; the two "latest at setup" plugins are looked up once and pinned (Q4) | PASS (pending Q4 answer for the two plugin versions) |
| II. Independent Verification | Dev loop writes no verdicts, runs no curl/Playwright; the testing loop checks every phase | PASS |
| III. Backend Unit Tests First | tasks.md puts tests before code in every story phase; one named test per BR-1..BR-14 and per FR; three test kinds; JaCoCo ≥ 80% on `com.quickflow.domain` enforced by `jacoco:check` in `verify` | PASS |
| IV. Domain Separate, Time from a Clock | Rules in `com.quickflow.domain`; controllers map DTOs only; one `Clock` bean in `app.timezone`; status/rest time/overdue/streak computed on read; `/api/app-info` exposes the zone | PASS |
| V. No Guessing | Three spec clarifications (BR-13 scope, plan Completed rule, Settings content) and plugin versions are open questions; design defaults follow the recommended answers and are isolated in one class each | PASS |
| Technical Constraints | `/api`, JPA+H2, three profiles, springdoc at `/swagger-ui` and `/v3/api-docs`, `target/openapi.json` via `verify`, `contracts/openapi.yaml` written before code | PASS |

Post-design re-check (after Phase 1): PASS, no violations; Complexity Tracking not needed.

## Project Structure

### Documentation (this feature)

```text
specs/001-quickflow-backend/
├── plan.md              # This file
├── research.md          # Phase 0 decisions
├── data-model.md        # Phase 1 entities, rules, computed values
├── quickstart.md        # Phase 1 run/validation guide
├── contracts/
│   └── openapi.yaml     # OpenAPI 3.1 contract (frontend's swagger input for planning)
├── checklists/
│   └── requirements.md
└── tasks.md             # /speckit-tasks output
```

### Source Code (repository root)

```text
backend/                                   # scaffolded by the runner (start.spring.io) before the first implement
├── pom.xml                                # + springdoc starter, test starters, jacoco, springdoc + boot start/stop plugins
├── src/main/java/com/quickflow/
│   ├── QuickflowApplication.java
│   ├── config/                            # ClockConfig, AppProperties (app.timezone), OpenApiConfig
│   ├── domain/                            # all business rules (coverage package)
│   │   ├── common/                        # NotFoundException, ConflictException, ValidationException, TimeService
│   │   ├── task/                          # Task, TaskStatus, TaskPriority, TaskRepository, TaskQuery, TaskService
│   │   ├── habit/                         # Habit, HabitFrequency, HabitCompletion, repositories, HabitProgressCalculator, HabitService
│   │   ├── learning/                      # LearningCard, LearningStatus, LearningMilestone, LearningNote, repositories, LearningService
│   │   ├── plan/                          # Plan, PlanItem, PlanSourceType, PlanStatus, PlanStatusPolicy, PlanProgress, PlanRepository, PlanService
│   │   ├── dashboard/                     # DashboardService, DashboardSummary
│   │   └── settings/                      # AppSettings, DefaultPage, SettingsRepository, SettingsService
│   └── web/                               # controllers + request/response records + ApiExceptionHandler (ProblemDetail)
│       ├── task/  habit/  learning/  plan/  dashboard/  settings/  info/
│       └── ApiExceptionHandler.java
├── src/main/resources/
│   ├── application.properties             # default profile + app.timezone + springdoc paths
│   ├── application-test.properties
│   └── application-openapi.properties
└── src/test/java/com/quickflow/
    ├── domain/…                           # plain JUnit + AssertJ, fixed Clock, Mockito for repositories
    ├── web/…                              # @WebMvcTest + MockMvcTester + @MockitoBean
    └── persistence/…                      # @DataJpaTest
```

**Structure Decision**: Web application, backend layer only (`backend/`). Domain logic and JPA
repositories live under `com.quickflow.domain.<area>`; HTTP mapping lives under `com.quickflow.web.<area>`.
Domain services depend on repositories and `Clock` only; controllers depend on domain services only.

## Design highlights

- **Clarification-sensitive code is isolated**: `PlanStatusPolicy` (FR-08.4 / Q2), `PlanService.setItemDone`
  propagation (FR-07.5 / BR-13 / Q1) and the `settings` package (FR-10.2 / Q3) are the only places the
  open answers touch. Defaults follow the recommended answers.
- **Deletes are hard deletes** (BR-14 holds by construction); habit completions and card milestones/notes
  cascade. Plan items have no foreign key to their source (polymorphic); they keep a `sourceTitle`
  snapshot so an item whose source was deleted is shown as removed (FR-07.6).
- **Time**: `ClockConfig` exposes `Clock.system(ZoneId.of(app.timezone))`; tests replace it with
  `Clock.fixed`. Instants are stored in UTC (`hibernate.jdbc.time_zone=UTC`) and rendered as
  `OffsetDateTime` in the app zone.
- **Errors**: RFC 9457 `ProblemDetail` (`application/problem+json`) with an `errors` array of
  `{field, message}` for validation failures (400), 404 for unknown ids, 409 for conflicts
  (duplicate habit completion, completing an inactive habit).
- **OpenAPI**: springdoc serves `/v3/api-docs` and `/swagger-ui`; during `./mvnw verify` the
  spring-boot-maven-plugin starts the app with profile `openapi` (port 18080) around
  `integration-test`, and springdoc-openapi-maven-plugin writes `target/openapi.json`. The generated
  document must match `contracts/openapi.yaml` (paths, operationIds, schemas).

## Complexity Tracking

Not needed: no constitution violations.
