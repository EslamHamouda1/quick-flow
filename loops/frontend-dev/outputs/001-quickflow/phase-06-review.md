# phase-06 review: US4 Build and follow Todo Plans
layer: frontend
story_id: US4
spec_phase: 6
depends_on: [phase-02, phase-03, phase-04, phase-05, backend-dev:US4]

## Goal
The Todo Plans page: builder from existing items, grouped plans with progress, live rest time, item toggles, edit, remove, plan-start notification.

## Tasks
- [ ] T030 [US4] Check that the generated `PlansService` has `listPlans` (`group`), `createPlan`, `getPlan`, `updatePlan`, `deletePlan`, `setPlanItemDone` and that `Plan`/`PlanItem` have `status`, `progressPercent`, `restSeconds`, `sourceTitle`, `sourceRemoved`; check the Plans section of contracts/ui-contract.md covers AC-US4-1..13; raise a question for anything missing
- [ ] T031 [P] [US4] Create the rest-time component (input `endDateTime`; text `formatDuration(end − clock.now())`, never negative) in frontend/src/app/shared/rest-time.ts (FR-08.2, NFR-2, research R-4)
- [ ] T032 [P] [US4] Create the plan builder component (title, estimated duration minutes, start/end `datetime-local` in the app zone converted with `toZonedIso`, priority order, source checkboxes loaded from `listTasks()`, `listHabits(active=true)`, `listLearningCards()`; edit mode shows only the `PlanUpdate` fields; field errors incl. `error-items` and `error-endDateTime`) in frontend/src/app/pages/plans/plan-builder.ts (AC-US4-1..3, A-7)
- [ ] T033 [P] [US4] Create the plan card component (status label, `progressPercent`, rest time only when `status` is `IN_PROGRESS` and `restSeconds` is not null, window in the app zone, items with done checkbox → `setPlanItemDone` and removed badge, edit/delete actions, started highlight) in frontend/src/app/pages/plans/plan-card.ts (AC-US4-4..7, AC-US4-10, FR-07.6)
- [ ] T034 [US4] Implement the Todo Plans page: `plans-active` (`group=active`) and `plans-completed` (`group=completed`, history with percent), create flow (also `?add=1`), edit, delete, item toggle replacing the plan from the response, empty state, re-read when `now` passes a shown plan's start or end, in frontend/src/app/pages/plans/plans.page.ts (AC-US4-4, AC-US4-8, AC-US4-10..13, research R-4)
- [ ] T035 [US4] Create `PlanStartWatcher` (root, started by the shell) (research R-6, FA-2: app-wide notice "Plan “<title>” has started" + `plan-started-<id>` highlight when `now` reaches a `NOT_STARTED` plan's start, once per plan via `localStorage['quickflow.notifiedPlanIds']`, also on app open for plans already `IN_PROGRESS`; reads `getSettings()` right before showing a notice and shows nothing when `planStartNotifications` is false, FR-10.2) in frontend/src/app/core/plan-start-watcher.service.ts and wire it in frontend/src/app/app.ts (FR-08.1, AC-US4-9, A-10)

Order: first `generate_client` (the loop's app-wide step, run at the start of implement so `PlansService` exists;
`frontend/src/app/api/api` has only `InfoService`, `TasksService`, `HabitsService` and `LearningService` now), then
T030, then T031, T032 and T033 (the `[P]` group: one subagent each, different files; T033 uses T031's selector
`app-rest-time`, agreed up front below), then T035 (the page reads its highlight set), then T034.

## Acceptance criteria
- AC-US4-1: Given existing tasks, habits and learning cards, When the user creates a plan selecting one or more of them with a title, estimated duration, start date-time, end date-time and priority order, Then the plan is stored with one item per selected entity (each not done) and its creation time set.
- AC-US4-2: Given the plan builder, When the user selects no items, or an item that does not exist (or was deleted), Then the plan is rejected (BR-10).
- AC-US4-3: Given the plan builder, When the end date-time is equal to or before the start date-time, Then the plan is rejected with a message naming the end date-time (BR-11).
- AC-US4-4: Given a plan, When the user marks an item done or not done, Then the item's done flag is saved and the plan's progress percentage becomes (items done) / (total items), shown immediately on the plan and on the Dashboard (FR-08).
- AC-US4-5: Given a plan item that references a task, habit or learning card, When the user marks it done, Then the original entity's completion state changes only as stated in FR-07.5 (BR-13).
- AC-US4-6: Given a plan whose start date-time is in the future, When the plan is read, Then its status is Not Started and no rest time is shown (BR-12).
- AC-US4-7: Given a plan whose start date-time has passed and whose end date-time has not, When the plan is read, Then its status is In Progress (unless all its items are done, which makes it Completed per FR-08.4) and its rest time is the time left until its end date-time, updating continuously.
- AC-US4-8: Given a plan whose status follows FR-08.4, When the app is closed and reopened, Then the same status is shown, computed from the stored times, the item flags and the current time (NFR-4).
- AC-US4-9: Given the Todo Plans page is open, When a plan's start date-time is reached, Then the user sees an in-app notification/highlight for that plan.
- AC-US4-10: Given several plans, When the user opens the Todo Plans page, Then plans are grouped into active/upcoming and completed, each showing its progress, rest time (when active), items with done toggles and a remove action; active/upcoming plans are ordered by priority order.
- AC-US4-11: Given past (completed) plans, When the user views the history, Then each shows how much of it was completed (percentage of items done).
- AC-US4-12: Given a plan, When the user removes it, Then the plan and its items are never returned again, and the tasks, habits and learning cards it referenced are unchanged (BR-14).
- AC-US4-13: Given no plans exist, When the page shows the list, Then an empty state guides the user to the Create Plan action.

## Files
- app-wide: re-run `generate_client` → `frontend/src/app/api/**` (adds `PlansService` and the `Plan`, `PlanItem`,
  `PlanCreate`, `PlanUpdate`, `PlanItemRef`, `PlanItemUpdate`, `PlanStatus`, `PlanSourceType` models; the other
  files are regenerated unchanged); `loops/frontend-dev/outputs/ui-url.md` rewritten unchanged.
- edit `specs/001-quickflow-frontend/research.md` R-2: the Plans names and signatures found in the generated client
  (T030), in particular `listPlans(group?)`'s parameter type, `setPlanItemDone(id, itemId, body)`'s argument order,
  the `PlanStatus` / `PlanSourceType` constant names and the type of `restSeconds` (`number | null`).
- create `frontend/src/app/shared/rest-time.ts` (T031): standalone `app-rest-time`, input `endDateTime` (ISO string),
  renders `formatDuration(Date.parse(endDateTime) − clock.now())` (existing `core/date-format.ts`, `H:MM:SS`, clamped
  at 0) as a `<span data-testid="plan-rest-time">`. It never decides whether to show itself (the parent does, R-4).
  Reused by the Dashboard in US5.
- create `frontend/src/app/pages/plans/plan-builder.ts` (T032): standalone `app-plan-builder`; inputs `plan`
  (`Plan | null`, null = create) and `errors` (`Record<string,string>`); outputs `save` (a `PlanCreate` in create mode,
  a `PlanUpdate` in edit mode) and `cancel`. Selectors `plan-builder`, `plan-title`, `plan-duration`
  (`type="number"`), `plan-start`, `plan-end` (`type="datetime-local"`), `plan-priority` (`type="number"`),
  `plan-save`, `plan-cancel`; create mode only: source checkboxes `plan-source-task-<id>`, `plan-source-habit-<id>`,
  `plan-source-learning-<id>` in three labelled groups (Tasks / Habits / Learning), loaded by the builder itself from
  `listTasks()` (defaults: non-archived), `listHabits(true)`, `listLearningCards()` (A-7). `app-field-error` for
  `title`, `estimatedDurationMinutes`, `startDateTime`, `endDateTime`, `priorityOrder` and `items` (FA-31). Start/end
  are sent through `toZonedIso(value, clock.timeZone())`; edit mode prefills them with `toDatetimeLocal` (both
  already in `core/date-format.ts`). No `required`/`min`/`maxlength` attributes (FA-3).
- create `frontend/src/app/pages/plans/plan-card.ts` (T033): standalone `app-plan-card`; inputs `plan` (`Plan`),
  `highlighted` (boolean), `busy` (boolean); outputs `toggle` (`{item: PlanItem, done: boolean}`), `edit`, `remove`
  (the page makes the calls, so the Dashboard can reuse the card in US5). Selectors `plan-<id>` (root), title text,
  `plan-status` (Not Started / In Progress / Completed), `plan-progress` ("<progressPercent>%", plus
  "<doneItems>/<totalItems> items"), `app-rest-time` only when `status === IN_PROGRESS && restSeconds !== null`,
  `plan-window` ("<start> – <end>" with `formatDateTime(…, clock.timeZone())`), `plan-priority-order`
  ("Priority <n>"), estimated duration ("<n> min"), `plan-item-<itemId>` rows (type label Task / Habit / Learning,
  `sourceTitle`, `plan-item-done-<itemId>` checkbox bound to `done`, `plan-item-removed-<itemId>` badge "Removed" when
  `sourceRemoved`), `plan-edit-<id>`, `plan-delete-<id>`, and `plan-started-<id>` (badge "Started") when
  `highlighted`.
- create `frontend/src/app/core/plan-start-watcher.service.ts` (T035): root service `PlanStartWatcher` with
  `start()`, `refresh()` (the page calls it after create/edit/delete) and a read-only `highlighted: Signal<Set<id>>`;
  loads `listPlans('active')`; an `effect` on `clock.now()` fires the notice (`NoticeService.info`) when a
  `NOT_STARTED` plan's `startDateTime` is reached (FA-2, FA-30, FA-32).
- edit `frontend/src/app/app.ts` (T035): inject `PlanStartWatcher` and call `start()` once (constructor). No template
  change: the notice uses the existing `notice` area.
- rewrite `frontend/src/app/pages/plans/plans.page.ts` (T034): keeps `page-title` "Todo Plans"; `plan-create`, the
  `app-plan-builder` above the lists, `plans-active` ("Active & upcoming") and `plans-completed` ("History") with one
  `app-plan-card` each in the server's order, `app-empty-state` when both groups are empty; `?add=1` handled like the
  Learning page (`replaceUrl` when the form closes). Uses `NoticeService`, `readProblem`, `ClockService` (phase-02,
  unchanged).
- endpoints used: `GET /api/plans?group=active|completed` (`listPlans`), `POST /api/plans` (`createPlan`),
  `PUT /api/plans/{id}` (`updatePlan`), `DELETE /api/plans/{id}` (`deletePlan`), `PUT /api/plans/{id}/items/{itemId}`
  (`setPlanItemDone`); builder sources `GET /api/tasks`, `GET /api/habits?active=true`, `GET /api/learning-cards`.
  `getPlan` is not used (every write returns the plan or is followed by a re-read).
- no other file changes (routes, shell template and other pages stay as they are).

## Planned checks
- Implement session (quick feedback only, not verification): `cd frontend && npm run build` succeeds; output in
  `loops/frontend-dev/runs/001-quickflow/phase-06-build.log`.
- No unit tests: the frontend layer has no `coverage` and `test` is empty (FA-1, approved).
- Testing loop (Playwright MCP, headless) can check, on `http://localhost:4200/plans` (create a task, an active habit
  and a learning card through the API first):
  - no plans → `empty-state` "No plans yet" with `empty-state-action` "Create Plan", which opens `plan-builder`
    (AC-US4-13). `/plans?add=1` opens `plan-builder` directly.
  - `plan-create` → builder lists `plan-source-task-<id>`, `plan-source-habit-<id>`, `plan-source-learning-<id>`
    (archived tasks and inactive habits absent, A-7) → tick all three, title, duration, start in the future, end after
    it, priority 1 → `plan-save` → `plan-<id>` in `plans-active`, `plan-status` "Not Started", `plan-progress` "0%",
    no `plan-rest-time`, three `plan-item-<itemId>` rows unchecked; a `notice` shows; API: three items `done: false`,
    `createdAt` set (AC-US4-1, AC-US4-6).
  - nothing ticked → `plan-save` → form stays open, `error-items` shows the server's message, no plan added; a source
    deleted through the API after the builder loaded, then ticked → `error-items` (AC-US4-2).
  - end equal to / before start → `error-endDateTime` shows the server's message ("must be after startDateTime"),
    no plan added (AC-US4-3).
  - a plan with start in the past and end in the future → `plan-status` "In Progress", `plan-rest-time` `H:MM:SS`
    decreasing between two reads a few seconds apart and roughly equal to end − now (AC-US4-7).
  - `plan-item-done-<itemId>` → checked, `plan-progress` "33%"; API item `done: true`; the TASK item's task becomes
    `DONE`, a HABIT / LEARNING item's source is unchanged; un-tick → "0%", the task stays `DONE` (AC-US4-4, AC-US4-5).
    Ticking every item of an In Progress plan → the plan moves to `plans-completed` with "100%" (FR-08.4).
  - reload the page → same statuses and percentages as the API (AC-US4-8).
  - a plan with start ~1 minute ahead, page open → when the start is reached: a `notice` "Plan “<title>” has
    started", `plan-started-<id>` on that card, `plan-status` "In Progress" and `plan-rest-time` appear without a
    manual reload (AC-US4-9, R-4). The notice is not shown again after a reload (FA-2).
  - several active plans with priority 2, 1, 3 → `plans-active` lists them 1, 2, 3; plans whose end has passed are in
    `plans-completed` with their percent and no rest time (AC-US4-10, AC-US4-11).
  - `plan-edit-<id>` → builder prefilled (start/end in the app zone), no source checkboxes → change title and
    priority → `plan-save` → card shows them, items unchanged.
  - `plan-delete-<id>` → card gone, no confirm dialog (FA-7); `GET /api/plans/{id}` → 404; the referenced task, habit
    and card are unchanged (AC-US4-12).
  - a plan whose task was deleted → `plan-item-removed-<itemId>` badge, the item still counts in `plan-progress`
    (FR-07.6).

## Risks
- **Client not regenerated yet**: the swagger has all six plan operations (checked now), so the implement session
  runs `generate_client` first; T030 then checks the generated signatures before code uses them. A generated file
  that doesn't compile under Angular 22.2.0 becomes a question (generated files are never patched).
- **`/api/settings` does not exist yet** (checked now: no `settings` path in `loops/backend-dev/outputs/openapi.json`;
  backend US6 is a later story), so `SettingsService.getSettings()` can't be called in this phase as T035 says. See
  FA-30.
- **Error field names** (checked now in `backend/.../domain/plan/Plan.java`, `PlanService.java`,
  `web/plan/PlanCreateRequest.java`, `web/ApiExceptionHandler.java`): no items → `items` "must not be empty" (bean
  validation) or "must contain at least one item"; a missing/deleted source → `items[<i>].sourceId` "must reference an
  existing <TYPE>"; a repeated source → `items[<i>]`; BR-11 → `endDateTime` "must be after startDateTime"; empty
  numbers/dates sent as `null` → `<field>` "must not be null"; unparseable → "Malformed request body" with the field.
  Bean validation runs before the domain checks, so BR-11 is only reported once the other fields are valid. The UI
  therefore maps every `items` / `items[…]` field to `error-items` (FA-31). 404 `detail`s: "Plan <id> not found",
  "Plan item <i> not found on plan <id>".
- **Status boundary vs. clock skew**: `ClockService.now` is aligned with the server's clock at start-up, but a re-read
  exactly at a start/end instant could still get the old status. The page and the Dashboard re-read at the first tick
  where `now ≥ boundary + 1 s` and pick the next boundary among the times still ahead, so a stale answer never loops
  (at worst it stays stale until the next boundary or action). Status itself is always the server's (R-4).
- **Item toggle moves a plan between groups**: `setPlanItemDone` returns the whole plan; the page replaces it in place,
  and when the returned `status` puts it in the other group (all items done → Completed; un-tick of an ended plan
  keeps it Completed), it re-reads both groups instead of moving it itself, so the server's ordering holds.
- **Checkbox state after a failed call**: bound to `item.done`; after an error the list is re-read, which snaps it back
  (as FA-22 / FA-26).
- **`datetime-local` in the app zone**: the browser's own zone plays no part; values are converted with `toZonedIso` /
  `toDatetimeLocal` (phase-02, R-3). A wall time inside a DST gap resolves to the shifted instant (existing helper
  behaviour); the server stores the instant.
- **Notified ids in `localStorage`** survive only in that browser profile; a Playwright run with a fresh profile sees
  every In Progress plan notified once on open (FA-2, as approved). Unreadable storage (private mode) → treated as
  empty, no crash.

## Assumptions
- **FA-30 (new) Plan-start notices before Settings exist**: in this phase `PlanStartWatcher` treats
  `planStartNotifications` as on, the default written in FR-10.2 and the clarification "planStartNotifications
  (default on)". The check sits behind one private method (`notificationsEnabled(): Promise<boolean>`, returning
  `true` now), and T035's settings read is recorded in tasks.md as done "with the default until US6". Phase-08 (US6)
  replaces it with `getSettings().planStartNotifications` read right before each notice once `SettingsService` is
  generated; phase-08's review adds that as a task on `core/plan-start-watcher.service.ts`. The testing loop can't
  check "notifications off" before US6.
- **FA-31 (new) Builder form**: starts empty (title, duration, start, end, priority, no source ticked); empty number
  and date inputs are sent as `null`, so the server names the field. Field errors show under their input; every
  `items` / `items[<i>]…` field shows as one `error-items` message above the source groups (the first one). A 400 field
  with no input in the form and every other error go to `notice-error` with the server's `detail`. The form stays open
  and keeps its values after an error. Edit mode (`PlanUpdate`) shows title, duration, start, end and priority only;
  the items are not editable (FR-07.3, contract "items are unchanged"). Source labels: task title, habit name, card
  title; an empty source group shows "No tasks" / "No active habits" / "No learning cards" (text only). Only one form
  is open at a time, above the lists.
- **FA-32 (new) Watcher details**: `PlanStartWatcher` loads `listPlans('active')` on start, after every plan
  create/edit/delete (`refresh()`), and at each start time it has scheduled (so the page's list and the watcher agree).
  On the first load after app open, plans already `IN_PROGRESS` whose id isn't stored are notified once (A-10). On
  later loads (after a change), an `IN_PROGRESS` plan whose id isn't stored is stored silently (the user has just
  created/edited it with a past start); no notice. A plan is notified at most once (edits don't reset it). Every
  plan notified in this app session is in `highlighted`, so its card shows `plan-started-<id>` until the app is
  reloaded. The notice uses the existing `notice` area with kind `info`; text "Plan “<title>” has started". Load errors
  are silent (the page shows its own errors).
- **FA-33 (new) Page behaviour**: success notices "Plan created", "Plan updated", "Plan deleted", "Item marked done",
  "Item marked not done". While a call for a plan runs, that card's controls are disabled. After create/edit/delete
  both groups are re-read; after a toggle, see Risks. Empty state text "No plans yet", action "Create Plan"; the page
  button `plan-create` reads "Create Plan". The empty state shows only when both groups are empty; an empty
  single group shows "No active or upcoming plans" / "No completed plans yet" (text only).
- FA-1, FA-3 (server-side validation messages only), FA-2 (app-wide plan-start notice, once per plan, `localStorage`),
  FA-7 (deletes without confirmation), FA-10 (no extra dependencies, plain CSS) and FA-11 apply as approved at
  phase-00.

## Open questions
None.

## Swagger input
`loops/backend-dev/outputs/openapi.json` (from task.md).

## Contract differences
Compared with `specs/001-quickflow-backend/contracts/openapi.yaml` for the US4 endpoints (checked now):
- Paths, methods, operationIds (`listPlans`, `createPlan`, `getPlan`, `updatePlan`, `deletePlan`, `setPlanItemDone`),
  path parameters (`id`, `itemId`: `integer` `int64`), the `group` query parameter (`active | completed | all`,
  default `all`), request bodies (all required) and success responses (200 / 201 / 204 with `Plan` / `Plan[]`):
  match.
- Schemas `PlanStatus`, `PlanSourceType`, `PlanItemRef`, `PlanCreate`, `PlanUpdate`, `PlanItemUpdate`, `PlanItem`,
  `Plan`: match (same required lists, enums, limits: title 1–200, duration 1–100000, priority ≥ 1, `items`
  `minItems: 1`, `progressPercent` 0–100, `restSeconds` `integer | null` `int64`). Extras in the generated swagger
  only: `format: int32` on the integer fields. No effect on the client (`number`).
- Error responses: the contract declares `400 BadRequest` (create, update, item) and `404 NotFound` (every `{id}`
  operation) as `application/problem+json` `Problem`; the generated swagger declares none. The backend does return
  them (see Risks); the frontend keeps its local `Problem` type (`core/problem.ts`).
- Descriptions differ in wording only: `listPlans` has its text as `summary` with response "OK" (contract: response
  "Plans with computed status, progress and rest time"); responses "OK" / "No Content" vs "Updated" / "Deleted" /
  "The whole plan with its new progress and status". No effect.
- `/api/settings` (`getSettings`, used by T035) is not in the generated swagger yet: it belongs to backend US6
  (FA-30).
