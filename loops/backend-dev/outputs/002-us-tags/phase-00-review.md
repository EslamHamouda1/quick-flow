# phase-00 review: Plan (002-us-tags, backend)

Requirements: `examples/single-us/US-tags.md` (US-TAGS-1, mapped to `US1`).
Feature dir: `specs/002-us-tags-backend` (spec.md, plan.md, research.md, data-model.md,
contracts/openapi.yaml, quickstart.md, tasks.md, checklists/requirements.md).
No sibling spec existed (`specs/002-us-tags-frontend` is absent), so spec.md was written from the
requirements file. The contract `contracts/openapi.yaml` (OpenAPI 3.1) covers every endpoint of US1
and is the frontend layer's swagger input (`contract_from: backend`).

## Phases
| phase | title | story | tasks | depends_on |
|---|---|---|---|---|
| phase-01 | US1 Tag tasks and filter by tag | US1 | T001–T010 (T001–T004 tests first) | [] |
| phase-02 | Polish & Cross-Cutting Concerns | none | T011–T012 | [phase-01] |

There is no Setup or Foundational phase: the backend of feature 001 is in place and tags only
extend the task package.

## Open questions
- Q1 (FR-002, BR-T2): Which spelling does the user see for a tag: always lower case ("Work" is stored and shown as "work"), or the spelling first used on that task? **Recommended: always lower case** (one spelling for storage, uniqueness, filter and display; research R-2).
- Q2 (FR-003): Is adding a tag the task already has (e.g. "Work" when it has "work") a silent no-op that returns the task unchanged, or is it rejected with a validation message? **Recommended: silent no-op** (200, tag kept once, counts once toward the 10; research R-5).
- Q3 (FR-005): Is removing a tag the task doesn't have a silent no-op (success, task unchanged) or a "not found" error? **Recommended: silent no-op** (200 with the task unchanged; an unknown task id is still 404; research R-6).

## Analyze findings
- HIGH (Constitution V): the 3 questions above are unresolved `[NEEDS CLARIFICATION]` markers in spec.md. The plan, contract (parts marked "Qn") and tasks (T001, T005, marked Q1/Q2/Q3) use the recommended answers; plan_apply must update them if an answer differs.
- No CRITICAL findings. Coverage: every FR-001..FR-008 and BR-T1..BR-T4 maps to at least one test task (T001–T004) and one implementation task (T005–T010); every AC-US1-1..6 is in quickstart.md.
- Fixed during analysis: the contract had a `maxLength: 30` on the `tag` list filter that no task enforces (removed; an unknown tag gives an empty list), and a per-item `maxLength` on the add-tags body that would reject a padded tag the domain accepts after trimming (moved to the description; the rule is checked in the domain after trim).

## Unchecked checklist items
- `checklists/requirements.md`: "No [NEEDS CLARIFICATION] markers remain" (Q1–Q3 above). All other items are checked.

## Assumptions
Not written in the requirements; built only once approved.
- A1 (research R-3): API shape: `POST /api/tasks/{id}/tags` `{"tags":[...]}` adds (200, returns the Task); `DELETE /api/tasks/{id}/tags?tag=...` removes one (200, returns the Task; query parameter, not a path segment, so tags with `/`, `%` or spaces work); `GET /api/tasks?tag=...` filters; `tags` is added to the `Task` schema, so every endpoint returning a task (including the dashboard) returns its tags. `PUT /api/tasks/{id}` does not touch tags.
- A2 (research R-4): adding is all or nothing: one invalid tag, or a total over 10, rejects the whole request. Errors use the existing problem-detail shape with `errors[].field` = `tags` (add) or `tag` (remove with a blank value). A missing or empty `tags` array is a 400.
- A3 (spec Edge Cases / R-4): spaces around a tag are trimmed; a tag of only spaces counts as empty; any characters are allowed inside a tag (including inner spaces).
- A4 (spec Assumptions): the filter takes one tag and matches the whole tag ignoring case ("wor" doesn't match "work"); it combines with the existing filters by AND; archived tasks stay excluded by default; a blank `tag=` is ignored.
- A5 (research R-8): adding or removing a tag sets the task's `updatedAt` when the tag set changes; a no-op doesn't.
- A6 (spec Assumptions): tags are managed only on the task: no tag list endpoint, no rename, no colours; existing tasks start with no tags.
- A7 (research R-1): tags are stored in a new table `task_tag (task_id, tag)` as an eager JPA element collection of `Task`, created by Hibernate `ddl-auto=update` on the existing H2 file database (no migration tool in this project), deleted with the task.
- A8 (process): the spec-kit shell scripts (`setup-plan.sh`, `setup-tasks.sh`, `check-prerequisites.sh`) were not run in this session, because they persist `.specify/feature.json`, which is outside the backend-dev plan whitelist; their only other effect (copying the plan template into `FEATURE_DIR`) was done by hand. `.specify/feature.json` still points at `specs/001-quickflow-backend`; later sessions must rely on `SPECIFY_FEATURE_DIRECTORY` (exported by the runner).

## Reviewer notes
- 2026-09-26T22:47:11+03:00: Q1: tags are always lower case (stored, matched, filtered and shown as lower case). Q2: adding a tag the task already has is a silent no-op (200, task unchanged, counts once toward the 10). Q3: removing a tag the task doesn't have is a silent no-op (200, task unchanged); an unknown task id is still 404.

