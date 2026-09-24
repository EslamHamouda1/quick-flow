# phase-09: Polish
layer: frontend
story_id: none
spec_phase: 9
depends_on: [phase-03, phase-04, phase-05, phase-06, phase-07, phase-08]
## Goal
Selectors match the UI contract, no business rules in components, docs up to date.
## Acceptance criteria
## Tasks
- [ ] T043 Check every selector of specs/001-quickflow-frontend/contracts/ui-contract.md exists in the templates under frontend/src/app/ and fix the templates (never the contract) where one is missing
- [ ] T044 [P] Check no component computes a status, overdue flag, streak, progress or dashboard figure (Constitution IV) by reading frontend/src/app/pages/ and move any such logic to the API response it belongs to (raise a question if the API lacks the field)
- [ ] T045 [P] Update specs/001-quickflow-frontend/quickstart.md if any command, route or path changed during implementation
