# phase-01: Setup
layer: backend
story_id: none
spec_phase: 1
depends_on: []
## Goal
Build configuration on the runner-scaffolded backend: springdoc and test starters, JaCoCo gate, OpenAPI generation during `verify`, the three profiles.
## Acceptance criteria
## Tasks
- [x] T001 Check the scaffolded `backend/pom.xml` (starter artifact ids, Boot 4.1.1, `java.version` 25) and add `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1` plus test-scoped `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-jpa-test` (Boot-managed versions, keep `spring-boot-starter-test`) in backend/pom.xml (research R-1)
- [x] T002 Add `jacoco-maven-plugin` at the exact version answered for Q4 in backend/pom.xml with executions `prepare-agent`, `report` (phase `verify`) and `check` (phase `verify`, rule: BUNDLE limited to `com/quickflow/domain/**` classes, LINE COVEREDRATIO minimum 0.80)
- [x] T003 Add OpenAPI generation in backend/pom.xml: `spring-boot-maven-plugin` executions `start` (pre-integration-test, `<profiles>openapi</profiles>`) and `stop` (post-integration-test); `springdoc-openapi-maven-plugin` at the exact version answered for Q4, goal `generate` at `integration-test`, `apiDocsUrl` `http://localhost:18080/v3/api-docs`, `outputDir` `${project.build.directory}`, `outputFileName` `openapi.json` (research R-3; check the plugin parameter names in the pinned version)
- [x] T004 [P] Write backend/src/main/resources/application.properties: `spring.application.name=quickflow`, `spring.datasource.url=jdbc:h2:file:./data/quickflow`, `spring.jpa.hibernate.ddl-auto=update`, `spring.jpa.open-in-view=false`, `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`, `app.timezone=Africa/Cairo`, `springdoc.api-docs.path=/v3/api-docs`, `springdoc.swagger-ui.path=/swagger-ui`
- [x] T005 [P] Write backend/src/main/resources/application-test.properties: `spring.datasource.url=jdbc:h2:file:./data/test`, `spring.jpa.hibernate.ddl-auto=update`
- [x] T006 [P] Write backend/src/main/resources/application-openapi.properties: `server.port=18080`, `spring.datasource.url=jdbc:h2:mem:openapi;DB_CLOSE_DELAY=-1`, `spring.jpa.hibernate.ddl-auto=create-drop`
- [x] T007 [P] Add `data/` to backend/.gitignore so the H2 files are never committed
