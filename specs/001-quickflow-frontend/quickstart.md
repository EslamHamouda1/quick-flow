# Quickstart: QuickFlow (frontend layer)

## Prerequisites
- Node.js 24.x LTS (≥ 24.15), JDK 25 (for the generator CLI's jar), network on first client generation.
- `frontend/` scaffolded by the runner (`layers.frontend.scaffold` in `project.config.yaml`).
- Backend running on http://localhost:8080 (`cd backend && ./mvnw spring-boot:run`, or the `test` profile for checks)
  and `loops/backend-dev/outputs/openapi.json` present.

## Build and run (from the repo root)
1. Generate the client: `cd frontend && npx -y @openapitools/openapi-generator-cli@2.41.0 generate -i ../loops/backend-dev/outputs/openapi.json -g typescript-angular -o src/app/api`
2. Build: `cd frontend && npm run build` (must finish without `error TS` / `✘ [ERROR]`).
3. Run: `cd frontend && npm start`, open http://localhost:4200. `/api/*` is proxied to the backend (`proxy.conf.json`).

## Validation scenarios (performed by the testing loop, headless Playwright; selectors in [contracts/ui-contract.md](contracts/ui-contract.md))
1. Navigation: each `nav-*` link opens its page; `page-title` matches (AC-US5-7).
2. Tasks: add a task, see it in `task-list`; submit an empty title → `error-title`; edit, complete, archive
   (gone from default list, visible with `filter-archived`), restore, delete; search and filters; overdue view (US1).
3. Habits: add daily + weekly habit; toggle today; second completion shows the "already complete" notice; deactivate, delete (US2).
4. Learning: add card, expand, add/tick/remove milestone, add/remove note, change status, remove card (US3).
5. Plans: create a plan from a task, a habit and a card with start in the past and end in the future → In Progress with
   `plan-rest-time` counting down; tick items → `plan-progress` changes; end ≤ start → `error-endDateTime`; a plan
   starting a minute from now → `plan-started-<id>` / notice when it starts; remove plan (US4).
6. Dashboard: figures match the data; quick-add opens each add form; ticking a plan item changes `dash-plans` (US5).
7. Settings: change display name, notifications, default page; reload → kept; opening `/` lands on the default page (US6).
