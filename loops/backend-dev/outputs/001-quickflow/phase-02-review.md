# phase-02-review: Foundational

## Goal
Clock and app time zone, RFC 9457 error model, `GET /api/app-info`, OpenAPI metadata: the base every story builds on.

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

Order: the three tests T008–T010 are written first (in parallel, they touch different files), then
T013 and T017 in parallel, then T011 → T012 → T014 (each needs the previous), then T015 (needs T013)
and T016 (needs T014).

## Acceptance criteria
None from spec.md (Foundational phase, no user story). What the testing loop can observe after this phase:
- `GET /api/app-info` → 200 `application/json` `{"timeZone": "Africa/Cairo", "now": "<ISO-8601 date-time with offset, e.g. +03:00 / +02:00>"}` (contract schema `AppInfo`, operationId `getAppInfo`), `now` close to the current wall-clock time in Cairo.
- An unknown path under `/api` still gets Spring's default handling (this phase adds no mapping for it).
- `/v3/api-docs` shows `info.title = "QuickFlow API"`, `info.version = "0.1.0"` and the path `/api/app-info` with operationId `getAppInfo`; `./mvnw verify` writes the same into `backend/target/openapi.json`.
- The error model (400/404/409 as `application/problem+json`) has no real endpoint that triggers it yet; it is covered by T009's web-slice test and observed through story endpoints from phase-03 on.

## Files
| File | Change |
|---|---|
| `backend/src/main/java/com/quickflow/QuickflowApplication.java` | + `@ConfigurationPropertiesScan` |
| `backend/src/main/java/com/quickflow/config/AppProperties.java` | new record `AppProperties(String timezone)`, prefix `app` (value from `app.timezone=Africa/Cairo`, already in `application.properties`) |
| `backend/src/main/java/com/quickflow/config/ClockConfig.java` | new, `@Bean Clock clock(AppProperties)` |
| `backend/src/main/java/com/quickflow/config/OpenApiConfig.java` | new, `@Bean OpenAPI` with `Info` title/version |
| `backend/src/main/java/com/quickflow/domain/common/NotFoundException.java` | new (unchecked) |
| `backend/src/main/java/com/quickflow/domain/common/ConflictException.java` | new (unchecked) |
| `backend/src/main/java/com/quickflow/domain/common/ValidationException.java` | new (unchecked, carries `List<FieldError>`) |
| `backend/src/main/java/com/quickflow/domain/common/FieldError.java` | new record `FieldError(String field, String message)` (T013 "list of `FieldError(field, message)`"; own file so the web layer can reuse it) |
| `backend/src/main/java/com/quickflow/domain/common/TimeService.java` | new `@Service`: `now()` → `OffsetDateTime`, `today()` → `LocalDate`, `zone()` → `ZoneId`, `toOffset(Instant)` → `OffsetDateTime` |
| `backend/src/main/java/com/quickflow/web/ApiExceptionHandler.java` | new `@RestControllerAdvice` returning `ProblemDetail` (+ `errors` property) |
| `backend/src/main/java/com/quickflow/web/info/AppInfoResponse.java` | new record `AppInfoResponse(String timeZone, OffsetDateTime now)` |
| `backend/src/main/java/com/quickflow/web/info/AppInfoController.java` | new, `GET /api/app-info`, method `getAppInfo` |
| `backend/src/test/java/com/quickflow/domain/common/TimeServiceTest.java` | new |
| `backend/src/test/java/com/quickflow/web/ApiExceptionHandlerTest.java` | new (with a nested test-only controller) |
| `backend/src/test/java/com/quickflow/web/info/AppInfoControllerTest.java` | new |

Endpoint added: `GET /api/app-info` (contract `paths./api/app-info`, schema `AppInfo`).
No change to `pom.xml` or the properties files.

## Planned checks
- **Tests written before the code they test** (layer has `coverage`): T008 before T014, T009 before T013/T015, T010 before T016.
- Before coding, check in the resolved jars (not from memory):
  - the Jackson 3 exception Spring wraps in `HttpMessageNotReadableException` for an unknown enum value and for malformed JSON (T015; e.g. an `InvalidFormatException` / `MismatchedInputException` under `tools.jackson.databind.exc`), and how to read the offending field path from it;
  - the packages of `@WebMvcTest`, `MockMvcTester` and `@MockitoBean` in Boot 4.1.1 / the `spring-boot-starter-webmvc-test` jar (research R-1);
  - that `ProblemDetail` serializes a property set with `setProperty("errors", …)` as a top-level `errors` field, and that the response content type is `application/problem+json`;
  - `io.swagger.v3.oas.models.OpenAPI` / `Info` on the classpath from springdoc 3.1.1 (T017).
- Run `L.build` and `L.test`; save output to `loops/backend-dev/runs/001-quickflow/phase-02-build.log`.
- Copy `backend/target/openapi.json` to `loops/backend-dev/outputs/openapi.json` (`L.openapi_copy`) and compare `/api/app-info` (operationId, `AppInfo` properties `timeZone`, `now` as `date-time`) and `info` with `contracts/openapi.yaml`.
- JaCoCo gate: `com/quickflow/domain/**` now has classes (`TimeService` + 3 exceptions + `FieldError`); T008 covers `TimeService`; the exceptions are covered through T009 (they're thrown by the test controller). Line ratio must reach ≥ 0.80.
- **Unit tests (one per business rule)**: no BR-1..BR-14 rule belongs to this phase, so none named with a BR id here (those are written test-first in phases 03–08). Tests for this phase's own rules:
  - `TimeServiceTest` (Constitution IV): `todayIsCairoDate`, `todayIsNextDayAt2330Utc`, `nowHasCairoOffset`, `zoneIsAfricaCairo`, `toOffsetRendersInstantInCairo`.
  - `ApiExceptionHandlerTest` (R-9 / BR-3 error format): `validationExceptionIs400ProblemWithErrors`, `notFoundIs404Problem`, `conflictIs409Problem`, `malformedJsonIs400Problem`, `unknownEnumValueIs400ProblemWithField`, plus bean validation (`@Valid` on the test request) → 400 with `errors` (R-9 names it; the test controller makes it cheap).
  - `AppInfoControllerTest`: `getAppInfoReturnsZoneAndNow`.

## Risks
- **springdoc and the `@RestControllerAdvice`**: springdoc adds responses declared by a controller advice to every operation. If the generated `openapi.json` gets extra 400/404/409 responses or a `ProblemDetail` schema that the contract doesn't list for `getAppInfo`, the generated swagger drifts from `contracts/openapi.yaml`. The contract's error schema is named `Problem`; springdoc may name it `ProblemDetail`. Seen only after `verify`; if it differs, the difference is written as a question rather than hidden with annotations nobody approved.
- **Clock bean in slice tests**: `@WebMvcTest` doesn't load `ClockConfig`/`AppProperties`; T010 mocks `TimeService` with `@MockitoBean`, so no `Clock` bean is needed there. T009's test controller must not need one either.
- **`Clock.system(zone)` ≠ fixed in tests**: `TimeServiceTest` builds `TimeService` directly with `Clock.fixed(instant, ZoneId.of("Africa/Cairo"))`, no Spring context.
- **DST in Cairo**: Egypt uses summer time (+03:00) and winter time (+02:00). Tests assert the offset `ZoneId.of("Africa/Cairo").getRules().getOffset(instant)` for the fixed instant instead of a hard-coded `+03:00`, and pick the 23:30 UTC instant so that the next-day rule holds under both offsets.
- **Default Spring Boot `problemdetails`**: `spring.mvc.problemdetails.enabled` isn't set; the advice extends nothing Boot needs to enable. If extending `ResponseEntityExceptionHandler` is used to catch `HttpMessageNotReadableException`/`MethodArgumentNotValidException`, overriding its handler methods is the way to add `errors` (checked in the resolved Spring Framework sources).
- **Context-load test**: the scaffold's `QuickflowApplicationTests` now loads `ClockConfig` and `AppProperties`; it needs `app.timezone`, which `application.properties` already sets.

## Assumptions
- A-1: `TimeService` is a Spring `@Service` in `com.quickflow.domain.common` with a constructor taking `Clock` (plan: "Domain services depend on repositories and `Clock` only").
- A-2: `FieldError` is a separate record in `com.quickflow.domain.common` (T013 names it but not its file); the web layer puts the same `{field, message}` objects into `errors`.
- A-3: `toOffset(Instant)` returns `instant.atZone(zone()).toOffsetDateTime()` (R-4 "rendered in the app zone").
- A-4: for 400/404/409 the problem `title` is the HTTP reason phrase (`Bad Request`, `Not Found`, `Conflict`) and `detail` is the exception message (the contract only requires `title` and `status`).
- A-5: bean-validation failures (`MethodArgumentNotValidException`) map to 400 with `errors` from the field errors (R-9 says "400s from bean validation and domain `ValidationException`"), even though T009 lists only the domain exception explicitly.
- A-6: `AppInfoResponse.timeZone` is `zone().getId()` (`"Africa/Cairo"`), and `now` is serialized by Jackson as an ISO-8601 string with offset (Boot's default, no timestamp numbers; checked in the test output).
- A-7: `OpenApiConfig` sets only `info.title` and `info.version`; no servers or tags block (T017 names only those two).

## Open questions
None.
