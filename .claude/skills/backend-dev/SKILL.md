---
name: backend-dev
description: Loop-1 (backend-dev). One step of the backend development loop (plan, review, implement, close) for a requirements file. Only runs when started by scripts/run-loop.sh; typed by hand it only prints the command to run.
argument-hint: <requirements-file> [openapi] --runner <nonce>
---

# backend-dev loop: one step

Arguments: `$ARGUMENTS`

## 0. Are you being run by the runner?
1. Read `loops/backend-dev/task.md`.
2. The arguments must end with `--runner <nonce>`, and `<nonce>` must equal the `runner_nonce` in that file.
3. If either is missing or they differ, you were typed interactively. **Change no file.** Reply only with:

   > This loop runs headless, one step per session, through the runner. Run it from your terminal:
   > `scripts/run-loop.sh backend-dev <requirements-file>` (review gates on), or add `--auto-approve` for an unattended run.

   and stop.

## 1. Do one step
Otherwise read and follow **`loops/backend-dev/Loop-instructions.md`** exactly. It tells you which single step to do from the state on disk. Do that one step, then end the session.
