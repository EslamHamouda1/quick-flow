# phase-08-report: US6 Adjust settings (backend-dev, attempt 1)

Test plan: `phase-08-test.md` (new this attempt, C1–C5). Run against `http://localhost:8080` (fresh test
database) on 2026-09-26 around 02:12 +03:00.

**How the checks were run:** `phase-08-verify.sh` was written, but running it with `bash` was blocked by the
session's permission check ("requires approval"), as in phase-05 to phase-07. Shell variables and loops were
blocked too. So the same checks were run as direct `curl` calls with literal bodies (the 100/101-character
bodies were generated with `jq -n` into `C3-100.req` / `C3-101.req` and sent with `--data-binary`). Every
response is saved in the run folder `loops/testing/runs/001-quickflow/backend-dev/phase-08/attempt-1` as
`<check>*.json` (headers in `*.head` for the 400s and C1); there is no `curl.log` this attempt.

Note: a first C3 try used hand-typed strings that turned out to be 96 / 97 characters (both 200, correctly,
`C3-100-get.json` was then overwritten); C3 was redone with exact 100 / 101-character bodies, results below.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US6-1, FR-10.2, swagger | pass | `C1.head`: 200 `application/json`; `C1.json`: `{"displayName":null,"planStartNotifications":true,"defaultPage":"DASHBOARD"}`, keys exactly `defaultPage, displayName, planStartNotifications`; `C1-b.json` identical; `C1-docs.json`: `getSettings` / `updateSettings`, both tag `settings`, `Settings` required `[defaultPage, planStartNotifications]`, `displayName` `[string, null]` maxLength 100, `planStartNotifications` boolean, `defaultPage` `$ref DefaultPage`, `DefaultPage` enum of the six values |
| C2 | AC-US6-2, FR-10.2, NFR-3 | pass | `C2.json` PUT Eve/false/HABITS → 200 same body; `C2-get.json` same; `C2-{DASHBOARD,TASKS,HABITS,LEARNING,PLANS,SETTINGS}.json` each 200 and `*-get.json` read back the value; `C2-null.json` / `C2-null-get.json` `displayName null`; `C2-omit.json` (no `displayName`) → 200, `C2-omit-get.json` `displayName null`, `true`, `TASKS` |
| C3 | FR-10.2 `displayName` ≤ 100 | pass | `C3-100.json` 200, length 100; `C3-100-get.json` equals the request (`jq` compare true); `C3-101.head` 400 `application/problem+json`, `C3-101.json` `status 400`, `errors [{field displayName, "size must be between 0 and 100"}]`; `C3-101-get.json` equals `C3-100-get.json` (unchanged) |
| C4 | FR-10.2 `defaultPage`, swagger enum/required | pass | stored `Keep/false/PLANS` (`C4-set.json`); `"FOO"` (`C4-foo.json`), missing (`C4-missing.json`), `null` (`C4-null.json`), `"dashboard"` (`C4-lower.json`): each 400 `application/problem+json`, `errors[].field defaultPage` (messages "must be one of [DASHBOARD, TASKS, HABITS, LEARNING, PLANS, SETTINGS]" / "must not be null"); every `C4-*-get.json` still `Keep/false/PLANS` |
| C5 | FR-10.2 `planStartNotifications`, swagger boolean/required | pass | missing (`C5-missing.json`), `null` (`C5-null.json`): 400 problem, `errors[].field planStartNotifications` "must not be null"; `"yes"` (`C5-yes.json`): 400 problem, field `planStartNotifications` "invalid value"; every `C5-*-get.json` still `Keep/false/PLANS` |

5 / 5 pass, 0 fail, 0 unclear.

Not checked here: "takes effect" in AC-US6-2 (default page, plan-start notifications, greeting: frontend
layer); persistence across a backend restart (the testing loop never restarts processes; covered by the
`nfr3_*` repository tests and the file database). Swagger note (not a failure, same as every earlier phase):
the generated swagger lists only the 200 response for `updateSettings`; the contract's `400 BadRequest` is
what the API actually returns.

## Unit tests
From `unit/unit-result.json`: layer backend, exit code 0, **passed 329, failed 0, errors 0, skipped 0**,
outcome `pass`. The runner reported `coverage: null`, `coverage_skipped: true` ("no classes in
com/quickflow/domain": the gate matches the package exactly and the classes are in sub-packages; same as
phase-02–07). Read from `unit/coverage/jacoco.csv` (packages `com.quickflow.domain.*`): lines covered 672,
missed 23 → **96.7 %** (≥ 0.80); `com.quickflow.domain.settings` alone 41 / 41 = 100 %.
Settings test classes (`unit/surefire/`): SettingsServiceTest 12, SettingsRepositoryTest 3,
SettingsControllerTest 8, all passing.

## Requirement coverage
| id | unit tests | checks |
|---|---|---|
| FR-10.2 defaults on first read | SettingsServiceTest `fr10_2_firstReadReturnsDefaults`, `fr10_2_firstReadStoresDefaults`, `fr10_2_existingRowReturnedNotRecreated` | C1 |
| FR-10.2 save / replace | SettingsServiceTest `fr10_2_updateSavesAllThreeFields`, `fr10_2_updateOnEmptyDatabaseCreatesRow`, `fr10_2_displayNameNullClears`, `fr10_2_blankDisplayNameStoredAsNull`; SettingsControllerTest `updateSettingsReturns200WithSavedValues` | C2 |
| FR-10.2 `displayName` ≤ 100 | SettingsServiceTest `fr10_2_displayNameOver100Rejected`, `fr10_2_displayNameOf100Accepted`; SettingsControllerTest `updateSettingsTooLongDisplayNameIs400` | C3 |
| FR-10.2 `defaultPage` one of six | SettingsServiceTest `fr10_2_nullDefaultPageRejected`; SettingsControllerTest `updateSettingsUnknownDefaultPageIs400`, `updateSettingsMissingDefaultPageIs400` | C2, C4 |
| FR-10.2 `planStartNotifications` on/off | SettingsServiceTest `fr10_2_nullPlanStartNotificationsRejected`; SettingsControllerTest `updateSettingsMissingPlanStartNotificationsIs400`, `updateSettingsNonBooleanPlanStartNotificationsIs400` | C2, C5 |
| rejected update leaves settings unchanged | SettingsServiceTest `fr10_2_rejectedUpdateLeavesValuesUnchanged`; SettingsControllerTest `updateSettingsInvalidDoesNotCallService` | C3, C4, C5 |
| NFR-3 | SettingsRepositoryTest `nfr3_settingsRowSavedAndReadBack`, `nfr3_updatedRowReadBackAfterFlushAndClear`, `nfr3_onlyOneRowAfterRepeatedSaves` | C2 (across requests; no restart) |
| AC-US6-1 | as FR-10.2 defaults; SettingsControllerTest `getSettingsReturns200WithExactlyContractFields` | C1 |
| AC-US6-2 | as FR-10.2 save and NFR-3 | C2 ("takes effect": frontend layer) |
| contract `Settings` / `DefaultPage` | SettingsControllerTest `getSettingsReturns200WithExactlyContractFields` | C1 |

## Questions
None.
