# test-phase-03: US2 Track recurring habits

## Goal
US2's acceptance criteria (AC-US2-1..9) and rules BR-6, BR-7, BR-14 hold through the API, then the same
behaviour holds on the Habits page (`/habits`) through Playwright MCP, headless.

Depends on: test-phase-01. Layers: backend → frontend.

## API checks (curl)
- [ ] A1 AC-US2-1 / FR-03.2: create Daily and Weekly habits → 201, active, createdAt set.
- [ ] A2 AC-US2-2 / BR-6: empty name and 151-char name → 400 naming `name`; 150 → 201; invalid frequency → 400.
- [ ] A3 AC-US2-3 / FR-04.1: complete with no date → 201, one record for today (app zone); a future date → 400.
- [ ] A4 AC-US2-4 / BR-7 / FR-04.2: complete again for the same date → 409, still one record.
- [ ] A5 AC-US2-5 / FR-04.5: frequency, completedToday / done this Monday–Sunday week, and streak reflect the completions.
- [ ] A6 AC-US2-6: edit name, description, frequency → saved.
- [ ] A7 AC-US2-7 / FR-04.4: deactivate → absent from `active=true` list and dashboard habits, completions kept, complete → rejected (409); activate → completable again.
- [ ] A8 FR-04.3: undo a completion for a date → 204, record gone.
- [ ] A9 AC-US2-8 / BR-14: delete → 204; habit and completions 404 / absent from every list and the dashboard.

## UI checks (Playwright MCP, `/habits`)
- [ ] U1 AC-US2-9 / NFR-5: `empty-state` + `empty-state-action` opening `habit-form`; `/habits?add=1` opens it.
- [ ] U2 AC-US2-1: create through `habit-form` → `habit-card-<id>` with `habit-frequency-label`.
- [ ] U3 AC-US2-2: empty / 151-char name → `error-name`, nothing saved.
- [ ] U4 AC-US2-3 / AC-US2-5: `habit-toggle-<id>` → `habit-period-done` "Done today" / "Done this week", `habit-streak` 1, one API record.
- [ ] U5 AC-US2-4: completion already made through the API, then toggle → `notice-error` telling it is already complete; still one record.
- [ ] U6 AC-US2-6: `habit-edit-<id>` prefilled, save → card and API updated.
- [ ] U7 AC-US2-7: `habit-deactivate-<id>` → `habit-inactive` badge, toggle disabled; `habit-activate-<id>` → enabled.
- [ ] U8 AC-US2-8: `habit-delete-<id>` → card gone, API 404.
- [ ] U9 Navigation: `nav-habits` reaches `/habits` with `page-title` "Habits".
