# phase-08-review: US6 Adjust settings

## Goal
`GET /api/settings` (operationId `getSettings`) and `PUT /api/settings` (operationId `updateSettings`), both on
contract schema `Settings` (`displayName`, `planStartNotifications`, `defaultPage`), backed by one server-side
`AppSettings` row (id 1) created with the defaults on first read, so the preferences survive restarts
(FR-10.2, NFR-3, reviewer answer Q3, research R-10, data-model `AppSettings`).

## Tasks
- [ ] T075 [P] [US6] Domain test backend/src/test/java/com/quickflow/domain/settings/SettingsServiceTest.java: `FR-10.2` first read returns defaults (recommended Q3: `displayName` null, `planStartNotifications` true, `defaultPage` DASHBOARD) and stores them; update saves; `displayName` > 100 rejected (field `displayName`); null `defaultPage` rejected
- [ ] T076 [P] [US6] Persistence test backend/src/test/java/com/quickflow/persistence/SettingsRepositoryTest.java: `NFR-3` the single settings row (id 1) is saved and read back
- [ ] T077 [P] [US6] Web-slice test backend/src/test/java/com/quickflow/web/settings/SettingsControllerTest.java: `getSettings` 200 with exactly the contract `Settings` fields (`displayName`, `planStartNotifications`, `defaultPage`), `updateSettings` 200, unknown `defaultPage` 400, too-long `displayName` 400
- [ ] T078 [P] [US6] Create enum `DefaultPage {DASHBOARD, TASKS, HABITS, LEARNING, PLANS, SETTINGS}` and entity `AppSettings` (`id` always 1, `displayName` "≤ 100", `planStartNotifications` default true, `defaultPage` default DASHBOARD) in backend/src/main/java/com/quickflow/domain/settings/
- [ ] T079 [US6] Create `SettingsRepository` in backend/src/main/java/com/quickflow/domain/settings/SettingsRepository.java
- [ ] T080 [US6] Create `SettingsService` (`get()` transactional read-or-create of the default row, so it works on an empty database; `update(...)`) in backend/src/main/java/com/quickflow/domain/settings/SettingsService.java
- [ ] T081 [US6] Create `SettingsDto` record and `SettingsController` (`getSettings`, `updateSettings`) in backend/src/main/java/com/quickflow/web/settings/

tasks.md `## Phase 8` matches `phase-08.md` (T075–T081, same wording).

Order: the three tests T075–T077 first (parallel, different files), then T078, T079 (needs the entity), T080
(needs the repository), T081 (needs the service).

## Acceptance criteria
Copied from `phase-08.md` (spec.md US6; the "(pending Q3)" note is stale: Q3 was answered on 2026-09-24 and is
now FR-10.2):
- AC-US6-1: Given the Settings page, When the user opens it, Then the profile information and current preferences are shown.
- AC-US6-2: Given the Settings page, When the user changes a preference and saves, Then it is kept between sessions and takes effect (FR-10).

Backend share: the page reads `GET /api/settings` (AC-US6-1) and saves with `PUT /api/settings` (AC-US6-2,
kept between sessions); "takes effect" (default page, plan-start notifications, greeting) is the frontend's.
The time zone shown read-only comes from the existing `GET /api/app-info` (unchanged).

What the testing loop can observe after this phase (`http://localhost:8080`):
- Empty database: `GET /api/settings` → 200 `application/json`
  `{"displayName": null, "planStartNotifications": true, "defaultPage": "DASHBOARD"}`; a second GET returns the same.
- `PUT /api/settings` with `{"displayName": "Eve", "planStartNotifications": false, "defaultPage": "HABITS"}` → 200
  with the saved body; a following GET returns the same values, and so does a GET after the backend restarts
  (file database, NFR-3 / quickstart step 7).
- `PUT` with `displayName: null` (or omitted) clears it → GET shows `null`.
- 400 problem detail (same shape as other endpoints, `errors[].field`):
  - `displayName` of 101 characters → field `displayName`;
  - `defaultPage: "FOO"` → field `defaultPage`, message listing the six values;
  - `defaultPage` missing or `null` → field `defaultPage`;
  - `planStartNotifications` missing or `null` → field `planStartNotifications`;
  - non-boolean `planStartNotifications` (e.g. `"yes"`) → 400.
  After any 400 the stored settings are unchanged.
- `/v3/api-docs` and `backend/target/openapi.json` list `getSettings` / `updateSettings` under tag `settings`,
  with schemas `Settings` (required `[planStartNotifications, defaultPage]`, `displayName` `[string, null]`
  maxLength 100) and `DefaultPage` (enum of six, as a `$ref`).

## Files
| File | Change |
|---|---|
| `backend/src/main/java/com/quickflow/domain/settings/DefaultPage.java` | new enum, `@Schema(enumAsRef = true)` like `HabitFrequency`, so the generated schema is `$ref: DefaultPage` as in the contract |
| `backend/src/main/java/com/quickflow/domain/settings/AppSettings.java` | new `@Entity @Table(name = "app_settings")`; `@Id Long id` assigned `1L` (no generator); `displayName` (`length = 100`, nullable), `planStartNotifications` (not null), `defaultPage` (`@Enumerated(STRING)`, not null); `static AppSettings defaults()`; `update(displayName, planStartNotifications, defaultPage)` validating like `Habit.apply` (collects `FieldError`s, throws `ValidationException`) |
| `backend/src/main/java/com/quickflow/domain/settings/SettingsRepository.java` | new `JpaRepository<AppSettings, Long>` |
| `backend/src/main/java/com/quickflow/domain/settings/SettingsService.java` | new `@Service @Transactional`; `get()` = `findById(1)` or save `defaults()`; `update(displayName, planStartNotifications, defaultPage)` = `get()` then `update(...)` (dirty checking saves it) |
| `backend/src/main/java/com/quickflow/web/settings/SettingsDto.java` | new record `@Schema(name = "Settings")` used as request **and** response (the contract uses one schema for both): `@Schema(types = {"string","null"}) @Size(max = 100) String displayName`, `@Schema(requiredMode = REQUIRED) @NotNull Boolean planStartNotifications`, `@Schema(requiredMode = REQUIRED) @NotNull DefaultPage defaultPage`; `static from(AppSettings)` |
| `backend/src/main/java/com/quickflow/web/settings/SettingsController.java` | new `@RestController @RequestMapping("/api/settings") @Tag(name = "settings")`; `@GetMapping getSettings()`, `@PutMapping updateSettings(@Valid @RequestBody SettingsDto)` |
| `backend/src/test/java/com/quickflow/domain/settings/SettingsServiceTest.java` | new |
| `backend/src/test/java/com/quickflow/persistence/SettingsRepositoryTest.java` | new |
| `backend/src/test/java/com/quickflow/web/settings/SettingsControllerTest.java` | new |

Endpoints added: `GET /api/settings`, `PUT /api/settings`. No change to `pom.xml`, properties,
`ApiExceptionHandler` (its existing `MethodArgumentNotValidException` and `HttpMessageNotReadableException` /
`InvalidFormatException` handlers already produce the 400s above) or any other story's files (tasks.md: "US6
touches no other story's files").

## Planned checks
- **Tests written before the code they test** (layer has `coverage`): T075 before T078/T080, T076 before T079,
  T077 before T081.
- Check before relying on it (in the existing code / resolved jars, not from memory):
  - that saving an entity with an assigned id (`1L`, no `@GeneratedValue`) through `JpaRepository.save` on a new
    row works in the `@DataJpaTest` slice (Spring Data calls `merge` for a non-null id; a `Persistable`/`@Version`
    is only added if the check shows it's needed);
  - that a `null` JSON value for the `Boolean` / enum record components reaches `@NotNull` (400 with the field
    name) and that `"FOO"` for `defaultPage` goes through the existing `InvalidFormatException` branch (field
    `defaultPage`); that `"yes"` for `planStartNotifications` is rejected by Jackson 3's default coercion (if it's
    accepted, it becomes a question, not a config change);
  - the `@WebMvcTest` + `@MockitoBean` + `MockMvcTester` pattern of the existing controller tests
    (`AppInfoControllerTest`, `HabitControllerTest`) and that `ApiExceptionHandler` is picked up in the slice.
- Run `L.build` and `L.test`; save output to `loops/backend-dev/runs/001-quickflow/phase-08-build.log`.
- Copy `backend/target/openapi.json` to `loops/backend-dev/outputs/openapi.json`; compare `getSettings`,
  `updateSettings`, `Settings` and `DefaultPage` with `contracts/openapi.yaml`; earlier operations must stay
  unchanged. Known differences (int32 formats, servers url) are logged, not changed.
- JaCoCo: domain line coverage ≥ 0.80 over `com/quickflow/domain/**`.
- **Unit tests, one per business rule** (named with the rule id):
  - `SettingsServiceTest` (Mockito `SettingsRepository`, plus plain tests of `AppSettings`):
    `fr10_2_firstReadReturnsDefaults` (`displayName` null, `planStartNotifications` true, `defaultPage` DASHBOARD),
    `fr10_2_firstReadStoresDefaults` (repository `save` called once with id 1), `fr10_2_existingRowReturnedNotRecreated`
    (no `save` when the row exists), `fr10_2_updateSavesAllThreeFields`, `fr10_2_updateOnEmptyDatabaseCreatesRow`,
    `fr10_2_displayNameOver100Rejected` (field `displayName`), `fr10_2_displayNameOf100Accepted`,
    `fr10_2_displayNameNullClears`, `fr10_2_blankDisplayNameStoredAsNull` (A-3), `fr10_2_nullDefaultPageRejected`
    (field `defaultPage`), `fr10_2_rejectedUpdateLeavesValuesUnchanged`.
  - `SettingsRepositoryTest` (`@DataJpaTest`): `nfr3_settingsRowSavedAndReadBack` (id 1, all three fields),
    `nfr3_updatedRowReadBackAfterFlushAndClear`, `nfr3_onlyOneRowAfterRepeatedSaves` (`count() == 1`).
  - `SettingsControllerTest` (`@WebMvcTest(SettingsController.class)`, `@MockitoBean SettingsService`):
    `getSettingsReturns200WithExactlyContractFields` (the three keys, nothing else, `displayName` present as
    `null`), `updateSettingsReturns200WithSavedValues`, `updateSettingsUnknownDefaultPageIs400`,
    `updateSettingsTooLongDisplayNameIs400`, `updateSettingsMissingDefaultPageIs400`,
    `updateSettingsMissingPlanStartNotificationsIs400`, `updateSettingsInvalidDoesNotCallService`.

## Risks
- **Coverage gate reads only package `com/quickflow/domain`** (phase-02–07 finding, `scripts/lib/loopctl.py`):
  `com/quickflow/domain/settings` may be reported as "skipped"; the tests aim at ≥ 0.80 either way. I can't
  change `scripts/`.
- **Two first reads at once** (e.g. the Dashboard greeting and the Settings page loading together on an empty
  database) could both try to insert row 1. With an assigned id, `save` merges, so the second insert either
  merges into the same row or fails with a duplicate-key error on one request. Single user, one-time window;
  if the check above shows a failure mode, the service catches `DataIntegrityViolationException` and re-reads
  (the pattern `HabitService` already uses for BR-7). Logged, not over-engineered.
- **`displayName` 100-character limit** is checked on the Java `String.length()` (UTF-16 units), the same way
  every other length rule in this backend is (`Habit`, `Task`); an emoji counts as 2. Consistent with the
  frontend's `maxlength`.
- **Existing file databases** (`backend/data/quickflow`, `data/test`) use `ddl-auto=update`, so Hibernate adds
  the `app_settings` table on start; no migration needed.
- **springdoc output vs. contract**: `planStartNotifications` has `default: true` in the contract; springdoc
  won't emit a `default` unless annotated. Adding `@Schema(defaultValue = "true")` is harmless and is done if it
  renders as a boolean; otherwise the difference is logged.

## Assumptions
- A-1: `PUT /api/settings` is a **full replace** with the contract `Settings` body (the contract's request schema
  is `Settings` with `required: [planStartNotifications, defaultPage]`): both required fields must be present
  and non-null (400 otherwise); `displayName` omitted or `null` clears it. No PATCH-style partial update.
- A-2: The settings row always has id 1 and there is never more than one row; `GET` never 404s (it creates the
  defaults on first read, data-model "created on first read"). The `PUT` works on an empty database too (reads
  or creates the row, then applies the body).
- A-3: `displayName` is stored trimmed; an empty or whitespace-only value is stored as `null` (it is optional, and
  a blank greeting name is meaningless); the 100-character limit applies to the trimmed value. (`Task`/`Habit`
  names trim; optional descriptions store `""` as `null`.)
- A-4: `planStartNotifications` accepts only JSON `true`/`false`; `defaultPage` only the six enum names, exact
  case (as every other enum in this API).
- A-5: No field is added beyond the three in the contract (no user id, timestamps or time zone in `Settings`; the
  time zone stays on `GET /api/app-info`).

## Open questions
None.
