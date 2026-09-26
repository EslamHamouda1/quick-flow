# Research: Task tags

Decisions for `plan.md`. Items marked **(assumption)** are not written in the requirements or the
spec; they are listed in the phase-00 review under `## Assumptions` and are built only once approved.
Items marked **(Qn)** depend on an open question in `spec.md`; the recommended answer is used here
and the decision changes with the reviewer's answer.

## R-1 Storage: a collection table owned by the task
- **Decision**: `Task.tags` is a JPA `@ElementCollection(fetch = EAGER)` of `String` in table
  `task_tag (task_id, tag)`, with a unique constraint on `(task_id, tag)` and an index on `tag`.
  Created by Hibernate (`ddl-auto=update` / `create-drop`, checked in
  `backend/src/main/resources/application*.properties`).
- **Rationale**: Tags have no life of their own (spec Key Entities); an element collection is deleted
  with its owner (BR-T4, FR-008) and needs no repository. `EAGER` because the app runs with
  `spring.jpa.open-in-view=false` and `TaskResponse.from` is called in the controller (and in
  `DashboardResponse`) outside the transaction, so a lazy collection would fail there.
- **Alternatives**: a `Tag` entity shared between tasks (many-to-many): adds orphan clean-up and a
  tag lifecycle the spec doesn't ask for; a single comma-separated column: can't be filtered
  exactly without string tricks and breaks on tags containing commas.

## R-2 Letter case (Q1)
- **Decision** (recommended answer to Q1): tags are stored and returned in lower case
  (`toLowerCase(Locale.ROOT)` after trimming). The filter lower-cases its value the same way.
- **Rationale**: one spelling makes "Work" = "work" hold everywhere (storage, uniqueness, filter,
  display) with no extra column.
- **If Q1 is answered "first spelling used"**: store the trimmed spelling, compare with
  `lower(tag)` in the domain check and the filter; the unique constraint then moves to the domain only.

## R-3 API shape
- **Decision**: `POST /api/tasks/{id}/tags` with body `{"tags": ["work", "urgent"]}` adds tags and
  returns the updated `Task` (200). `DELETE /api/tasks/{id}/tags?tag=work` removes one tag and returns
  the updated `Task` (200). `GET /api/tasks?tag=work` filters. `tags` (array of strings, alphabetical)
  is added to the `Task` schema, so every endpoint returning a task returns its tags.
- **Rationale**: the ACs speak of adding tags and removing a tag, not replacing the set; returning
  the task matches the other task actions (`/complete`, `/archive`, `/restore`). The tag to remove
  is a query parameter, not a path segment, because a tag may contain characters (`/`, `%`, spaces)
  that a path segment can't carry safely.
- **Alternatives**: `PUT /api/tasks/{id}/tags` (replace the whole set): the frontend would have to
  resend all tags to remove one; tags inside `PUT /api/tasks/{id}`: would change the 001 contract and
  make every edit rewrite tags (against BR-T4).

## R-4 Validation (BR-T1, BR-T3) **(assumption for the details)**
- **Decision**: in `Task.addTags(List<String>, Instant)`: each value is trimmed; `null`, empty or
  over 30 characters → field error `tags` ("must not be blank" / "must be at most 30 characters");
  values are merged case-insensitively with the task's tags; more than 10 in total → field error
  `tags` ("must have at most 10 tags"). Any error → `ValidationException`, nothing changes (all or
  nothing). A request body without `tags` or with an empty array → 400 on `tags` ("must not be
  empty"). The filter's `tag` value is trimmed; an empty `tag=` is ignored (as a blank `q` is today).
- **Rationale**: AC-US1-4 / AC-US1-6 say "the task's tags are unchanged", so a batch is all or
  nothing; the error body is the existing RFC 9457 shape with `errors[].field`.

## R-5 Adding a tag the task already has (Q2)
- **Decision** (recommended answer to Q2): a silent no-op: the tag stays once, the request succeeds
  (200) and returns the task; a repeated tag in the same request counts once. It counts toward the
  limit only once.
- **If Q2 is answered "rejected"**: 400 with field error `tags` ("already on the task"), tags unchanged.

## R-6 Removing a tag the task doesn't have (Q3)
- **Decision** (recommended answer to Q3): a silent no-op: 200 with the task unchanged. An unknown
  task id is still 404. A blank `tag` parameter → 400 on `tag`.
- **If Q3 is answered "not found"**: 404 problem detail "Tag <tag> not found on task <id>".

## R-7 The filter query
- **Decision**: `TaskQuery` gains `tag`; `TaskSpecifications.matching` adds an `EXISTS` subquery
  (`task_tag` of the same task with `tag = :tag`) so a task is never listed twice and the existing
  order is kept. Combined with the other predicates by `AND`. Before relying on it, the implement
  session checks (small compile / test) that the Spring Data JPA version managed by Boot 4.1.1 gives
  a non-null `CriteriaQuery` to `findAll(Specification)` (the current code already guards `cq != null`
  for ordering).
- **Alternatives**: a join on the collection: duplicates rows unless `distinct`, and `distinct`
  interferes with the `CASE` order expression used for null due dates.

## R-8 Timestamps **(assumption)**
- **Decision**: adding or removing a tag (when it changes the tag set) sets the task's `updatedAt`
  from the clock; a no-op doesn't.
- **Rationale**: tags are part of the task; `updatedAt` already means "last changed". Not stated
  in the requirements, hence an assumption.
