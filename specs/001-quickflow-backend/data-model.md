# Data Model: QuickFlow backend

All entities live in `com.quickflow.domain.<area>`. Ids are `Long` (identity). `Instant` columns are stored in
UTC and exposed as `OffsetDateTime` in the app zone; `LocalDate` columns are app-zone calendar dates.
"Computed" fields are never stored; they are derived on read from stored values and the `Clock`.

## Task (`task`)
| Field | Type | Rules |
|---|---|---|
| id | Long | generated |
| title | String | required, trimmed non-blank, ≤ 200 (BR-1) |
| description | String? | ≤ 2,000 (BR-2) |
| status | TaskStatus `TODO, IN_PROGRESS, DONE` | required, default TODO (BR-3) |
| priority | TaskPriority `LOW, MEDIUM, HIGH` | required, default MEDIUM (BR-3) |
| dueDate | LocalDate? | optional |
| createdAt | Instant | set on create |
| updatedAt | Instant | set on create and every change |
| completedAt | Instant? | set when status becomes DONE, cleared when it leaves DONE (FR-01.4, BR-5) |
| archived | boolean | default false (BR-4) |

Computed: `overdue` = `dueDate != null && dueDate < today && status != DONE && !archived` (FR-01.7).
Transitions: any status ↔ any status by update; `complete` → DONE; `archive`/`restore` toggle `archived`.
Queries (`TaskQuery`): `q` (title contains, ignore case), `status`, `priority`, `dueFrom`, `dueTo` (inclusive),
`archived` (default false), `sort` (`createdAt` | `dueDate`, default `createdAt`), `direction` (default `desc`);
null due dates sort last.

## Habit (`habit`)
| Field | Type | Rules |
|---|---|---|
| id | Long | generated |
| name | String | required, non-blank, ≤ 150 (BR-6) |
| description | String? | ≤ 2,000 |
| frequency | HabitFrequency `DAILY, WEEKLY` | required |
| createdAt | Instant | set on create |
| active | boolean | default true |

Relationship: 1:N `HabitCompletion`, deleted with the habit.
Computed (`HabitProgressCalculator`, R-8): `completedToday`, `doneForCurrentPeriod`, `currentStreak`.
Transitions: `deactivate` (active → false), `activate` (→ true). Completing an inactive habit → 409 (FR-04.4).

## HabitCompletion (`habit_completion`)
| Field | Type | Rules |
|---|---|---|
| id | Long | generated |
| habit | Habit (FK `habit_id`) | required |
| completionDate | LocalDate | required, not after today (FR-04.1) |
| createdAt | Instant | set on create |

Unique constraint `(habit_id, completion_date)` (BR-7, R-11).

## LearningCard (`learning_card`)
| Field | Type | Rules |
|---|---|---|
| id | Long | generated |
| title | String | required, non-blank (BR-8), ≤ 200 |
| description | String? | ≤ 2,000 (description or source) |
| status | LearningStatus `NOT_STARTED, IN_PROGRESS, COMPLETED` | default NOT_STARTED, set by the user (A-6) |
| createdAt | Instant | set on create |

Relationships: 1:N `LearningMilestone`, 1:N `LearningNote`, both deleted with the card.

## LearningMilestone (`learning_milestone`)
| Field | Type | Rules |
|---|---|---|
| id | Long | generated |
| card | LearningCard (FK `learning_card_id`, not null) | exactly one card (BR-9) |
| title | String | required, ≤ 200 |
| done | boolean | default false |
| targetDate | LocalDate? | optional |

A milestone is addressed only through its card (`/learning-cards/{cardId}/milestones/{id}`); a mismatched card → 404.

## LearningNote (`learning_note`)
| Field | Type | Rules |
|---|---|---|
| id | Long | generated |
| card | LearningCard (FK, not null) | exactly one card |
| text | String | required, non-blank, ≤ 5,000 (A-9) |
| createdAt | Instant | set on create |

## Plan (`plan`)
| Field | Type | Rules |
|---|---|---|
| id | Long | generated |
| title | String | required, ≤ 200 |
| estimatedDurationMinutes | int | 1..100,000 (A-9, A-11) |
| startDateTime | Instant | required |
| endDateTime | Instant | required, after start (BR-11) |
| priorityOrder | int | ≥ 1, 1 = highest, not unique (A-8) |
| createdAt | Instant | set on create |
| items | List<PlanItem> | ≥ 1 (BR-10), cascade ALL, orphan removal |

Computed (`PlanStatusPolicy`, R-6, pending Q2): `status` NOT_STARTED / IN_PROGRESS / COMPLETED; `progressPercent`;
`restSeconds` (only when `start <= now < end`, BR-12); `doneItems`, `totalItems`.
Lifecycle: NOT_STARTED --(now reaches start)--> IN_PROGRESS --(all items done or now reaches end)--> COMPLETED.
Un-ticking an item of a plan before its end can move it back to IN_PROGRESS (status is computed, not stored).
Ordering: active/upcoming (not COMPLETED) by `priorityOrder` asc, then `startDateTime` asc; completed by `endDateTime` desc.

## PlanItem (`plan_item`)
| Field | Type | Rules |
|---|---|---|
| id | Long | generated |
| plan | Plan (FK, not null) | |
| sourceType | PlanSourceType `TASK, HABIT, LEARNING_RESOURCE` | required |
| sourceId | Long | required; must exist when the plan is created (BR-10) |
| sourceTitle | String | snapshot of the source's title/name, refreshed on read while the source exists |
| done | boolean | default false, independent of the plan status |

Unique per plan: `(plan_id, source_type, source_id)` (FR-07.2).
Computed: `sourceRemoved` = the source no longer exists (FR-07.6).
Side effect (pending Q1): setting `done=true` on a TASK item completes the task (BR-13).

## AppSettings (`app_settings`) — pending Q3
| Field | Type | Rules |
|---|---|---|
| id | Long | always 1 (single row, created on first read) |
| displayName | String? | ≤ 100 |
| planStartNotifications | boolean | default true |
| defaultPage | DefaultPage `DASHBOARD, TASKS, HABITS, LEARNING, PLANS, SETTINGS` | default DASHBOARD |

## Dashboard (computed, `DashboardService`, FR-09)
`dueToday` (non-archived tasks with dueDate = today), `overdue` (FR-01.7), `completedToday` (non-archived,
DONE, completedAt on today), `taskCompletionPercent` (floor(100 · DONE / non-archived), 0 when none),
`taskCounts` (total, done), `habits` (active habits with `completedToday`), `habitCounts` (active, completedToday),
`activePlans` (status IN_PROGRESS, ordered by priority), `planCounts` (notStarted, inProgress, completed),
`learning` (cards per status, milestonesDone, milestonesTotal).
