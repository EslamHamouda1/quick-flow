# test-phase-regression: every phase-mode check again, plus the unit tests

## Goal
Every check of the phase-mode test plans under `loops/testing/outputs/001-quickflow/{backend-dev,frontend-dev}/`
is run again unchanged on a fresh DB, and the unit tests the runner ran into the attempt folder's `unit/` pass
with domain line coverage ≥ 80 % (`com/quickflow/domain`, config `coverage.min_line`).

Depends on: test-phase-01..07 and test-phase-E2E.

## Checks
- [x] R1 backend-dev phase-01 Setup: C1–C7 (curl).
- [x] R2 backend-dev phase-02 Foundational: C1–C7 (curl).
- [x] R3 backend-dev phase-03 US1: C1–C12 (curl).
- [x] R4 backend-dev phase-04 US2: C1–C13 (curl).
- [x] R5 backend-dev phase-05 US3: C1–C10 (curl).
- [x] R6 backend-dev phase-06 US4: C1–C15 (curl).
- [x] R7 backend-dev phase-07 US5: C1–C6 (curl).
- [x] R8 backend-dev phase-08 US6: C1–C5 (curl).
- [x] R9 backend-dev phase-09 Polish: C1–C7 (curl / file checks).
- [x] R10 frontend-dev phase-01 Setup: C1–C3 (Playwright MCP).
- [x] R11 frontend-dev phase-02 Foundational: C1–C9 (Playwright MCP).
- [x] R12 frontend-dev phase-03 US1: C1–C13 (Playwright MCP).
- [x] R13 frontend-dev phase-04 US2: C1–C9 (Playwright MCP).
- [x] R14 frontend-dev phase-05 US3: C1–C9 (Playwright MCP).
- [x] R15 frontend-dev phase-06 US4: C1–C13 (Playwright MCP).
- [x] R16 frontend-dev phase-07 US5: C1–C9 (Playwright MCP).
- [x] R17 frontend-dev phase-08 US6: C1–C8 (Playwright MCP).
- [x] R18 frontend-dev phase-09 Polish: C1–C9 (Playwright MCP).
- [x] R19 frontend-dev phase-polish-1 Convergence: C1–C3 (Playwright MCP / file checks).
- [x] R20 Unit tests: the attempt folder's `unit/` result shows 0 failed and domain line coverage ≥ 80 %.
