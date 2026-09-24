# phase-05-report: US3 Track learning resources with milestones and notes (backend-dev), attempt 1

Plan: `phase-05-test.md` (new on this attempt). Evidence folder:
`loops/testing/runs/001-quickflow/backend-dev/phase-05/attempt-1/`.

Note: `phase-05-verify.sh` could not be executed (the session's permission check asked for
approval, as in phases 01–04), so its checks were run as direct curl calls against the same URLs
with the same inputs. Each response (status line, headers, body) is saved as `http/<name>.http`;
`curl.log` lists every call's status code in order and the ids created; long request bodies are
`req/*.json`; the live swagger is `C10-api-docs.json`. Run 2026-09-24 23:10–23:14 +03:00.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US3-1, FR-05.1 | pass | `http/C1.http` → 201 `application/json`, `status NOT_STARTED`, `createdAt 2026-09-24T23:10:51.493837825+03:00` (response `Date` 20:10:51 GMT, same instant), title/description echoed, `milestones []`, `notes []`, `milestonesDone 0`, `milestonesTotal 0`; `C1-get` → 200 same values (createdAt `.493838`, µs rounding); `C1-title-only` → 201 `description null`, `NOT_STARTED` |
| C2 | AC-US3-2, BR-8 | pass | `http/C2-empty`, `C2-blank`, `C2-missing`, `C2-put-empty` → each 400 `application/problem+json` with `errors[{field:"title", message:"must not be blank"}]` (empty also "size must be between 1 and 200"); `C2-get1-unchanged` card 1 unchanged |
| C3 | FR-05.1, FR-05.2 | pass | `http/C3-t201` → 400 `{field:"title","size must be between 1 and 200"}`; `C3-t200` → 201 id 3, title length 200; `C3-d2001` → 400 `{field:"description","size must be between 0 and 2000"}`; `C3-d2000` → 201 id 4, description length 2000 |
| C4 | AC-US3-3, FR-05.2 | pass | card 5: `http/C4-IN_PROGRESS`, `C4-COMPLETED`, `C4-NOT_STARTED` → 200 with that status and the new title/description; each `…-get` shows the same; `createdAt` 23:12:01.928816 unchanged throughout; `C4-done` (`"DONE"`) → 400 problem `{field:"status","must be one of [NOT_STARTED, IN_PROGRESS, COMPLETED]"}`; `C4-nostatus` → 400 `{field:"status","must not be null"}`; `C4-after` unchanged (NOT_STARTED, "C4 NOT_STARTED"); `C4-unk` → 404 problem "Learning card 999999 not found" |
| C5 | AC-US3-4, BR-9, FR-06.1 | pass | `http/C5-m1` → 201 `{id 1, cardId 6, done false, targetDate "2026-10-15"}`; `C5-m2` → 201 `{id 2, cardId 6, done false, targetDate null}`; `C5-ga` card 6 lists milestones 1, 2 (`milestonesTotal 2`); `C5-gb` card 7 `milestones []`; `C5-blank` → 400 `{field:"title","must not be blank"}`; `C5-m201` → 400 `{field:"title","size must be between 1 and 200"}`; `C5-unk` → 404 problem; `C5-ga2` still 2 milestones |
| C6 | AC-US3-5, BR-9, FR-06.3 | pass | `http/C6-done` → 200 `done true`; `C6-g1` milestone 1 `done true`, `milestonesDone 1 / milestonesTotal 2`; `C6-undone` → 200 `done false`; `C6-g2` `done false`, 0/2; `C6-otherput`, `C6-otherdel` (via card 7) → 404 problem "Milestone 1 not found on learning card 7"; `C6-g3` milestone 1 unchanged; `C6-del` → 204; `C6-g4` milestones = [2], `milestonesTotal 1`; `C6-del2`, `C6-put-gone` → 404 |
| C7 | AC-US3-6, FR-06.2, A-9 | pass | `http/C7-n1` → 201 `{id 1, cardId 6, text, createdAt 2026-09-24T23:12:55.55528958+03:00}`; `C7-g1` card 6 shows note 1; `C7-gb` card 7 `notes []`; `C7-blank`, `C7-missing` → 400 `{field:"text","must not be blank"}`; `C7-5001` → 400 `{field:"text","size must be between 1 and 5000"}`; `C7-5000` → 201 id 2, text length 5000; `C7-unk` → 404; `C7-other` (via card 7) → 404 "Note 1 not found on learning card 7" and `C7-g-after-other` still has note 1; `C7-del` → 204; `C7-g2` notes = [2]; `C7-del2` → 404 |
| C8 | AC-US3-7, BR-14, FR-06.3 | pass | card 8 with milestones 3, 4 and notes 3, 4 (`http/C8-pre`); `C8-del` → 204; `C8-get` → 404 `application/problem+json` "Learning card 8 not found"; `C8-list` ids [7,6,5,4,3,2,1] (no 8), milestone ids listed [2], note ids [2] (none of 3, 4); `C8-mput`, `C8-mdel`, `C8-ndel`, `C8-madd`, `C8-nadd`, `C8-put`, `C8-del2` → 404 each |
| C9 | AC-US3-8 (backend part), contract "newest first" | pass | `http/C9-get` card 6 embeds milestones [2] and notes [2]; `C9-list` → card 6 embeds the same, every card has `milestones` and `notes` arrays, order ids [7,6,5,4,3,2,1] = creation order reversed |
| C10 | swagger | pass | `C10-api-docs.json` (live, 200): `listLearningCards`, `createLearningCard` (201), `getLearningCard`, `updateLearningCard`, `deleteLearningCard` (204), `addMilestone` (201), `updateMilestone`, `deleteMilestone` (204), `addNote` (201), `deleteNote` (204) on their paths; schemas `LearningCard`, `LearningCardCreate`, `LearningCardUpdate`, `Milestone`, `MilestoneCreate`, `MilestoneUpdate`, `Note`, `NoteCreate`, `LearningStatus` enum [NOT_STARTED, IN_PROGRESS, COMPLETED]; live, `backend/target/openapi.json` and the copy have identical learning paths/schemas; target and copy byte-identical (`curl.log`) |

AC-US3-9 is frontend layer: not checked here.

Observations, not checks (no criterion of this phase covers them):
- As in phases 03–04, the generated swagger lists only success responses; the contract's `400` /
  `404` responses on the learning operations are not in `openapi.json`. The live API returns them.
- An empty title (`http/C2-empty.http`, `C2-put-empty.http`) gets two `errors[]` entries for `title`
  (size and blank); both name the field.
- `"status":"DONE"` returns `detail "Malformed request body"` (the unknown enum value is rejected at
  parse time) with an `errors[]` entry for `status`; missing status returns `detail "Validation failed"`.

## Unit tests
From `unit/unit-result.json`: passed 194, failed 0, errors 0, skipped 0; exit code 0; outcome **pass**;
coverage: reported as skipped ("no classes in com/quickflow/domain").

The skip is wrong again (same runner exact-package match as phases 02–04). From
`unit/coverage/jacoco.csv`, domain line coverage (`com.quickflow.domain.*`) is **399/409 = 97.6%**
(learning 126/127 = 99.2%, task 137/142, habit 118/122, common 18/18), above `min_line` 0.80. The
unit rule passes either way. The four learning test classes (`LearningCardTest`,
`LearningServiceTest`, `LearningCardRepositoryTest`, `LearningCardControllerTest`) have no failures
or errors (`unit/surefire/`).

## Requirement coverage
| id | unit tests | checks |
|---|---|---|
| FR-05.1 card fields, default Not Started, description ≤ 2,000 | `LearningCardTest.fr05_1_newCardNotStartedWithCreatedAt`, `fr05_1_description2001CharsRejected`, `fr05_1_description2000CharsAccepted`, `fr05_1_nullDescriptionAccepted`, `fr05_1_emptyDescriptionStoredAsNull`; `LearningCardControllerTest.createLearningCardReturns201NotStarted` | C1, C3 |
| FR-05.2 add/read/update/remove cards, title ≤ 200, status values | `LearningCardTest.fr05_2_*` (title201/200, titleTrimmed, nullStatusRejectedOnUpdate, updateChangesTitleDescriptionStatus); `LearningServiceTest.fr05_2_*` (create, update, delete, list, unknownCardNotFound ×8, validation not saved); `LearningCardRepositoryTest.fr05_2_listNewestFirst`; `LearningCardControllerTest.updateLearningCardReturns200`, `unknownStatusIs400WithStatusField`, `missingStatusIs400WithStatusField`, `getLearningCardUnknownIs404`, `deleteLearningCardIs204` | C1, C3, C4, C9 |
| BR-8 title required | `LearningCardTest.br8_blankTitleRejectedWithFieldTitle`, `br8_nullTitleRejected`; `LearningCardControllerTest.br8_blankTitleIs400WithTitleField` | C2 |
| FR-06.1 milestones: title required ≤ 200, done flag, optional target date | `LearningCardTest.fr06_1_*` (6); `LearningServiceTest.fr06_1_addMilestoneReturnsNotDoneMilestoneOnCard`; `LearningCardControllerTest.addMilestoneIs201`, `missingMilestoneDoneIs400WithDoneField` | C5 |
| BR-9 milestone belongs to exactly one card | `LearningServiceTest.br9_updateMilestoneOfOtherCardNotFound`, `br9_deleteMilestoneOfOtherCardNotFound`, `br9_deleteNoteOfOtherCardNotFound`; `LearningCardRepositoryTest.br9_milestoneWithoutCardRejected`, `br9_milestoneFoundOnlyByItsOwnCard`; `LearningCardControllerTest.br9_milestoneOfOtherCardIs404` | C5, C6, C7 |
| FR-06.2 notes: text required (≤ 5,000, A-9), creation timestamp | `LearningCardTest.fr06_2_*` (blank, 5001, 5000, notTrimmed, remove); `LearningServiceTest.fr06_2_noteCreatedAtFromClock`; `LearningCardRepositoryTest.fr06_2_notesOrderedByCreatedAt`; `LearningCardControllerTest.addNoteIs201`, `blankNoteTextIs400WithTextField` | C7 |
| FR-06.3 complete/un-complete/remove milestones, add/remove notes, card removal removes them | `LearningCardTest.fr06_3_milestoneCounts`; `LearningServiceTest.fr06_3_*` (3); `LearningCardRepositoryTest.fr06_3_milestoneDoneAndTotalCounts`, `orphanRemoval_removedMilestoneRowDeleted`; `LearningCardControllerTest.updateMilestoneIs200`, `deleteMilestoneIs204`, `deleteNoteIs204` | C6, C7, C8 |
| BR-14 deleted card, milestones, notes not returned | `LearningCardRepositoryTest.br14_deletingCardDeletesMilestonesAndNotes`; `LearningServiceTest.fr05_2_deleteRemovesCard` | C8 |

None uncovered.

## Questions
None.
