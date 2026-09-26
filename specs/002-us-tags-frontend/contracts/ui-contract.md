# UI contract: Task tags (frontend layer)

Selectors this feature adds to the Tasks page `/tasks`, so the testing loop can write its Playwright checks from
AC-US1-1..6 without reading the code. It extends
[specs/001-quickflow-frontend/contracts/ui-contract.md](../../001-quickflow-frontend/contracts/ui-contract.md): every
selector listed there (shell, `notice`, `notice-error`, `error-<field>`, `empty-state`, `task-list`,
`task-row-<id>`, filters, row actions) stays as it is. `<id>` is the task's API id.

## Tasks `/tasks` (US1, tags)
| Selector | Element |
|---|---|
| `filter-tag` | text input in the toolbar, label "Tag"; its value is sent as the `tag` query parameter (blank = no tag filter); the list re-reads on input; disabled in the Overdue view |
| `task-row-tags` | inside `task-row-<id>`: the container of that task's tags (present also when the task has none) |
| `task-row-tag` | inside `task-row-tags`: one element per tag, in the order the API returns them; its text is the tag, followed by its remove button |
| `task-tag-remove` | inside each `task-row-tag`: button that removes that tag (text "×", `aria-label` "Remove tag <tag>"); no confirmation |
| `task-tag-input-<id>` | text input to add tags to task `<id>` (placeholder "Add tags, comma-separated"); several tags are separated by commas |
| `task-tag-add-<id>` | button "Add tag" for task `<id>`; pressing Enter in `task-tag-input-<id>` does the same |
| `error-tags` | inside `task-row-<id>`: the server's validation message after a rejected add to that task (AC-US1-4, AC-US1-6) |

After a successful add or remove the notice "Tags added" / "Tag removed" appears (`notice`), the add input is emptied
and the list is re-read with the current filters. After a rejected add the list isn't re-read: the row's tags stay as
they were and the input keeps its text.

## Acceptance criteria → selectors
| AC | Checked with |
|---|---|
| AC-US1-1 | type `work, urgent` in `task-tag-input-<id>`, click `task-tag-add-<id>` → row shows `task-row-tag` "urgent" and "work"; reload `/tasks` → still shown |
| AC-US1-2 | `filter-tag` = `work` → `task-list` holds only the rows whose `task-row-tag` includes "work" (or `empty-state` "No tasks match" if none) |
| AC-US1-3 | click `task-tag-remove` in the "work" `task-row-tag` → tag gone; with `filter-tag` = `work` the row is not listed |
| AC-US1-4 | add an empty value, or a 31-character tag → `error-tags` in that row; row tags unchanged |
| AC-US1-5 | add `Work` to a task tagged "work" → still one "work"; `filter-tag` = `WORK` lists it |
| AC-US1-6 | task with 10 tags, add an 11th different tag → `error-tags`; still 10 `task-row-tag` |
