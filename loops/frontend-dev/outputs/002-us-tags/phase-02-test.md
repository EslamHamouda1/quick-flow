# phase-02-test: Polish & Cross-Cutting Concerns (frontend-dev)

Layer: frontend (UI, `url` http://localhost:4200, page `/tasks`). Checks run with Playwright MCP (headless).
Selectors from `specs/002-us-tags-frontend/contracts/ui-contract.md` (plus the 001 ui-contract:
`task-list`, `task-row-<id>`, `notice`, `task-view-overdue`). Screenshots saved as
`<run_dir>/shot-<NN>-<check>.png`. "Given" tasks are created beforehand with `POST /api/tasks {"title": ...}`
(result in `<run_dir>/setup-tasks.json`); every "When" is done in the UI; "reading the task back" is
`GET /api/tasks/<id>` after the UI action.

Criteria this plan was written against (from `loops/frontend-dev/outputs/002-us-tags/phase-02.md`):
the phase has **no acceptance criteria of its own** (`## Acceptance criteria` is empty). The checks come from
its `## Goal` ("Check the tag UI against the UI contract and the swagger, and finish with a clean build"),
tasks T006/T007, and the review's statements "The phase must not change the behaviour already verified for
AC-US1-1..6 in phase-01" and "Verification is done by the testing loop (Playwright MCP, headless) re-running
AC-US1-1..6 with the selectors of `contracts/ui-contract.md`".

| check | source | Playwright MCP steps | expected |
|---|---|---|---|
| C1–C6 | review "must not change the behaviour already verified for AC-US1-1..6" | phase-01 C1–C6 exactly as in `phase-01-test.md` | as in `phase-01-test.md` |
| C7 | T006 + ui-contract "Selector / Element" table | on `/tasks` with tasks from C1–C6 and one untagged task: read `filter-tag` label, `task-row-tags` of the untagged row, `task-tag-remove` text/aria-label, `task-tag-input-<id>` placeholder, `task-tag-add-<id>` text; click `task-view-overdue` and read `filter-tag` disabled state | `filter-tag` label "Tag"; `task-row-tags` present in a row with no tags; remove button text "×", `aria-label` "Remove tag <tag>"; placeholder "Add tags, comma-separated"; button "Add tag"; `filter-tag` disabled in the Overdue view |
| C8 | ui-contract "pressing Enter in `task-tag-input-<id>` does the same"; "After a successful add … the notice "Tags added" … the add input is emptied"; "After a rejected add … the input keeps its text" | type `enter1` in an input and press Enter; read the notice and input right away; `GET /api/tasks/<id>` | tag `enter1` added (row + API), notice "Tags added", input empty (the rejected-add input keeping text is read in C4) |
| C9 | T007 "Run `cd frontend && npm run build` with no errors and save the output to loops/frontend-dev/runs/002-us-tags/phase-02-build.log" | read the build log | log exists, contains "Application bundle generation complete", no `error TS` / `✘ [ERROR]` |

Unit tests: the frontend layer has no `test` command and no `coverage` (`unit-result.json` outcome `none`).
