---
name: orchestrator
description: Plans an orchestrated run - reads both dev layers' requirements.json and phases.json, builds the cross-layer dependency graph and writes the story order used by scripts/orchestrate.sh. Typed by hand it only shows the graph.
argument-hint: plan <requirements-file> --runner <nonce>
---

# orchestrator: plan

Arguments: `$ARGUMENTS`

## 0. Are you being run by the runner?
1. Read `loops/orchestrator/task.md`.
2. The arguments must end with `--runner <nonce>`, and `<nonce>` must equal the `runner_nonce` in that file.
3. If either is missing or they differ, you were typed interactively. **Change no file.** Build the dependency graph exactly as `loops/orchestrator/Loop-instructions.md` describes, show it as a mermaid graph in your reply (nothing written to disk), and end with:

   > To run it: `scripts/orchestrate.sh <requirements-file>` in your own terminal (review gates on), or add `--auto-approve`.

## 1. Plan
Otherwise read and follow **`loops/orchestrator/Loop-instructions.md`** exactly, then end the session.
