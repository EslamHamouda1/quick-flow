# phase-02: Foundational
layer: backend
story_id: none
spec_phase: 2
depends_on: [phase-01]
## Goal
Clock and app time zone, RFC 9457 error model, `GET /api/app-info`, OpenAPI metadata: the base every story builds on.
## Acceptance criteria
## Tasks
- [ ] T008 [P] Domain test backend/src/test/java/com/quickflow/domain/common/TimeServiceTest.java: with a fixed clock in `Africa/Cairo`, `today()` returns the Cairo date (also at 23:30 UTC, when Cairo is already on the next day), `now()` returns an `OffsetDateTime` with the Cairo offset, `zone()` is `Africa/Cairo` (Constitution IV)
- [ ] T009 [P] Web-slice test backend/src/test/java/com/quickflow/web/ApiExceptionHandlerTest.java with a test-only controller: `ValidationException` → 400 `application/problem+json` with `title`, `status`, `errors[{field,message}]`; `NotFoundException` → 404; `ConflictException` → 409; malformed JSON and an unknown enum value → 400 (research R-9)
- [ ] T010 [P] Web-slice test backend/src/test/java/com/quickflow/web/info/AppInfoControllerTest.java: `GET /api/app-info` → 200 `{timeZone: "Africa/Cairo", now: <ISO offset date-time>}` (TimeService mocked)
- [ ] T011 Create `AppProperties` record (`@ConfigurationProperties("app")`, field `timezone`) in backend/src/main/java/com/quickflow/config/AppProperties.java and enable it (`@ConfigurationPropertiesScan`) in backend/src/main/java/com/quickflow/QuickflowApplication.java
- [ ] T012 Create `ClockConfig` exposing `@Bean Clock clock(AppProperties p)` = `Clock.system(ZoneId.of(p.timezone()))` in backend/src/main/java/com/quickflow/config/ClockConfig.java
- [ ] T013 [P] Create `NotFoundException`, `ConflictException` and `ValidationException` (list of `FieldError(field, message)`) in backend/src/main/java/com/quickflow/domain/common/
- [ ] T014 Create `TimeService` (`now()`, `today()`, `zone()`, `toOffset(Instant)`, all from the injected `Clock`) in backend/src/main/java/com/quickflow/domain/common/TimeService.java
- [ ] T015 Create `ApiExceptionHandler` (`@RestControllerAdvice`, RFC 9457 `ProblemDetail`, property `errors`) in backend/src/main/java/com/quickflow/web/ApiExceptionHandler.java (check the Jackson 3 exception types for unknown enum values in the resolved jars)
- [ ] T016 Create `AppInfoResponse` record and `AppInfoController` (`getAppInfo`, `GET /api/app-info`) in backend/src/main/java/com/quickflow/web/info/
- [ ] T017 [P] Create `OpenApiConfig` (title "QuickFlow API", version "0.1.0") in backend/src/main/java/com/quickflow/config/OpenApiConfig.java
