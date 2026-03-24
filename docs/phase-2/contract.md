# Phase 2 Contract

Updated: March 24, 2026

This document defines the agreed contract for Phase 2 implementation.

## Scope

Phase 2 covers:
1. Application session creation (`company`, `position`, `jobDescription`, optional `resumeId`).
2. Position-specific advice generation.
3. Resume-fit recommendations based on resume + session context.
4. Cover letter generation with version history.

## Data Contract

### Resume-fit rewrite suggestions

- `rewriteSuggestions` is stored as `JSONB`.
- It follows a structured list format (validated in service layer), similar to resume analysis suggestions.

Recommended item shape:
- `section`: target section (e.g. `summary`, `experience`, `project`).
- `priority`: `high` | `medium` | `low`.
- `issue`: problem identified in current resume.
- `recommendation`: concrete rewrite guidance.
- `example` (optional): example improved sentence.

### Cover letter content

- Cover letter output format starts as **Markdown**.
- Storage type is **TEXT** in database.
- Add metadata:
  - `version`: incremental integer per session.
- Source-of-truth remains raw Markdown text.

## API Response Baseline

### Session advice response

- `session`: `id`, `company`, `position`, `jobDescription`, `resumeId`, `createdAt`
- `positionAdvice`: `summary`, `keyRequirements[]`, `likelyInterviewFocus[]`, `redFlags[]`
- `resumeFitAdvice`: `fitScore`, `gaps[]`, `rewriteSuggestions[]`

### Cover letter response

- Default returns latest version.
- Support reading specific version in future extension.
- Regenerate creates a **new version** (no overwrite).

## Execution Rules

1. Sync generation first (advice + cover letter) for Phase 2 simplicity.
2. Enforce ownership checks on all session/advice/cover-letter operations.
3. Enforce field limits and non-empty validation in service layer.
4. API docs and tests are required before marking Phase 2 task as completed.
