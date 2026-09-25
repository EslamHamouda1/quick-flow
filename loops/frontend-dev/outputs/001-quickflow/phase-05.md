# phase-05: US3 Track learning resources with milestones and notes
layer: frontend
story_id: US3
spec_phase: 5
depends_on: [phase-02, backend-dev:US3]
## Goal
The Learning Resources page: card grid, expandable milestones and notes, status, remove.
## Acceptance criteria
- AC-US3-1: Given no cards, When the user adds a card with a title and optional description/source, Then it is stored with status Not Started and its creation time set.
- AC-US3-2: Given the card form, When the user submits an empty title, Then the card is rejected with a message naming the title field (BR-8).
- AC-US3-3: Given a card, When the user changes its status to Not Started, In Progress or Completed, Then the new status is saved; any other value is rejected.
- AC-US3-4: Given a card, When the user adds a milestone with a title and an optional target date, Then the milestone is stored under that card only, not done (BR-9).
- AC-US3-5: Given a milestone, When the user marks it done (or not done again), Then its done flag is saved; When the user removes it, Then it is no longer returned.
- AC-US3-6: Given a card, When the user adds a note with text, Then it is stored under that card with its timestamp; When the user removes the note, Then it is no longer returned.
- AC-US3-7: Given a card with milestones and notes, When the user removes the card, Then the card, its milestones and its notes are never returned by normal queries again (BR-14).
- AC-US3-8: Given a card, When the user expands it, Then its milestones and notes are shown.
- AC-US3-9: Given no cards exist, When the page shows the grid, Then an empty state guides the user to the Add Learning Card action.
## Tasks
- [x] T026 [US3] Check that the generated `LearningService` has `listLearningCards`, `createLearningCard`, `updateLearningCard`, `deleteLearningCard`, `addMilestone`, `updateMilestone`, `deleteMilestone`, `addNote`, `deleteNote`; check the Learning section of contracts/ui-contract.md covers AC-US3-1..9; raise a question for anything missing
- [x] T027 [P] [US3] Create the card form component (title, description/source, field errors; edit also saves status) in frontend/src/app/pages/learning/card-form.ts (AC-US3-1, AC-US3-2)
- [x] T028 [P] [US3] Create the learning card component (title, status select saving via `updateLearningCard`, milestone count, expand toggle, milestone list with done checkbox/remove and add row, note list with time in the app zone/remove and add row, field errors per sub-form) in frontend/src/app/pages/learning/learning-card.ts (AC-US3-3..6, AC-US3-8)
- [x] T029 [US3] Implement the Learning Resources page (card grid, add flow also from `?add=1`, card delete, re-read after every change, empty state) in frontend/src/app/pages/learning/learning.page.ts (AC-US3-1, AC-US3-7, AC-US3-9)
