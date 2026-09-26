# Feature Specification: Task tags

**Feature Branch**: `002-us-tags`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "examples/single-us/US-tags.md: US-TAGS-1, Tag tasks and filter by tag. As a user, I can add tags to a task and filter my task list by tag, so that I can group related work across priorities and due dates. Tags are case-insensitive; a task can have at most 10 tags; an empty tag or a tag longer than 30 characters is rejected."

Ids used in this spec: user story `US1` (the requirements file's `US-TAGS-1`); acceptance criteria
`AC-US1-k` (k = the requirements file's acceptance criterion number, plus AC-US1-5/6 for its Notes);
functional requirements `FR-001..FR-008`; business rules `BR-T1..BR-T4` (tag rules, numbered apart
from the QuickFlow PRD's `BR-1..BR-14`, which still apply unchanged).

This feature extends the existing QuickFlow task feature (`specs/001-quickflow-backend`, User Story 1
"Manage tasks"): the task, its fields, its list filters and its rules stay as they are; this spec
only adds tags.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Tag tasks and filter by tag (Priority: P1)

As a user, I can add tags to a task and filter my task list by tag, so that I can group related work
across priorities and due dates.

**Why this priority**: It is the only story of this feature; tags are useless without both adding
them and filtering by them.

**Independent Test**: With existing tasks, add tags to some of them, read them back, filter the task
list by a tag, remove a tag, and try invalid tags; delivers grouping of tasks by tag.

**Acceptance Scenarios**:

1. **AC-US1-1**: **Given** an existing task, **When** the user adds the tags "work" and "urgent" to it, **Then** the task shows both tags, and reading the task back returns both tags.
2. **AC-US1-2**: **Given** tasks tagged "work" and tasks without that tag, **When** the user filters the task list by "work", **Then** only the tasks tagged "work" are listed.
3. **AC-US1-3**: **Given** a task tagged "work", **When** the user removes the tag "work", **Then** the task no longer shows it and no longer appears when filtering by "work".
4. **AC-US1-4**: **Given** a task, **When** the user tries to add an empty tag or a tag longer than 30 characters, **Then** the tag is rejected with a validation message and the task's tags are unchanged (BR-T1).
5. **AC-US1-5**: **Given** a task tagged "work", **When** the user adds "Work" or filters by "WORK", **Then** "Work" and "work" are treated as the same tag: the task still has one such tag, and it is listed by the filter (BR-T2).
6. **AC-US1-6**: **Given** a task with 10 tags, **When** the user adds another, different tag, **Then** the tag is rejected with a validation message and the task's tags are unchanged (BR-T3).

### Edge Cases

- Adding a tag the task already has (in any letter case): a silent no-op; the request succeeds, the task is unchanged, and the tag counts once toward the 10 (FR-003; answered at the phase-00 review, 2026-09-26).
- Removing a tag the task doesn't have: a silent no-op; the request succeeds and the task is unchanged. An unknown task id is still "not found" (FR-005; answered at the phase-00 review, 2026-09-26).
- Several tags added at once where one is invalid (empty, over 30 characters, or the total would exceed 10): the whole request is rejected and none of them is added (AC-US1-4, "tags are unchanged").
- A tag of only spaces counts as empty; spaces around a tag are dropped ("  work " is "work").
- A tag is looked up and stored in one letter case form: always lower case; "Work" is stored, matched, filtered and shown as "work" (FR-002; answered at the phase-00 review, 2026-09-26).
- Filtering by a tag no task has returns an empty list, not an error.
- The tag filter combines with the existing filters (search text, status, priority, due dates, archived) and sort: a task is listed only if it matches all of them; archived tasks stay excluded by default (BR-4).
- Deleting a task deletes its tags with it; a deleted task is never listed by a tag filter (BR-14, BR-T4).
- Operations on a task id that doesn't exist (or was deleted) fail with "not found", as other task operations do.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to add one or more tags to an existing task in one action (AC-US1-1).
- **FR-002**: Every read of a task (single task and task list) MUST return the task's tags, in a stable order (alphabetical) (AC-US1-1). Tags are shown in lower case (see Edge Cases, tag spelling).
- **FR-003**: Tags MUST be case-insensitive: two tags that differ only in letter case are the same tag, a task never holds both, and adding a tag the task already has never creates a second one: it is a silent no-op (BR-T2, AC-US1-5).
- **FR-004**: Users MUST be able to filter the task list by one tag; only tasks that have that tag (ignoring case) are listed; the filter combines with all existing list filters and sort (AC-US1-2, AC-US1-5).
- **FR-005**: Users MUST be able to remove a tag from a task; afterwards the task no longer shows it and the tag filter no longer lists it; removing a tag the task doesn't have is a silent no-op (AC-US1-3).
- **FR-006**: The system MUST reject an empty tag (after trimming spaces) or a tag longer than 30 characters with a validation message naming the tags field, leaving the task's tags unchanged (BR-T1, AC-US1-4).
- **FR-007**: The system MUST reject an addition that would give a task more than 10 tags with a validation message, leaving the task's tags unchanged (BR-T3, AC-US1-6).
- **FR-008**: Tags MUST be kept with the task (they survive an app restart) and removed with it when the task is deleted (BR-T4).

### Business Rules

- **BR-T1**: A tag is 1 to 30 characters long after spaces around it are removed.
- **BR-T2**: Tags are case-insensitive: "Work" and "work" are the same tag.
- **BR-T3**: A task has at most 10 tags.
- **BR-T4**: A task's tags belong to it alone and are deleted with it; editing, completing, archiving or restoring a task never changes its tags.

### Key Entities

- **Task** (existing, `specs/001-quickflow-backend`): gains a set of tags (0 to 10).
- **Tag**: a short label (1–30 characters) on one task; identified, within that task, by its text ignoring letter case. Tags have no life of their own: there is no list of tags separate from tasks.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can tag a task and see it listed under that tag's filter in under 10 seconds.
- **SC-002**: Adding a tag, removing a tag and filtering by tag each complete in under 500 ms (the app-wide limit, PRD §10).
- **SC-003**: 100% of invalid tags (empty, over 30 characters, an 11th tag) are rejected with a message and leave the task's tags unchanged.
- **SC-004**: Filtering by a tag lists exactly the tasks with that tag (no missing, no extra), whatever the letter case typed.

## Assumptions

- Single user, as in QuickFlow: tags need no owner and no permissions.
- Tags are managed on the task only; there is no screen or list for managing tags on their own, no rename-everywhere and no tag colours (not in the requirements).
- Filtering is by one tag at a time; filtering by several tags at once is out of scope.
- The tag filter matches the whole tag (ignoring case), not part of it: filtering by "wor" doesn't list tasks tagged "work".
- Any characters are allowed inside a tag (including inner spaces), as the requirements set only the length limits.
- Existing tasks start with no tags.
