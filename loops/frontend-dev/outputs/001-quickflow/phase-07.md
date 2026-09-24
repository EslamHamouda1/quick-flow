# phase-07: US5 See everything on the Dashboard
layer: frontend
story_id: US5
spec_phase: 7
depends_on: [phase-02, phase-03, phase-04, phase-05, phase-06, backend-dev:US5]
## Goal
The Dashboard page with every figure from `getDashboard`, live in-progress plans and quick-add actions.
## Acceptance criteria
- AC-US5-1: Given tasks due today, overdue tasks and tasks completed today, When the user opens the Dashboard, Then each group lists exactly those tasks (non-archived only).
- AC-US5-2: Given active and inactive habits, When the user opens the Dashboard, Then today's habits list the active habits and marks those completed today, with counts matching the data.
- AC-US5-3: Given non-archived tasks, When the Dashboard shows the task completion percentage, Then it equals (non-archived tasks with status Done) / (all non-archived tasks), and 0% when there are none.
- AC-US5-4: Given plans that are In Progress, When the user opens the Dashboard, Then each is shown with its progress percentage and live rest time; ticking an item in a plan changes the Dashboard figures on the next view without other action.
- AC-US5-5: Given learning cards and milestones, When the user opens the Dashboard, Then the learning snapshot shows the number of cards per status and milestones done out of total, matching the data.
- AC-US5-6: Given the Dashboard, When the user uses a quick-add action (task, habit, learning card, plan), Then the matching add form opens.
- AC-US5-7: Given any page, When the user uses the persistent navigation, Then Dashboard, Tasks, Habits, Learning Resources, Todo Plans and Settings are each reachable.
## Tasks
- [ ] T036 [US5] Check that the generated `DashboardService.getDashboard` returns every `Dashboard` field of the contract (dueToday, overdue, completedToday, taskCompletionPercent, taskCounts, habits, habitCounts, activePlans, planCounts, learning) and that the Dashboard section of contracts/ui-contract.md covers AC-US5-1..7; raise a question for anything missing
- [ ] T037 [US5] Implement the Dashboard page: greeting (`Hello, <displayName>` / `Hello`, from `getSettings()` on every load), metric cards, task completion percent, due today / overdue / completed today lists, today's habits with done marks and count, learning snapshot, in frontend/src/app/pages/dashboard/dashboard.page.ts (FR-09.1, FR-09.2, AC-US5-1..3, AC-US5-5)
- [ ] T038 [US5] Add the in-progress plans section (reuse `plan-card` from US4: progress, live rest time, item toggles; re-read the dashboard after a toggle and when a shown plan's end is reached) in frontend/src/app/pages/dashboard/dashboard.page.ts (AC-US5-4, FR-08.5)
- [ ] T039 [US5] Add quick-add actions navigating to `/tasks?add=1`, `/habits?add=1`, `/learning?add=1`, `/plans?add=1` in frontend/src/app/pages/dashboard/dashboard.page.ts (AC-US5-6, research R-7)
