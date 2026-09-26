# phase-01-test: US1 Tag tasks and filter by tag (backend-dev)

Layer: backend (API, `base_url` http://localhost:8080). Checks run by `phase-01-verify.sh <attempt-folder>`
with curl; raw requests/responses in `<attempt-folder>/curl.log`. Each check creates its own tasks
(`POST /api/tasks {"title": ...}`), so checks don't depend on each other.

Criteria this plan was written against (from `loops/backend-dev/outputs/002-us-tags/phase-01.md`):
AC-US1-1 .. AC-US1-6, as quoted below.

Swagger: `POST /api/tasks/{id}/tags` (`TaskTags {tags: [string]}`) → 200 `Task`;
`DELETE /api/tasks/{id}/tags?tag=` → 200 `Task`; `GET /api/tasks?tag=`; `Task.tags` required,
"Lower case, alphabetical; empty when the task has no tags". Validation errors: the project's
400 problem body `{status, title, detail, errors: [{field, message}]}`; `TaskTags.tags` "error field `tags`".

| check | criterion | steps | expected |
|---|---|---|---|
| C1 | AC-US1-1 "adds the tags "work" and "urgent" … the task shows both tags, and reading the task back returns both tags" | create A; `POST /api/tasks/A/tags {"tags":["work","urgent"]}`; `GET /api/tasks/A`; `GET /api/tasks` | POST 200 with `tags` = `["urgent","work"]` (FR-002 alphabetical); GET 200 with the same `tags`; A in the list with the same `tags` (FR-002 "single task and task list") |
| C2 | AC-US1-2 "filters the task list by "work" Then only the tasks tagged "work" are listed" | create W1 tagged `work`, W2 tagged `home`,`work`, N1 tagged `home`, N2 with no tags, N3 tagged `workshop`; `GET /api/tasks?tag=work`; `GET /api/tasks?tag=nosuchtag` | 200; the ids listed are exactly {W1, W2} (N3: whole-tag match, spec Assumptions); unknown tag → 200 `[]` (spec Edge Cases) |
| C3 | AC-US1-3 "removes the tag "work" Then the task no longer shows it and no longer appears when filtering by "work"" | create R tagged `work`,`home`; `DELETE /api/tasks/R/tags?tag=work`; `GET /api/tasks/R`; `GET /api/tasks?tag=work` | DELETE 200 with `tags` = `["home"]`; GET `tags` = `["home"]`; R not in the `work` filter |
| C4 | AC-US1-4 "add an empty tag or a tag longer than 30 characters Then the tag is rejected with a validation message and the task's tags are unchanged (BR-T1)" | create V tagged `alpha`; POST `[""]`, `["   "]`, a 31-char tag, `["beta", <31-char>]`; `GET /api/tasks/V`; then POST a 30-char tag | each invalid POST 400 with an `errors[]` entry on field `tags` with a non-empty message (FR-006 "naming the tags field"); `tags` still `["alpha"]` after all four (no `beta`: all or nothing, spec Edge Cases); 30-char tag → 200 (BR-T1 "1 to 30 characters") |
| C5 | AC-US1-5 "adds "Work" or filters by "WORK" … the task still has one such tag, and it is listed by the filter (BR-T2)" | create K tagged `work`; POST `["Work"]`; `GET /api/tasks/K`; `GET /api/tasks?tag=WORK` | POST 200 (Edge Cases: silent no-op) with `tags` = `["work"]`; GET `tags` = `["work"]`; K listed by `?tag=WORK` |
| C6 | AC-US1-6 "a task with 10 tags When the user adds another, different tag Then the tag is rejected with a validation message and the task's tags are unchanged (BR-T3)" | create F; POST `t01`..`t10` → 200 with 10 tags; POST `["t11"]`; `GET /api/tasks/F` | 400 with a non-empty `errors[].message` (FR-007); `tags` still the 10 `t01`..`t10` |
