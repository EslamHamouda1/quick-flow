# Data Model: QuickFlow (frontend layer)

The frontend owns no persistent data. It uses the generated API models (from the backend swagger) and keeps
per-page view state in signals. Field names below are the swagger's (`specs/001-quickflow-backend/contracts/openapi.yaml`).

## API models used, per page

| Page / piece | Reads | Writes |
|---|---|---|
| Shell, ClockService | `AppInfo {timeZone, now}` (`getAppInfo`) | – |
| Default-page guard, Dashboard greeting, PlanStartWatcher | `Settings {displayName, planStartNotifications, defaultPage}` (`getSettings`, read where used) | – |
| Dashboard | `Dashboard` (`getDashboard`): `dueToday[]`, `overdue[]`, `completedToday[]` (Task), `taskCompletionPercent`, `taskCounts {total, done}`, `habits[]` (Habit), `habitCounts {active, completedToday}`, `activePlans[]` (Plan), `planCounts {notStarted, inProgress, completed}`, `learning {notStarted, inProgress, completed, milestonesDone, milestonesTotal}` | `setPlanItemDone` (tick on an active plan, then re-read) |
| Tasks | `Task[]` (`listTasks` with `q, status, priority, dueFrom, dueTo, archived, sort, direction`), `listOverdueTasks` | `createTask (TaskCreate)`, `updateTask (TaskUpdate)`, `completeTask`, `archiveTask`, `restoreTask`, `deleteTask` |
| Habits | `Habit[]` (`listHabits`, `active` omitted = all) | `createHabit`/`updateHabit (HabitWrite)`, `completeHabit`, `undoHabitCompletion(id, date)`, `deactivateHabit`, `activateHabit`, `deleteHabit` |
| Learning | `LearningCard[]` with `milestones[]`, `notes[]`, `milestonesDone`, `milestonesTotal` (`listLearningCards`) | `createLearningCard`, `updateLearningCard (LearningCardUpdate)`, `deleteLearningCard`, `addMilestone`, `updateMilestone (MilestoneUpdate)`, `deleteMilestone`, `addNote`, `deleteNote` |
| Todo Plans | `Plan[]` (`listPlans group=active` and `group=completed`); builder sources: `listTasks` (default, non-archived), `listHabits active=true`, `listLearningCards` (A-7) | `createPlan (PlanCreate)`, `updatePlan (PlanUpdate)`, `setPlanItemDone (PlanItemUpdate)`, `deletePlan` |
| Settings | `Settings`, `AppInfo.timeZone` | `updateSettings (Settings)` |

## View state (signals)

- **ClockService**: `timeZone: string`, `offsetMs: number`, `now: Signal<number>` (epoch ms, ticks every 1 s),
  `today(): string` (`YYYY-MM-DD` in the app zone).
- **Tasks page**: `filters {q, status, priority, dueFrom, dueTo, archived, sort, direction}` (defaults: empty,
  `archived=false`, `sort=createdAt`, `direction=desc`, as in the contract), `view: 'list' | 'overdue'`,
  `tasks: Task[]`, `editing: Task | 'new' | null`, `formErrors: Record<string,string>`.
- **Habits page**: `habits: Habit[]`, `editing`, `formErrors`.
- **Learning page**: `cards: LearningCard[]`, `expanded: Set<id>`, `editing`, `formErrors` per form.
- **Plans page**: `active: Plan[]`, `completed: Plan[]`, `builderOpen`, `editing: Plan | null`, builder
  selection `PlanItemRef[]`, `formErrors`, `highlighted: Set<planId>` (started while open).
- **PlanStartWatcher**: `upcoming: Plan[]` (status `NOT_STARTED`), notified ids in
  `localStorage['quickflow.notifiedPlanIds']` (FA-2).

## Display mappings (labels only, no rules)

| Value | Label |
|---|---|
| TaskStatus `TODO / IN_PROGRESS / DONE` | Todo / In Progress / Done |
| TaskPriority `LOW / MEDIUM / HIGH` | Low / Medium / High |
| HabitFrequency `DAILY / WEEKLY` | Daily / Weekly |
| LearningStatus, PlanStatus `NOT_STARTED / IN_PROGRESS / COMPLETED` | Not Started / In Progress / Completed |
| PlanSourceType `TASK / HABIT / LEARNING_RESOURCE` | Task / Habit / Learning |
| DefaultPage `DASHBOARD / TASKS / HABITS / LEARNING / PLANS / SETTINGS` | Dashboard / Tasks / Habits / Learning Resources / Todo Plans / Settings |

## Routes

| Path | Page | Nav label |
|---|---|---|
| `''` | redirect by `Settings.defaultPage` | – |
| `/dashboard` | Dashboard | Dashboard |
| `/tasks` (`?add=1`) | Tasks | Tasks |
| `/habits` (`?add=1`) | Habits | Habits |
| `/learning` (`?add=1`) | Learning Resources | Learning Resources |
| `/plans` (`?add=1`) | Todo Plans | Todo Plans |
| `/settings` | Settings | Settings |
| `**` | redirect to `''` | – |
