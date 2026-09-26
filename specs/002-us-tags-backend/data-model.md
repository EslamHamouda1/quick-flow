# Data Model: Task tags

Extends the `Task` entity of `specs/001-quickflow-backend/data-model.md`; every other field and rule
of the task stays unchanged.

## Task (changed)

| Field | Type | Rules |
|---|---|---|
| tags | set of string, 0..10 | BR-T1 each 1–30 characters after trim; BR-T2 unique ignoring case (stored lower case, R-2 / Q1); BR-T3 at most 10; returned sorted alphabetically |

Operations (domain, `com.quickflow.domain.task.Task`):

| Operation | Effect | Errors |
|---|---|---|
| `addTags(values, now)` | trims each value, merges it into the set ignoring case; `updatedAt = now` if the set changed | `ValidationException` on field `tags`: blank, > 30 characters, total > 10. All or nothing. |
| `removeTag(value, now)` | removes the tag matching the value ignoring case; `updatedAt = now` if it was there | `ValidationException` on `tag` when blank; absent tag: no-op (R-6 / Q3) |
| `update`, `changeStatus`, `complete`, `archive`, `restore` | unchanged; never touch `tags` (BR-T4) | — |

## Table `task_tag` (new, JPA element collection of `Task`)

| Column | Type | Constraint |
|---|---|---|
| task_id | bigint | FK → `task.id`, not null |
| tag | varchar(30) | not null |

- Unique `(task_id, tag)`; index on `tag` (filter).
- Rows are deleted when their task is deleted (owned collection), so no tag of a deleted task is ever
  returned (BR-14, BR-T4, FR-008).

## TaskQuery (changed)

`TaskQuery(q, status, priority, dueFrom, dueTo, archived, tag, sort, direction)`: `tag` null or
blank = no tag filter; otherwise only tasks that have that tag (trimmed, ignoring case) are listed,
combined with the other filters by AND (FR-004).
