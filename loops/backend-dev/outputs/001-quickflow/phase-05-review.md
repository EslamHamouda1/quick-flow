# phase-05-review: US3 Track learning resources with milestones and notes

## Goal
Learning cards with a user-set status, milestones (done flag, optional target date) and notes, all addressed
under their card; deleting a card removes its milestones and notes (`/api/learning-cards*`).

## Tasks
- [ ] T042 [P] [US3] Domain test backend/src/test/java/com/quickflow/domain/learning/LearningCardTest.java: `BR-8` blank title rejected (field `title`), title > 200 rejected, `FR-05.1` description > 2,000 rejected, default status NOT_STARTED, null status rejected; `FR-06.1` milestone title required and ≤ 200, new milestone not done; `FR-06.2` note text required and ≤ 5,000
- [ ] T043 [P] [US3] Domain test backend/src/test/java/com/quickflow/domain/learning/LearningServiceTest.java (Mockito, fixed clock): `FR-05.2` create/update/get/delete, unknown card → `NotFoundException`; `BR-9` updating or deleting a milestone/note through another card's id → `NotFoundException`; `FR-06.3` milestone done/un-done, note `createdAt` from the clock
- [ ] T044 [P] [US3] Persistence test backend/src/test/java/com/quickflow/persistence/LearningCardRepositoryTest.java: `BR-9` milestone row requires a card, `BR-14` deleting a card deletes its milestones and notes and none is returned, milestone done/total counts
- [ ] T045 [P] [US3] Web-slice test backend/src/test/java/com/quickflow/web/learning/LearningCardControllerTest.java: `createLearningCard` 201, `BR-8` 400 `errors[].field == "title"`, unknown status 400, `listLearningCards` includes milestones and notes, `addMilestone` 201, `updateMilestone` 200, `deleteMilestone` 204, `addNote` 201, `deleteNote` 204, `getLearningCard` 404, `deleteLearningCard` 204
- [ ] T046 [P] [US3] Create enum `LearningStatus {NOT_STARTED, IN_PROGRESS, COMPLETED}` in backend/src/main/java/com/quickflow/domain/learning/LearningStatus.java
- [ ] T047 [US3] Create entity `LearningCard` in backend/src/main/java/com/quickflow/domain/learning/LearningCard.java: `title` "required, non-blank (BR-8), ≤ 200"; `description` "≤ 2,000"; `status` default NOT_STARTED; `createdAt`; `@OneToMany(mappedBy="card", cascade=ALL, orphanRemoval=true)` milestones (ordered by id) and notes (ordered by createdAt)
- [ ] T048 [P] [US3] Create entity `LearningMilestone` (`card` ManyToOne `nullable=false` (BR-9), `title` "required, ≤ 200", `done` default false, `targetDate` LocalDate optional) in backend/src/main/java/com/quickflow/domain/learning/LearningMilestone.java
- [ ] T049 [P] [US3] Create entity `LearningNote` (`card` ManyToOne not null, `text` "required, non-blank, ≤ 5,000 (A-9)", `createdAt`) in backend/src/main/java/com/quickflow/domain/learning/LearningNote.java
- [ ] T050 [US3] Create `LearningCardRepository`, `LearningMilestoneRepository` (find by id and card id), `LearningNoteRepository` (find by id and card id) in backend/src/main/java/com/quickflow/domain/learning/
- [ ] T051 [US3] Create `LearningService` (`create, get, list, update, delete, addMilestone, updateMilestone, deleteMilestone, addNote, deleteNote`) in backend/src/main/java/com/quickflow/domain/learning/LearningService.java
- [ ] T052 [P] [US3] Create records `LearningCardCreateRequest`, `LearningCardUpdateRequest`, `LearningCardResponse` (with `milestonesDone`, `milestonesTotal`), `MilestoneCreateRequest`, `MilestoneUpdateRequest`, `MilestoneResponse`, `NoteCreateRequest`, `NoteResponse` in backend/src/main/java/com/quickflow/web/learning/
- [ ] T053 [US3] Create `LearningCardController` with `listLearningCards, createLearningCard, getLearningCard, updateLearningCard, deleteLearningCard, addMilestone, updateMilestone, deleteMilestone, addNote, deleteNote` in backend/src/main/java/com/quickflow/web/learning/LearningCardController.java

tasks.md `## Phase 5` matches `phase-05.md` (T042–T053, same wording).

Order: the four tests T042–T045 first (in parallel, different files), then T046, then T048 and T049
(parallel, different files, both need `LearningCard` as the owning type, so T047 is written first or
together with them in one step), T050 → T051, then T052 (needs T046 and the entities) → T053.

## Acceptance criteria
Copied from `phase-05.md` (spec.md US3):
- AC-US3-1: Given no cards, When the user adds a card with a title and optional description/source, Then it is stored with status Not Started and its creation time set.
- AC-US3-2: Given the card form, When the user submits an empty title, Then the card is rejected with a message naming the title field (BR-8).
- AC-US3-3: Given a card, When the user changes its status to Not Started, In Progress or Completed, Then the new status is saved; any other value is rejected.
- AC-US3-4: Given a card, When the user adds a milestone with a title and an optional target date, Then the milestone is stored under that card only, not done (BR-9).
- AC-US3-5: Given a milestone, When the user marks it done (or not done again), Then its done flag is saved; When the user removes it, Then it is no longer returned.
- AC-US3-6: Given a card, When the user adds a note with text, Then it is stored under that card with its timestamp; When the user removes the note, Then it is no longer returned.
- AC-US3-7: Given a card with milestones and notes, When the user removes the card, Then the card, its milestones and its notes are never returned by normal queries again (BR-14).
- AC-US3-8: Given a card, When the user expands it, Then its milestones and notes are shown. (backend part: every `LearningCard` response embeds its `milestones` and `notes`)
- AC-US3-9: (frontend layer, not built or observable here.)

What the testing loop can observe after this phase (all under `http://localhost:8080`):
- `POST /api/learning-cards` `{"title": "..."}` (optional `description`) → 201 `LearningCard` (`status: NOT_STARTED`, `createdAt` set, `milestones: []`, `notes: []`, `milestonesDone: 0`, `milestonesTotal: 0`); 400 `application/problem+json` with `errors[].field` `title` (missing, empty, blank, 201 chars) or `description` (2,001 chars).
- `PUT /api/learning-cards/{id}` `{"title", "description", "status"}` → 200 with the new status for each of `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`; `"status": "DONE"` or a missing status → 400 with field `status`; unknown id → 404.
- `POST /api/learning-cards/{id}/milestones` `{"title", "targetDate"?}` → 201 `Milestone` (`cardId` = `{id}`, `done: false`); blank / 201-char title → 400 field `title`; unknown card → 404.
- `PUT /api/learning-cards/{id}/milestones/{milestoneId}` `{"title", "targetDate", "done": true}` → 200 `done: true`, then `done: false` → 200 `done: false`; `GET` of the card shows the flag and `milestonesDone`/`milestonesTotal`; the same `milestoneId` under another card's id → 404; `DELETE` → 204, then it is gone from the card and a second `DELETE` → 404.
- `POST /api/learning-cards/{id}/notes` `{"text"}` → 201 `Note` (`cardId`, `createdAt` in the app zone); blank / 5,001-char text → 400 field `text`; `DELETE .../notes/{noteId}` → 204, then gone; through another card's id → 404.
- `DELETE /api/learning-cards/{id}` → 204, then `GET /api/learning-cards/{id}` → 404, the card is absent from `GET /api/learning-cards`, and its milestone/note URLs → 404.
- `GET /api/learning-cards` → all cards newest first, each with its milestones and notes.
- `/v3/api-docs` and `backend/target/openapi.json` list these 10 operationIds with schemas `LearningCard`, `LearningCardCreate`, `LearningCardUpdate`, `LearningStatus`, `Milestone`, `MilestoneCreate`, `MilestoneUpdate`, `Note`, `NoteCreate`.

## Files
| File | Change |
|---|---|
| `backend/src/main/java/com/quickflow/domain/learning/LearningStatus.java` | new enum, `@Schema(enumAsRef = true)` as for `TaskStatus`/`HabitFrequency` |
| `backend/src/main/java/com/quickflow/domain/learning/LearningCard.java` | new `@Entity` (table `learning_card`), BR-8 / FR-05.1 / FR-05.2 checks throwing `ValidationException` (fields `title`, `description`, `status`), `update(title, description, status)`, `addMilestone(...)`, `addNote(...)`, `removeMilestone(...)`, `removeNote(...)`; `milestones` `@OrderBy("id")` and `notes` `@OrderBy("createdAt, id")`, both cascade ALL + orphanRemoval; `milestonesDone()` / `milestonesTotal()` |
| `backend/src/main/java/com/quickflow/domain/learning/LearningMilestone.java` | new `@Entity` (table `learning_milestone`, FK `learning_card_id` not null), FR-06.1 title check (field `title`), `update(title, targetDate, done)` |
| `backend/src/main/java/com/quickflow/domain/learning/LearningNote.java` | new `@Entity` (table `learning_note`, FK `learning_card_id` not null, `text` column length 5,000), FR-06.2 text check (field `text`) |
| `backend/src/main/java/com/quickflow/domain/learning/LearningCardRepository.java` | new, `JpaRepository<LearningCard, Long>`, `findAllByOrderByCreatedAtDescIdDesc` |
| `backend/src/main/java/com/quickflow/domain/learning/LearningMilestoneRepository.java` | new, find by id and card id (explicit `@Query` JPQL, see Risks) |
| `backend/src/main/java/com/quickflow/domain/learning/LearningNoteRepository.java` | new, find by id and card id (explicit `@Query` JPQL) |
| `backend/src/main/java/com/quickflow/domain/learning/LearningService.java` | new `@Service`, `@Transactional`, uses the three repositories + `TimeService` |
| `backend/src/main/java/com/quickflow/web/learning/LearningCardCreateRequest.java` | new record, `@Schema(name = "LearningCardCreate")`, `@NotBlank @Size(min = 1, max = 200)` title, `@Size(max = 2000)` description |
| `backend/src/main/java/com/quickflow/web/learning/LearningCardUpdateRequest.java` | new record, `@Schema(name = "LearningCardUpdate")`, as above plus `@NotNull` status |
| `backend/src/main/java/com/quickflow/web/learning/LearningCardResponse.java` | new record, `@Schema(name = "LearningCard")`, required list per contract, `createdAt` via `TimeService.toOffset`, embedded milestones and notes, `milestonesDone`, `milestonesTotal` |
| `backend/src/main/java/com/quickflow/web/learning/MilestoneCreateRequest.java` | new record, `@Schema(name = "MilestoneCreate")`, title, nullable `targetDate` |
| `backend/src/main/java/com/quickflow/web/learning/MilestoneUpdateRequest.java` | new record, `@Schema(name = "MilestoneUpdate")`, title, nullable `targetDate`, `@NotNull Boolean done` |
| `backend/src/main/java/com/quickflow/web/learning/MilestoneResponse.java` | new record, `@Schema(name = "Milestone")` |
| `backend/src/main/java/com/quickflow/web/learning/NoteCreateRequest.java` | new record, `@Schema(name = "NoteCreate")`, `@NotBlank @Size(min = 1, max = 5000)` text |
| `backend/src/main/java/com/quickflow/web/learning/NoteResponse.java` | new record, `@Schema(name = "Note")` |
| `backend/src/main/java/com/quickflow/web/learning/LearningCardController.java` | new, `@RequestMapping("/api/learning-cards")`, `@Tag(name = "learning")`, 10 methods |
| `backend/src/test/java/com/quickflow/domain/learning/LearningCardTest.java` | new |
| `backend/src/test/java/com/quickflow/domain/learning/LearningServiceTest.java` | new |
| `backend/src/test/java/com/quickflow/persistence/LearningCardRepositoryTest.java` | new |
| `backend/src/test/java/com/quickflow/web/learning/LearningCardControllerTest.java` | new |

Endpoints added (contract `paths./api/learning-cards*`): `listLearningCards`, `createLearningCard`,
`getLearningCard`, `updateLearningCard`, `deleteLearningCard`, `addMilestone`, `updateMilestone`,
`deleteMilestone`, `addNote`, `deleteNote`.
No change to `pom.xml`, the properties files or `ApiExceptionHandler` (validation 400 with `errors[]`,
unknown enum value 400 with the field, bad path id 400, `NotFoundException` → 404 exist from phase-02/03).

## Planned checks
- **Tests written before the code they test** (layer has `coverage`): T042 before T046–T049, T043 before T051, T044 before T047–T050, T045 before T052/T053.
- Check before relying on it (in the resolved jars / the phase-03/04 code, not from memory):
  - JPQL `@Query` for "milestone/note by id and card id" (phase-04 found that a derived `...ByHabitId...` path resolved to the wrong property; use explicit JPQL from the start);
  - Hibernate 7 `@OrderBy` on the two `@OneToMany` collections and orphan removal when a milestone/note is removed from the card's collection, and cascade on `repository.delete(card)` (BR-14);
  - that `LearningCard` with two `List` collections fetched in one query doesn't hit `MultipleBagFetchException` (lazy collections loaded separately inside the transaction; no `JOIN FETCH` of both);
  - the response is built inside the service transaction (or the collections are initialised there), since `spring.jpa.open-in-view` must be checked in `application.properties` before the controller touches lazy collections;
  - how springdoc 3.1.1 renders `Boolean done` with `@NotNull` (required, `type: boolean`) and nullable `LocalDate targetDate`;
  - the test imports used by the phase-03/04 tests (`@DataJpaTest`, `@WebMvcTest`, `@MockitoBean`, `MockMvcTester`).
- Run `L.build` and `L.test`; save output to `loops/backend-dev/runs/001-quickflow/phase-05-build.log`.
- Copy `backend/target/openapi.json` to `loops/backend-dev/outputs/openapi.json` and compare the 10 learning operations (paths, operationIds, `id`/`milestoneId`/`noteId` path params, 201/204 codes, the 9 schemas' properties, required lists, nullable types, `LearningStatus` `$ref`) with `contracts/openapi.yaml`; task and habit operations from phase-03/04 must stay unchanged. Known differences (no generated 4xx responses, `minLength: 0` from `@Size` on description, int32 formats, servers url) are logged, not changed.
- JaCoCo: domain line coverage ≥ 0.80 over `com/quickflow/domain/**` (LearningCard, LearningMilestone, LearningNote, LearningService, enum).
- **Unit tests, one per business rule** (named with the rule id):
  - `LearningCardTest`: `br8_blankTitleRejectedWithFieldTitle`, `br8_nullTitleRejected`, `fr05_2_title201CharsRejected`, `fr05_2_title200CharsAccepted`, `fr05_1_description2001CharsRejected`, `fr05_1_nullDescriptionAccepted`, `fr05_1_newCardNotStartedWithCreatedAt`, `fr05_2_nullStatusRejectedOnUpdate`, `fr05_2_updateChangesTitleDescriptionStatus`, `fr06_1_milestoneBlankTitleRejected`, `fr06_1_milestoneTitle201CharsRejected`, `fr06_1_newMilestoneNotDone`, `fr06_1_milestoneTargetDateOptional`, `fr06_2_noteBlankTextRejectedWithFieldText`, `fr06_2_noteText5001CharsRejected`, `fr06_2_noteText5000CharsAccepted`, `fr06_3_milestoneCounts`.
  - `LearningServiceTest`: `fr05_2_createSetsCreatedAtFromClock`, `fr05_2_updateSavesStatus`, `fr05_2_deleteRemovesCard`, `fr05_2_unknownCardNotFound` (parameterized over get/update/delete/addMilestone/updateMilestone/deleteMilestone/addNote/deleteNote), `br9_updateMilestoneOfOtherCardNotFound`, `br9_deleteMilestoneOfOtherCardNotFound`, `br9_deleteNoteOfOtherCardNotFound`, `fr06_3_milestoneDoneThenUndone`, `fr06_3_deleteMilestoneRemovesIt`, `fr06_2_noteCreatedAtFromClock`, `fr06_3_deleteNoteRemovesIt`.
  - `LearningCardRepositoryTest`: `br9_milestoneWithoutCardRejected`, `br9_milestoneFoundOnlyByItsOwnCard`, `br14_deletingCardDeletesMilestonesAndNotes`, `fr06_3_milestoneDoneAndTotalCounts`, `fr05_2_listNewestFirst`, `fr06_2_notesOrderedByCreatedAt`.
  - `LearningCardControllerTest`: `createLearningCardReturns201NotStarted`, `br8_blankTitleIs400WithTitleField`, `unknownStatusIs400WithStatusField`, `updateLearningCardReturns200`, `listLearningCardsIncludesMilestonesAndNotes`, `addMilestoneIs201`, `updateMilestoneIs200`, `deleteMilestoneIs204`, `addNoteIs201`, `deleteNoteIs204`, `getLearningCardUnknownIs404`, `deleteLearningCardIs204`, `br9_milestoneOfOtherCardIs404`.

## Risks
- **Coverage gate reads only package `com/quickflow/domain`** (phase-02/03/04 finding, `scripts/lib/loopctl.py`): coverage for `com/quickflow/domain/learning` may be reported as "skipped" by the runner; the tests above aim at ≥ 0.80 either way. I can't change `scripts/`.
- **Derived query on the owning id** (phase-04 finding): a derived `findByIdAndCardId` may not resolve to `card.id`; explicit JPQL (`where m.id = :id and m.card.id = :cardId`) avoids it.
- **Lazy collections outside the transaction**: if open-in-view is off, mapping `milestones`/`notes` in the controller would fail with `LazyInitializationException`; the service returns cards with both collections initialised (or the mapping happens inside the transaction). Checked before coding.
- **Two bags on one entity**: fetching both `List` collections with `JOIN FETCH` in one query fails in Hibernate; they are loaded lazily one after the other. `listLearningCards` then does 1 + 2N reads; acceptable for a single-user app (plan: no pagination), noted, not optimised.
- **Removing a milestone/note through the repository vs. the collection**: as in phase-04, removal goes through the card's collection (orphanRemoval) so the loaded card and the database stay consistent within the request.
- **Deleting a card used by a plan item** (FR-07.6, `sourceRemoved`): plans don't exist yet; delete is a plain hard delete (R-5). The plan-item side belongs to phase-06.
- **springdoc output vs. contract**: as in phase-03/04, 400/404 responses aren't generated from the advice, `@Size` adds `minLength: 0` to `description`, `milestonesDone`/`milestonesTotal` get `format: int32`; logged, not changed.

## Assumptions
- A-1: stored card and milestone titles are **trimmed** and the ≤ 200 check applies to the trimmed value (as for task title and habit name); a card description is stored as given, an empty-string description is stored as `null`. Note text is checked non-blank but stored as given (not trimmed), ≤ 5,000 on the given value.
- A-2: `PUT /api/learning-cards/{id}` takes the full `LearningCardUpdate` (title and status required, missing description → cleared to `null`); it never changes `createdAt`, milestones or notes. The status is only what the user sets (spec A-6): no status change from milestones and no transition rule (any status → any status).
- A-3: `PUT .../milestones/{milestoneId}` takes the full `MilestoneUpdate` (title and done required; missing `targetDate` → cleared to `null`). A target date in the past is allowed (no rule limits it).
- A-4: a milestone or note id that exists but belongs to another card → 404 `NotFoundException` with the same message as a missing one ("Milestone <mid> not found on learning card <id>"), so the other card's data is not revealed (BR-9); an unknown card is checked first → 404 "Learning card <id> not found".
- A-5: order: `GET /api/learning-cards` newest first (contract), by `createdAt` desc then `id` desc; milestones by `id` ascending (creation order, T047); notes by `createdAt` ascending then `id` (oldest first, T047 "ordered by createdAt").
- A-6: notes can't be edited (FR-06.3 lists only add and remove; the contract has no update operation).
- A-7: the card has no `updatedAt` (FR-05.1 and the contract don't list one).
- A-8: `milestonesDone`/`milestonesTotal` are computed from the card's milestones on read; they are also the per-card figures the Dashboard's learning snapshot (phase-07) will sum.

## Open questions
None.
