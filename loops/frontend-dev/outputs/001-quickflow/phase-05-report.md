# phase-05-report: US3 Track learning resources (frontend-dev), attempt 1

**Verdict: pass.** Re-run of attempt 1 after the earlier harness failure (Playwright MCP did not connect).
Test plan `phase-05-test.md` reused unchanged: the phase's criteria are still AC-US3-1..9 with the same text.

Evidence folder: `loops/testing/runs/001-quickflow/frontend-dev/phase-05/attempt-1/`. Playwright MCP was headless
against `http://localhost:4200/learning`, starting on a fresh test DB. API read-backs went through the dev-server proxy (`/api/...`).

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | AC-US3-9 | pass | fresh DB: `empty-state` "No learning cards yet" with `empty-state-action` "Add Learning Card" (shot-01); clicking it opens `card-form` (shot-02); `/learning?add=1` opens `card-form` (shot-03) |
| C2 | AC-US3-1 | pass | card with title and description, then a title-only card: `card-1`/`card-2` in `card-grid`, `card-title-text` matches, `card-status` = Not Started, `card-milestone-count` "0/0", notice "Learning card created". API: both `NOT_STARTED`, `createdAt` 2026-09-25T23:19:21+03:00 / 23:19:26+03:00, card B `description` null (shot-05) |
| C3 | AC-US3-2 | pass | Save with an empty title: API 400 (console log), form stays open, `error-title` "size must be between 1 and 200" under the Title field, API list still `[]` (shot-04) |
| C4 | AC-US3-3 | pass | card A select changed to In Progress, then Completed, then Not Started. After each reload the select keeps the value and the API returns `IN_PROGRESS`, `COMPLETED`, `NOT_STARTED`. The only options are Not Started / In Progress / Completed. A PUT with `status: "DONE"` returns 400 `errors[{field: status, message: "must be one of [NOT_STARTED, IN_PROGRESS, COMPLETED]"}]` (shot-06) |
| C5 | AC-US3-4, AC-US3-8 | pass | on card A: milestone "Finish module 1" with date 2026-10-15, and "Build a sample app" with no date. Rows `milestone-1`/`milestone-2` are under `card-details-1` with `milestone-done` unchecked; count "0/2" on A, "0/0" on B. API: both have `cardId` 1 and `done` false, `targetDate` 2026-10-15 / null; card B `milestones` is `[]` (shot-07) |
| C6 | AC-US3-5 | pass | tick milestone 1: count "1/2", API `done` true, still checked after a reload (shot-08). Untick: "0/2", API false. Remove milestone 2: the row is gone, count "0/1", API returns only milestone 1 (shot-09) |
| C7 | AC-US3-6 | pass | two notes added on card A: `note-1`/`note-2` show the text and "Sep 25, 2026, 11:20 PM" (app zone). API: `cardId` 1, `createdAt` 23:20:55+03:00 / 23:20:59+03:00, card B `notes` is `[]` (shot-10). Remove note 2: the row is gone and the API returns only note 1 (shot-11) |
| C8 | AC-US3-8 | pass | `card-expand-1` collapses the card: `card-details-1` is gone and the button reads "Show details" (shot-12). Expanding again shows "Finish module 1 / Oct 15, 2026" and the note "Signals replace zone.js change detection" (shot-13) |
| C9 | AC-US3-7 | pass | card A, with milestone 1 and note 1, removed through `card-delete-1`: it is gone from `card-grid`, only card B remains. API `GET /api/learning-cards/1` returns 404 "Learning card 1 not found"; the list has ids `[2]` with no milestones and no notes (shot-14) |

Console errors seen during the run: all are expected. They are the 400s of C3 and C4 and the 404 of C9, plus my own probe calls (a CORS-blocked direct call to :8080, and a 405 from a PATCH to a milestone of the deleted card).
The 405 is not used as evidence. The app showed no other console errors.

## Unit tests
From `unit/unit-result.json`: layer `frontend`, outcome `none` ("no unit test command configured"). Passed: n/a,
failed: n/a, coverage: n/a (no `coverage` configured for this layer).

## Requirement coverage
| requirement | unit tests | checks |
|---|---|---|
| AC-US3-1 | none (layer has none) | C2 |
| AC-US3-2 / BR-8 | none | C3 |
| AC-US3-3 | none | C4 |
| AC-US3-4 / BR-9 | none | C5 |
| AC-US3-5 | none | C6 |
| AC-US3-6 | none | C7 |
| AC-US3-7 / BR-14 | none | C9 |
| AC-US3-8 | none | C5, C8 |
| AC-US3-9 | none | C1 |

Every requirement of this phase is covered by at least one check. None is covered by unit tests, because the layer has none.

## Questions
None. No check is `unclear`.
