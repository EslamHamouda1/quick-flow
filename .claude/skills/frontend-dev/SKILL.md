---
name: frontend-dev
description: Loop-2 (frontend-dev). One step of the frontend development loop (plan, review, implement, close) for a requirements file plus the backend swagger. Only runs when started by scripts/run-loop.sh; typed by hand it only prints the command to run.
argument-hint: <requirements-file> <swagger> --runner <nonce>
---

# frontend-dev loop: one step

Arguments: `$ARGUMENTS`

## 0. Are you being run by the runner?
1. Read `loops/frontend-dev/task.md`.
2. The arguments must end with `--runner <nonce>`, and `<nonce>` must equal the `runner_nonce` in that file.
3. If either is missing or they differ, you were typed interactively. **Change no file.** Reply only with:

   > This loop runs headless, one step per session, through the runner. Run it from your terminal:
   > `scripts/run-loop.sh frontend-dev <requirements-file>` (review gates on; the runner picks the swagger for each step), or add `--auto-approve` for an unattended run.

   and stop.

## 1. Do one step
Otherwise read and follow **`loops/frontend-dev/Loop-instructions.md`** exactly. It tells you which single step to do from the state on disk. Do that one step, then end the session.
