# phase-00 review: Plan (frontend layer)

Feature `001-quickflow`, `FEATURE_DIR` = `specs/001-quickflow-frontend`. `spec.md` and `checklists/` were copied
from `specs/001-quickflow-backend/` (spec already carries the reviewer's answers of 2026-09-24). Planned artifacts:
`plan.md`, `research.md`, `data-model.md`, `contracts/ui-contract.md`, `quickstart.md`, `tasks.md`.

Note: the spec-kit scripts (`.specify/scripts/bash/setup-plan.sh`, `check-prerequisites.sh`) could not be run in this
session (not permitted); `.specify/feature.json` still points at `specs/001-quickflow-backend`, so it was not used.
The plan/tasks/analyze steps were done by hand against the templates in `.specify/templates/`, writing only inside
`specs/001-quickflow-frontend/`.

## Swagger input
`specs/001-quickflow-backend/contracts/openapi.yaml` (OpenAPI 3.1, planning). From implementation on, the client is
generated from `loops/backend-dev/outputs/openapi.json` by `layers.frontend.generate_client`.

## Phases

| Phase | Title | Story | Tasks | depends_on |
|---|---|---|---|---|
| phase-01 | Setup | – | T001–T004 | – |
| phase-02 | Foundational | – | T005–T014 | phase-01 |
| phase-03 | US1 Manage tasks | US1 | T015–T020 | phase-02, backend-dev:US1 |
| phase-04 | US2 Track recurring habits | US2 | T021–T025 | phase-02, backend-dev:US2 |
| phase-05 | US3 Track learning resources with milestones and notes | US3 | T026–T029 | phase-02, backend-dev:US3 |
| phase-06 | US4 Build and follow Todo Plans | US4 | T030–T035 | phase-02, phase-03, phase-04, phase-05, backend-dev:US4 |
| phase-07 | US5 See everything on the Dashboard | US5 | T036–T039 | phase-02, phase-03..phase-06, backend-dev:US5 |
| phase-08 | US6 Adjust settings | US6 | T040–T042 | phase-02, backend-dev:US6 |
| phase-09 | Polish | – | T043–T045 | phase-03..phase-08 |

## Open questions
None. `spec.md` has no `[NEEDS CLARIFICATION]` marker left (the backend gate answered Q1–Q4). The UI choices the
spec leaves open are listed under Assumptions.

## Analyze findings
- **HIGH – cross-layer ordering of Foundational**: phase-02 (T005, T006 and the loop's `generate_client` step) needs
  `loops/backend-dev/outputs/openapi.json`, which exists only after backend phase-02 (Foundational) is implemented.
  `phases.json` can express cross-layer dependencies only as `backend-dev:<story>` on story phases, so phase-02 has
  none. Recommendation: the orchestrator runs backend phase-02 before frontend phase-02 (backend phase-01/02 are
  still `planned`); if the file is missing, the implement session stops with a question instead of coding.
- **HIGH – generated client vs Angular 22.2.0 (unchecked)**: whether `typescript-angular` from generator CLI 2.41.0
  compiles under Angular 22.2.0, and its provider API, can only be checked once the client is generated (T005). The
  CLI also downloads its jar on first run (network) and needs Java (JDK 25.0.4.1 found). If it does not compile, the
  session raises a question; the generated files are never patched by hand.
- Fixed during analysis: US6 originally edited US4/US5 files (watcher, dashboard) while depending only on phase-02;
  the settings are now read where they are used (T035, T037) so US6 touches only its own files.
- Coverage: every AC-US1-1..AC-US6-2 maps to at least one task; FR-01..FR-10 and NFR-2/4/5 are covered; no
  constitution violation (Constitution III is backend-only).

## Unchecked checklist items
None (`checklists/requirements.md`: all items checked).

## Assumptions
- **FA-1 Tests**: no frontend unit tests are written (Constitution III makes them optional; `layers.frontend.test`
  is empty). "Tests first" for each story phase = the selectors in `contracts/ui-contract.md`, pinned now before any
  code, plus a first task per story that checks the generated client and contract; the testing loop writes the
  Playwright checks from the ACs.
- **FA-2 Plan-start notification** (FR-08.1, AC-US4-9, A-10): shown app-wide (a notice on any page plus a highlight on
  the plan card), once per plan; the ids already notified are kept in the browser's `localStorage`; on opening the app,
  plans already In Progress and not yet notified are notified once; nothing is shown when `planStartNotifications`
  is off.
- **FA-3 Validation messages**: forms do not pre-validate required/length (no `maxlength`); the API's 400
  `errors[{field, message}]` is shown under the named field, so the message is the server's (keeps rules out of the UI).
- **FA-4 Quick-add** (AC-US5-6): navigates to the list page with `?add=1`, which opens that page's add form.
- **FA-5 Default page** (FR-10.2): applies when the app is opened at `/`; deep links (e.g. `/tasks`) open as asked.
- **FA-6 Habit toggle**: the checkbox shows `completedToday`; checking records today's completion, unchecking undoes
  today's (FR-04.3); the card also shows `doneForCurrentPeriod` ("Done this week" for weekly) and the streak.
- **FA-7 Deletes**: act immediately, without a confirmation dialog (A-3: permanent, no undo in the spec).
- **FA-8 Greeting**: "Hello, <displayName>" or "Hello" when no name is set.
- **FA-9 Scaffold**: `frontend/` is created by the runner's `layers.frontend.scaffold` before the first implement
  session (as for the backend); no task creates it.
- **FA-10 No extra dependencies**: no UI component, date or state library (none is pinned); plain CSS, English labels
  as in `data-model.md`; time zone handling with `Intl` (research R-3).
- **FA-11 Invalid enum values** (AC-US1-4, "any other value is rejected" in AC-US3-3): enforced by the API; the UI
  offers only the valid values in selects, so the UI part of these ACs is that only valid values are offered.

## Reviewer notes
- 2026-09-24T17:25:43+03:00: All frontend assumptions FA-1..FA-11 approved as listed (including FA-7 deletes without confirmation and FA-3 server-side validation messages only). No open questions.
