# phase-polish-1-test: Convergence (frontend-dev)

Source of the checks: `loops/frontend-dev/outputs/001-quickflow/phase-polish-1.md` has an empty
`## Acceptance criteria` section (Convergence, `story_id: none`). The review `phase-polish-1-review.md` says:
"None of its own (Convergence). The story ACs of phases 03–08 must still hold after the change." and lists under
`## Planned checks` what the testing loop can check. Each listed item is one criterion (compare on later attempts):

- R1: "`frontend/src/app/app.spec.ts` no longer exists, no other `*.spec.ts` under `frontend/src/`"
- R2: "the build passes"
- R3: "a Playwright MCP (headless) smoke run of each page (`/dashboard`, `/tasks`, `/habits`, `/learning`, `/plans`,
  `/settings`) via `nav-*` shows `page-title` with no console errors."

| check | criterion | steps | expected |
|---|---|---|---|
| C1 | R1 | `ls frontend/src/app/app.spec.ts`; `find frontend/src -name '*.spec.ts'` (read only), output to `run_dir/file-checks.txt` | file absent; no `*.spec.ts` found |
| C2 | R2 | the testing loop may not write under `frontend/` so it does not run `npm run build`; read the runner's dev-server log `run_dir/server-frontend.log` (`npm start` compiles the same sources) and the dev loop's `phase-polish-1-build.log` | "Application bundle generation complete" with no `error TS` / `✘ [ERROR]` |
| C3 | R3 | Playwright MCP: open `http://localhost:4200/settings`; click `nav-dashboard`, `nav-tasks`, `nav-habits`, `nav-learning`, `nav-plans`, `nav-settings` in turn; after each read URL and `page-title` text; screenshot each; `browser_console_messages` level error | each click lands on its route with a `page-title` (text = nav label, as in phase-09 C8); no console errors |

Screenshots: `<run_dir>/shot-NN-<check>.png`.
Unit tests: the frontend layer has no `test` command and no `coverage` (`unit-result.json` outcome `none`).
