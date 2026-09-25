# phase-06-test: US4 Build and follow Todo Plans (frontend-dev)

Criteria listed in `loops/frontend-dev/outputs/001-quickflow/phase-06.md` (compare on later attempts):
AC-US4-1 … AC-US4-13, text as in `specs/001-quickflow-frontend/spec.md` lines 120–132.

Selectors from `specs/001-quickflow-frontend/contracts/ui-contract.md` (Todo Plans `/plans`). Visible texts only where
the criteria, spec or contract state them. Server-side values (items, done flags, status, progress, createdAt, source
state) are read back through the API on `http://localhost:8080` as supporting evidence. Source entities (tasks,
habits, learning cards) and plans with past start times are set up through the API (the builder can also do the
latter; the API is used so the times are exact).

Scope note: AC-US4-4's "and on the Dashboard" part is checked in frontend phase-07 (US5: AC-US5-4, FR-08.5, T038),
where the approved phase plan (`phase-00-review.md`) builds the Dashboard; in this phase the Dashboard is still the
placeholder. This plan checks the Todo Plans half.

All steps use Playwright MCP (headless) on `http://localhost:4200`. Screenshots go to
`<run_dir>/shot-<NN>-<check id>.png`.

| check | criterion | Playwright MCP steps | expected |
|---|---|---|---|
| C1 | AC-US4-13 | fresh DB; navigate `/plans`; read the empty state; click its action; also `/plans?add=1` | an empty state guiding to the Create Plan action; its action opens `plan-builder`; `?add=1` opens `plan-builder` |
| C2 | AC-US4-1, AC-US4-6 | API: a task, an archived task, an active habit, an inactive habit, a learning card. `plan-create` → builder; tick task, habit, card; title, duration, start in the future, end after it, priority 1 → `plan-save`; API `GET /api/plans/{id}` | builder offers the task, active habit and card (FR-07.3 "existing" items; contract: non-archived tasks, active habits, all cards); new `plan-<id>` in `plans-active` with `plan-status` "Not Started", `plan-progress` "0%", no `plan-rest-time`, 3 unchecked `plan-item-done-*`; API: 3 items `done: false`, `createdAt` set, status `NOT_STARTED`, `restSeconds` null |
| C3 | AC-US4-2 | builder with nothing ticked → `plan-save`; then builder open, delete a source through the API, tick it → `plan-save` | plan rejected both times: an error shown, no plan added (API list unchanged) |
| C4 | AC-US4-3 | builder with end = start, then end before start → `plan-save` | rejected with a message naming the end date-time; no plan added |
| C5 | AC-US4-7 | API: plan with start in the past, end ~2 h ahead; reload `/plans`; read `plan-rest-time` twice a few seconds apart | `plan-status` "In Progress"; `plan-rest-time` `H:MM:SS` ≈ end − now and lower on the second read |
| C6 | AC-US4-4, AC-US4-5, FR-08.4 | on the In Progress plan (task, habit, card items) tick the task item, then the habit item; un-tick the task item; tick all items | progress (done)/(total) shown at once ("33%", "67%", "33%"); API item flags saved; task becomes DONE on tick and stays DONE on un-tick; habit (no completion) and card (status) unchanged; all done → plan Completed, in `plans-completed` with "100%" |
| C7 | AC-US4-8 | reload `/plans`; compare every card's status and percent with `GET /api/plans` | same status and percent as the API after reopen |
| C8 | AC-US4-9 | API: plan with start ~1 min ahead; page open, no reload; wait past the start | a notice/highlight for that plan (`notice`, `plan-started-<id>`); card turns "In Progress" with `plan-rest-time` |
| C9 | AC-US4-10 | API: active plans with priority 2, 1, 3 (plus the existing ones); open `/plans` | two groups `plans-active` / `plans-completed`; active ordered by priority order; each card shows progress, rest time only when In Progress, items with done toggles and `plan-delete-<id>` |
| C10 | AC-US4-11 | API: plan with start and end in the past, 1 of 3 items done; open `/plans` | in `plans-completed` with "33%" and no rest time |
| C11 | AC-US4-12 | `plan-delete-<id>` on a plan with task/habit/card items; API get plan and its sources | card gone; `GET /api/plans/{id}` 404 and not listed; task, habit and card unchanged |
| C12 | FR-07.3 (edit) | `plan-edit-<id>` → builder prefilled; change title and priority → `plan-save` | card shows new title and "Priority <n>"; API items unchanged |
| C13 | FR-07.6 | delete (API) the task used by a plan's item; reload `/plans` | the item stays with `plan-item-removed-<itemId>` and still counts in `plan-progress` |

Unit tests: the frontend layer has no `test` command and no `coverage` (`unit-result.json` outcome `none`).
