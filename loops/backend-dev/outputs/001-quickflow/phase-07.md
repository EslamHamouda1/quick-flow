# phase-07: US5 See everything on the Dashboard
layer: backend
story_id: US5
spec_phase: 7
depends_on: [phase-02, phase-03, phase-04, phase-05, phase-06]
## Goal
One `GET /api/dashboard` computed from current data on every read.
## Acceptance criteria
- AC-US5-1: Given tasks due today, overdue tasks and tasks completed today, When the user opens the Dashboard, Then each group lists exactly those tasks (non-archived only).
- AC-US5-2: Given active and inactive habits, When the user opens the Dashboard, Then today's habits list the active habits and marks those completed today, with counts matching the data.
- AC-US5-3: Given non-archived tasks, When the Dashboard shows the task completion percentage, Then it equals (non-archived tasks with status Done) / (all non-archived tasks), and 0% when there are none.
- AC-US5-4: Given plans that are In Progress, When the user opens the Dashboard, Then each is shown with its progress percentage and live rest time; ticking an item in a plan changes the Dashboard figures on the next view without other action.
- AC-US5-5: Given learning cards and milestones, When the user opens the Dashboard, Then the learning snapshot shows the number of cards per status and milestones done out of total, matching the data.
- AC-US5-6: Given the Dashboard, When the user uses a quick-add action (task, habit, learning card, plan), Then the matching add form opens. (frontend layer)
- AC-US5-7: Given any page, When the user uses the persistent navigation, Then Dashboard, Tasks, Habits, Learning Resources, Todo Plans and Settings are each reachable. (frontend layer)
## Tasks
- [x] T068 [P] [US5] Domain test backend/src/test/java/com/quickflow/domain/dashboard/DashboardServiceTest.java (Mockito services/repositories, fixed clock): `FR-09.1` dueToday, overdue, completedToday (completedAt on today in Cairo, non-archived), `AC-US5-3` taskCompletionPercent floor(100·DONE/non-archived) and 0 with no tasks, habit counts (active, completedToday), activePlans only IN_PROGRESS ordered by priority, plan counts, learning snapshot (cards per status, milestones done/total); `FR-08.5` after a plan item toggle the next summary changes; `FR-09.2` figures computed on each call
- [x] T069 [P] [US5] Persistence test backend/src/test/java/com/quickflow/persistence/DashboardQueriesTest.java: task due-today and completed-between queries, archived tasks excluded (`BR-4`), deleted rows never counted (`BR-14`), card counts per status and milestone done/total counts
- [x] T070 [P] [US5] Web-slice test backend/src/test/java/com/quickflow/web/dashboard/DashboardControllerTest.java: `getDashboard` 200 with every field of contract schema `Dashboard`
- [x] T071 [US5] Add the count/list queries the Dashboard needs (tasks due on a date, completed between two instants, non-archived totals by status; cards per status; milestone done/total) to backend/src/main/java/com/quickflow/domain/task/TaskRepository.java and backend/src/main/java/com/quickflow/domain/learning/LearningMilestoneRepository.java / LearningCardRepository.java
- [x] T072 [P] [US5] Create `DashboardSummary` record (and nested count records) in backend/src/main/java/com/quickflow/domain/dashboard/DashboardSummary.java
- [x] T073 [US5] Create `DashboardService` in backend/src/main/java/com/quickflow/domain/dashboard/DashboardService.java
- [x] T074 [US5] Create `DashboardResponse` record and `DashboardController` (`getDashboard`, `GET /api/dashboard`) in backend/src/main/java/com/quickflow/web/dashboard/
