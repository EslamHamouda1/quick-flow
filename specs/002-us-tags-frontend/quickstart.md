# Quickstart: Task tags (frontend layer)

## Prerequisites
- The QuickFlow frontend of `specs/001-quickflow-frontend` (already built), Node.js 24.x LTS (≥ 24.15), JDK 25 for the
  generator CLI's jar.
- The backend with the tags story (`backend-dev` US1 of 002-us-tags) running on http://localhost:8080, and
  `loops/backend-dev/outputs/openapi.json` containing `/api/tasks/{id}/tags` and the `tag` list parameter.

## Build and run (from the repo root)
1. Regenerate the client: `cd frontend && npx -y @openapitools/openapi-generator-cli@2.41.0 generate -i ../loops/backend-dev/outputs/openapi.json -g typescript-angular -o src/app/api`
2. Build: `cd frontend && npm run build` (no `error TS` / `✘ [ERROR]`).
3. Run: `cd frontend && npm start`, open http://localhost:4200/tasks.

## Validation scenarios (performed by the testing loop, headless Playwright; selectors in [contracts/ui-contract.md](contracts/ui-contract.md))
1. Create two tasks. Add `work, urgent` to the first → both tags shown; reload → still shown (AC-US1-1).
2. Filter by `work` → only the first task listed; filter by a tag nobody has → "No tasks match" (AC-US1-2).
3. Add `Work` to the first task → still one "work"; filter by `WORK` → first task listed (AC-US1-5).
4. Remove "work" → gone from the row; filter by `work` → not listed (AC-US1-3).
5. Add an empty value and a 31-character tag → `error-tags`, tags unchanged (AC-US1-4).
6. Give a task 10 tags, add an 11th → `error-tags`, still 10 tags (AC-US1-6).
