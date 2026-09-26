# phase-02: Polish & Cross-Cutting Concerns
layer: frontend
story_id: none
spec_phase: 2
depends_on: [phase-01]
## Goal
Check the tag UI against the UI contract and the swagger, and finish with a clean build.
## Acceptance criteria
## Tasks
- [x] T006 Compare the selectors rendered by frontend/src/app/pages/tasks/tasks.page.ts and frontend/src/app/pages/tasks/task-tags.ts with specs/002-us-tags-frontend/contracts/ui-contract.md and the calls with specs/002-us-tags-backend/contracts/openapi.yaml (`listTasks` `tag`, `addTaskTags`, `removeTaskTag`); fix the code where it differs
- [x] T007 Run `cd frontend && npm run build` with no errors and save the output to loops/frontend-dev/runs/002-us-tags/phase-02-build.log
