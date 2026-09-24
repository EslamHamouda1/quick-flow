---
name: testing
description: Loop-3 (testing). `phase <dev-loop> <phase>` verifies one dev phase (curl for backend-layer phases, Playwright MCP for frontend-layer phases); `suite <requirements-file> [openapi] [ui-url]` runs one test phase of the standalone suite and files bug reports. Only runs when started by scripts/run-loop.sh.
argument-hint: phase <dev-loop> <phase> --runner <nonce> | suite <requirements-file> [openapi] [ui-url] --runner <nonce>
---

# testing loop: one step

Arguments: `$ARGUMENTS`

## 0. Are you being run by the runner?
1. Read `loops/testing/task.md`.
2. The arguments must end with `--runner <nonce>`, and `<nonce>` must equal the `runner_nonce` in that file.
3. If either is missing or they differ, you were typed interactively. **Change no file.** Reply only with:

   > The testing loop needs the runner: it runs the unit tests and starts the app on a fresh test database before any check.
   > Re-check one dev phase: `scripts/run-loop.sh <dev-loop> <requirements-file>` (it tests every phase that is `ready_for_test`).
   > Run the suite: `scripts/run-loop.sh testing <requirements-file>`.

   and stop.

## 1. Do one step
Otherwise the first word of the arguments is the mode (`phase` or `suite`). Read and follow **`loops/testing/Loop-instructions.md`** for that mode exactly, then end the session.
