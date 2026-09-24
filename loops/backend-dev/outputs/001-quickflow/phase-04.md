# phase-04: US2 Track recurring habits
layer: backend
story_id: US2
spec_phase: 4
depends_on: [phase-02]
## Goal
Daily/weekly habits with one completion per date, progress and streak, deactivate/activate and delete.
## Acceptance criteria
- AC-US2-1: Given no habits, When the user creates a habit with a name, optional description and frequency Daily or Weekly, Then it is stored as active with its creation time set.
- AC-US2-2: Given the habit form, When the user submits an empty name or a name longer than 150 characters, Then the habit is rejected with a message naming the name field (BR-6).
- AC-US2-3: Given an active habit, When the user marks it complete for today, Then one completion record for that habit and today's date is stored.
- AC-US2-4: Given a habit already completed for a date, When the user tries to complete it again for the same date, Then no second record is created and the user is told it is already complete for that date (BR-7).
- AC-US2-5: Given a habit with completions, When the user views it, Then its frequency label, whether it is complete for the current period and its current streak are shown.
- AC-US2-6: Given an existing habit, When the user edits its name, description or frequency, Then the changes are saved.
- AC-US2-7: Given an active habit, When the user deactivates it, Then it is no longer listed among active habits or today's habits, keeps its history, and cannot be completed until it is active again.
- AC-US2-8: Given a habit, When the user removes it, Then the habit and its completions are never returned by normal queries again (BR-14).
- AC-US2-9: Given no habits exist, When the Habits page shows the list, Then an empty state guides the user to the Add Habit action. (frontend layer)
## Tasks
- [ ] T029 [P] [US2] Domain test backend/src/test/java/com/quickflow/domain/habit/HabitTest.java: `BR-6` (blank name and 151 chars rejected with field `name`, 150 accepted), `FR-03.3` null frequency rejected, `FR-03.1` description > 2,000 rejected, `FR-03.2` new habit active, `deactivate`/`activate`
- [ ] T030 [P] [US2] Domain test backend/src/test/java/com/quickflow/domain/habit/HabitProgressCalculatorTest.java (fixed clock): `FR-04.5` DAILY `completedToday`, `doneForCurrentPeriod`, streak counting back from today, or from yesterday when today is not done, broken by a gap; WEEKLY period = Monday–Sunday week (spec A-12), streak over consecutive weeks, done for the current week by any date in it
- [ ] T031 [P] [US2] Domain test backend/src/test/java/com/quickflow/domain/habit/HabitServiceTest.java (Mockito repositories, fixed clock): `FR-04.1` completion defaults to today, future date → `ValidationException` (field `date`); `BR-7` existing (habit, date) → `ConflictException`, and a `DataIntegrityViolationException` from save → `ConflictException`; `FR-04.4` inactive habit → `ConflictException`; `FR-04.3` undo missing completion → `NotFoundException`; `FR-03.2` unknown habit → `NotFoundException`
- [ ] T032 [P] [US2] Persistence test backend/src/test/java/com/quickflow/persistence/HabitRepositoryTest.java: `BR-7` unique constraint rejects a second (habit, date) row, `BR-14` deleting a habit deletes its completions and neither is returned, `FR-03.2` active filter
- [ ] T033 [P] [US2] Web-slice test backend/src/test/java/com/quickflow/web/habit/HabitControllerTest.java: `createHabit` 201, `BR-6` 400 `errors[].field == "name"`, unknown frequency 400, `listHabits?active=true`, `completeHabit` 201 with and without body, `BR-7` 409 problem body, `undoHabitCompletion` 204, `deactivateHabit`/`activateHabit` 200, `deleteHabit` 204, `getHabit` 404
- [ ] T034 [P] [US2] Create enum `HabitFrequency {DAILY, WEEKLY}` in backend/src/main/java/com/quickflow/domain/habit/HabitFrequency.java
- [ ] T035 [US2] Create entity `Habit` in backend/src/main/java/com/quickflow/domain/habit/Habit.java: `name` "required, non-blank, ≤ 150 (BR-6)"; `description` "≤ 2,000"; `frequency` required; `createdAt` Instant; `active` default true; `@OneToMany(mappedBy="habit", cascade=ALL, orphanRemoval=true)` completions; `activate()`/`deactivate()`
- [ ] T036 [US2] Create entity `HabitCompletion` (`habit` ManyToOne not null, `completionDate` LocalDate, `createdAt`; `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"habit_id", "completion_date"}))`) in backend/src/main/java/com/quickflow/domain/habit/HabitCompletion.java
- [ ] T037 [US2] Create `HabitRepository` (find by active) and `HabitCompletionRepository` (exists by habit and date, find by habit ordered by date desc, find dates by habit) in backend/src/main/java/com/quickflow/domain/habit/
- [ ] T038 [US2] Create pure `HabitProgressCalculator` (`progress(frequency, completionDates, today)` → `HabitProgress(completedToday, doneForCurrentPeriod, currentStreak)`) in backend/src/main/java/com/quickflow/domain/habit/HabitProgressCalculator.java
- [ ] T039 [US2] Create `HabitService` (`create, get, list(active), update, activate, deactivate, delete, complete(id, date), undo(id, date), completions(id)`) in backend/src/main/java/com/quickflow/domain/habit/HabitService.java
- [ ] T040 [P] [US2] Create records `HabitWriteRequest`, `HabitResponse`, `HabitCompletionCreateRequest`, `HabitCompletionResponse` (per contract) in backend/src/main/java/com/quickflow/web/habit/
- [ ] T041 [US2] Create `HabitController` with `listHabits, createHabit, getHabit, updateHabit, deleteHabit, deactivateHabit, activateHabit, listHabitCompletions, completeHabit, undoHabitCompletion` in backend/src/main/java/com/quickflow/web/habit/HabitController.java
