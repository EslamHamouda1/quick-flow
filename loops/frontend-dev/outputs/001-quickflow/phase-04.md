# phase-04: US2 Track recurring habits
layer: frontend
story_id: US2
spec_phase: 4
depends_on: [phase-02, backend-dev:US2]
## Goal
The Habits page: add, edit, complete for today / undo, streak and period status, deactivate/activate, remove.
## Acceptance criteria
- AC-US2-1: Given no habits, When the user creates a habit with a name, optional description and frequency Daily or Weekly, Then it is stored as active with its creation time set.
- AC-US2-2: Given the habit form, When the user submits an empty name or a name longer than 150 characters, Then the habit is rejected with a message naming the name field (BR-6).
- AC-US2-3: Given an active habit, When the user marks it complete for today, Then one completion record for that habit and today's date is stored.
- AC-US2-4: Given a habit already completed for a date, When the user tries to complete it again for the same date, Then no second record is created and the user is told it is already complete for that date (BR-7).
- AC-US2-5: Given a habit with completions, When the user views it, Then its frequency label, whether it is complete for the current period and its current streak are shown.
- AC-US2-6: Given an existing habit, When the user edits its name, description or frequency, Then the changes are saved.
- AC-US2-7: Given an active habit, When the user deactivates it, Then it is no longer listed among active habits or today's habits, keeps its history, and cannot be completed until it is active again.
- AC-US2-8: Given a habit, When the user removes it, Then the habit and its completions are never returned by normal queries again (BR-14).
- AC-US2-9: Given no habits exist, When the Habits page shows the list, Then an empty state guides the user to the Add Habit action.
## Tasks
- [ ] T021 [US2] Check that the generated `HabitsService` has `listHabits`, `createHabit`, `updateHabit`, `deleteHabit`, `deactivateHabit`, `activateHabit`, `completeHabit`, `undoHabitCompletion` and that `Habit` has `completedToday`, `doneForCurrentPeriod`, `currentStreak`; check the Habits section of contracts/ui-contract.md covers AC-US2-1..9; raise a question for anything missing
- [ ] T022 [P] [US2] Create the habit form component (name, description, frequency Daily/Weekly, field errors) in frontend/src/app/pages/habits/habit-form.ts (AC-US2-1, AC-US2-2, AC-US2-6)
- [ ] T023 [US2] Implement the Habits page with habit cards (frequency label, period done text, streak, inactive badge), add/edit flow (also `?add=1`) and empty state in frontend/src/app/pages/habits/habits.page.ts (AC-US2-1, AC-US2-5, AC-US2-6, AC-US2-9)
- [ ] T024 [US2] Add the completion toggle (`completeHabit(id)` / `undoHabitCompletion(id, clock.today())`, disabled when inactive, 409 `detail` shown as a notice and the list re-read) in frontend/src/app/pages/habits/habits.page.ts (AC-US2-3, AC-US2-4, research R-9)
- [ ] T025 [US2] Add deactivate/activate and delete actions, each re-reading the list, in frontend/src/app/pages/habits/habits.page.ts (AC-US2-7, AC-US2-8)
