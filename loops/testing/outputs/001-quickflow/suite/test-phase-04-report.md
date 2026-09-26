# test-phase-04 report: US3 Track learning resources with milestones and notes (attempt 1)

Run 2026-09-26 (Africa/Cairo) against a fresh test database. Evidence folder:
`loops/testing/runs/001-quickflow/suite/test-phase-04/attempt-1/`: `curl.log` (API checks, raw responses with an
`== <check> ... HTTP <code>` line after each), `title201.json` / `desc2001.json` / `title200-desc2000.json` /
`note5001.json` (jq-generated boundary bodies), `ui-results.json` (Playwright MCP return values),
`shot-01..16-*.png` (screenshots), `page-*.yml` / `console-*.log` (Playwright MCP).

API checks ran as direct curl calls (`curl --next`, appended to `curl.log`). UI checks ran through Playwright MCP,
headless, with the `data-testid` selectors from `specs/001-quickflow-frontend/contracts/ui-contract.md`; UI
read-backs used `fetch()` from the page, through the dev-server proxy. The API test cards (2, 3) were deleted
before the UI checks, so `/learning` started empty (card 1 had already been deleted by A7).

**Result: 16/16 pass, 0 fail, 0 unclear. No bugs filed.**

| check | criterion | result | evidence |
|---|---|---|---|
| A1 | AC-US3-1 / FR-05.1 | pass | curl.log `== A1`: card 1 (with description) and card 2 (no description) → 201, `status: NOT_STARTED`, `createdAt` set, empty milestones/notes; list returns both |
| A2 | AC-US3-2 / BR-8 / FR-05.2 | pass | curl.log `== A2`: empty and missing title → 400 `errors[].field=title` ("must not be blank"); 201-char title → 400 field `title` "size must be between 1 and 200"; 2001-char description → 400 field `description` "size must be between 0 and 2000"; 200-char title + 2000-char description → 201 (card 3); no rejected card stored |
| A3 | AC-US3-3 | pass | curl.log `== A3`: PUT IN_PROGRESS, COMPLETED, NOT_STARTED → 200, each read back by GET; `DONE` → 400 field `status` "must be one of [NOT_STARTED, IN_PROGRESS, COMPLETED]"; missing status → 400 field `status`; empty title on update → 400 field `title`; GET after the invalid PUTs unchanged |
| A4 | AC-US3-4 / BR-9 / FR-06.1 | pass | curl.log `== A4`: milestone with target date 2026-10-15 and one without → 201, `cardId: 1`, `done: false`; empty and missing title → 400 field `title`; card 999 → 404; card 1 lists both (milestonesTotal 2), card 2 has none |
| A5 | AC-US3-5 | pass | curl.log `== A5`: PUT done=true → 200, GET milestonesDone 1; done=false → 200, milestonesDone 0; PUT and DELETE of milestone 1 through card 2 → 404 "Milestone 1 not found on learning card 2"; DELETE milestone 2 → 204, again → 404, GET lists only milestone 1 |
| A6 | AC-US3-6 / FR-06.2 | pass | curl.log `== A6`: notes → 201 with `text` and `createdAt`; empty, spaces-only and missing text → 400 field `text`; 5001 chars → 400 "size must be between 1 and 5000"; DELETE note 1 through card 2 → 404; DELETE note 2 → 204, again → 404; GET lists only note 1 |
| A7 | AC-US3-7 / BR-14 / FR-06.3 | pass | curl.log `== A7`: DELETE card 1 (1 milestone, 1 note) → 204; GET, PUT milestone 1, DELETE note 1, POST milestone → 404 "Learning card 1 not found"; list ids [3, 2]; dashboard `learning` notStarted 3 → 2, milestonesTotal 1 → 0; second DELETE → 404 |
| U1 | AC-US3-9 / NFR-5 | pass | shot-01 `empty-state` "No learning cards yet" + `empty-state-action` "Add Learning Card", no form; shot-02 the action opens `card-form` (title, description); shot-14 `/learning?add=1` opens it; `card-cancel` closes it |
| U2 | AC-US3-1 | pass | shot-04 `card-4` with `card-title-text`, description, `card-status` showing Not Started, `card-milestone-count` 0/0, empty state gone; API: card 4 NOT_STARTED with `createdAt` and the entered fields |
| U3 | AC-US3-2 | pass | shot-03 empty title → `error-title` "size must be between 1 and 200", form stays open; API count 0 |
| U4 | AC-US3-3 | pass | `card-status` options exactly Not Started / In Progress / Completed; selecting each → API status IN_PROGRESS, COMPLETED, NOT_STARTED (shot-05 Completed); value kept after reload |
| U5 | AC-US3-4 / AC-US3-5 | pass | shot-06 milestones 3 (target Oct 10, 2026) and 4 added under card 4 (API `cardId` 4); `milestone-done-3` off → count 0/2, API done false (shot-07; the file names shot-07 "done" / shot-08 "undone" are swapped, see note), on → count 1/2, API done true (shot-08); `milestone-delete-4` → row gone, count 1/1, API [3] (shot-09); empty milestone title → `error-title`, nothing added |
| U6 | AC-US3-6 | pass | shot-10 `note-3` / `note-4` show text and "Sep 26, 2026, 12:55 PM" (API `createdAt` 12:55:53 / 12:55:54 +03:00); shot-11 `note-delete-4` → row gone, API notes [3] |
| U7 | AC-US3-8 | pass | shot-12 `card-details-4` shows the milestone and the note; shot-13 `card-expand-4` → details removed, button "Show details"; clicking again shows them ("Hide details") |
| U8 | AC-US3-7 | pass | shot-15 `card-delete-4` (no confirm dialog) → card gone, empty state back; API GET card 4 → 404, list empty |
| U9 | Navigation | pass | shot-16 `nav-learning` ("Learning Resources") from /dashboard → `/learning`, `page-title` is an H1 reading "Learning Resources" |

Note on U5: the first tick used Playwright's `locator.check()`, which reported "Clicking the checkbox did not change
its state" right after the click. The API then showed milestone 3 `done: true` and the count read 1/2, so the click
was saved; the checkbox is re-rendered from the API response. The two later toggles (plain clicks, read after 1 s)
show the checkbox, the count and the API in agreement. Because milestone 3 was already done when they ran, the
first of them turned it off and the second on again, so `shot-07-U5-milestone-done.png` shows the unticked state
and `shot-08-U5-milestone-undone.png` the ticked one (file names chosen before the run). The criterion (done flag saved, count follows) holds; this
is recorded, not filed.

Console: 1 × 400 on POST /api/learning-cards (U3 empty title, expected), 1 × 400 on POST
/api/learning-cards/4/milestones (U5 empty milestone title, expected), 1 × 404 on /api/learning-cards/4 (U8 read-back
after delete, expected); no other errors.

## Unit tests
Not part of this test phase: the runner didn't run unit tests into this attempt folder (no `unit/` folder, and
`task.md` has no `unit_result`). They run in `test-phase-regression`.

## Requirement coverage
| id | covered by |
|---|---|
| FR-05.1 card fields (id, title, description ≤ 2000, createdAt, status default Not Started) | A1, A2, U2 |
| FR-05.2 add/read/update/remove cards; title required ≤ 200 | A1, A2, A3, A7, U2, U3, U4, U8 |
| FR-06.1 milestones: title required ≤ 200, done flag, optional target date, one card | A4, A5, U5 |
| FR-06.2 notes: text required, timestamp | A6, U6 |
| FR-06.3 add/complete/un-complete/remove milestones, add/remove notes; card removal removes both | A5, A6, A7, U5, U6, U8 |
| BR-8 | A2, A3, U3 |
| BR-9 | A4, A5 (wrong card → 404), A6 (wrong card → 404), U5 |
| BR-14 | A7, U8 |
| NFR-5 empty state | U1, U8 |

All ids of this phase are covered. Editing a card's title/description through `card-edit-<id>` has no UI check
because the plan lists none for this phase; the update itself is covered through the API by A3.

## Questions
None.
