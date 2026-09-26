# test-phase-E2E: end-to-end flow

## Goal
The end-to-end flow of Task_PRD.md §11 Testing Requirements, **exactly as listed**: "create task, complete
task, create habit, complete habit, add learning card with milestones, build a plan from existing items, mark
plan items done, verify plan achievement and dashboard update accordingly." Run in one browser session through
the UI (Playwright MCP, headless) on a fresh DB, with API read-backs after each step; live, time-based
behaviour is checked in real time.

Depends on: test-phase-02..07. Layers: frontend (UI steps), backend (read-backs).

## Steps
- [ ] E1 Create task: two tasks through `/tasks` (task A, task B) → rows shown, API returns them as `TODO`.
- [ ] E2 Complete task: `task-complete-<A>` → Done in the list and the API (completedAt set).
- [ ] E3 Create habit: a Daily habit through `/habits` → card active.
- [ ] E4 Complete habit: `habit-toggle-<id>` → "Done today", one completion for today in the API.
- [ ] E5 Add learning card with milestones: card through `/learning`, two milestones added under it → `card-milestone-count` "0/2".
- [ ] E6 Build a plan from existing items: `/plans` builder selects task B, the habit and the card, start about one minute ahead, end later → plan Not Started 0 %, no rest time.
- [ ] E7 Live start: with `/plans` open, wait for the start time → plan-start `notice` / `plan-started-<id>`, status In Progress, `plan-rest-time` shown; read twice a few seconds apart, the second is lower.
- [ ] E8 Mark plan items done: tick items one by one → `plan-progress` 33 % → 66 % → 100 %; task B becomes Done in the API (BR-13), habit and card unchanged.
- [ ] E9 Verify plan achievement: all items done → plan Completed (in `plans-completed`) with 100 %.
- [ ] E10 Dashboard update accordingly: `/dashboard` shows completed today = A and B, task percent 100 %, habits 1/1 done, learning 1 Not Started and milestones 0/2, the plan no longer among in-progress plans, and every figure equals `GET /api/dashboard`.
