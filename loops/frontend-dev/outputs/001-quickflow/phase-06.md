# phase-06: US4 Build and follow Todo Plans
layer: frontend
story_id: US4
spec_phase: 6
depends_on: [phase-02, phase-03, phase-04, phase-05, backend-dev:US4]
## Goal
The Todo Plans page: builder from existing items, grouped plans with progress, live rest time, item toggles, edit, remove, plan-start notification.
## Acceptance criteria
- AC-US4-1: Given existing tasks, habits and learning cards, When the user creates a plan selecting one or more of them with a title, estimated duration, start date-time, end date-time and priority order, Then the plan is stored with one item per selected entity (each not done) and its creation time set.
- AC-US4-2: Given the plan builder, When the user selects no items, or an item that does not exist (or was deleted), Then the plan is rejected (BR-10).
- AC-US4-3: Given the plan builder, When the end date-time is equal to or before the start date-time, Then the plan is rejected with a message naming the end date-time (BR-11).
- AC-US4-4: Given a plan, When the user marks an item done or not done, Then the item's done flag is saved and the plan's progress percentage becomes (items done) / (total items), shown immediately on the plan and on the Dashboard (FR-08).
- AC-US4-5: Given a plan item that references a task, habit or learning card, When the user marks it done, Then the original entity's completion state changes only as stated in FR-07.5 (BR-13).
- AC-US4-6: Given a plan whose start date-time is in the future, When the plan is read, Then its status is Not Started and no rest time is shown (BR-12).
- AC-US4-7: Given a plan whose start date-time has passed and whose end date-time has not, When the plan is read, Then its status is In Progress (unless all its items are done, which makes it Completed per FR-08.4) and its rest time is the time left until its end date-time, updating continuously.
- AC-US4-8: Given a plan whose status follows FR-08.4, When the app is closed and reopened, Then the same status is shown, computed from the stored times, the item flags and the current time (NFR-4).
- AC-US4-9: Given the Todo Plans page is open, When a plan's start date-time is reached, Then the user sees an in-app notification/highlight for that plan.
- AC-US4-10: Given several plans, When the user opens the Todo Plans page, Then plans are grouped into active/upcoming and completed, each showing its progress, rest time (when active), items with done toggles and a remove action; active/upcoming plans are ordered by priority order.
- AC-US4-11: Given past (completed) plans, When the user views the history, Then each shows how much of it was completed (percentage of items done).
- AC-US4-12: Given a plan, When the user removes it, Then the plan and its items are never returned again, and the tasks, habits and learning cards it referenced are unchanged (BR-14).
- AC-US4-13: Given no plans exist, When the page shows the list, Then an empty state guides the user to the Create Plan action.
## Tasks
- [x] T030 [US4] Check that the generated `PlansService` has `listPlans` (`group`), `createPlan`, `getPlan`, `updatePlan`, `deletePlan`, `setPlanItemDone` and that `Plan`/`PlanItem` have `status`, `progressPercent`, `restSeconds`, `sourceTitle`, `sourceRemoved`; check the Plans section of contracts/ui-contract.md covers AC-US4-1..13; raise a question for anything missing
- [x] T031 [P] [US4] Create the rest-time component (input `endDateTime`; text `formatDuration(end − clock.now())`, never negative) in frontend/src/app/shared/rest-time.ts (FR-08.2, NFR-2, research R-4)
- [x] T032 [P] [US4] Create the plan builder component (title, estimated duration minutes, start/end `datetime-local` in the app zone converted with `toZonedIso`, priority order, source checkboxes loaded from `listTasks()`, `listHabits(active=true)`, `listLearningCards()`; edit mode shows only the `PlanUpdate` fields; field errors incl. `error-items` and `error-endDateTime`) in frontend/src/app/pages/plans/plan-builder.ts (AC-US4-1..3, A-7)
- [x] T033 [P] [US4] Create the plan card component (status label, `progressPercent`, rest time only when `status` is `IN_PROGRESS` and `restSeconds` is not null, window in the app zone, items with done checkbox → `setPlanItemDone` and removed badge, edit/delete actions, started highlight) in frontend/src/app/pages/plans/plan-card.ts (AC-US4-4..7, AC-US4-10, FR-07.6)
- [x] T034 [US4] Implement the Todo Plans page: `plans-active` (`group=active`) and `plans-completed` (`group=completed`, history with percent), create flow (also `?add=1`), edit, delete, item toggle replacing the plan from the response, empty state, re-read when `now` passes a shown plan's start or end, in frontend/src/app/pages/plans/plans.page.ts (AC-US4-4, AC-US4-8, AC-US4-10..13, research R-4)
- [x] T035 [US4] Create `PlanStartWatcher` (root, started by the shell) (research R-6, FA-2: app-wide notice "Plan “<title>” has started" + `plan-started-<id>` highlight when `now` reaches a `NOT_STARTED` plan's start, once per plan via `localStorage['quickflow.notifiedPlanIds']`, also on app open for plans already `IN_PROGRESS`; reads `getSettings()` right before showing a notice and shows nothing when `planStartNotifications` is false, FR-10.2) in frontend/src/app/core/plan-start-watcher.service.ts and wire it in frontend/src/app/app.ts (FR-08.1, AC-US4-9, A-10)
