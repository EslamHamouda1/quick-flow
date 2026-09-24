# phase-01-review: Setup

## Goal
Build configuration on the runner-scaffolded backend: springdoc and test starters, JaCoCo gate, OpenAPI generation during `verify`, the three profiles.

## Tasks
- [ ] T001 Check the scaffolded `backend/pom.xml` (starter artifact ids, Boot 4.1.1, `java.version` 25) and add `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1` plus test-scoped `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-jpa-test` (Boot-managed versions, keep `spring-boot-starter-test`) in backend/pom.xml (research R-1)
- [ ] T002 Add `jacoco-maven-plugin` 0.8.15 (Q4) in backend/pom.xml with executions `prepare-agent`, `report` (phase `verify`) and `check` (phase `verify`, rule: BUNDLE limited to `com/quickflow/domain/**` classes, LINE COVEREDRATIO minimum 0.80)
- [ ] T003 Add OpenAPI generation in backend/pom.xml: `spring-boot-maven-plugin` executions `start` (pre-integration-test, `<profiles>openapi</profiles>`) and `stop` (post-integration-test); `springdoc-openapi-maven-plugin` 1.5 (Q4), goal `generate` at `integration-test`, `apiDocsUrl` `http://localhost:18080/v3/api-docs`, `outputDir` `${project.build.directory}`, `outputFileName` `openapi.json` (research R-3)
- [ ] T004 [P] backend/src/main/resources/application.properties (default profile, `app.timezone=Africa/Cairo`, springdoc paths)
- [ ] T005 [P] backend/src/main/resources/application-test.properties (`jdbc:h2:file:./data/test`, `ddl-auto=update`)
- [ ] T006 [P] backend/src/main/resources/application-openapi.properties (port 18080, in-memory H2, `ddl-auto=create-drop`)
- [ ] T007 [P] Add `data/` to backend/.gitignore

T001–T003 all edit `backend/pom.xml`, so they run one after another; T004–T007 are independent files and run in parallel.

## Acceptance criteria
None (Setup phase, no user story). What the testing loop can observe after this phase:
- `cd backend && ./mvnw -q -DskipTests package` succeeds with Java 25 / Boot 4.1.1.
- `cd backend && ./mvnw -q verify` runs the JaCoCo `report` + `check` goals and writes `backend/target/openapi.json` (an OpenAPI document with no `/api` paths yet).
- The app starts with the default profile on port 8080 and serves `/v3/api-docs` (the `health` URL in the config) and `/swagger-ui`.
- The `test` profile writes its H2 file under `backend/data/test*` (matches `test_db_glob`).

## Files
| File | Change |
|---|---|
| `backend/pom.xml` | + `springdoc-openapi-starter-webmvc-ui` 3.1.1; + test-scoped `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`; + `jacoco-maven-plugin` 0.8.15 (prepare-agent, report, check); + `spring-boot-maven-plugin` executions `start`/`stop` with profile `openapi`; + `springdoc-openapi-maven-plugin` 1.5 (`generate`) |
| `backend/src/main/resources/application.properties` | rewritten with the T004 keys (the scaffold's file only has `spring.application.name`) |
| `backend/src/main/resources/application-test.properties` | new |
| `backend/src/main/resources/application-openapi.properties` | new |
| `backend/.gitignore` | + `data/` |

No endpoints, no Java classes (the scaffolded `QuickflowApplication` and its context test stay as generated).

## Planned checks
- Before editing: read the scaffolded `pom.xml` and confirm parent `spring-boot-starter-parent` 4.1.1, `java.version` 25, and the exact starter artifact ids start.spring.io generated for `web, data-jpa, h2, validation` (R-1); keep them as generated.
- Confirm the test starter artifact ids `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-jpa-test` resolve under Boot 4.1.1's dependency management (build output; the runner treats "Could not resolve dependencies" as a harness error).
- Confirm the parameter names `apiDocsUrl`, `outputDir`, `outputFileName` and the goal `generate` against the resolved `springdoc-openapi-maven-plugin` 1.5 plugin descriptor (`./mvnw help:describe -Dplugin=org.springdoc:springdoc-openapi-maven-plugin:1.5 -Ddetail`), and the `profiles` parameter of `spring-boot-maven-plugin:start` in Boot 4.1.1 the same way.
- Run `L.build` and `L.test`; save the output to `loops/backend-dev/runs/001-quickflow/phase-01-build.log`.
- If `backend/target/openapi.json` was produced, copy it to `loops/backend-dev/outputs/openapi.json` (`L.openapi_copy`).
- **Unit tests (one per business rule)**: none in this phase. Setup has no business rules; the BR-1..BR-14 tests are written test-first in phases 03–08. The scaffold's context-load test is kept.

## Risks
- **Backend not scaffolded yet**: `backend/` does not exist at review time. The plan says the runner scaffolds it (`L.scaffold`) before the first implement. If it is still missing at implement, the session stops with a question rather than writing a hand-made `pom.xml`.
- **JaCoCo check with no domain classes**: in phase-01 `com/quickflow/domain/**` has no classes. The check should pass or be skipped when nothing matches, but this is only confirmed by running `verify`; if it fails on an empty bundle, the fix is a question (e.g. `haltOnFailure` only once domain code exists), not a lower threshold.
- **`verify` starts a JVM on port 18080**: the `start`/`stop` executions launch the app during `./mvnw verify`. This is the build plugin the constitution names, not a server the session starts by hand. If port 18080 is busy, `verify` fails and the log will show it.
- **springdoc 3.1.1 on Boot 4.1.1 / Jackson 3**: if the starter fails to start the context on Boot 4.1.1, it's a pinned-version conflict and becomes a question (no version change without approval).
- **JaCoCo 0.8.15 and Java 25 class files**: the reviewer checked this in the dry run (Q4); a failure here would show up as an instrumentation error in the build log.

## Assumptions
- A-1: the runner has scaffolded `backend/` with `L.scaffold` before implement starts (plan.md "Source Code").
- A-2: the scaffold's `.gitignore` in `backend/` is kept and `data/` is appended to it (T007); if the scaffold has none, a new `backend/.gitignore` with only `data/` is created.
- A-3: the scaffold's `application.properties` content (`spring.application.name=quickflow`) is replaced by the full T004 list, which includes the same key.
- A-4: `jacoco:report` and `jacoco:check` bind to `verify` as the task says; `prepare-agent` uses its default phase (`initialize`).

## Open questions
None.
