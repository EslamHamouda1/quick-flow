# test-phase-06: US5 See everything on the Dashboard

## Goal
US5's acceptance criteria (AC-US5-1..7) hold: `getDashboard` figures match the underlying task, habit, plan and
learning data (FR-09.2), then the Dashboard page (`/dashboard`) shows exactly those figures, its quick-adds open
the matching forms and the persistent navigation reaches every page, through Playwright MCP, headless.

Depends on: test-phase-02..05. Layers: backend → frontend.

## API checks (curl)
- [ ] A1 FR-09.1: empty DB → zeros and 0 % (AC-US5-3 "0% when there are none").
- [ ] A2 AC-US5-1: dueToday, overdue and completedToday list exactly the matching non-archived tasks (archived and deleted excluded).
- [ ] A3 AC-US5-3: task completion percent = non-archived Done / non-archived, whole percent.
- [ ] A4 AC-US5-2: today's habits = active habits only, completedToday flags and counts match the data.
- [ ] A5 AC-US5-5: learning snapshot cards per status and milestones done / total match the data; follows a card delete.
- [ ] A6 AC-US5-4 / FR-08.5: activePlans = IN_PROGRESS plans with progress and rest seconds; ticking an item changes the figures on the next read.

## UI checks (Playwright MCP, `/dashboard`)
- [ ] U1 FR-09.1: `dash-greeting`, `metric-*`, `dash-task-percent` equal the API; greeting "Hello" with no name.
- [ ] U2 AC-US5-1: `dash-due-today`, `dash-overdue`, `dash-completed-today` rows = API lists.
- [ ] U3 AC-US5-2: `dash-habits` active only, `dash-habit-done-<id>` for those done, `dash-habit-count` matches.
- [ ] U4 AC-US5-5: `dash-learning-*` and `dash-milestones` match the API.
- [ ] U5 AC-US5-4: `dash-plan-<id>` shows `plan-progress` and `plan-rest-time` going down; a tick on `/plans` shows on the next Dashboard view.
- [ ] U6 AC-US5-6: `quick-add-task`, `-habit`, `-learning`, `-plan` each open the matching add form.
- [ ] U7 AC-US5-7 / FR-10.1: from every page, `nav-dashboard`, `nav-tasks`, `nav-habits`, `nav-learning`, `nav-plans`, `nav-settings` reach their route with the matching `page-title`.
