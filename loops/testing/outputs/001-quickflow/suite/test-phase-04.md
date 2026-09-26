# test-phase-04: US3 Track learning resources with milestones and notes

## Goal
US3's acceptance criteria (AC-US3-1..9) and rules BR-8, BR-9, BR-14 hold through the API, then the same
behaviour holds on the Learning Resources page (`/learning`) through Playwright MCP, headless.

Depends on: test-phase-01. Layers: backend → frontend.

## API checks (curl)
- [ ] A1 AC-US3-1 / FR-05.1: create with title (and optional description) → 201, `NOT_STARTED`, createdAt set.
- [ ] A2 AC-US3-2 / BR-8 / FR-05.2: empty title → 400 naming `title`; 201 chars → 400; 2001-char description → 400.
- [ ] A3 AC-US3-3: status NOT_STARTED / IN_PROGRESS / COMPLETED saved; any other value → 400.
- [ ] A4 AC-US3-4 / BR-9 / FR-06.1: add milestone (title, optional target date) → 201, not done, only under that card; empty title → 400.
- [ ] A5 AC-US3-5: milestone done / not done saved; wrong card id → 404; delete → gone.
- [ ] A6 AC-US3-6 / FR-06.2: add note → 201 with text and timestamp; blank → 400; delete → gone.
- [ ] A7 AC-US3-7 / BR-14 / FR-06.3: delete card → 204; card, milestones and notes never returned (404 / absent).

## UI checks (Playwright MCP, `/learning`)
- [ ] U1 AC-US3-9 / NFR-5: `empty-state` + `empty-state-action` opening `card-form`; `/learning?add=1` opens it.
- [ ] U2 AC-US3-1: create through `card-form` → `card-<id>` with `card-status` Not Started.
- [ ] U3 AC-US3-2: empty title → `error-title`, nothing saved.
- [ ] U4 AC-US3-3: `card-status` change to each of the three values → saved (API read-back).
- [ ] U5 AC-US3-4 / AC-US3-5: add milestone, tick `milestone-done-<id>` on / off, `card-milestone-count` follows, `milestone-delete-<id>` removes it.
- [ ] U6 AC-US3-6: add note (`note-<id>` shows text and time), `note-delete-<id>` removes it.
- [ ] U7 AC-US3-8: `card-expand-<id>` shows / hides `card-details-<id>` with milestones and notes.
- [ ] U8 AC-US3-7: `card-delete-<id>` → card gone, API 404.
- [ ] U9 Navigation: `nav-learning` reaches `/learning` with `page-title` "Learning Resources".
