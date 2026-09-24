# phase-04-review: US2 Track recurring habits

## Goal
Daily/weekly habits with one completion per date, progress and streak, deactivate/activate and delete (`/api/habits*`).

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

Order: the five tests T029–T033 first (in parallel, different files), then T034 and T040 (T040 needs
the enum from T034, so it starts after T034 if run by subagents), then T035 → T036 → T037 → T038 →
T039 → T041 (T038 only needs T034 and could run beside T035–T037).

## Acceptance criteria
Copied from `phase-04.md` (spec.md US2):
- AC-US2-1: Given no habits, When the user creates a habit with a name, optional description and frequency Daily or Weekly, Then it is stored as active with its creation time set.
- AC-US2-2: Given the habit form, When the user submits an empty name or a name longer than 150 characters, Then the habit is rejected with a message naming the name field (BR-6).
- AC-US2-3: Given an active habit, When the user marks it complete for today, Then one completion record for that habit and today's date is stored.
- AC-US2-4: Given a habit already completed for a date, When the user tries to complete it again for the same date, Then no second record is created and the user is told it is already complete for that date (BR-7).
- AC-US2-5: Given a habit with completions, When the user views it, Then its frequency label, whether it is complete for the current period and its current streak are shown.
- AC-US2-6: Given an existing habit, When the user edits its name, description or frequency, Then the changes are saved.
- AC-US2-7: Given an active habit, When the user deactivates it, Then it is no longer listed among active habits or today's habits, keeps its history, and cannot be completed until it is active again.
- AC-US2-8: Given a habit, When the user removes it, Then the habit and its completions are never returned by normal queries again (BR-14).
- AC-US2-9: (frontend layer, not built or observable here.)

What the testing loop can observe after this phase (all under `http://localhost:8080`):
- `POST /api/habits` → 201 `Habit` (`active: true`, `createdAt` set, `completedToday: false`, `doneForCurrentPeriod: false`, `currentStreak: 0`); 400 `application/problem+json` with `errors[].field` `name` (empty / 151 chars), `description` (2,001 chars), `frequency` (missing or `"MONTHLY"`).
- `POST /api/habits/{id}/completions` (no body, `{}` or `{"date": "<today>"}`) → 201 `HabitCompletion` with today's date; again for the same date → 409 problem; future date → 400 field `date`; on a deactivated habit → 409; unknown habit → 404.
- `GET /api/habits/{id}` after completing today → `completedToday: true`, `doneForCurrentPeriod: true`, `currentStreak ≥ 1`; `GET /api/habits/{id}/completions` newest date first; `DELETE /api/habits/{id}/completions/{date}` → 204, then 404 for the same date.
- `GET /api/habits?active=true` excludes deactivated habits; `GET /api/habits` lists all, oldest first; `PUT /api/habits/{id}` → 200; `POST .../deactivate|activate` → 200; `DELETE /api/habits/{id}` → 204, then `GET` → 404 and `GET .../completions` → 404.
- `/v3/api-docs` and `backend/target/openapi.json` list these 10 operationIds with schemas `Habit`, `HabitWrite`, `HabitFrequency`, `HabitCompletion`, `HabitCompletionCreate`.

## Files
| File | Change |
|---|---|
| `backend/src/main/java/com/quickflow/domain/habit/HabitFrequency.java` | new enum, `@Schema(enumAsRef = true)` as for `TaskStatus` (swagger `$ref` to `HabitFrequency`) |
| `backend/src/main/java/com/quickflow/domain/habit/Habit.java` | new `@Entity` (table `habit`), BR-6 / FR-03.1 / FR-03.3 checks throwing `ValidationException`, `update(...)`, `activate()`, `deactivate()`, completions collection (cascade ALL, orphanRemoval) |
| `backend/src/main/java/com/quickflow/domain/habit/HabitCompletion.java` | new `@Entity` (table `habit_completion`), unique `(habit_id, completion_date)` |
| `backend/src/main/java/com/quickflow/domain/habit/HabitRepository.java` | new, `JpaRepository<Habit, Long>`, `findByActiveOrderByCreatedAtAscIdAsc`, `findAllByOrderByCreatedAtAscIdAsc` |
| `backend/src/main/java/com/quickflow/domain/habit/HabitCompletionRepository.java` | new, `existsByHabitIdAndCompletionDate`, `findByHabitIdOrderByCompletionDateDesc`, `findByHabitIdAndCompletionDate`, a `@Query` selecting the dates of one habit |
| `backend/src/main/java/com/quickflow/domain/habit/HabitProgress.java` | new record `(completedToday, doneForCurrentPeriod, currentStreak)` (T038 names it; own file or nested in the calculator) |
| `backend/src/main/java/com/quickflow/domain/habit/HabitProgressCalculator.java` | new, pure (static or stateless `@Component`), R-8 |
| `backend/src/main/java/com/quickflow/domain/habit/HabitService.java` | new `@Service`, `@Transactional`, uses both repositories + `TimeService`; returns habits together with their `HabitProgress` (a small `HabitView(habit, progress)` record in the same package) |
| `backend/src/main/java/com/quickflow/web/habit/HabitWriteRequest.java` | new record, `@Schema(name = "HabitWrite")`, `@NotBlank @Size(min = 1, max = 150)` name, `@Size(max = 2000)` description, `@NotNull` frequency |
| `backend/src/main/java/com/quickflow/web/habit/HabitResponse.java` | new record, `@Schema(name = "Habit")`, required list per contract, `createdAt` via `TimeService.toOffset` |
| `backend/src/main/java/com/quickflow/web/habit/HabitCompletionCreateRequest.java` | new record, `@Schema(name = "HabitCompletionCreate")`, nullable `date` |
| `backend/src/main/java/com/quickflow/web/habit/HabitCompletionResponse.java` | new record, `@Schema(name = "HabitCompletion")` |
| `backend/src/main/java/com/quickflow/web/habit/HabitController.java` | new, `@RequestMapping("/api/habits")`, 10 methods |
| `backend/src/test/java/com/quickflow/domain/habit/HabitTest.java` | new |
| `backend/src/test/java/com/quickflow/domain/habit/HabitProgressCalculatorTest.java` | new |
| `backend/src/test/java/com/quickflow/domain/habit/HabitServiceTest.java` | new |
| `backend/src/test/java/com/quickflow/persistence/HabitRepositoryTest.java` | new |
| `backend/src/test/java/com/quickflow/web/habit/HabitControllerTest.java` | new |

Endpoints added (contract `paths./api/habits*`): `listHabits`, `createHabit`, `getHabit`, `updateHabit`,
`deleteHabit`, `deactivateHabit`, `activateHabit`, `listHabitCompletions`, `completeHabit`,
`undoHabitCompletion`.
No change to `pom.xml`, the properties files or `ApiExceptionHandler` (its A-10 type-mismatch handler
from phase-03 already covers a bad `{date}` / `{id}` / `active` value; `ConflictException` → 409 exists).

## Planned checks
- **Tests written before the code they test** (layer has `coverage`): T029 before T035, T030 before T038, T031 before T039, T032 before T036/T037, T033 before T040/T041.
- Check before relying on it (in the resolved jars / the phase-03 code, not from memory):
  - Spring Data JPA 4.1.1 derived-query naming for a nested property (`existsByHabitIdAndCompletionDate` → `habit.id`) and a `@Query` returning `List<LocalDate>`;
  - that `saveAndFlush` makes a unique-constraint violation surface as `DataIntegrityViolationException` inside `HabitService.complete` (so it's mapped to `ConflictException` there, R-11) rather than at commit;
  - Hibernate 7 orphan removal when a completion is removed from `Habit.completions` (undo) and cascade on `repository.delete(habit)` (BR-14);
  - how springdoc 3.1.1 renders an optional request body (`@RequestBody(required = false)`) for `completeHabit`, and a `LocalDate` path variable (`format: date`);
  - the test imports already used by the phase-03 tests (`@DataJpaTest`, `@WebMvcTest`, `@MockitoBean`, `MockMvcTester`).
- Run `L.build` and `L.test`; save output to `loops/backend-dev/runs/001-quickflow/phase-04-build.log`.
- Copy `backend/target/openapi.json` to `loops/backend-dev/outputs/openapi.json` and compare the 10 habit operations (paths, operationIds, `active` query param, `date` path param, optional request body, 201/204 codes, `Habit`/`HabitWrite`/`HabitCompletion`/`HabitCompletionCreate` properties, required lists, nullable types, `HabitFrequency` `$ref`) with `contracts/openapi.yaml`; the task operations from phase-03 must stay unchanged. Differences are logged; any that can't be closed without unapproved annotations are raised as a question.
- JaCoCo: domain line coverage ≥ 0.80 over `com/quickflow/domain/**` (Habit, HabitCompletion, HabitProgressCalculator, HabitService, enum).
- **Unit tests, one per business rule** (named with the rule id):
  - `HabitTest`: `br6_blankNameRejected`, `br6_name151CharsRejected`, `br6_name150CharsAccepted`, `fr03_3_nullFrequencyRejected`, `fr03_1_description2001CharsRejected`, `fr03_1_nullDescriptionAccepted`, `fr03_2_newHabitActiveWithCreatedAt`, `fr03_2_deactivateThenActivate`, `fr03_2_updateChangesNameDescriptionFrequency`.
  - `HabitProgressCalculatorTest`: `fr04_5_dailyCompletedToday`, `fr04_5_dailyNotDoneWithoutTodayCompletion`, `fr04_5_dailyStreakFromToday`, `fr04_5_dailyStreakFromYesterdayWhenTodayOpen`, `fr04_5_dailyStreakBrokenByGap`, `fr04_5_dailyStreakZeroWhenTodayAndYesterdayOpen`, `fr04_5_weeklyDoneByAnyDateInCurrentWeek`, `fr04_5_weeklyWeekRunsMondayToSunday` (a Sunday and the next Monday are different weeks), `fr04_5_weeklyStreakOverConsecutiveWeeks`, `fr04_5_weeklyStreakFromPreviousWeekWhenCurrentOpen`, `fr04_5_weeklyStreakBrokenByEmptyWeek`, `fr04_5_weeklyCompletedTodayOnlyForTodaysDate`, `fr04_5_noCompletions`.
  - `HabitServiceTest`: `fr04_1_completionDefaultsToToday`, `fr04_1_futureDateRejectedWithFieldDate`, `fr04_1_pastDateAccepted`, `br7_existingCompletionIsConflict`, `br7_dataIntegrityViolationIsConflict`, `fr04_4_inactiveHabitCompletionIsConflict`, `fr04_3_undoRemovesCompletion`, `fr04_3_undoMissingCompletionNotFound`, `fr03_2_unknownHabitNotFound` (parameterized over get/update/activate/deactivate/delete/complete/undo/completions), `fr03_2_createSetsCreatedAtFromClock`.
  - `HabitRepositoryTest`: `br7_uniqueConstraintRejectsSecondRowForSameDate`, `br7_sameHabitDifferentDatesAllowed`, `br14_deletingHabitDeletesCompletions`, `fr03_2_activeFilter`, `fr04_5_completionDatesOfOneHabitOnly`.
  - `HabitControllerTest`: `createHabitReturns201Habit`, `br6_blankNameIs400WithNameField`, `unknownFrequencyIs400WithFrequencyField`, `listHabitsActiveTrue`, `listHabitsWithoutFilter`, `completeHabitWithoutBodyIs201`, `completeHabitWithDateIs201`, `br7_duplicateCompletionIs409Problem`, `undoHabitCompletionIs204`, `deactivateAndActivateReturn200`, `deleteHabitIs204`, `getHabitUnknownIs404`, `listHabitCompletionsNewestFirst`.

## Risks
- **Coverage gate reads only package `com/quickflow/domain`** (phase-02/03 finding, `scripts/lib/loopctl.py`): domain coverage for `com/quickflow/domain/habit` may be reported as "skipped" by the runner; the tests above aim at ≥ 0.80 either way. I can't change `scripts/`.
- **Duplicate detection timing (BR-7, R-11)**: with a plain `save` inside `@Transactional`, the unique-constraint violation appears at commit, after the service method, and would reach the advice as an unmapped `DataIntegrityViolationException` (500). `saveAndFlush` + catch in the service avoids that; checked before coding.
- **Undo via the collection vs. the repository**: removing a completion through `completionRepository.delete` while the habit's `completions` collection is loaded could leave a stale entry in the same persistence context; the service removes it through the habit's collection (orphanRemoval) so both stay consistent.
- **N+1 reads in `listHabits`**: each habit's completion dates are read to compute progress. Acceptable for a single-user app (plan: no pagination); noted, not optimised.
- **Deleting a habit used by a plan item** (FR-07.6, `sourceRemoved`): plans don't exist yet; in this phase delete is a plain hard delete (R-5). The plan-item side belongs to phase-06.
- **springdoc output vs. contract**: as in phase-03, 400/404/409 responses aren't generated from the advice, `@Size` adds `minLength: 0` to `description`, and the servers url is the openapi profile's; these known differences are logged, not changed.
- **Time zone at the week boundary**: "today" and the Monday–Sunday week come from `TimeService.today()` (app zone `Africa/Cairo`); a request just after local midnight belongs to the new day/week. Covered by the fixed-clock tests.

## Assumptions
- A-1: the stored name is the **trimmed** name and the ≤ 150 check applies to the trimmed value (same as the task title, phase-03 A-1); a description is stored as given, an empty-string description is stored as `null`.
- A-2: `PUT /api/habits/{id}` takes the full `HabitWrite` (name and frequency required, missing description → cleared to `null`); it never changes `active`, `createdAt` or completions; editing an inactive habit is allowed. Changing the frequency keeps all completions and progress is recomputed with the new frequency.
- A-3: `deactivate` / `activate` on a habit already in that state is a no-op and still returns 200 with the habit.
- A-4: `completeHabit` accepts no body, `{}` or `{"date": null}` as "today"; any past date is accepted, including dates before the habit's `createdAt` (no rule forbids back-filling). Check order: unknown habit → 404, future date → 400 (field `date`), inactive → 409, already completed for that date → 409 with detail "Habit <id> is already complete for <date>".
- A-5: undoing a completion (`DELETE .../completions/{date}`) is allowed on an inactive habit (FR-04.4 limits only completing); a date with no completion → 404 `NotFoundException("Habit <id> has no completion for <date>")`.
- A-6: `completedToday` is "a completion exists for today's date" for both frequencies; for WEEKLY a completion earlier in the week makes `doneForCurrentPeriod` true but `completedToday` false.
- A-7: `currentStreak` is 0 when neither the current nor the previous period has a completion; otherwise it counts consecutive periods backwards from the current period (if done) or from the previous period. Several completions in one week count as one WEEKLY period.
- A-8: `GET /api/habits` without `active` lists all habits; `active=true|false` filters; order is `createdAt` ascending (contract: "oldest first"), ties by `id` ascending. `GET .../completions` orders by `completionDate` descending ("newest date first").
- A-9: the `Habit` response always includes the computed progress fields, also for inactive habits (computed the same way; history is kept, AC-US2-7). "Today's habits" (active habits only) is the Dashboard's view and belongs to phase-07.
- A-10: a `{date}` path value that isn't an ISO date (`yyyy-MM-dd`) gets the existing phase-03 A-10 400 problem with `errors[{field: "date"}]`; a bad `date` in the request body gets the existing unreadable-body 400 with field `date`.

## Open questions
None.
