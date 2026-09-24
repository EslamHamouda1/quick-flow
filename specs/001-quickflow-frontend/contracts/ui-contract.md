# UI contract: QuickFlow (frontend layer)

Stable routes and `data-testid` selectors the frontend provides, so the testing loop can write its Playwright
checks from the acceptance criteria without reading the code. `<id>` is the API id of the row. The frontend
MUST render every selector listed for a story once that story's phase is implemented. Visible text is given where
a check may rely on it.

## Shell (Foundational, AC-US5-7, FR-10.1)
| Selector | Element |
|---|---|
| `nav-main` | persistent navigation, present on every page |
| `nav-dashboard`, `nav-tasks`, `nav-habits`, `nav-learning`, `nav-plans`, `nav-settings` | links; text "Dashboard", "Tasks", "Habits", "Learning Resources", "Todo Plans", "Settings"; routes `/dashboard`, `/tasks`, `/habits`, `/learning`, `/plans`, `/settings` |
| `page-title` | `<h1>` of the current page, text equal to its nav label |
| `notice` | each transient notice (success / error / plan started); `notice-error` for errors |
| `error-<field>` | message under a form field named `<field>` by the API's `errors[].field` (e.g. `error-title`, `error-name`, `error-endDateTime`) |
| `empty-state` + `empty-state-action` | empty list message and its add button (NFR-5) |

## Tasks `/tasks` (US1)
| Selector | Element |
|---|---|
| `task-add` | Add Task button (opens `task-form`; also opened by `/tasks?add=1`) |
| `task-form`, `task-title`, `task-description`, `task-status`, `task-priority`, `task-due-date`, `task-save`, `task-cancel` | create/edit form (status select shown when editing) |
| `task-search` | search box (title substring) |
| `filter-status`, `filter-priority`, `filter-due-from`, `filter-due-to`, `filter-archived`, `sort-field` (`createdAt`/`dueDate`), `sort-direction` (`asc`/`desc`) | filters and sort; the list re-reads on change |
| `task-view-overdue` | toggles the overdue list (`listOverdueTasks`) |
| `task-list` / `task-row-<id>` | list and rows; row shows `task-row-title`, `task-row-status`, `task-row-priority`, `task-row-due`, and `task-row-overdue` badge when `overdue` is true |
| `task-edit-<id>`, `task-complete-<id>`, `task-archive-<id>`, `task-restore-<id>`, `task-delete-<id>` | row actions (restore shown for archived rows) |

## Habits `/habits` (US2)
| Selector | Element |
|---|---|
| `habit-add`, `habit-form`, `habit-name`, `habit-description`, `habit-frequency`, `habit-save`, `habit-cancel` | add/edit form |
| `habit-list` / `habit-card-<id>` | cards; card shows `habit-frequency-label` (Daily/Weekly), `habit-period-done` ("Done today" / "Done this week" / "Not done"), `habit-streak` (number), `habit-inactive` badge |
| `habit-toggle-<id>` | completion checkbox for today (checked = `completedToday`; disabled when inactive) |
| `habit-edit-<id>`, `habit-deactivate-<id>`, `habit-activate-<id>`, `habit-delete-<id>` | actions |

## Learning Resources `/learning` (US3)
| Selector | Element |
|---|---|
| `card-add`, `card-form`, `card-title`, `card-description`, `card-save`, `card-cancel` | add form |
| `card-grid` / `card-<id>` | cards; show `card-title-text`, `card-status` select (Not Started / In Progress / Completed; change saves), `card-milestone-count` ("<done>/<total>") |
| `card-expand-<id>` | expands/collapses milestones and notes (`card-details-<id>`) |
| `card-edit-<id>`, `card-delete-<id>` | edit title/description, remove card |
| `milestone-title-input-<cardId>`, `milestone-date-input-<cardId>`, `milestone-add-<cardId>` | add milestone |
| `milestone-<id>`, `milestone-done-<id>` (checkbox), `milestone-delete-<id>` | milestone row |
| `note-text-input-<cardId>`, `note-add-<cardId>` | add note |
| `note-<id>` (shows text and time), `note-delete-<id>` | note row |

## Todo Plans `/plans` (US4)
| Selector | Element |
|---|---|
| `plan-create` | Create Plan button (opens `plan-builder`; also `/plans?add=1`) |
| `plan-builder`, `plan-title`, `plan-duration` (minutes), `plan-start`, `plan-end` (`datetime-local`, app zone), `plan-priority`, `plan-save`, `plan-cancel` | builder / edit form |
| `plan-source-task-<id>`, `plan-source-habit-<id>`, `plan-source-learning-<id>` | checkboxes for selectable items (non-archived tasks, active habits, all cards) |
| `error-items` | message when no item / a missing item is selected |
| `plans-active` / `plans-completed` | the two groups (active/upcoming ordered by priority order, as returned) |
| `plan-<id>` | plan card; shows `plan-status` (label), `plan-progress` ("<n>%"), `plan-rest-time` (only while In Progress, `H:MM:SS` left), `plan-window` (start – end in app zone), `plan-priority-order` |
| `plan-item-<itemId>`, `plan-item-done-<itemId>` (checkbox), `plan-item-removed-<itemId>` (badge when `sourceRemoved`) | items |
| `plan-edit-<id>`, `plan-delete-<id>` | actions |
| `plan-started-<id>` | highlight on a plan whose start was reached while the app was open (AC-US4-9) |

## Dashboard `/dashboard` (US5)
| Selector | Element |
|---|---|
| `dash-greeting` | "Hello, <displayName>" or "Hello" when no name |
| `metric-tasks`, `metric-habits`, `metric-plans`, `metric-learning` | summary cards (`taskCounts`, `habitCounts`, `planCounts`, `learning`) |
| `dash-task-percent` | "<taskCompletionPercent>%" |
| `dash-due-today`, `dash-overdue`, `dash-completed-today` | task lists; rows `dash-task-<id>` |
| `dash-habits` / `dash-habit-<id>` (with `dash-habit-done-<id>` when `completedToday`) | today's habits; `dash-habit-count` "<completedToday>/<active>" |
| `dash-plans` / `dash-plan-<id>` with `plan-progress`, `plan-rest-time` | in-progress plans |
| `dash-learning` with `dash-learning-not-started`, `dash-learning-in-progress`, `dash-learning-completed`, `dash-milestones` ("<done>/<total>") | learning snapshot |
| `quick-add-task`, `quick-add-habit`, `quick-add-learning`, `quick-add-plan` | quick-add actions (open the matching add form) |

## Settings `/settings` (US6)
| Selector | Element |
|---|---|
| `settings-form`, `settings-display-name`, `settings-notifications` (checkbox), `settings-default-page` (select), `settings-save` | preferences |
| `settings-time-zone` | read-only app time zone |
