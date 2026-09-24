# User story: Task tags

This story is **not** in `Task_PRD.md`. It exists to prove the loops are reusable: it is run
through the unchanged loops, directly, with no orchestrator.

## US-TAGS-1: Tag tasks and filter by tag
As a user, I can add tags to a task and filter my task list by tag, so that I can group related
work across priorities and due dates.

Priority: P1

## Acceptance criteria
1. **Add tags.** Given an existing task, when I add the tags "work" and "urgent" to it, then the
   task shows both tags, and reading the task back returns both tags.
2. **Filter by tag.** Given tasks tagged "work" and tasks without that tag, when I filter the task
   list by "work", then only the tasks tagged "work" are listed.
3. **Remove a tag.** Given a task tagged "work", when I remove the tag "work", then the task no
   longer shows it and no longer appears when filtering by "work".
4. **Validation.** Given a task, when I try to add an empty tag or a tag longer than 30 characters,
   then the tag is rejected with a validation message and the task's tags are unchanged.

## Notes
- Tags are case-insensitive: "Work" and "work" are the same tag.
- A task can have at most 10 tags.
