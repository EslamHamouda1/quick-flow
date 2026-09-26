# test-phase-01 report: API contract (002-us-tags), attempt 1

Run 2026-09-26 23:50–23:52 Africa/Cairo against `http://localhost:8080` on the runner's fresh test DB.
Evidence folder: `loops/testing/runs/002-us-tags/suite/test-phase-01/attempt-1/` (below: `A/`).

**How it ran.** `test-phase-01-verify.sh` (C1–C10, adapted from the 001-quickflow suite script) was written, but
running it (`bash <script>`) needed approval in this session, as did `jq -f`, `--slurpfile` and shell globs. So the
same calls ran as five chained `curl --next` commands: every body is in `A/b/NN.json`, and every call has one line
in `A/status.tsv` / `A/status.json` (label, HTTP code, `time_total`, content type). Request bodies are listed in
`A/requests.md`, expectations in `A/expect.json`. Response schemas were checked with the program in
`test-phase-01-schema.jq` passed inline to `jq -rn` (`A/schema-check.tsv`). A negative control of seven planted
bodies (`A/neg/`) shows it catches every `Task.tags` rule (case/order, missing, >10, duplicates, >30 chars, inside
arrays), and the valid control passes. The plan named `curl.log` as evidence; the `b/` + `status.tsv` pair holds
the same request/response/time data.

102 calls cover all 41 swagger operations. 102/102 status codes = expected. 47/47 4xx responses are
`application/problem+json` with the matching `status`. 92/92 non-empty bodies pass the schema check. The 9 204
bodies are empty. Timed calls: 82, slowest 0.092 s.

| check | criterion | result | evidence |
|---|---|---|---|
| C1 | Swagger 200; its 41 operationIds = the copy's | pass | `A/b/01.json` (200, 0.006 s): live 41 ops = `loops/backend-dev/outputs/openapi.json` 41 ops, `addTaskTags`/`removeTaskTag` present, `listTasks` has `tag` param |
| C2 | `getAppInfo` 200 `AppInfo`, time zone `Africa/Cairo` | pass | `A/b/02.json`: `timeZone` = `Africa/Cairo`, schema ok |
| C3 | Task operations 201/200/204/400/404, new task `tags` = `[]`, invalid → 400 `errors[].field` | pass | calls 03–20: create 201 `TODO MEDIUM`, `tags` `[]`. `{"title":""}` → 400 field `title`. Filtered list (all 9 query params except `tag`) = `[1]`. Overdue 200. get 200/404. update 200 / 400 (no status, field `status`) / 404. complete 200 `DONE` + `completedAt` / 404. archive 200 `archived:true` / 404. restore 200 `archived:false` / 404. delete 204 empty / 404 |
| C4 | `addTaskTags` 200/400/404, `removeTaskTag` 200/400/404, `listTasks?tag=` 200 | pass | calls 21–29 (task 2): add `["Work"," urgent "]` → 200 `tags` `["urgent","work"]`. `{"tags":[]}` → 400 field `tags`. Unknown id → 404. `?tag=work` → `[2]`. `?tag=nosuchtag` → `[]`. Remove `work` → 200 `["urgent"]`. No `tag` query → 400 (field `tag`). Unknown id → 404 |
| C5 | Habit operations incl. 409 | pass | calls 31–53: create 201 active / 400 field `name`. list 200, `?active=true` 200. get 200/404. update 200 / 400 field `frequency` / 404. deactivate 200 false / 404. activate 200 true / 404. complete (no body) 201 date 2026-09-26. Future date → 400 field `date`. Unknown → 404. Same date → 409. completions 200/404. undo 204/404. delete 204/404 |
| C6 | Learning card operations | pass | calls 54–77: create 201 `NOT_STARTED` / 400 `title`. list, get 200/404. update 200 / 400 `status` / 404. milestone add 201 not done / 400 `title` / 404, update 200 / 400 `done` / 404. note add 201 / 400 `text` / 404. get with children 200. Deletes 204, repeats 404 (note, milestone, card) |
| C7 | Plan operations 201/400/404/204 | pass | calls 78–97: create 201 (item TASK/3 not done). No items → 400 `items`. End < start and end = start → 400 `endDateTime`. list + `group=active/completed/all` 200. get 200/404. update 200 / 400 `endDateTime` / 404. item done 200 → 1/1 100% / `{}` → 400 `done` / 404. delete 204/404 |
| C8 | `getDashboard` 200 with task `tags`, settings 200/400 | pass | `A/b/30.json`: dashboard schema ok, `dueToday` = `[{id 2, tags ["urgent"]}]` (read at 23:51:16, before midnight). Settings get 200. Update 200 echoes `Contract/false/TASKS`. `defaultPage:"NOPE"` → 400 field `defaultPage`. Defaults restored (102) |
| C9 | Every 2xx body has exactly the schema's required properties, types and enums. Every `Task.tags` is unique, 1–30 chars, ≤ 10, lower case, alphabetical | pass | `A/schema-check.tsv`: 92/92 ok. Each 2xx body had no missing or extra property. The negative control shows each tag rule is enforced |
| C10 | `addTaskTags`, `removeTaskTag`, `listTasks?tag=` (SC-002) and every other create/update/delete/filter call < 0.500 s | pass | `A/status.json`: 82 timed calls, 0 at or above 0.5 s, slowest `createTask` 0.092 s (first write on the fresh DB). Tag ops: add 0.013, add-400 0.003, add-404 0.003, filter 0.014, filter-unknown 0.004, remove 0.005, remove-400 0.002, remove-404 0.002 |

## Unit tests
Not part of this test phase. The runner runs them into `unit/` for `test-phase-regression`.

## Requirement coverage
| id | covered by (this phase) |
|---|---|
| SC-002 (tag ops < 500 ms, PRD §10) | C10 |
| FR-001 add tags | C4 (contract shape: 200 `Task`, several tags in one call) |
| FR-002 tags on every task read, alphabetical, lower case | C3, C4, C8 (`tags` present on every `Task`, incl. dashboard), C9 (order/case on all bodies) |
| FR-003 case-insensitive | C4 (`Work` → `work`), C9. Duplicates and no-op re-adds are checked in test-phase-02 |
| FR-004 filter by tag | C4 (`?tag=work` exact, unknown → `[]`). Combining with other filters is checked in test-phase-02 |
| FR-005 remove tag | C4 (200 remaining tags, 400 no tag, 404 unknown task) |
| FR-006 empty / > 30 chars → 400 `tags` | C4 (empty list → 400 `tags`). Blank and 31-char tags are checked in test-phase-02 |
| FR-007 > 10 tags | not covered here (business rule, test-phase-02). C9 checks `maxItems` 10 on responses |
| FR-008 kept / deleted with the task | not covered here (restart never done by sessions; deletion in test-phase-02) |
| BR-T1..T4 | business rules, test-phase-02. BR-T1/T3 limits appear here only as response-schema checks (C9) |

## Questions
None.

## Notes
- The run happened at 23:50–23:52 Africa/Cairo, just before the date changed. The date-dependent calls (dashboard
  `dueToday`, habit completion "today", undo by date) all ran before 23:52, and the server's `now` in `A/b/30.json`
  confirms 2026-09-26.
- Removing a tag without the `tag` query returns 400 with field `tag` and detail "Invalid parameter". The contract
  allows field `tags` or `tag`.
