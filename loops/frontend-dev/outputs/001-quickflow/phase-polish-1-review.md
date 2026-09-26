# phase-polish-1 review: Convergence
layer: frontend
story_id: none
spec_phase: 10
depends_on: []

## Goal
Close the gaps /speckit-converge found after every phase was done: remove the stale scaffold spec file.

## Tasks
- [ ] T048 Remove the stale scaffold spec frontend/src/app/app.spec.ts (asserts an `h1` "Hello, frontend" the shell no longer renders; the frontend writes no spec files) per plan: Testing / FA-1 (unrequested)

## Acceptance criteria
None of its own (Convergence). The story ACs of phases 03–08 must still hold after the change.

## Files
- T048: delete `frontend/src/app/app.spec.ts`. Checked now:
  - it is the only `*.spec.ts` under `frontend/src/` (the generated `src/app/api/` has none);
  - its second test expects `h1` text "Hello, frontend", and `frontend/src/app/app.html` has no `<h1>` and no
    "Hello" (the shell was replaced in phase-02), so the file is stale scaffold output;
  - plan.md "Testing" says the frontend has no unit tests (FA-1, approved) and `layers.frontend.test` is empty.
- The build does not read it: `angular.json` build uses `tsconfig.app.json`, which excludes `src/**/*.spec.ts`. So
  removing it cannot change the built app.
- Not changed: `tsconfig.spec.json`, the `test` target in `angular.json`, the `test` script and the `vitest`
  devDependency in `package.json` (scaffold config, outside T048).
- app-wide: `loops/frontend-dev/outputs/ui-url.md` rewritten unchanged (step I.8); `generate_client` re-run
  (swagger unchanged, generated files should come out the same).
- No endpoint, component or template is added or changed.

## Planned checks
- Implement session (quick feedback only): `cd frontend && npm run build` succeeds; output in
  `loops/frontend-dev/runs/001-quickflow/phase-polish-1-build.log`.
- No unit tests: the frontend layer has no `coverage` and `test` is empty (FA-1, approved).
- Testing loop can check: `frontend/src/app/app.spec.ts` no longer exists, no other `*.spec.ts` under
  `frontend/src/`, the build passes, and a Playwright MCP (headless) smoke run of each page (`/dashboard`, `/tasks`,
  `/habits`, `/learning`, `/plans`, `/settings`) via `nav-*` shows `page-title` with no console errors.

## Risks
- **Empty spec project**: with no `*.spec.ts` and no `src/**/*.d.ts` left, `tsconfig.spec.json` matches no input
  file. An editor or a `tsc -b` over the root `tsconfig.json` (which references it) may report TS18003 "No inputs
  were found", and `npm test` (`ng test`) would find no spec. Neither is part of this layer's build or test commands
  (`npm run build` only; `test` is empty), so the loop is not affected. Not fixed here (see FA-44).
- **Regression**: none expected; the file is excluded from the app build.

## Assumptions
- **FA-44 (new) Scaffold test config left in place**: only the spec file is removed; `tsconfig.spec.json`, the
  `angular.json` `test` target and the `vitest` devDependency stay as the scaffold made them, since T048 does not
  name them and nothing in the loop runs them.
- FA-1 applies as approved.

## Open questions
None.

## Swagger input
`loops/backend-dev/outputs/openapi.json` (from task.md).

## Contract differences
None for this phase: it has no story and uses no endpoint.
