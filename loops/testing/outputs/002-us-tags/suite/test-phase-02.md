# test-phase-02: US1 Tag tasks and filter by tag

## Goal
US1 (`US-TAGS-1`) is checked end to end: its acceptance criteria AC-US1-1..6, FR-001..FR-008 and BR-T1..BR-T4 through
the API with curl against `http://localhost:8080`, then the same behaviour on the Tasks page `/tasks` at
`http://localhost:4200` through Playwright MCP (headless), with the selectors of
`specs/002-us-tags-frontend/contracts/ui-contract.md`.

Sources: `examples/single-us/US-tags.md`, `specs/002-us-tags-backend/spec.md` (acceptance scenarios, Edge Cases,
FR, BR, SC), both layers' `requirements.json` (phase-00 answers Q1–Q3), the swagger, the ui-contract. Evidence:
`runs/002-us-tags/suite/test-phase-02/attempt-<n>/` (`curl.log`, `shot-<NN>-<check id>.png`).

Not checked here: FR-008 "they survive an app restart", because sessions never stop or start the app (the runner
does); it is listed with no suite check in the report's Requirement coverage.

## API checks (curl)
- [x] A1 AC-US1-1 / FR-001 / FR-002: create A; `POST /api/tasks/A/tags {"tags":["work","urgent"]}` → 200, `tags` = `["urgent","work"]`; `GET /api/tasks/A` and A in `GET /api/tasks` carry the same `tags`.
- [x] A2 AC-US1-2 / FR-004 / SC-004: W1 [work], W2 [home, work], N1 [home], N2 [], N3 [workshop]; `GET /api/tasks?tag=work` lists exactly W1, W2 (and A) — N3 not listed (whole-tag match, spec Assumptions); `?tag=nosuchtag` → 200 `[]` (Edge Cases).
- [x] A3 AC-US1-3 / FR-005: R [work, home]; `DELETE /api/tasks/R/tags?tag=work` → 200, `tags` = `["home"]`; GET agrees; R not in `?tag=work`; removing `nosuchtag` from R → 200, task unchanged (Q3 silent no-op); unknown task id → 404.
- [x] A4 AC-US1-4 / FR-006 / BR-T1 / SC-003: V [alpha]; POST `[""]`, `["   "]`, a 31-char tag, `["beta", <31-char>]` → each 400 with an `errors[]` entry on field `tags` and a non-empty message; `tags` still `["alpha"]` (all or nothing, Edge Cases); a 30-char tag → 200; `"  gamma "` → stored as `gamma` (Edge Cases, trim).
- [x] A5 AC-US1-5 / FR-003 / BR-T2: K [work]; POST `["Work"]` → 200, `tags` = `["work"]` (Q1 lower case, Q2 silent no-op); `?tag=WORK` lists K; POST `["Home"]` → stored and shown as `home`.
- [x] A6 AC-US1-6 / FR-007 / BR-T3 / SC-003: F with `t01`..`t10` → 200 with 10 tags; POST `["t11"]` → 400 with a non-empty message, `tags` still `t01`..`t10`; POST `["T05"]` → 200, still 10 (Q2 "counts once toward the 10").
- [x] A7 FR-004 combines with existing filters: `?tag=work` together with a priority / status / search filter lists only the tasks matching all of them; an archived task tagged `work` is not listed by `?tag=work` by default (BR-4, Edge Cases).
- [x] A8 BR-T4: editing (`PUT`), completing, archiving and restoring a tagged task leave its `tags` unchanged.
- [x] A9 FR-008 / BR-T4 delete: delete a tagged task → it is no longer listed by its tag filter; its tag and tag-remove calls on that id → 404 (Edge Cases).
- [x] A10 SC-002: every `addTaskTags`, `removeTaskTag` and `listTasks?tag=` call above has `time_total` < 0.500 s.

## UI checks (Playwright MCP, `/tasks`)
Tasks for the Given parts are created through `POST /api/tasks` from the page's own origin; every tag action is done
in the UI.
- [x] U1 Reachability: the Tasks page is reachable from the app shell navigation; `filter-tag` (label "Tag"), `task-row-tags` in every row (also an untagged one), `task-tag-input-<id>` (placeholder "Add tags, comma-separated") and `task-tag-add-<id>` ("Add tag") are present.
- [x] U2 AC-US1-1: type `work, urgent` in `task-tag-input-A`, click `task-tag-add-A` → notice "Tags added", input emptied, row shows `task-row-tag` "urgent" then "work"; still shown after a reload; API `tags` = `["urgent","work"]`.
- [x] U3 AC-US1-2: `filter-tag` = `work` → `task-list` holds only the rows tagged "work"; `filter-tag` = a tag no task has → `empty-state` "No tasks match" (ui-contract).
- [x] U4 AC-US1-3: click `task-tag-remove` (text "×", `aria-label` "Remove tag work") in R's "work" tag → notice "Tag removed", R shows only "home"; with `filter-tag` = `work` R is not listed.
- [x] U5 AC-US1-4: add an empty value, then a 31-char tag → each time `error-tags` in that row with a message, the row's tags unchanged, the input keeps its text; API tags unchanged.
- [x] U6 AC-US1-5: add `Work` to a task tagged "work" → still one "work"; `filter-tag` = `WORK` lists it.
- [x] U7 AC-US1-6: a task with 10 tags, add `t11` → `error-tags`, still 10 `task-row-tag`; API tags unchanged.
- [x] U8 ui-contract: Enter in `task-tag-input-<id>` adds like the button; `filter-tag` is disabled in the Overdue view; no console errors besides the expected 400s of U5/U7.
