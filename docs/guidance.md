# Engineering Guidance

This document defines how new agents/engineers should plan and execute work in this repository.

## Planning Rules

1. Start with scope clarity.
- Confirm objective, inputs, outputs, and acceptance criteria before coding.
- If requirements are unclear, keep the relevant task in `idle` and ask for confirmation.

2. Split work into small, testable tasks.
- Each task should represent one coherent outcome (schema, repo, service, API, docs, tests).
- Avoid mixing unrelated changes in one task.

3. Use task metadata consistently.
- Track work in `docs/phase-*/task.json`.
- Each task must include `id`, `title`, `description`, `related`, and `status`.

## Status Lifecycle

Use only:
- `pending`: not started.
- `planning`: requirements/design in progress.
- `processing`: implementation or verification in progress.
- `idle`: blocked by discussion, decision, or confirmation.
- `completed`: done and validated.

Update status in real time:
1. Set to `planning` when analyzing/designing.
2. Set to `processing` before edits/tests.
3. Set to `completed` only after code + tests/docs are finished.
4. Set to `idle` immediately when blocked.

Do not leave stale `processing` tasks.

## Execution Standards

1. Implement with repository conventions.
- Follow `docs/style/domain-repo-style.md`.
- Keep ownership checks and error handling consistent.

2. Validate every task.
- Run the smallest relevant tests first, then broader tests when needed.
- Record key verification commands/results in summary updates.

3. Keep docs in sync.
- Update phase summary and API docs when behavior changes.
- `task.json` is part of the deliverable, not optional bookkeeping.

## Handoff Checklist

Before ending work:
1. All touched tasks have correct status.
2. New/changed behavior is documented.
3. Test results are known (pass/fail + scope).
4. Next actionable tasks are clear.
