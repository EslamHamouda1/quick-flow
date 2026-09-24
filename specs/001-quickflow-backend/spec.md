# Feature Specification: QuickFlow

**Feature Branch**: `001-quickflow`

**Created**: 2026-09-24

**Status**: Draft

**Input**: User description: "Task_PRD.md: QuickFlow, a single-user personal productivity app that unifies tasks, recurring habits, learning resources (cards with milestones and notes) and time-boxed Todo Plans built from those items, with a dashboard summary and a settings page."

Ids used in this spec: user stories `US1..US6`; acceptance criteria `AC-USn-k`; functional requirements
`FR-01..FR-10` (FR-01..FR-09 are the PRD's §6 ids, FR-10 comes from PRD §4/§8); business rules
`BR-1..BR-14` (PRD §7 rules 1–14, same numbering); non-functional requirements `NFR-1..NFR-5` (PRD §10).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manage tasks (Priority: P1)

As the user, I create, edit, complete, archive, restore and delete tasks, and I find them again with
free-text search, status / priority / due-date filters and sorting. Tasks whose due date has passed
without being done are shown as overdue.

**Why this priority**: Tasks are the core of the app and the most common source of plan items; the
Dashboard and Todo Plans build on them.

**Independent Test**: Create, edit, complete, archive, restore, search, filter and delete tasks on the
Tasks page alone; delivers a working task manager.

**Acceptance Scenarios**:

1. **AC-US1-1**: **Given** no tasks, **When** the user creates a task with a title, optional description, priority and due date, **Then** the task is stored with status Todo, not archived, and with its creation and update times set.
2. **AC-US1-2**: **Given** the task form, **When** the user submits an empty title or a title longer than 200 characters, **Then** the task is rejected with a message naming the title field (BR-1).
3. **AC-US1-3**: **Given** the task form, **When** the user submits a description longer than 2,000 characters, **Then** the task is rejected with a message naming the description field (BR-2).
4. **AC-US1-4**: **Given** a task, **When** a status other than Todo / In Progress / Done or a priority other than Low / Medium / High is submitted, **Then** the change is rejected (BR-3).
5. **AC-US1-5**: **Given** an existing task, **When** the user edits its title, description, status, priority or due date, **Then** the changes are saved and its update time changes.
6. **AC-US1-6**: **Given** a task, **When** the user marks it completed, **Then** its status is Done (BR-5).
7. **AC-US1-7**: **Given** a task, **When** the user archives it, **Then** it no longer appears in the default task list but can still be listed by asking for archived tasks, and restoring it brings it back to the default list (BR-4).
8. **AC-US1-8**: **Given** a task, **When** the user deletes it, **Then** it is never returned by any normal list, search, lookup or summary again (BR-14).
9. **AC-US1-9**: **Given** several tasks, **When** the user searches by text, **Then** only tasks whose title contains that text (ignoring case) are listed.
10. **AC-US1-10**: **Given** several tasks, **When** the user filters by status, priority and/or due date and chooses a sort (due date or creation date), **Then** only matching tasks are listed, in that order.
11. **AC-US1-11**: **Given** a task with a due date before today that is not Done and not archived, **When** the user asks for overdue tasks, **Then** that task is listed as overdue; a task due today, a Done task or an archived task is not.
12. **AC-US1-12**: **Given** no tasks match (or none exist), **When** the Tasks page shows the list, **Then** an empty state guides the user to the Add Task action.

---

### User Story 2 - Track recurring habits (Priority: P1)

As the user, I create daily or weekly habits, mark a habit complete for today (never twice for the
same date), see my completion progress and streak, and deactivate or remove habits I no longer track.

**Why this priority**: Habits are the second core feature and appear on the Dashboard and in plans.

**Independent Test**: Create a daily and a weekly habit on the Habits page, complete one for today,
try to complete it again, deactivate one, delete one; delivers a working habit tracker.

**Acceptance Scenarios**:

1. **AC-US2-1**: **Given** no habits, **When** the user creates a habit with a name, optional description and frequency Daily or Weekly, **Then** it is stored as active with its creation time set.
2. **AC-US2-2**: **Given** the habit form, **When** the user submits an empty name or a name longer than 150 characters, **Then** the habit is rejected with a message naming the name field (BR-6).
3. **AC-US2-3**: **Given** an active habit, **When** the user marks it complete for today, **Then** one completion record for that habit and today's date is stored.
4. **AC-US2-4**: **Given** a habit already completed for a date, **When** the user tries to complete it again for the same date, **Then** no second record is created and the user is told it is already complete for that date (BR-7).
5. **AC-US2-5**: **Given** a habit with completions, **When** the user views it, **Then** its frequency label, whether it is complete for the current period and its current streak are shown.
6. **AC-US2-6**: **Given** an existing habit, **When** the user edits its name, description or frequency, **Then** the changes are saved.
7. **AC-US2-7**: **Given** an active habit, **When** the user deactivates it, **Then** it is no longer listed among active habits or today's habits, keeps its history, and cannot be completed until it is active again.
8. **AC-US2-8**: **Given** a habit, **When** the user removes it, **Then** the habit and its completions are never returned by normal queries again (BR-14).
9. **AC-US2-9**: **Given** no habits exist, **When** the Habits page shows the list, **Then** an empty state guides the user to the Add Habit action.

---

### User Story 3 - Track learning resources with milestones and notes (Priority: P2)

As the user, I add a learning card (a course, book or topic) with a description or source, break it
into milestones that I tick off, attach free-form notes, and remove cards, milestones and notes I no
longer need.

**Why this priority**: Third item source for plans and part of the Dashboard, but independent of
tasks and habits.

**Independent Test**: On the Learning Resources page, add a card, add and complete milestones, add
and remove notes, remove the card; delivers a working learning tracker.

**Acceptance Scenarios**:

1. **AC-US3-1**: **Given** no cards, **When** the user adds a card with a title and optional description/source, **Then** it is stored with status Not Started and its creation time set.
2. **AC-US3-2**: **Given** the card form, **When** the user submits an empty title, **Then** the card is rejected with a message naming the title field (BR-8).
3. **AC-US3-3**: **Given** a card, **When** the user changes its status to Not Started, In Progress or Completed, **Then** the new status is saved; any other value is rejected.
4. **AC-US3-4**: **Given** a card, **When** the user adds a milestone with a title and an optional target date, **Then** the milestone is stored under that card only, not done (BR-9).
5. **AC-US3-5**: **Given** a milestone, **When** the user marks it done (or not done again), **Then** its done flag is saved; **When** the user removes it, **Then** it is no longer returned.
6. **AC-US3-6**: **Given** a card, **When** the user adds a note with text, **Then** it is stored under that card with its timestamp; **When** the user removes the note, **Then** it is no longer returned.
7. **AC-US3-7**: **Given** a card with milestones and notes, **When** the user removes the card, **Then** the card, its milestones and its notes are never returned by normal queries again (BR-14).
8. **AC-US3-8**: **Given** a card, **When** the user expands it, **Then** its milestones and notes are shown.
9. **AC-US3-9**: **Given** no cards exist, **When** the page shows the grid, **Then** an empty state guides the user to the Add Learning Card action.

---

### User Story 4 - Build and follow Todo Plans (Priority: P2)

As the user, I build a plan by selecting existing tasks, habits and/or learning resources, give it a
title, an estimated duration, a start and end date-time and a priority order, and then follow it:
I am notified when its start time arrives, I see a live rest-time (time remaining) countdown while it
runs, I tick individual plan items done, I see its progress percentage, and I see a history of past
plans with how much of each was completed. I can remove a plan.

**Why this priority**: The feature that distinguishes QuickFlow; it needs items from US1–US3 to exist.

**Independent Test**: With at least one task, habit and learning card present, create a plan from
them, tick items, watch status and rest time change across start and end, remove the plan.

**Acceptance Scenarios**:

1. **AC-US4-1**: **Given** existing tasks, habits and learning cards, **When** the user creates a plan selecting one or more of them with a title, estimated duration, start date-time, end date-time and priority order, **Then** the plan is stored with one item per selected entity (each not done) and its creation time set.
2. **AC-US4-2**: **Given** the plan builder, **When** the user selects no items, or an item that does not exist (or was deleted), **Then** the plan is rejected (BR-10).
3. **AC-US4-3**: **Given** the plan builder, **When** the end date-time is equal to or before the start date-time, **Then** the plan is rejected with a message naming the end date-time (BR-11).
4. **AC-US4-4**: **Given** a plan, **When** the user marks an item done or not done, **Then** the item's done flag is saved and the plan's progress percentage becomes (items done) / (total items), shown immediately on the plan and on the Dashboard (FR-08).
5. **AC-US4-5**: **Given** a plan item that references a task, habit or learning card, **When** the user marks it done, **Then** the original entity's completion state changes only as stated in FR-07.5 (BR-13).
6. **AC-US4-6**: **Given** a plan whose start date-time is in the future, **When** the plan is read, **Then** its status is Not Started and no rest time is shown (BR-12).
7. **AC-US4-7**: **Given** a plan whose start date-time has passed and whose end date-time has not, **When** the plan is read, **Then** its status is In Progress (unless FR-08.4 makes it Completed) and its rest time is the time left until its end date-time, updating continuously.
8. **AC-US4-8**: **Given** a plan whose status follows FR-08.4, **When** the app is closed and reopened, **Then** the same status is shown, computed from the stored times, the item flags and the current time (NFR-4).
9. **AC-US4-9**: **Given** the Todo Plans page is open, **When** a plan's start date-time is reached, **Then** the user sees an in-app notification/highlight for that plan.
10. **AC-US4-10**: **Given** several plans, **When** the user opens the Todo Plans page, **Then** plans are grouped into active/upcoming and completed, each showing its progress, rest time (when active), items with done toggles and a remove action; active/upcoming plans are ordered by priority order.
11. **AC-US4-11**: **Given** past (completed) plans, **When** the user views the history, **Then** each shows how much of it was completed (percentage of items done).
12. **AC-US4-12**: **Given** a plan, **When** the user removes it, **Then** the plan and its items are never returned again, and the tasks, habits and learning cards it referenced are unchanged (BR-14).
13. **AC-US4-13**: **Given** no plans exist, **When** the page shows the list, **Then** an empty state guides the user to the Create Plan action.

---

### User Story 5 - See everything on the Dashboard (Priority: P3)

As the user, when I open the app I see today's tasks, overdue tasks, tasks completed today, today's
habits and which are done, a task completion percentage, active plans with live progress and rest
time, a learning-progress snapshot, and quick-add actions.

**Why this priority**: A read-only summary of US1–US4; valuable only once they exist.

**Independent Test**: With known tasks, habits, plans and cards present, open the Dashboard and
compare every number and list with the underlying data.

**Acceptance Scenarios**:

1. **AC-US5-1**: **Given** tasks due today, overdue tasks and tasks completed today, **When** the user opens the Dashboard, **Then** each group lists exactly those tasks (non-archived only).
2. **AC-US5-2**: **Given** active and inactive habits, **When** the user opens the Dashboard, **Then** today's habits list the active habits and marks those completed today, with counts matching the data.
3. **AC-US5-3**: **Given** non-archived tasks, **When** the Dashboard shows the task completion percentage, **Then** it equals (non-archived tasks with status Done) / (all non-archived tasks), and 0% when there are none.
4. **AC-US5-4**: **Given** plans that are In Progress, **When** the user opens the Dashboard, **Then** each is shown with its progress percentage and live rest time; ticking an item in a plan changes the Dashboard figures on the next view without other action.
5. **AC-US5-5**: **Given** learning cards and milestones, **When** the user opens the Dashboard, **Then** the learning snapshot shows the number of cards per status and milestones done out of total, matching the data.
6. **AC-US5-6**: **Given** the Dashboard, **When** the user uses a quick-add action (task, habit, learning card, plan), **Then** the matching add form opens.
7. **AC-US5-7**: **Given** any page, **When** the user uses the persistent navigation, **Then** Dashboard, Tasks, Habits, Learning Resources, Todo Plans and Settings are each reachable.

---

### User Story 6 - Adjust settings (Priority: P3)

As the user, I view my profile information and set application preferences (e.g. notification
behavior, default view) on the Settings page.

**Why this priority**: Required page of the MVP (PRD §13), but no other feature depends on it.

**Independent Test**: Open Settings, change a preference, reload the app and see it kept.

**Acceptance Scenarios**:

1. **AC-US6-1**: **Given** the Settings page, **When** the user opens it, **Then** the profile information and current preferences are shown.
2. **AC-US6-2**: **Given** the Settings page, **When** the user changes a preference and saves, **Then** it is kept between sessions and takes effect (FR-10).

---

### Edge Cases

- A task's due date is today: it is "due today", not overdue; it becomes overdue from the next day (app time zone) if not Done.
- A task with no due date: never overdue and never due today; sorted after dated tasks when sorting by due date.
- An archived task that is also overdue: excluded from the overdue list and the Dashboard.
- Completing a habit twice for the same date (e.g. two browser tabs): only one record exists (BR-7).
- Completing an inactive or deleted habit: rejected.
- Weekly habit: completion is recorded per date; it counts as done for the current week when any date in that week has a completion.
- Deleting a task, habit or learning card that is used by a plan item: see FR-07.6.
- A plan whose start and end are both in the past when created: allowed; it is read as Completed per FR-08.4.
- Marking a plan item done after the plan's end date-time: allowed; progress changes, rest time stays hidden.
- A plan with all items done before its start date-time: status follows FR-08.4.
- Midnight in the app time zone: "today", "overdue" and "completed today" switch at midnight in that zone, not the browser's.
- Removing a milestone or note of another card (wrong card id): rejected as not found (BR-9).
- Very long notes: note text is required and at most 5,000 characters (assumption A-9).

## Requirements *(mandatory)*

### Functional Requirements

**FR-01 Task Management**
- **FR-01.1**: A task MUST hold an id, title, description, status (Todo, In Progress, Done), priority (Low, Medium, High), due date (a calendar date, optional), creation time, update time and an archived flag.
- **FR-01.2**: The user MUST be able to create, read, update, archive, restore and delete tasks.
- **FR-01.3**: New tasks MUST default to status Todo, priority Medium (when none is given) and not archived.
- **FR-01.4**: Marking a task completed MUST set its status to Done and record when it was completed; setting it back to Todo or In Progress MUST clear that completion time (BR-5).
- **FR-01.5**: A task title MUST be required and at most 200 characters (BR-1); a description MUST be optional and at most 2,000 characters (BR-2); status and priority MUST be one of the listed values (BR-3).
- **FR-01.6**: Archived tasks MUST be excluded from the default task list and returned only when archived tasks are asked for (BR-4).
- **FR-01.7**: A task is overdue when its due date is before today (app time zone), its status is not Done and it is not archived; the user MUST be able to list overdue tasks.

**FR-02 Task Search and Filtering**
- **FR-02.1**: The task list MUST support free-text search on the title (case-insensitive substring).
- **FR-02.2**: The task list MUST support filtering by status, by priority, by due date (a from/to date range, inclusive, either end optional) and by archived (default: not archived), combinable.
- **FR-02.3**: The task list MUST support sorting by due date or by creation date, ascending or descending (default: creation date, newest first).

**FR-03 Habit Management**
- **FR-03.1**: A habit MUST hold an id, name, description (optional, at most 2,000 characters), frequency (Daily, Weekly), creation time and an active flag.
- **FR-03.2**: The user MUST be able to create, read, update, deactivate, reactivate and delete habits; new habits are active.
- **FR-03.3**: A habit name MUST be required and at most 150 characters (BR-6); frequency MUST be Daily or Weekly.

**FR-04 Habit Completion**
- **FR-04.1**: The system MUST record a completion event with an id, habit id, completion date and creation time when the user marks a habit complete for a date (default: today in the app time zone; a future date is rejected).
- **FR-04.2**: The system MUST prevent a second completion record for the same habit and date and tell the user it already exists (BR-7).
- **FR-04.3**: The user MUST be able to undo a habit's completion for a date.
- **FR-04.4**: Only active habits can be completed.
- **FR-04.5**: For each habit the system MUST provide its completion progress: whether it is done for the current period (today for Daily, the current Monday–Sunday week for Weekly) and its current streak (consecutive periods with at least one completion, ending with the current period, or the previous one if the current is not yet done).

**FR-05 Learning Resources**
- **FR-05.1**: A learning card MUST hold an id, title, description/source (optional, e.g. link, book, course name, at most 2,000 characters), creation time and status (Not Started, In Progress, Completed; default Not Started).
- **FR-05.2**: The user MUST be able to add, read, update (title, description, status) and remove learning cards; the title is required (BR-8) and at most 200 characters.

**FR-06 Learning Milestones and Notes**
- **FR-06.1**: Each learning card MUST support a list of milestones, each with a title (required, at most 200 characters), a done flag and an optional target date, belonging to exactly one card (BR-9).
- **FR-06.2**: Each learning card MUST support a list of notes, each with text (required) and a creation timestamp.
- **FR-06.3**: The user MUST be able to add, complete/un-complete and remove milestones, and add and remove notes, under a given card; removing a card removes its milestones and notes.

**FR-07 Plan Composition (Todo Plans)**
- **FR-07.1**: A plan MUST hold an id, title (required, at most 200 characters), a collection of plan items, an estimated duration (a positive whole number of minutes), a start date-time, an end date-time, a priority order (a positive whole number; 1 is the highest), a status (Not Started, In Progress, Completed) and a creation time.
- **FR-07.2**: Each plan item MUST reference one existing task, habit or learning card by type and id and carry its own done flag (default not done); an entity appears at most once in the same plan.
- **FR-07.3**: The user MUST be able to create a plan by selecting one or more existing, non-deleted tasks, habits and/or learning cards (BR-10), set its title, estimated duration, start and end date-time and priority order, edit those fields later, mark an item done or not done, and remove the plan.
- **FR-07.4**: A plan's end date-time MUST be after its start date-time (BR-11).
- **FR-07.5**: Marking a plan item done MUST NOT change the original task, habit or learning card, except where the item represents that entity's own completion action: marking a task-type item done MUST set the underlying task's status to Done (BR-13). [NEEDS CLARIFICATION: BR-13 names only the task case. Should a habit-type item marked done also record the habit's completion for today, and a learning-type item set the card to Completed? And when an item is set back to not done, should the underlying task/habit/card be reverted? Recommended: only task-type items propagate, one way (done → task Done); un-ticking an item never changes the source; habit and learning items never propagate.]
- **FR-07.6**: When a task, habit or learning card used by a plan item is deleted, the plan item MUST remain in the plan with its done flag, marked as referring to a removed item, and still count toward the plan's progress; the deleted entity itself is never returned (BR-14).

**FR-08 Plan Progress and Rest Time**
- **FR-08.1**: The system MUST notify the user in the app when a plan's start date-time is reached.
- **FR-08.2**: Between a plan's start date-time and end date-time the system MUST show its rest time (end date-time minus now), updating continuously; outside that window no rest time is shown (BR-12).
- **FR-08.3**: A plan's completion percentage MUST be (items marked done) / (total items), shown as a whole percentage.
- **FR-08.4**: A plan's status MUST be computed when read from its stored start/end date-times, its item flags and the current time: Not Started before the start date-time; In Progress from the start date-time until it becomes Completed; Completed [NEEDS CLARIFICATION: when does a plan become Completed? (a) when the end date-time has passed, whatever its items; (b) when all its items are done, even before the end; (c) whichever comes first. And can a plan whose items are all done before its start be Completed before it started? Recommended: (c), but never before the start date-time — Not Started until start; Completed once all items are done or the end has passed; history shows the percentage done at that moment.]
- **FR-08.5**: Item-level completion changes MUST be reflected immediately in the owning plan's progress and in the Dashboard summary.

**FR-09 Dashboard**
- **FR-09.1**: The Dashboard MUST display: a greeting/header; summary metric cards (tasks, habits, plans, learning); tasks due today; overdue tasks; tasks completed today; active habits with those completed today; the task completion percentage (AC-US5-3); plans that are In Progress with live rest time and progress; a learning-progress snapshot (cards per status, milestones done out of total); quick-add actions.
- **FR-09.2**: All Dashboard figures MUST be computed from the current data when the Dashboard is read, so they always match the underlying data.

**FR-10 Navigation and Settings** (PRD §4, §8, §13)
- **FR-10.1**: The app MUST offer persistent navigation to six pages: Dashboard, Tasks, Habits, Learning Resources, Todo Plans, Settings.
- **FR-10.2**: The Settings page MUST show user/profile information and application preferences and keep changed preferences between sessions. [NEEDS CLARIFICATION: which profile fields and preferences exist, and what do they do? The PRD names only "user/profile information" and "e.g. notification behavior, default view" for a single user with no accounts. Recommended: profile = display name (used in the Dashboard greeting); preferences = plan-start notifications on/off and default start page (one of the six pages); all stored on the server; the app time zone is shown read-only.]

### Business Rules

- **BR-1**: Task title is required and at most 200 characters.
- **BR-2**: Task description is optional and at most 2,000 characters.
- **BR-3**: A task cannot have an invalid status or priority.
- **BR-4**: An archived task is excluded from the default task list.
- **BR-5**: A completed task has status Done.
- **BR-6**: A habit name is required and at most 150 characters.
- **BR-7**: A habit can have only one completion record for a specific date.
- **BR-8**: A learning card title is required.
- **BR-9**: A milestone belongs to exactly one learning card.
- **BR-10**: A plan must reference at least one existing task, habit or learning-resource item.
- **BR-11**: A plan's end date-time must be after its start date-time.
- **BR-12**: A plan's rest-time display is only active between its start date-time and end date-time.
- **BR-13**: Marking a plan item done does not alter the completion state of the original task/habit/learning card outside the plan, except where the plan item directly represents that entity's own completion action (e.g. completing a task-type plan item completes the underlying task).
- **BR-14**: Deleted resources cannot be returned by normal queries.

### Non-Functional Requirements

- **NFR-1**: Create, update, delete and filter actions complete in under 500 ms for a single user's data.
- **NFR-2**: Live rest-time indicators update at least once per minute while a plan is in progress (the app updates them every second).
- **NFR-3**: Application state persists between sessions.
- **NFR-4**: Plan status transitions are computed consistently from stored start/end times and item completion, including after the app is closed and reopened.
- **NFR-5**: Every list page (Tasks, Habits, Learning Resources, Todo Plans) supports adding and removing items directly on that page, and its empty state points to the page's add action.

### Key Entities

- **Task**: a unit of work; id, title, description, status, priority, due date, created/updated times, completion time, archived flag.
- **Habit**: a recurring activity; id, name, description, frequency, creation time, active flag. Has many HabitCompletions.
- **HabitCompletion**: one completion of a habit on a date; id, habit, completion date, creation time. Unique per (habit, date).
- **LearningCard**: a course, book or topic; id, title, description/source, status, creation time. Has many LearningMilestones and LearningNotes.
- **LearningMilestone**: a step of a card; id, card, title, done flag, target date.
- **LearningNote**: a free-form note on a card; id, card, text, creation time.
- **Plan**: a time-boxed block of work; id, title, estimated duration, start/end date-time, priority order, status (computed), creation time. Has many PlanItems.
- **PlanItem**: one selected entity inside a plan; id, plan, source type (Task, Habit, LearningResource), source id, done flag.
- **Settings**: the single user's profile information and preferences (see FR-10.2).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can create a task, a habit and a learning card each in under 30 seconds from the page that lists them.
- **SC-002**: A user can build a plan from three existing items in under 1 minute.
- **SC-003**: 100% of Dashboard figures match a manual count of the underlying tasks, habits, plans and learning cards.
- **SC-004**: Create, update, delete and filter actions give feedback in under 500 ms (NFR-1).
- **SC-005**: While a plan is in progress, its rest time never lags the true remaining time by more than one minute (NFR-2).
- **SC-006**: After closing and reopening the app, every plan shows the same status and progress it would have had if the app had stayed open (NFR-4).
- **SC-007**: Every business rule BR-1..BR-14 has at least one automated check that passes.

## Assumptions

- **A-1**: Single user, no sign-in: the app is used by one person on their own machine; no accounts, authentication or multi-user separation (PRD §3, non-goals).
- **A-2**: One time zone for the whole app, the configured app time zone (currently Africa/Cairo): "today", "overdue", "completed today", plan start/end and notifications all use it.
- **A-3**: Deleting is permanent from the user's point of view (no trash or undo); archived tasks are the only restorable items.
- **A-4**: A task's due date is a calendar date without a time; a task may have no due date.
- **A-5**: "Tasks completed today" means tasks whose completion time falls on today; this requires recording when a task is completed (FR-01.4), which the PRD data model does not list.
- **A-6**: Learning card status is set by the user; it is not derived from its milestones.
- **A-7**: The plan builder offers non-archived tasks, active habits and all learning cards; selecting a deleted or missing entity is rejected.
- **A-8**: Priority order is not required to be unique; plans with the same priority order are ordered by start date-time.
- **A-9**: Limits the PRD does not state are set here: habit and card descriptions at most 2,000 characters, card/milestone/plan titles at most 200, note text at most 5,000, estimated duration 1 to 100,000 minutes.
- **A-10**: The plan-start notification is in-app only (shown while the app is open, and on opening the app for plans that started while it was closed); no email or push notifications (PRD §14).
- **A-11**: Estimated duration is informational; it is not checked against the start/end window.
- **A-12**: The habit week runs Monday to Sunday.
