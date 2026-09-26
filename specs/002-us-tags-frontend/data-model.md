# Data model: Task tags (frontend layer)

The frontend stores nothing; it uses the API models of
[specs/002-us-tags-backend/contracts/openapi.yaml](../002-us-tags-backend/contracts/openapi.yaml) through the
generated client (names checked after regeneration, research R-1).

## API models used

| Model | Fields used | Where |
|---|---|---|
| `Task` | existing fields + `tags: string[]` (lower case, alphabetical, 0–10) | Tasks page rows |
| `TaskTags` | `tags: string[]` (request body of `addTaskTags`) | tag add |
| `Problem` / `FieldError` | `detail`/`title`, `errors[{field: "tags" \| "tag", message}]` | error display (existing `readProblem`) |

## Operations used

| Operation | Call | Used for |
|---|---|---|
| `listTasks` | existing parameters + `tag` (blank → omitted) | list with tag filter (FR-004) |
| `addTaskTags` | `POST /api/tasks/{id}/tags` `{tags}` | add one or more tags (FR-001) |
| `removeTaskTag` | `DELETE /api/tasks/{id}/tags?tag=` | remove one tag (FR-005) |

## View state (Tasks page, `tasks.page.ts`)

| State | Type | Notes |
|---|---|---|
| `filters().tag` | `string`, default `''` | added to the existing `TaskFilters`; sent as `tag` when not blank; part of the "filtered" test for the empty message |
| `tagErrors` | `signal<Record<number, Record<string, string>>>` | per task id, the `tags` field message of the last failed add; cleared for that task on its next add/remove and on every list re-read |

## Tag component (`task-tags.ts`)

| Member | Kind | Notes |
|---|---|---|
| `task` | `input.required<Task>()` | the row's task |
| `errors` | `input<Record<string, string>>({})` | field messages for this row (`tags`) |
| `add` | `output<string[]>()` | the input's value split on `,`, parts as typed |
| `remove` | `output<string>()` | the tag to remove, as shown |
| `draft` | `linkedSignal(() => { task(); return ''; })` | resets when a new task object arrives (research R-5) |
