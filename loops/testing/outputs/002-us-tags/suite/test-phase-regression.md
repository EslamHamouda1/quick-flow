# test-phase-regression: every phase-mode check plus unit tests

## Goal
Every phase-mode check of feature `002-us-tags` is run again, unchanged, on the runner's fresh test database, plus
the unit tests the runner has already run into the attempt folder's `unit/`.

Sources: the phase-mode test plans under `loops/testing/outputs/002-us-tags/{backend-dev,frontend-dev}/`,
`project.config.yaml` (`coverage`: `com/quickflow/domain`, min line 0.80). Evidence:
`runs/002-us-tags/suite/test-phase-regression/attempt-<n>/`.

## Checks
- [ ] R1 backend-dev phase-01 C1–C6 (`phase-01-test.md`, `phase-01-verify.sh`).
- [ ] R2 backend-dev phase-02 C1–C7 (`phase-02-test.md`: swagger vs contract, app-wide copy = live, phase-01 re-run, `@NotEmpty` bodies / 404 / missing `tag`, dashboard `tags`, unit, coverage).
- [ ] R3 frontend-dev phase-01 C1–C6 (`phase-01-test.md`, Playwright MCP).
- [ ] R4 frontend-dev phase-02 C1–C9 (`phase-02-test.md`: phase-01 re-run, ui-contract selectors/texts, Enter/notice, build log).
- [ ] R5 Unit tests: `unit/` backend result has 0 failed and 0 errors; frontend outcome `none` (no unit tests configured).
- [ ] R6 Coverage: line coverage on `com.quickflow.domain*` ≥ 0.80 (constitution III).
