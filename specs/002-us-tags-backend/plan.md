# Implementation Plan: Task tags

**Branch**: `002-us-tags` | **Date**: 2026-09-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-us-tags-backend/spec.md`

## Summary

Add tags to the existing QuickFlow task (`specs/001-quickflow-backend`, US1). A task gets a set of
0–10 tags (1–30 characters each, case-insensitive). Two new endpoints add tags to a task and remove
one; `GET /api/tasks` gets a `tag` filter; every task read returns `tags`. Rules live in the domain
`Task` entity; tags are stored in a collection table owned by the task, so they are deleted with it.
Nothing else in the task API changes (`PUT /api/tasks/{id}` never touches tags, BR-T4).

## Technical Context

**Language/Version**: Java, JDK 25 (LTS) (constitution I)

**Primary Dependencies**: Spring Boot 4.1.1 (webmvc, data-jpa, validation), springdoc-openapi-starter-webmvc-ui 3.1.1; build plugins already pinned in `backend/pom.xml` (springdoc-openapi-maven-plugin 1.5, jacoco-maven-plugin 0.8.15). No new dependency.

**Storage**: H2 (Boot-managed) through JPA; `spring.jpa.hibernate.ddl-auto=update` (default and test profiles), `create-drop` (openapi profile), so the new `task_tag` table is created by Hibernate: no migration tool in this project.

**Testing**: spring-boot-starter-test, spring-boot-starter-webmvc-test (`@WebMvcTest` + `MockMvcTester`, `@MockitoBean`), spring-boot-starter-data-jpa-test (`@DataJpaTest`); JUnit 5 + AssertJ; fixed `java.time.Clock` in domain tests; JaCoCo ≥ 80% lines on `com.quickflow.domain`.

**Target Platform**: Local single-user web app; backend on http://localhost:8080.

**Project Type**: Web service (REST API under `/api`), the backend layer of a web application.

**Performance Goals**: Add tag, remove tag and filter by tag each under 500 ms (PRD §10, SC-002).

**Constraints**: Domain rules in `com.quickflow.domain` only (constitution IV); RFC 9457 errors with the existing `errors[]` shape; deleted tasks and their tags never returned (BR-14).

**Scale/Scope**: One user, one story, 2 new endpoints, 1 changed list endpoint, 1 changed response schema.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Status |
|---|---|---|
| I. Pinned stack | No new dependency or version; only the pinned stack in `backend/pom.xml`. | PASS |
| II. Independent verification | Dev loop writes no verdicts and runs no curl; the testing loop checks the ACs with curl against `contracts/openapi.yaml`. | PASS |
| III. Backend unit tests first | Every rule BR-T1..BR-T4 and FR-001..FR-008 has a named test (domain, web slice, persistence slice), written before the code in each phase. | PASS |
| IV. Domain separate, time from a clock | Tag rules live in `Task` (domain); controller only maps. `updatedAt` on tag changes comes from `TimeService` (clock). | PASS |
| V. No guessing | 3 open questions (tag spelling, adding an existing tag, removing an absent tag) answered at the phase-00 review and written into spec.md; plan choices that aren't in the spec are listed as assumptions (research.md) for approval. | PASS |
| Technical constraints | REST under `/api`; contract written before code (`contracts/openapi.yaml`); BR-14 deleted rows never returned. | PASS |

**Post-design re-check**: PASS. The design adds no project, dependency or pattern beyond the ones
the 001 backend already uses (entity + service + controller + DTO records + Specification).

## Project Structure

### Documentation (this feature)

```text
specs/002-us-tags-backend/
├── plan.md              # This file
├── research.md          # Phase 0: decisions R-1..R-8
├── data-model.md        # Phase 1: Task.tags, task_tag table, rules
├── quickstart.md        # Phase 1: how to run and check it
├── contracts/
│   └── openapi.yaml     # Phase 1: OpenAPI 3.1, every endpoint of US1 (frontend's swagger input)
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
backend/src/main/java/com/quickflow/
├── domain/task/
│   ├── Task.java                 # changed: tags collection, addTags / removeTag, rules BR-T1..T3
│   ├── TaskQuery.java            # changed: + tag
│   ├── TaskSpecifications.java   # changed: tag filter (case-insensitive, whole tag)
│   └── TaskService.java          # changed: addTags(id, tags), removeTag(id, tag)
└── web/task/
    ├── TaskController.java       # changed: tag query param, POST /{id}/tags, DELETE /{id}/tags
    ├── TaskTagsRequest.java      # new: { tags: [string] }
    └── TaskResponse.java         # changed: + tags

backend/src/test/java/com/quickflow/
├── domain/task/TaskTest.java               # + BR-T1..T4 tests
├── domain/task/TaskServiceTest.java        # + addTags / removeTag / not found
├── persistence/TaskRepositoryTest.java     # + tag filter, persistence, cascade on delete
└── web/task/TaskControllerTest.java        # + endpoints, 400 shape, 404, tag param
```

**Structure Decision**: The existing `backend/` Spring Boot project from `specs/001-quickflow-backend`;
tags are part of the task package (domain and web), no new package.

## Complexity Tracking

No constitution violations.
