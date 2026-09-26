# Reusability proof: a single user story

[`US-tags.md`](US-tags.md) is a user story that isn't in `Task_PRD.md`. It goes through the
**unchanged** loops, either directly, one loop at a time:

```bash
scripts/run-loop.sh backend-dev  examples/single-us/US-tags.md
scripts/run-loop.sh frontend-dev examples/single-us/US-tags.md   # plans from US-tags' own contract, then openapi.json
scripts/run-loop.sh testing      examples/single-us/US-tags.md   # suite mode
```

or all at once with `scripts/orchestrate.sh examples/single-us/US-tags.md` (what the run below used).

It becomes its own feature, `002-us-tags` (QuickFlow is `001-quickflow`), so none of QuickFlow's
files are touched. Its files:

| What | Where |
|---|---|
| spec-kit features | [`specs/002-us-tags-backend/`](../../specs/002-us-tags-backend/), [`specs/002-us-tags-frontend/`](../../specs/002-us-tags-frontend/) |
| backend phases, reviews, test copies | [`loops/backend-dev/outputs/002-us-tags/`](../../loops/backend-dev/outputs/002-us-tags/) |
| frontend phases, reviews, test copies | [`loops/frontend-dev/outputs/002-us-tags/`](../../loops/frontend-dev/outputs/002-us-tags/) |
| state | `loops/*/state/002-us-tags/` |
| test plans, reports, bugs | [`loops/testing/outputs/002-us-tags/`](../../loops/testing/outputs/002-us-tags/) |
| evidence (curl logs, screenshots, unit reports) | `loops/testing/runs/002-us-tags/`, indexed from `loops/*/runs/002-us-tags/*/attempt-*/INDEX.md` |
| milestone rows | `backend-dev:002-us-tags:*`, `frontend-dev:002-us-tags:*` in each `progress.md` |

Check that nothing in the engine changed: `git diff --stat <start>..HEAD -- .claude/skills loops/*/Loop-instructions.md`
shows nothing, and `git diff --stat <start>..HEAD -- '*001-quickflow*'` shows nothing.

## Result (run 2026-09-26 22:35 → 2026-09-27 00:11)

Run with `scripts/orchestrate.sh examples/single-us/US-tags.md --auto-approve`, starting at commit `02a917d`.

| Check | Result |
|---|---|
| Skills and `Loop-instructions.md` changed during the run | **none** (`git diff --stat 02a917d..318fbaa -- .claude/skills 'loops/*/Loop-instructions.md'` is empty) |
| `001-quickflow` files changed | **none** |
| Own feature folders | `specs/002-us-tags-{backend,frontend}/`, `loops/*/{state,outputs}/002-us-tags/` |
| backend-dev / frontend-dev | `complete` / `complete`: 3 phases each (plan, the story, Polish), every one verified on attempt 1 |
| test suite | `complete`: API contract, the story (API + browser), regression — all passed, **0 bugs** |
| Questions asked instead of guessing | 3 at the backend plan (tag case, duplicate add, removing a missing tag), answered by the owner |
| Cost / time | 25 sessions, $32.54, 82 min of session time |

Commits: plans `5517e77` `b772ca7`, story order `840a8b8`, story `dbe139e` (backend) `13ad7c5`
(frontend), Polish `45ed52e` `76bd8e8`, suite `31faafa` → `914ca22` `ac0dd20` `318fbaa`.

Two things happened that the proof doesn't hide:
- **Two runner scripts changed during the run** (`scripts/orchestrate.sh`, `scripts/status.sh`, commit
  `91be504`): `orchestrate.sh` stopped at step 2 because a brand-new feature had no `order.json` yet
  (a bug in an earlier engine fix). That is a runner fix; no skill or loop instruction changed.
- The run was stopped once by the guard as a `violation` because a runner-owned file
  (`docs/commit-reviews.md`) was edited by hand during a session; the guard restored it, as designed.
