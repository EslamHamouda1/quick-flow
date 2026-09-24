# phase-01-test: Setup (backend-dev)

Source of the checks: `loops/backend-dev/outputs/001-quickflow/phase-01.md` has an empty
`## Acceptance criteria` section (Setup phase, no user story). The approved review file
`phase-01-review.md` lists what the testing loop can observe after this phase; each of those
statements is one check. Criteria listed (compare on later attempts):

- R1: "`cd backend && ./mvnw -q -DskipTests package` succeeds with Java 25 / Boot 4.1.1."
- R2: "`cd backend && ./mvnw -q verify` runs the JaCoCo `report` + `check` goals and writes `backend/target/openapi.json` (an OpenAPI document with no `/api` paths yet)."
- R3: "The app starts with the default profile on port 8080 and serves `/v3/api-docs` (the `health` URL in the config) and `/swagger-ui`."
- R4: "The `test` profile writes its H2 file under `backend/data/test*` (matches `test_db_glob`)."

| check | criterion | steps | expected |
|---|---|---|---|
| C1 | R3 | `curl GET {base_url}/v3/api-docs` (verify.sh) | HTTP 200, JSON with an `openapi` field |
| C2 | R3 | `curl -L GET {base_url}/swagger-ui` (verify.sh) | HTTP 200 after redirects, Swagger UI HTML page |
| C3 | R2 | `curl GET {base_url}/v3/api-docs` (verify.sh) | no path starting with `/api` ("no `/api` paths yet") |
| C4 | R1, R2 | runner's unit run (`unit-result.json`, `unit/unit-run.log`) | `./mvnw verify` exit 0 on Spring Boot 4.1.1 / Java 25 (`verify` includes `package`) |
| C5 | R2 | `unit/coverage/jacoco.xml` present; `jacoco-check` execution bound to `verify` in `backend/pom.xml` | JaCoCo `report` and `check` run in `verify` |
| C6 | R2 | `backend/target/openapi.json` and its copy `loops/backend-dev/outputs/openapi.json` | exists, OpenAPI document, no `/api` paths |
| C7 | R4 | `ls backend/data/test*` after the runner started the app with the `test` profile; server log | H2 file matching `backend/data/test*`; log shows profile `test` and `jdbc:h2:file:./data/test` |

C1–C3 run in `phase-01-verify.sh`; C4–C7 are file checks whose output is saved to
`<run_dir>/file-checks.txt`.

Note on R3: the runner starts the app with `run_test` (profile `test`), not the default profile.
Port 8080 and the same endpoints are checked; nothing default-profile-specific is observable here.
