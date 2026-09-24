# Reusability proof: a single user story

[`US-tags.md`](US-tags.md) is a user story that isn't in `Task_PRD.md`. It goes through the
**unchanged** loops, directly (no orchestrator):

```bash
scripts/run-loop.sh backend-dev  examples/single-us/US-tags.md
scripts/run-loop.sh frontend-dev examples/single-us/US-tags.md   # plans from US-tags' own contract, then openapi.json
scripts/run-loop.sh testing      examples/single-us/US-tags.md   # suite mode
```

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
