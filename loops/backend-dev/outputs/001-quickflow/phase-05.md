# phase-05: US3 Track learning resources with milestones and notes
layer: backend
story_id: US3
spec_phase: 5
depends_on: [phase-02]
## Goal
Learning cards with a status, milestones (done flag, target date) and notes.
## Acceptance criteria
- AC-US3-1: Given no cards, When the user adds a card with a title and optional description/source, Then it is stored with status Not Started and its creation time set.
- AC-US3-2: Given the card form, When the user submits an empty title, Then the card is rejected with a message naming the title field (BR-8).
- AC-US3-3: Given a card, When the user changes its status to Not Started, In Progress or Completed, Then the new status is saved; any other value is rejected.
- AC-US3-4: Given a card, When the user adds a milestone with a title and an optional target date, Then the milestone is stored under that card only, not done (BR-9).
- AC-US3-5: Given a milestone, When the user marks it done (or not done again), Then its done flag is saved; When the user removes it, Then it is no longer returned.
- AC-US3-6: Given a card, When the user adds a note with text, Then it is stored under that card with its timestamp; When the user removes the note, Then it is no longer returned.
- AC-US3-7: Given a card with milestones and notes, When the user removes the card, Then the card, its milestones and its notes are never returned by normal queries again (BR-14).
- AC-US3-8: Given a card, When the user expands it, Then its milestones and notes are shown.
- AC-US3-9: Given no cards exist, When the page shows the grid, Then an empty state guides the user to the Add Learning Card action. (frontend layer)
## Tasks
- [x] T042 [P] [US3] Domain test backend/src/test/java/com/quickflow/domain/learning/LearningCardTest.java: `BR-8` blank title rejected (field `title`), title > 200 rejected, `FR-05.1` description > 2,000 rejected, default status NOT_STARTED, null status rejected; `FR-06.1` milestone title required and ≤ 200, new milestone not done; `FR-06.2` note text required and ≤ 5,000
- [x] T043 [P] [US3] Domain test backend/src/test/java/com/quickflow/domain/learning/LearningServiceTest.java (Mockito, fixed clock): `FR-05.2` create/update/get/delete, unknown card → `NotFoundException`; `BR-9` updating or deleting a milestone/note through another card's id → `NotFoundException`; `FR-06.3` milestone done/un-done, note `createdAt` from the clock
- [x] T044 [P] [US3] Persistence test backend/src/test/java/com/quickflow/persistence/LearningCardRepositoryTest.java: `BR-9` milestone row requires a card, `BR-14` deleting a card deletes its milestones and notes and none is returned, milestone done/total counts
- [x] T045 [P] [US3] Web-slice test backend/src/test/java/com/quickflow/web/learning/LearningCardControllerTest.java: `createLearningCard` 201, `BR-8` 400 `errors[].field == "title"`, unknown status 400, `listLearningCards` includes milestones and notes, `addMilestone` 201, `updateMilestone` 200, `deleteMilestone` 204, `addNote` 201, `deleteNote` 204, `getLearningCard` 404, `deleteLearningCard` 204
- [x] T046 [P] [US3] Create enum `LearningStatus {NOT_STARTED, IN_PROGRESS, COMPLETED}` in backend/src/main/java/com/quickflow/domain/learning/LearningStatus.java
- [x] T047 [US3] Create entity `LearningCard` in backend/src/main/java/com/quickflow/domain/learning/LearningCard.java: `title` "required, non-blank (BR-8), ≤ 200"; `description` "≤ 2,000"; `status` default NOT_STARTED; `createdAt`; `@OneToMany(mappedBy="card", cascade=ALL, orphanRemoval=true)` milestones (ordered by id) and notes (ordered by createdAt)
- [x] T048 [P] [US3] Create entity `LearningMilestone` (`card` ManyToOne `nullable=false` (BR-9), `title` "required, ≤ 200", `done` default false, `targetDate` LocalDate optional) in backend/src/main/java/com/quickflow/domain/learning/LearningMilestone.java
- [x] T049 [P] [US3] Create entity `LearningNote` (`card` ManyToOne not null, `text` "required, non-blank, ≤ 5,000 (A-9)", `createdAt`) in backend/src/main/java/com/quickflow/domain/learning/LearningNote.java
- [x] T050 [US3] Create `LearningCardRepository`, `LearningMilestoneRepository` (find by id and card id), `LearningNoteRepository` (find by id and card id) in backend/src/main/java/com/quickflow/domain/learning/
- [x] T051 [US3] Create `LearningService` (`create, get, list, update, delete, addMilestone, updateMilestone, deleteMilestone, addNote, deleteNote`) in backend/src/main/java/com/quickflow/domain/learning/LearningService.java
- [x] T052 [P] [US3] Create records `LearningCardCreateRequest`, `LearningCardUpdateRequest`, `LearningCardResponse` (with `milestonesDone`, `milestonesTotal`), `MilestoneCreateRequest`, `MilestoneUpdateRequest`, `MilestoneResponse`, `NoteCreateRequest`, `NoteResponse` in backend/src/main/java/com/quickflow/web/learning/
- [x] T053 [US3] Create `LearningCardController` with `listLearningCards, createLearningCard, getLearningCard, updateLearningCard, deleteLearningCard, addMilestone, updateMilestone, deleteMilestone, addNote, deleteNote` in backend/src/main/java/com/quickflow/web/learning/LearningCardController.java
