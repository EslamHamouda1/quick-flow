# phase-02-test: Foundational (backend-dev)

Source of the checks: `loops/backend-dev/outputs/001-quickflow/phase-02.md` has an empty
`## Acceptance criteria` section (Foundational phase, no user story). The approved review file
`phase-02-review.md` lists what the testing loop can observe after this phase; each of those
statements is one criterion. Criteria listed (compare on later attempts):

- R1: "`GET /api/app-info` → 200 `application/json` `{"timeZone": "Africa/Cairo", "now": "<ISO-8601 date-time with offset, e.g. +03:00 / +02:00>"}` (contract schema `AppInfo`, operationId `getAppInfo`), `now` close to the current wall-clock time in Cairo."
- R2: "An unknown path under `/api` still gets Spring's default handling (this phase adds no mapping for it)."
- R3: "`/v3/api-docs` shows `info.title = "QuickFlow API"`, `info.version = "0.1.0"` and the path `/api/app-info` with operationId `getAppInfo`; `./mvnw verify` writes the same into `backend/target/openapi.json`."
- R4: "The error model (400/404/409 as `application/problem+json`) has no real endpoint that triggers it yet; it is covered by T009's web-slice test and observed through story endpoints from phase-03 on."

| check | criterion | steps | expected |
|---|---|---|---|
| C1 | R1 | `curl GET {base_url}/api/app-info` (verify.sh) | HTTP 200, `Content-Type` `application/json`, body has exactly the `AppInfo` required fields, `timeZone` = `"Africa/Cairo"` |
| C2 | R1 | same response (verify.sh) | `now` is an ISO-8601 date-time with offset; the offset equals Africa/Cairo's current UTC offset; `now` is within 60 s of the checker's clock (see note) |
| C3 | R2 | `curl GET {base_url}/api/does-not-exist` (verify.sh) | HTTP 404 (no mapping; Spring's default "no handler/resource" outcome) |
| C4 | R3 | `curl GET {base_url}/v3/api-docs` (verify.sh) | `info.title` = `"QuickFlow API"`, `info.version` = `"0.1.0"`, `paths./api/app-info.get.operationId` = `getAppInfo`, 200 response schema `AppInfo` with `timeZone` string and `now` `format: date-time`, both required (as in `contracts/openapi.yaml`) |
| C5 | R3 | `backend/target/openapi.json` (written by the runner's `./mvnw verify`) and its copy `loops/backend-dev/outputs/openapi.json` | same `info.title`, `info.version`, path and operationId as C4 (verify.sh) |
| C6 | R4 | runner's unit run: `unit/surefire/TEST-com.quickflow.web.ApiExceptionHandlerTest.xml` | T009's tests present and passing (0 failures, 0 errors) |
| C7 | R1 (T008, T010) | `unit/surefire/TEST-com.quickflow.domain.common.TimeServiceTest.xml`, `TEST-com.quickflow.web.info.AppInfoControllerTest.xml` | present and passing |

C1–C5 run in `phase-02-verify.sh`; C6–C7 are file checks whose output is saved to
`<run_dir>/file-checks.txt`.

Note on C2: R1 says "close to" without a number. 60 s is the tolerance this check uses to
decide "close"; the server and the checker run on the same machine, so the real gap is
milliseconds. If a stricter or looser bound is wanted, it goes in the review notes.
