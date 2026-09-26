# Research: Task tags (frontend layer)

No `[NEEDS CLARIFICATION]` is left in spec.md (Q1–Q3 were answered at the backend phase-00 review, 2026-09-26).
The decisions below cover what the spec leaves to the UI; each UI choice is also listed as an assumption (FA-T*) in
`loops/frontend-dev/outputs/002-us-tags/phase-00-review.md`.

## R-1 Generated client names

- **Decision**: regenerate the client with `generate_client` at the start of the story phase and use only the names
  found in `frontend/src/app/api/`. Expected from the contract: `Task.tags: Array<string>`, model `TaskTags { tags }`,
  `TasksService.addTaskTags(id, taskTags)`, `TasksService.removeTaskTag(id, tag)`, and a new `tag` parameter of
  `TasksService.listTasks`.
- **Rationale**: the generator reads the backend's generated `loops/backend-dev/outputs/openapi.json`, not the
  planning contract; operation ids, parameter names and **the position of `tag` among `listTasks`' positional
  parameters** follow that file. Today's client (checked 2026-09-26) has `listTasks(q, status, priority, dueFrom,
  dueTo, archived, sort, direction)` and no tag operations, because the backend story isn't built yet.
- **Alternatives**: hand-written `HttpClient` calls (rejected: the constitution says the client is generated).
- **Checked after regeneration (T001, 2026-09-26)** in `frontend/src/app/api/`:
  - `model/task.ts`: `Task.tags: Array<string>` (required).
  - `model/taskTags.ts`: `interface TaskTags { tags: Array<string> }`, exported from `model/models.ts`.
  - `api/tasks.service.ts`: `addTaskTags(id: number, taskTags: TaskTags): Observable<Task>`,
    `removeTaskTag(id: number, tag: string): Observable<Task>`, and
    `listTasks(q?, status?, priority?, dueFrom?, dueTo?, archived?, tag?: string, sort?, direction?)`: `tag` is the
    **7th** positional parameter (after `archived`, before `sort`).
  - Callers of `listTasks`: `pages/tasks/tasks.page.ts` `reload()` (positional, updated in T003) and
    `pages/plans/plan-builder.ts` (no arguments, unaffected).
  - `contracts/ui-contract.md` "Acceptance criteria → selectors" covers AC-US1-1..6. Nothing missing.

## R-2 Error mapping

- **Decision**: a 400 from `addTaskTags` shows `errors[field=tags]` (contract: "error field `tags`") under the row's
  tag input as `error-tags`; any other field or a 400 without field errors goes to the notice area (`readProblem`).
  A 400 from `removeTaskTag` (field `tag`) and any 404 go to the notice area and the list is re-read.
- **Rationale**: same pattern as the task form (`error-<field>` per 001 ui-contract); the existing `readProblem`
  and `FieldErrorComponent` are reused.

## R-3 Several tags in one action (FR-001)

- **Decision**: one text input per row; its value is split on commas and every part is sent as typed (no trimming,
  no dropping of empty parts, no case change) in one `addTaskTags` request. An empty input sends `[""]`, which the
  server rejects as an empty tag (AC-US1-4).
- **Rationale**: FR-001 asks for one or more tags in one action; the server trims and validates (BR-T1), so the UI adds
  no rule. Consequence: a tag containing a comma can't be entered from the UI (spec assumption allows any character;
  this is FA-T2 and needs approval).
- **Alternatives**: one tag per action (rejected: FR-001); a staged chip list before one "Add" (rejected: more UI for
  the same request).

## R-4 Keeping the list correct after a change

- **Decision**: after a successful add or remove, re-read the list with the current filters (as the existing row
  actions do via `act()`), rather than patching the row from the response.
- **Rationale**: AC-US1-3 requires the task to drop out of a "work" filter once "work" is removed; a re-read gives that
  from the server. On a failed add the list isn't re-read, so the shown tags stay unchanged.

## R-5 Clearing the add input

- **Decision**: the tag component keeps its draft in a `linkedSignal` derived from the `task` input, so it resets
  whenever the page passes a new task object (after the re-read that follows a success) and is kept after a 400
  (no re-read).
- **Rationale**: no extra "reset" signalling between page and component; `linkedSignal` is already used in
  `task-form.ts` (Angular 22.2.0).
