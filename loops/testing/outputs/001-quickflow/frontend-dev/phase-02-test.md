# phase-02-test: Foundational (frontend-dev)

Criteria listed in `loops/frontend-dev/outputs/001-quickflow/phase-02.md` (compare on later attempts):

- AC-US5-7: "Given any page, When the user uses the persistent navigation, Then Dashboard, Tasks,
  Habits, Learning Resources, Todo Plans and Settings are each reachable. (shell part; the pages are
  placeholders until their story phase)"

Selectors, labels and routes from `specs/001-quickflow-frontend/contracts/ui-contract.md` (Shell):
`nav-main` "persistent navigation, present on every page"; `nav-dashboard`, `nav-tasks`, `nav-habits`,
`nav-learning`, `nav-plans`, `nav-settings` with text "Dashboard", "Tasks", "Habits",
"Learning Resources", "Todo Plans", "Settings" and routes `/dashboard`, `/tasks`, `/habits`,
`/learning`, `/plans`, `/settings`; `page-title` "`<h1>` of the current page, text equal to its nav label".
Extra checks from the approved review's `## Planned checks` (R1: `/` → `/dashboard`, unknown path ends on
`/dashboard`, per data-model Routes `**` → `''`, and this phase's `''` → `/dashboard`; R2: the page loads with
`GET /api/app-info` and no console errors).

| check | criterion | Playwright MCP steps | expected |
|---|---|---|---|
| C1 | AC-US5-7 | navigate `/settings` (start on another page); click `nav-dashboard`; read URL, `page-title`; screenshot `shot-01-C1.png` | URL `/dashboard`, `page-title` "Dashboard" |
| C2 | AC-US5-7 | from `/dashboard` click `nav-tasks`; screenshot `shot-02-C2.png` | URL `/tasks`, `page-title` "Tasks" |
| C3 | AC-US5-7 | from `/tasks` click `nav-habits`; screenshot `shot-03-C3.png` | URL `/habits`, `page-title` "Habits" |
| C4 | AC-US5-7 | from `/habits` click `nav-learning`; screenshot `shot-04-C4.png` | URL `/learning`, `page-title` "Learning Resources" |
| C5 | AC-US5-7 | from `/learning` click `nav-plans`; screenshot `shot-05-C5.png` | URL `/plans`, `page-title` "Todo Plans" |
| C6 | AC-US5-7 | from `/plans` click `nav-settings`; screenshot `shot-06-C6.png` | URL `/settings`, `page-title` "Settings" |
| C7 | AC-US5-7 (FR-10.1, "present on every page") | navigate each of the six routes directly; evaluate `nav-main` present and six `nav-*` links with the contract texts; screenshot `shot-07-C7.png` | on every route `nav-main` exists with all six links and labels |
| C8 | R1 | navigate `/` and `/nope`; read final URL and `page-title`; screenshot `shot-08-C8.png` | both end on `/dashboard`, `page-title` "Dashboard" |
| C9 | R2 | after a fresh load, `browser_network_requests` and `browser_console_messages` (error); screenshot `shot-09-C9.png` | `GET /api/app-info` → 200; no console errors |

Unit tests: the frontend layer has no `test` command and no `coverage` (`unit-result.json` outcome `none`).
