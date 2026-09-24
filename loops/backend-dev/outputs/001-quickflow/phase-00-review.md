# phase-00: Plan review (backend, 001-quickflow)

Planned from `Task_PRD.md`. Artifacts in `specs/001-quickflow-backend/`: `spec.md`, `checklists/requirements.md`,
`plan.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml` (OpenAPI 3.1: the frontend's swagger input
for planning), `quickstart.md`, `tasks.md`. Normalized requirements: `loops/backend-dev/state/001-quickflow/requirements.json`.

## Phases

| Phase | Title | Story | Tasks | depends_on |
|---|---|---|---|---|
| phase-01 | Setup | – | T001–T007 | – |
| phase-02 | Foundational | – | T008–T017 | phase-01 |
| phase-03 | US1 Manage tasks | US1 | T018–T028 | phase-02 |
| phase-04 | US2 Track recurring habits | US2 | T029–T041 | phase-02 |
| phase-05 | US3 Track learning resources with milestones and notes | US3 | T042–T053 | phase-02 |
| phase-06 | US4 Build and follow Todo Plans | US4 | T054–T067 | phase-02, phase-03, phase-04, phase-05 |
| phase-07 | US5 See everything on the Dashboard | US5 | T068–T074 | phase-02 … phase-06 |
| phase-08 | US6 Adjust settings | US6 | T075–T081 | phase-02 |
| phase-09 | Polish | – | T083–T087 | phase-03 … phase-08 |

86 tasks. T082 was removed during analysis (see Analyze findings, C1); the ids are not renumbered.

## Answered questions

Answered by the reviewer on 2026-09-24 (see `## Reviewer notes`), all as recommended; Q4 pins
jacoco-maven-plugin 0.8.15 and springdoc-openapi-maven-plugin 1.5. Written into spec.md (FR-07.5, FR-08.4,
FR-10.2, Clarifications), requirements.json, plan.md and research.md by plan-apply.

- **Q1 (FR-07.5, BR-13, AC-US4-5)**: BR-13 names only the task case. Should a HABIT-type plan item marked done also record the habit's completion for today, and a LEARNING_RESOURCE item set the card to Completed? When an item is set back to not done, should the source be reverted? **Recommended**: only TASK items propagate, one way (item done → task status Done with completedAt); un-ticking never changes the source; HABIT and LEARNING_RESOURCE items never change their source.
- **Q2 (FR-08.4, AC-US4-7, NFR-4)**: When does a plan become Completed: (a) when its end date-time has passed, whatever its items; (b) when all its items are done, even before the end; (c) whichever comes first? Can a plan whose items are all done before its start be Completed before it started? **Recommended**: (c), but never before the start: NOT_STARTED while now < start; COMPLETED once now ≥ end or (now ≥ start and all items done); otherwise IN_PROGRESS. Status is computed on read, so un-ticking an item before the end moves a plan back to IN_PROGRESS. History shows the percent done.
- **Q3 (FR-10.2, AC-US6-2)**: Which profile fields and preferences does Settings have, and what do they do? The PRD only says "user/profile information" and "e.g. notification behavior, default view" for a single user with no accounts. **Recommended**: `displayName` (optional, ≤ 100, used in the Dashboard greeting via `GET /api/settings`), `planStartNotifications` (on/off, default on; the frontend shows plan-start notifications only when on), `defaultPage` (one of the six pages, default Dashboard; the page the app opens on). All stored on the server. The app time zone is shown read-only from `GET /api/app-info`.
- **Q4 (Constitution I, T002, T003)**: Which exact versions of `org.jacoco:jacoco-maven-plugin` (must support Java 25) and `org.springdoc:springdoc-openapi-maven-plugin` should be pinned in `pom.xml`? The stack table says "latest release at setup", no project file names a number, and this session can't reach Maven Central. **Recommended**: the reviewer looks up the latest release of each on Maven Central today and writes both numbers here. (From memory, not checked: JaCoCo 0.8.14 was the first release with official Java 25 support, and springdoc-openapi-maven-plugin 1.5 was the latest known release. Newer releases may exist.)

## Analyze findings

`/speckit-analyze` over spec.md, plan.md and tasks.md against the constitution. CRITICAL and HIGH only:

| ID | Severity | Location | Finding | Resolution |
|---|---|---|---|---|
| C1 | CRITICAL (resolved) | tasks.md Phase 8 (former T082) | A web-slice test was scheduled *after* the controller it tests (Constitution III: tests before code) | Fixed in this session: its check moved into T077 (written first); T082 removed |
| H1 | HIGH | spec.md FR-07.5, FR-08.4, FR-10.2 | AC-US4-5, AC-US4-7 and AC-US6-2 can't be tested until Q1–Q3 are answered | Answer Q1–Q3; plan-apply writes the answers into spec.md |
| H2 | HIGH | plan.md Technical Context, tasks T002/T003 | Plugin versions not pinned, so phase-01 can't be implemented without a version picked from memory (Constitution I) | Answer Q4 |
| H3 | HIGH | spec.md FR-08.1, FR-10.1, NFR-2, UI parts of FR-09.1 and NFR-5 | UI-only requirements (plan-start notification, navigation, per-second countdown, greeting/quick-add, empty states) have no backend task or named backend test; Constitution III requires a named test per FR | Confirm they are covered by the frontend layer's Playwright checks. The backend supplies the data (startDateTime, status, restSeconds, settings) and T086 excludes them from the backend test inventory |

Re-run by plan-apply on 2026-09-24 after the answers: H1 and H2 resolved (no `[NEEDS CLARIFICATION]` left;
plugin versions pinned in plan.md/research.md). H3 is still open as a cross-layer note, not a CRITICAL finding. No CRITICAL findings.

No other constitution conflicts. Coverage: every BR-1..BR-14 and every backend FR has ≥ 1 task and a named test task.

## Unchecked checklist items

From `specs/001-quickflow-backend/checklists/requirements.md`:
- [ ] No [NEEDS CLARIFICATION] markers remain (Q1–Q3)
- [ ] All functional requirements have clear acceptance criteria (FR-07.5, FR-08.4, FR-10.2, answered by Q1–Q3)

Implement sessions stop on any unchecked checklist item, so plan-apply must tick both once Q1–Q3 are answered.

## Assumptions

Need approval before they are built (from spec.md A-1..A-12 and the plan):
- A-1 Single user, no sign-in, no authentication.
- A-2 One app time zone (`Africa/Cairo`) for "today", overdue, completed today, habit periods, plan windows.
- A-3 Deletes are permanent (hard deletes); archived tasks are the only restorable items.
- A-4 Task due date is a calendar date (no time) and optional; tasks without one are never overdue and sort last.
- A-5 Tasks get a `completedAt` time (not in the PRD data model), needed for "completed today".
- A-6 Learning card status is set by the user, not derived from its milestones.
- A-7 The plan builder offers non-archived tasks, active habits and all cards. The backend rejects only missing or deleted sources.
- A-8 Plan priority order: a positive integer, 1 = highest, not unique; ties ordered by start date-time.
- A-9 Limits not in the PRD: habit/card descriptions ≤ 2,000; card, milestone and plan titles ≤ 200; note text ≤ 5,000; estimated duration 1–100,000 minutes; displayName ≤ 100.
- A-10 Plan-start notification is in-app only (also shown on opening the app for plans that started while it was closed).
- A-11 Estimated duration is informational, not checked against the start/end window.
- A-12 The habit week runs Monday to Sunday; streak = consecutive periods with ≥ 1 completion.
- A-13 Habit completion may be recorded for today or a past date (not the future); a completion can be undone.
- A-14 A plan item whose source was deleted stays in the plan (shown as removed, keeps its title snapshot, still counts toward progress) (FR-07.6).
- A-15 Plan fields (title, duration, start/end, priority) are editable after creation; the item list is not.
- A-16 Task defaults: status TODO, priority MEDIUM; task list default sort = creation date, newest first; no pagination.
- A-17 API shape: REST under `/api`, RFC 9457 problem details with `errors[{field,message}]`, 409 for a duplicate completion or completing an inactive habit, enum values in UPPER_SNAKE_CASE (see contracts/openapi.yaml).

## Reviewer notes
- 2026-09-24T17:11:27+03:00: Q1: only TASK items propagate, one way: a TASK item marked done sets the task to Done (with completedAt); un-ticking never changes the source; HABIT and LEARNING_RESOURCE items never change their source. Q2: status computed on read: NOT_STARTED while now < start; COMPLETED once now >= end, or now >= start and all items done; otherwise IN_PROGRESS (un-ticking before the end moves it back to IN_PROGRESS). Q3: Settings = displayName (optional, <= 100 chars, used in the Dashboard greeting via GET /api/settings), planStartNotifications (on/off, default on; the frontend shows plan-start notifications only when on), defaultPage (one of the six pages, default Dashboard); stored on the server; time zone shown read-only from GET /api/app-info. Q4 (tooling, looked up on Maven Central via ./mvnw dependency:get ...:RELEASE on 2026-09-24 in the dry run and built with Java 25): org.jacoco:jacoco-maven-plugin 0.8.15, org.springdoc:springdoc-openapi-maven-plugin 1.5.
