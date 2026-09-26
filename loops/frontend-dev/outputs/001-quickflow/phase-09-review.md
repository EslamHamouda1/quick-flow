# phase-09 review: Polish
layer: frontend
story_id: none
spec_phase: 9
depends_on: [phase-03, phase-04, phase-05, phase-06, phase-07, phase-08]

## Goal
Selectors match the UI contract, no business rules in components, docs up to date.

## Tasks
- [ ] T043 Check every selector of specs/001-quickflow-frontend/contracts/ui-contract.md exists in the templates under frontend/src/app/ and fix the templates (never the contract) where one is missing
- [ ] T044 [P] Check no component computes a status, overdue flag, streak, progress or dashboard figure (Constitution IV) by reading frontend/src/app/pages/ and move any such logic to the API response it belongs to (raise a question if the API lacks the field)
- [ ] T045 [P] Update specs/001-quickflow-frontend/quickstart.md if any command, route or path changed during implementation

T044 and T045 are `[P]` (one subagent each at implement); T043 runs on its own first.

## Acceptance criteria
None of its own (Polish). The story ACs of phases 03–08 must still hold after any change.

## Files
- T043: `frontend/src/app/app.html`, `frontend/src/app/pages/**`, `frontend/src/app/shared/**`: edited only if a
  contract selector is missing. Pre-check done now (a grep of every `data-testid` / `[attr.data-testid]` under
  `frontend/src/app/` outside `api/`): every selector of every section of `ui-contract.md` (Shell, Tasks, Habits,
  Learning, Plans, Dashboard, Settings) was found, including the dynamic ones (`error-<field>` via
  `shared/field-error.ts`, `error-items` in `plan-builder.ts`, `nav-<id>`, `plan-rest-time` in
  `shared/rest-time.ts`, `notice` / `notice-error` in `app.html`). So T043 is expected to change no file; the
  implement session repeats the check and records the result in `progress.md`. Extra selectors not in the
  contract (`card-description-text`, `habit-name-text`, `plan-title-text`, `plan-items`,
  `plan-estimated-duration`) stay; the contract is never edited.
- T044: `frontend/src/app/pages/**`: edited only if a rule is computed in a component. Pre-check done now: the
  pages show the API fields as returned (`overdue`, `currentStreak`, `doneForCurrentPeriod`, `progressPercent`,
  `doneItems`/`totalItems`, `milestonesDone`/`milestonesTotal`, `taskCompletionPercent`, `habitCounts`, `learning.*`,
  `status`). Presentation-only mappings found: `habits.page.ts` `periodDone()` (API `doneForCurrentPeriod` +
  `frequency` → label text) and `shared/rest-time.ts` (`endDateTime − clock.now()` countdown, approved in research
  R-4, shown only while the API `status` is `IN_PROGRESS` and `restSeconds` is not null). Neither decides a rule, so
  T044 is expected to change no file.
- T045: `specs/001-quickflow-frontend/quickstart.md`: its commands (generate client, `npm run build`, `npm start`),
  URL `http://localhost:4200`, `proxy.conf.json` (exists, checked now) and routes match `project.config.yaml` and
  `app.routes.ts`; edited only if a difference is found at implement.
- app-wide: `loops/frontend-dev/outputs/ui-url.md` rewritten unchanged (step I.8). `generate_client` is re-run
  (swagger unchanged since phase-08; the generated files should come out the same).
- No endpoint is added or changed.

## Planned checks
- Implement session (quick feedback only): `cd frontend && npm run build` succeeds; output in
  `loops/frontend-dev/runs/001-quickflow/phase-09-build.log`.
- No unit tests: the frontend layer has no `coverage` and `test` is empty (FA-1, approved).
- Testing loop (Playwright MCP, headless) can check:
  - every selector of `ui-contract.md` is present on its route once there is data (a task, a habit, a card with a
    milestone and a note, a plan with items, settings), including `empty-state` / `empty-state-action` on an empty
    list, `notice` / `notice-error`, and `error-title` after an invalid save.
  - a smoke run of each page (`/dashboard`, `/tasks`, `/habits`, `/learning`, `/plans`, `/settings`) via `nav-*`,
    `page-title` text equal to the nav label, no console errors.
  - quickstart steps 1–3 work as written.

## Risks
- **Regression from edits**: if T043/T044 do change a template, a story that already passed could break; the
  testing loop's smoke run over every page covers this. Per the pre-check no edit is expected.
- **Parallel subagents**: T044 (pages) and T045 (quickstart.md) touch different files, so they cannot conflict.
- **A missing API field** (T044): if a rule turns out to be computed in the UI with no API field for it, the phase
  goes back to `awaiting_approval` with a question (the backend layer is not edited from here).

## Assumptions
- **FA-42 (new) Presentation mappings allowed**: turning an API value into display text (`periodDone()` label,
  `H:MM:SS` countdown from `endDateTime`, `%` suffix) is not a business rule under Constitution IV and stays in the
  components.
- **FA-43 (new) Extra selectors kept**: `data-testid`s beyond the contract are kept; T043 only adds missing ones.
- FA-1, FA-3, FA-5 and FA-11 apply as approved.

## Open questions
None.

## Swagger input
`loops/backend-dev/outputs/openapi.json` (from task.md).

## Contract differences
None for this phase: Polish has no story and uses no new endpoint. The per-story differences were recorded in the
phase-03..08 reviews.
