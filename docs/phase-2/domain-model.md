# Phase 2 Domain Model (Draft)

Updated: March 24, 2026

This draft proposes the Phase 2 model for application session, advice generation, resume-fit output, and cover letter versioning.

## 1) application_sessions

Represents one target application context.

- `id`: uuid, pk
- `account_id`: uuid, fk -> `accounts.id`, not null
- `resume_id`: uuid, fk -> `resumes.id`, nullable (allow session-first flow)
- `company`: text, not null
- `position`: text, not null
- `job_description`: text, not null
- `created_at`: timestamptz, not null
- `updated_at`: timestamptz, not null

Indexes:
- `(account_id, created_at desc)`
- `(account_id, resume_id)`

Constraints:
- `company`, `position`, `job_description` trimmed non-blank.
- If `resume_id` is set, ownership must match `account_id` (enforced in service).

## 2) session_advice

Stores generated advice for a session (sync generation in Phase 2).

- `id`: uuid, pk
- `session_id`: uuid, fk -> `application_sessions.id`, not null
- `position_summary`: text, not null
- `key_requirements_json`: jsonb, not null, default `[]`
- `likely_interview_focus_json`: jsonb, not null, default `[]`
- `red_flags_json`: jsonb, not null, default `[]`
- `fit_score`: int, not null (0..100)
- `gaps_json`: jsonb, not null, default `[]`
- `rewrite_suggestions_json`: jsonb, not null, default `[]`
- `created_at`: timestamptz, not null

Indexes:
- `(session_id, created_at desc)`

Notes:
- Multiple rows allowed for regeneration/history.
- API returns latest by default.

### rewrite_suggestions_json item schema

Each item is an object:
- `section`: string
- `priority`: `high|medium|low`
- `issue`: string
- `recommendation`: string
- `example`: string (optional)

## 3) session_cover_letters

Stores cover letters as versioned Markdown text.

- `id`: uuid, pk
- `session_id`: uuid, fk -> `application_sessions.id`, not null
- `version`: int, not null (starts at 1, increments per session)
- `content`: text, not null
- `created_at`: timestamptz, not null

Unique:
- `(session_id, version)`

Indexes:
- `(session_id, created_at desc)`

Constraints:
- `content` trimmed non-blank.

## 4) Domain objects (Kotlin)

Recommended entities:
- `ApplicationSession`
- `SessionAdvice`
- `SessionCoverLetter`

Recommended value objects:
- `RewriteSuggestion`

## 5) API-aligned read models

- `SessionDetailView`: session base info.
- `SessionAdviceView`: latest advice + fit output.
- `SessionCoverLetterView`: latest or requested version.

## 6) Out-of-scope for this draft

- Async generation status tables (not needed for Phase 2 sync baseline).
- Provider-level prompt/version registry tables.
