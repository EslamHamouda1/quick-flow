# test-phase-05: US4 Build and follow Todo Plans

## Goal
US4's acceptance criteria (AC-US4-1..13) and rules BR-10..BR-14 hold through the API, then the same behaviour
holds on the Todo Plans page (`/plans`) through Playwright MCP, headless, with time-based behaviour checked in
real time.

Depends on: test-phase-02, test-phase-03, test-phase-04. Layers: backend → frontend.

## API checks (curl)
- [x] A1 AC-US4-1 / FR-07.1 / FR-07.2: plan from a task, a habit and a card (title, duration, start, end, priority) → 201, three items not done, createdAt set.
- [x] A2 AC-US4-2 / BR-10: no items, unknown id, deleted source → 400.
- [x] A3 AC-US4-3 / BR-11 / FR-07.4: end = start and end < start → 400 naming `endDateTime` (create and update).
- [x] A4 AC-US4-4 / FR-08.3: tick / un-tick items → done flag saved, progress = done/total whole percent; dashboard reflects it on the next read.
- [x] A5 AC-US4-5 / BR-13 / FR-07.5: TASK item done → task DONE with completedAt; un-tick leaves the task; HABIT / LEARNING_RESOURCE items change nothing.
- [x] A6 AC-US4-6 / BR-12: future start → `NOT_STARTED`, no rest time.
- [x] A7 AC-US4-7 / FR-08.2 / FR-08.4: start passed, end not → `IN_PROGRESS`, rest seconds ≈ end − now and decreasing on two reads; all items done → `COMPLETED`; un-tick → `IN_PROGRESS`; end passed → `COMPLETED`.
- [x] A8 AC-US4-8 / NFR-4: repeated reads give the same status from stored times, flags and now.
- [x] A9 AC-US4-10: list groups active/upcoming and completed; active/upcoming by priority order.
- [x] A10 AC-US4-11: completed plans carry their percentage done.
- [x] A11 FR-07.6: deleting a source keeps the item marked `sourceRemoved`, still counted; source never returned.
- [x] A12 AC-US4-12 / BR-14: delete plan → 204, 404 after; referenced task, habit and card unchanged.

## UI checks (Playwright MCP, `/plans`)
- [x] U1 AC-US4-13 / NFR-5: `empty-state` + `empty-state-action` opening `plan-builder`; `/plans?add=1` opens it.
- [x] U2 AC-US4-1: builder selects `plan-source-task-/habit-/learning-<id>`, save → `plan-<id>` Not Started 0% with its items.
- [x] U3 AC-US4-2: no item selected → `error-items`, nothing saved.
- [x] U4 AC-US4-3: end equal to / before start → `error-endDateTime`.
- [x] U5 AC-US4-4 / AC-US4-5: `plan-item-done-<itemId>` → `plan-progress` updates at once; task source Done in the API, habit / card unchanged.
- [x] U6 AC-US4-6 / AC-US4-7 / NFR-2: Not Started plan has no `plan-rest-time`; In Progress plan shows `plan-rest-time` ≈ end − now, lower when read again a few seconds later.
- [x] U7 AC-US4-9 / FR-08.1: page open, plan starting about a minute ahead → `notice` and `plan-started-<id>` appear at the start time without reload.
- [x] U8 AC-US4-8: reload → same status, progress and rest time as the API.
- [x] U9 AC-US4-10 / AC-US4-11: `plans-active` ordered by priority order, `plans-completed` shows each plan's percentage.
- [x] U10 FR-07.6: a plan whose source was deleted shows `plan-item-removed-<itemId>`.
- [x] U11 FR-07.3: `plan-edit-<id>` prefilled, save → updated.
- [x] U12 AC-US4-12: `plan-delete-<id>` → plan gone, API 404, sources unchanged.
- [x] U13 Navigation: `nav-plans` reaches `/plans` with `page-title` "Todo Plans".
