# phase-08-test: US6 Adjust settings (frontend-dev)

Criteria listed in `loops/frontend-dev/outputs/001-quickflow/phase-08.md` (compare on later attempts):
AC-US6-1, AC-US6-2, text as in `specs/001-quickflow-frontend/spec.md` lines 170–171. Also covered: FR-10.2 (what
Settings hold and how each takes effect), FR-10.1 (`nav-settings`), and the "Hello, <displayName>" greeting form that
phase-07 left to US6 (FA-34).

Selectors come from `specs/001-quickflow-frontend/contracts/ui-contract.md` (Shell, Dashboard, Settings `/settings`,
Todo Plans). Visible texts are used only where the criteria, the spec, the contract or the approved phase review
(FA-41 labels / "Settings saved") state them. Default-page routes are from research R-8. The data is set up and read
back through the API (`/api/...` through the dev-server proxy, from the page), and the read-backs are saved as JSON in
the attempt folder.

All steps use Playwright MCP (headless) on `http://localhost:4200`. Screenshots go to
`<run_dir>/shot-<NN>-<check id>.png`.

| check | criterion | Playwright MCP steps | expected |
|---|---|---|---|
| C1 | AC-US6-1, FR-10.2 | fresh DB; `GET /api/settings`, `GET /api/app-info`; navigate `/settings` | "the profile information and current preferences are shown": `settings-form` with `settings-display-name` empty, `settings-notifications` checked, `settings-default-page` "Dashboard" (FR-10.2 defaults, = `GET /api/settings`); `settings-time-zone` shows `app-info.timeZone` read-only |
| C2 | AC-US6-1 | API `PUT /api/settings` `{displayName:"Ada", planStartNotifications:false, defaultPage:"TASKS"}`; reload `/settings` | the three fields show "Ada", unchecked, "Tasks" (the server's values) |
| C3 | AC-US6-2 (kept) | on `/settings` set the name "Grace", tick notifications on, pick "Habits", click `settings-save`; `GET /api/settings`; open a new page load of `/settings` | a `notice` "Settings saved"; the API returns `Grace`/true/`HABITS`; after the reload the form shows the same values ("kept between sessions") |
| C4 | AC-US6-2 (takes effect: default page), FR-10.2 | with `defaultPage` HABITS open `/`; then for each of the six values set it through the Settings form (or API) and open `/`; open deep link `/tasks` with default HABITS | `/` opens the page of the setting (`HABITS→/habits` ... `SETTINGS→/settings`, R-8) with its `page-title`; the deep link `/tasks` still opens Tasks (R-8 "deep links are not redirected") |
| C5 | AC-US6-2 (takes effect: greeting), FR-10.2 | name "Grace" saved; open `/dashboard`; then clear the name on Settings, save, open `/dashboard` | `dash-greeting` "Hello, Grace"; after clearing, "Hello" (ui-contract `dash-greeting`) |
| C6 | AC-US6-2 (takes effect: notifications), FR-10.2, FR-08.1 | API: a task; `planStartNotifications` false; plan P-off starting ~1 min ahead; keep `/plans` open, no reload, wait past its start; then set it true, plan P-on starting ~1 min ahead, wait past its start | P-off: no `notice` "Plan “P-off” has started"; P-on: the notice appears ("shows plan-start notifications only when it is on") |
| C7 | FR-10.2 (displayName ≤ 100) | on `/settings` enter a 101-character name, click `settings-save`; `GET /api/settings` | `error-displayName` shown; the stored settings are unchanged |
| C8 | FR-10.1 | from `/dashboard` click `nav-settings` | `/settings` with `page-title` "Settings" |

Unit tests: the frontend layer has no `test` command and no `coverage` (`unit-result.json` outcome `none`).
