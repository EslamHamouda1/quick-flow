# phase-01-test: US1 Tag tasks and filter by tag (frontend-dev)

Layer: frontend (UI, `url` http://localhost:4200, page `/tasks`). Checks run with Playwright MCP
(headless). Selectors from `specs/002-us-tags-frontend/contracts/ui-contract.md` (plus the
001 ui-contract: `task-list`, `task-row-<id>`, `notice`, `empty-state`). Screenshots saved as
`<run_dir>/shot-<NN>-<check>.png`. "Given" tasks are created beforehand with
`POST /api/tasks {"title": ...}` (curl, logged in `<run_dir>/setup-curl.log`); every "When" is done in the
UI; "reading the task back" is `GET /api/tasks/<id>` after the UI action.

Criteria this plan was written against (from `loops/frontend-dev/outputs/002-us-tags/phase-01.md`):
AC-US1-1 .. AC-US1-6, as quoted below.

| check | criterion | Playwright MCP steps | expected |
|---|---|---|---|
| C1 | AC-US1-1 "adds the tags "work" and "urgent" to it, Then the task shows both tags, and reading the task back returns both tags" | task A (no tags); `/tasks`; type `work, urgent` in `task-tag-input-A`, click `task-tag-add-A`; snapshot; screenshot; reload `/tasks`; snapshot; `GET /api/tasks/A` | row A shows `task-row-tag` "urgent" and "work" (notice "Tags added"), still after reload; API `tags` = `["urgent","work"]` |
| C2 | AC-US1-2 "filters the task list by "work", Then only the tasks tagged "work" are listed" | tasks W1 [work], W2 [home, work], N1 [home], N2 [], N3 [workshop] (tagged via the UI add input); type `work` in `filter-tag`; snapshot; screenshot | `task-list` holds exactly W1, W2 (and A from C1, tagged work); N1, N2, N3 not listed |
| C3 | AC-US1-3 "removes the tag "work", Then the task no longer shows it and no longer appears when filtering by "work"" | task R tagged work, home (UI); clear filter; click `task-tag-remove` in R's "work" tag; snapshot; `filter-tag` = `work`; snapshot; screenshots | notice "Tag removed"; row R shows only "home"; R not listed under `work` |
| C4 | AC-US1-4 "tries to add an empty tag or a tag longer than 30 characters, Then the tag is rejected with a validation message and the task's tags are unchanged (BR-T1)" | task V tagged alpha (UI); click `task-tag-add-V` with empty input; snapshot; type a 31-char tag, click add; snapshot; `GET /api/tasks/V` | each time `error-tags` in row V with a non-empty message; row V tags still only "alpha"; API `tags` = `["alpha"]` |
| C5 | AC-US1-5 "adds "Work" or filters by "WORK", Then "Work" and "work" are treated as the same tag: the task still has one such tag, and it is listed by the filter (BR-T2)" | task K tagged work (UI); add `Work` to K; snapshot; `filter-tag` = `WORK`; snapshot; `GET /api/tasks/K` | row K has exactly one work tag (shown "work", FR-002 lower case); K listed under `WORK`; API `tags` = `["work"]` |
| C6 | AC-US1-6 "a task with 10 tags, When the user adds another, different tag, Then the tag is rejected with a validation message and the task's tags are unchanged (BR-T3)" | task F; add `t01, t02, …, t10` in the UI; add `t11`; snapshot; `GET /api/tasks/F` | after the 10: 10 `task-row-tag`; after `t11`: `error-tags` in row F with a message, still 10 `task-row-tag`, API `tags` = t01..t10 |

Unit tests: the frontend layer has no `test` command and no `coverage` (`unit-result.json` outcome `none`).
