# Research: QuickFlow backend

Decisions made while planning. "Checked at implement" means the fact comes from memory or the docs of a
pinned version and is confirmed against the scaffolded project (generated `pom.xml`, the installed
library sources, a small compile) before code relies on it (Constitution V).

## R-1 Scaffold and starters
- **Decision**: The runner scaffolds `backend/` from start.spring.io (Boot 4.1.1, Java 25, `web, data-jpa, h2,
  validation`). Setup adds `springdoc-openapi-starter-webmvc-ui:3.1.1` and the test starters
  `spring-boot-starter-webmvc-test` and `spring-boot-starter-data-jpa-test` (Boot-managed versions).
- **Rationale**: Boot 4 splits test auto-configuration into per-technology test starters; the stack table names them.
- **Checked at implement**: the exact artifact ids the scaffold generated (e.g. `spring-boot-starter-webmvc` vs `-web`),
  and the packages of `@WebMvcTest`, `@DataJpaTest`, `MockMvcTester`, `@MockitoBean` in the resolved jars.
- **Alternatives**: hand-written pom (rejected: the config pins the scaffold).

## R-2 Plugin versions ("latest release at setup")
- **Decision**: pin `springdoc-openapi-maven-plugin` and `jacoco-maven-plugin` to exact versions in `pom.xml`.
  The versions are not written in any project file and this session can't look them up, so they are
  **Open question Q4** in the phase-00 review.
- **Rationale**: Constitution I forbids picking a version from memory.

## R-3 OpenAPI generation during `verify`
- **Decision**: `spring-boot-maven-plugin` executions `start` (pre-integration-test, profile `openapi`) and `stop`
  (post-integration-test); `springdoc-openapi-maven-plugin` goal `generate` at `integration-test` with
  `apiDocsUrl=http://localhost:18080/v3/api-docs`, `outputDir=${project.build.directory}`, `outputFileName=openapi.json`.
  springdoc properties: `springdoc.api-docs.path=/v3/api-docs`, `springdoc.swagger-ui.path=/swagger-ui`.
- **Checked at implement**: plugin parameter names in the pinned plugin version.
- **Alternatives**: a test that dumps `/v3/api-docs` (rejected: the constitution names the plugin).

## R-4 Time handling
- **Decision**: `app.timezone=Africa/Cairo` in `application.properties`; `ClockConfig` defines
  `Clock clock(AppProperties p) = Clock.system(ZoneId.of(p.timezone()))`. Date-times are stored as `Instant`
  (UTC, `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`) and exchanged as ISO-8601 with offset
  (`OffsetDateTime`), rendered in the app zone. Calendar dates (`dueDate`, `completionDate`, `targetDate`)
  are `LocalDate` in the app zone. `GET /api/app-info` returns `{timeZone, now}`.
- **Rationale**: Constitution IV: one zone, one clock, computed-on-read values.

## R-5 Deletion model (BR-14, FR-07.6)
- **Decision**: hard deletes. `HabitCompletion` rows are removed with their habit, milestones and notes with their
  card (JPA `cascade = ALL, orphanRemoval = true` / bulk delete by parent id), plan items with their plan.
  `PlanItem` stores `sourceType`, `sourceId`, a `sourceTitle` snapshot (refreshed on read while the source
  exists) and computes `sourceRemoved` on read.
- **Rationale**: simplest way to guarantee deleted rows never come back; no filter to forget in custom queries.
- **Alternatives**: soft delete with `@SQLRestriction` (rejected: more code, every native query must remember it).

## R-6 Plan status and rest time (FR-08, BR-12) — depends on Q2
- **Decision (recommended answer, pending Q2)**: `PlanStatusPolicy.statusAt(plan, now)`:
  `now < start` → NOT_STARTED; else if `now >= end` or all items done → COMPLETED; else IN_PROGRESS.
  `restSeconds` = seconds from `now` to `end` when `start <= now < end`, else `null` (BR-12), independent of status.
  `progressPercent = floor(100 * done / total)`.
- **Rationale**: pure function of stored values and the clock (NFR-4); one class to change if Q2's answer differs.

## R-7 BR-13 propagation — depends on Q1
- **Decision (recommended answer, pending Q1)**: `PlanService.setItemDone(planId, itemId, true)` on a TASK item
  calls `TaskService.complete(taskId)` (status Done, `completedAt` set) when the task still exists. Un-ticking
  never changes the source. HABIT and LEARNING_RESOURCE items never change their source.

## R-8 Habit progress and streak (FR-04.5)
- **Decision**: `HabitProgressCalculator` (pure): Daily period = the date; Weekly period = ISO week (Monday start,
  spec A-12). `doneForCurrentPeriod` = a completion exists in the current period; `currentStreak` = number of
  consecutive periods with ≥ 1 completion ending at the current period if done, else at the previous one.
  `completedToday` = a completion exists for today.

## R-9 Error format
- **Decision**: `ApiExceptionHandler` (`@RestControllerAdvice`) returns Spring `ProblemDetail`
  (`type, title, status, detail, instance`) plus property `errors: [{field, message}]` for 400s from bean
  validation and domain `ValidationException`; 404 `NotFoundException`; 409 `ConflictException`; malformed
  JSON / unknown enum values → 400 with the offending field when known (BR-3).
- **Checked at implement**: Jackson 3 (Boot 4) exception types for unknown enum values.

## R-10 Settings — depends on Q3
- **Decision (recommended answer, pending Q3)**: one `AppSettings` row (id 1, created on first read) with
  `displayName` (optional, ≤ 100), `planStartNotifications` (boolean, default true) and `defaultPage`
  (DASHBOARD | TASKS | HABITS | LEARNING | PLANS | SETTINGS, default DASHBOARD). `GET/PUT /api/settings`.

## R-11 Duplicate completion under concurrency (BR-7)
- **Decision**: unique constraint on `(habit_id, completion_date)`; the service checks first and also maps
  `DataIntegrityViolationException` to 409, so two concurrent requests still create one row.
