# phase-05-test: US3 Track learning resources with milestones and notes (frontend-dev)

Criteria listed in `loops/frontend-dev/outputs/001-quickflow/phase-05.md` (compare on later attempts):
AC-US3-1 … AC-US3-9, text as in `specs/001-quickflow-frontend/spec.md` lines 93–101.

Selectors from `specs/001-quickflow-frontend/contracts/ui-contract.md` (Learning Resources `/learning`). Visible texts
only where the criteria or contract state them. Server-side values (status, createdAt, milestone/note lists) are read
back through the API on `http://localhost:8080` as supporting evidence.

All steps use Playwright MCP (headless) on `http://localhost:4200`. Screenshots go to
`<run_dir>/shot-<NN>-<check id>.png`.

| check | criterion | Playwright MCP steps | expected |
|---|---|---|---|
| C1 | AC-US3-9 | fresh DB; navigate `/learning`; read the empty state; click its action; also `/learning?add=1` | an empty state that guides to the Add Learning Card action; its action opens `card-form`; `?add=1` opens `card-form` |
| C2 | AC-US3-1 | `card-add` → fill `card-title`, `card-description` → `card-save`; read `card-grid`; API `GET /api/learning-cards` | a `card-<id>` with `card-title-text` = title, `card-status` "Not Started", `card-milestone-count` "0/0"; API status NOT_STARTED, `createdAt` set; also a card with title only is created |
| C3 | AC-US3-2 | open form; save with empty title | form stays open; a message naming the title field; no card added (API list unchanged) |
| C4 | AC-US3-3 | change `card-status` to In Progress, then Completed, then Not Started; reload; API get card | select keeps each value after reload; API status matches; select offers only Not Started / In Progress / Completed |
| C5 | AC-US3-4, AC-US3-8 | create two cards; `card-expand-<id>` on card A → `card-details-<id>` shown; fill `milestone-title-input-<A>` + `milestone-date-input-<A>` → `milestone-add-<A>`; add one without date | `milestone-<id>` rows under card A only, `milestone-done-<id>` unchecked; card A count "0/2"; card B count "0/0"; API milestones under A only, done false |
| C6 | AC-US3-5 | tick `milestone-done-<id>`; reload; untick; then `milestone-delete-<id>` | done saved (count "1/2", API done true), then false again; after delete the row is gone and the API no longer returns it |
| C7 | AC-US3-6 | fill `note-text-input-<A>` → `note-add-<A>`; read `note-<id>`; then `note-delete-<id>` | `note-<id>` shows the text and a time; API note under card A with timestamp; after delete the row is gone and the API no longer returns it |
| C8 | AC-US3-8 | collapse and expand card A again via `card-expand-<A>` | `card-details-<A>` hidden when collapsed and showing milestones and notes when expanded |
| C9 | AC-US3-7 | card A with a milestone and a note; `card-delete-<A>` | card gone from `card-grid`; API `GET /api/learning-cards/{A}` → 404, not in list; its milestones/notes not returned |

Unit tests: the frontend layer has no `test` command and no `coverage` (`unit-result.json` outcome `none`).
