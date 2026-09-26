# phase-00 review: Plan (frontend layer)

Feature `002-us-tags`, `FEATURE_DIR` = `specs/002-us-tags-frontend`. `spec.md` and `checklists/` were copied from
`specs/002-us-tags-backend/` (the spec already carries the reviewer's answers Q1–Q3 of 2026-09-26). Planned artifacts:
`plan.md`, `research.md`, `data-model.md`, `contracts/ui-contract.md`, `quickstart.md`, `tasks.md`.

Note: the spec-kit scripts (`setup-plan.sh`, `setup-tasks.sh`, `check-prerequisites.sh`) could not be run in this
session (they need approval); `.specify/feature.json` still points at `specs/001-quickflow-backend`, so it was not
used. The plan, tasks and analyze steps were done by hand against `.specify/templates/`, writing only inside
`specs/002-us-tags-frontend/`.

## Swagger input
`specs/002-us-tags-backend/contracts/openapi.yaml` (OpenAPI 3.1, planning). From implementation on, the client is
regenerated from `loops/backend-dev/outputs/openapi.json` by `layers.frontend.generate_client` (task T001).

## Phases

| Phase | Title | Story | Tasks | depends_on |
|---|---|---|---|---|
| phase-01 | US1 Tag tasks and filter by tag | US1 | T001–T005 | backend-dev:US1 |
| phase-02 | Polish & Cross-Cutting Concerns | – | T006–T007 | phase-01 |

No Setup or Foundational phase: the 001 frontend (scaffold, proxy, client wiring, error mapping, notices, Tasks page)
is already in place, so phase-01 has no Foundational phase to depend on.

## Answered questions
None were open. `spec.md` has no `[NEEDS CLARIFICATION]` marker left. The UI choices the spec leaves open are listed under
Assumptions below (FA-T2 is the one that limits what a user can type).

## Analyze findings
No CRITICAL or HIGH findings. For reference (MEDIUM/LOW):
- MEDIUM U1: the comma separator (FA-T2) means a tag containing a comma can't be entered from the UI, although the
  spec's assumptions allow any character inside a tag.
- LOW: FR-008 (tags kept) is server-only; checked in the UI by reloading `/tasks` (AC-US1-1). SC-002 (< 500 ms) has no
  measuring task; each action is one request plus one re-read. The tag filter is disabled in the Overdue view, as all
  other filters already are (`listOverdueTasks` has no `tag` parameter).

Coverage: FR-001..FR-007 and AC-US1-1..6 all map to T002–T004 (FR-008 is backend). 8 requirements, 7 tasks.

## Unchecked checklist items
None (`checklists/requirements.md`: all items checked).

## Assumptions
UI choices not written in the spec, built only once approved:
- **FA-T1 – where tags are shown and managed**: on the Tasks page (`/tasks`) only, inside each task row: the tags as
  chips in the order the API returns them (lower case, alphabetical), each with a "×" remove button, plus an inline
  add input and "Add tag" button. Selectors in `specs/002-us-tags-frontend/contracts/ui-contract.md`.
- **FA-T2 – several tags in one action (FR-001)**: the add input splits its value on commas; every part is sent as
  typed in one `addTaskTags` request (the server trims and validates). Consequence: a tag containing a comma can't be
  entered from the UI. Alternative if rejected: one tag per add (would not meet "one or more tags in one action").
- **FA-T3 – after adding**: success → notice "Tags added", input emptied, list re-read with the current filters.
  Rejected (400) → the server's message for field `tags` under that row's input (`error-tags`), input text kept, list
  not re-read (tags unchanged). No client-side checks (empty, length, count, case): the server's messages are shown,
  as the task form already does (001 FA-3).
- **FA-T4 – removing**: the "×" removes the tag at once, no confirmation (like task delete, 001 FA-7); notice "Tag
  removed", list re-read.
- **FA-T5 – tag filter**: a text input "Tag" (`filter-tag`) in the toolbar, exact whole-tag match done by the server
  (`tag` query parameter, blank = none), re-read on every input like the search box; disabled in the Overdue view; a
  non-blank tag makes the empty list say "No tasks match".
- **FA-T6 – dashboard**: the dashboard does not show tags (not asked by the story), although the API returns them.
- **FA-T7 – task form**: tags are not edited in the Add/Edit task form; they are managed only from the row (the API
  has separate tag endpoints).
- **FA-T8 – no frontend unit tests**: Constitution III makes them optional and `layers.frontend.test` is empty; the
  story is verified by the testing loop with Playwright MCP from the ui-contract.
- **FA-T9 – archived tasks**: archived rows (shown with the Archived filter) show their tags and can add/remove them
  like any row; the API has no rule against it and BR-T4 only says archiving doesn't change tags.

## Plan apply (2026-09-26)
- Approval: `auto` (--auto-approve), no reviewer notes; assumptions FA-T1..FA-T9 approved as written.
- Phase files match `tasks.md` (T001–T007); no task edits to sync. Checklist already fully checked.
- `/speckit-analyze` re-run (by hand, artifacts unchanged): no CRITICAL or HIGH findings; MEDIUM U1 and the LOW items
  above stand. Phase-00 set `done`.
