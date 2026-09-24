# Loop-instructions: orchestrator

You plan an orchestrated run; `scripts/orchestrate.sh` then drives every step through
`scripts/run-loop.sh`, one at a time. You do **only the planning**, in one session.

## Read first
1. `loops/orchestrator/task.md` (`requirements`, `feature`).
2. `project.config.yaml`: the `layers`, their `loop`s and `contract_from`.
3. For every layer's loop: `loops/<loop>/state/<FEATURE>/requirements.json` and `phases.json`.

## Rules
- Write only `loops/orchestrator/state/<FEATURE>/`, `loops/orchestrator/outputs/<FEATURE>/` and the
  action log of `loops/orchestrator/progress.md`. Never write `task.md` or anything else.
- Nothing is hardcoded: the graph comes only from the files above.
- No guessing: a story whose dependencies can't be read from the files is listed under
  `## Questions` in `plan.md` and left out of the order.

## Plan
1. Build the dependency graph. Nodes: `<loop>:<story id>` for every story that has a phase in
   that loop's `phases.json`. Edges: a story's `depends_on` (same loop), and for a layer with
   `contract_from: <other>`, `<other loop>:<story> → <this loop>:<story>` (a story's API before
   its UI).
2. Order it topologically; break ties by story priority (P1 first), then story id, then the
   contract layer first.
3. Write `loops/orchestrator/state/<FEATURE>/order.json`:
   `{"feature": "<FEATURE>", "steps": [{"loop": "<loop>", "story_id": "US1", "goal": "<story title>"}]}`.
4. Write `loops/orchestrator/outputs/<FEATURE>/plan.md`: a mermaid `graph TD` of the graph, the
   ordered step table, and `## Questions` if any.
5. Log one line to `loops/orchestrator/progress.md`. Stop.

## Stop condition
The orchestration (driven by `scripts/orchestrate.sh`) stops when the test suite ends `complete`,
when any `run-loop.sh` call stops early (`quit`, `cost_cap`, `harness`, `violation`,
`needs_input`), or after `orchestrator.max_fix_rounds` suite → fix rounds. This plan session is
retried up to 3 times if it writes no `order.json`. The reason is recorded as `stop_reason` in
`loops/orchestrator/state/<FEATURE>/current.json`.
