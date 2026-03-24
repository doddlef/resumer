## Database

### Operational Prerequisites
- PostgreSQL extensions `citext` and `pgcrypto` must be available.
- The database role used by Flyway/application startup must have permission to create extensions, or extensions must be pre-provisioned by an admin.
- If extension creation is restricted in production, provision extensions in a bootstrap step before running application migrations.

### Account and Auth

#### account_status
Enum type representing account status:
- `ACTIVE`
- `ARCHIVED`: account has been archived, cannot be used, and may be deleted in the future
- `LOCKED`: account has been locked

#### account_role
Enum type representing account role:
- `USER`
- `ADMIN`

#### accounts
Table representing accounts in the system:
- `id`: uuid, pk
- `email`: citext, unique, not null
- `password`: text, hashed password, not null
- `name`: text, display name, not null
- `status`: account_status, not null, default `ACTIVE`
- `role`: account_role, not null, default `USER`
- `created_at`: timestamptz, not null
- `updated_at`: timestamptz, not null

#### refresh_sessions
Table representing an authentication session. Each session should represent one active refresh token lineage.
- `id`: uuid, pk: session id
- `account_id`: uuid, fk -> `accounts.id`, not null
- `secret`: text, hashed secret, not null
- `prev_secret`: text, optional, hashed previous secret in the same session; used to detect reuse
- `last_used_at`: timestamptz, optional, last rotation time; null if never rotated
- `revoked_at`: timestamptz, optional, revoke time; null if active
- `revoke_reason`: text, optional, revoke reason; must be non-null when `revoked_at` is non-null
- `created_at`: timestamptz, not null
- `updated_at`: timestamptz, not null

### Registration Attempt Cache (Redis)
Registration verification state is stored in Redis (ephemeral, TTL-based), not PostgreSQL.

#### Key Patterns
- `register:attempt:{attemptId}`
- `register:email:{emailHash}`
- `register:cooldown:{attemptId}`

#### Cached Model (`RegisterAttempt`)
- `attemptId`
- `email` (normalized lowercase)
- `codeHash` (hashed verification code; raw code is never persisted)
- `verifiedAt` (null until verification succeeds)
- `tryCount`
- `createdAt`

#### Notes
- `attempt` and `email` keys use the same TTL (`attemptLifetime`).
- `resend` uses cooldown key with `resendCooldown` TTL.
- On successful registration confirmation, attempt/email/cooldown keys are deleted.

### Resume and Analysis

#### resume_status
Enum type representing resume processing status:
- `PENDING`: resume is uploaded, and waiting for analysis
- `ANALYZING`: resume is being analyzed
- `COMPLETED`: resume analysis is completed, and results are available
- `FAILED`: resume analysis failed, and error message is available

#### resumes
Table representing uploaded resumes and their analysis status:
- `id`: uuid, pk
- `account_id`: uuid, fk -> `accounts.id`, not null
- `filename`: text, original filename of the uploaded resume, not null
- `content`: text, extracted text content from the resume file for analysis
- `file_hash`: text, hash of the resume file for deduplication, not null
- `size`: bigint, size of the resume file in bytes, not null
- `storage_key`: text, key or path to access the stored resume file, not null
- `status`: resume_status, not null, default `PENDING`
- `error`: text, optional, error message if analysis failed
- `created_at`: timestamptz, not null
- `updated_at`: timestamptz, not null

#### resume_analysis
Table representing the results of resume analysis:
- `id`: uuid, pk
- `resume_id`: uuid, fk -> `resumes.id`, not null
- `score`: int, overall resume score (0-100), not null
- `summary`: text, overall comment on the resume, not null
- `strengthsJson`: jsonb, list of identified strengths with comments, not null
- `suggestionsJson`: jsonb, list of suggestions for improvement with comments, not null
- `created_at`: timestamptz, not null

**Note**: resume can have multiple analysis records if re-analyzed after updates,
but only the latest analysis is relevant for the current resume content.

### Application Session and Career Guidance (Phase 2)

#### session_task_status
Enum type representing async advice task status:
- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`

#### application_sessions
Table representing one target application context:
- `id`: uuid, pk
- `account_id`: uuid, fk -> `accounts.id`, not null
- `resume_id`: uuid, fk -> `resumes.id`, nullable, `on delete set null`
- `company`: text, target company name, not null
- `position`: text, target position title, not null
- `job_description`: text, target position description, not null
- `position_advice_status`: session_task_status, status of position advice job
- `position_advice_error`: text, optional error of position advice job
- `resume_fit_status`: session_task_status, status of resume-fit advice job
- `resume_fit_error`: text, optional error of resume-fit advice job
- `created_at`: timestamptz, not null
- `updated_at`: timestamptz, not null

#### session_advice
Table representing generated position and resume-fit guidance:
- `id`: uuid, pk
- `session_id`: uuid, fk -> `application_sessions.id`, not null
- `position_summary`: text, summary of role/company preparation, not null
- `key_requirements_json`: jsonb, list of key role requirements, not null
- `likely_interview_focus_json`: jsonb, list of likely interview focuses, not null
- `red_flags_json`: jsonb, list of missing/weak areas, not null
- `fit_score`: int, overall fit score (0-100), not null
- `gaps_json`: jsonb, list of candidate gaps for this session, not null
- `rewrite_suggestions_json`: jsonb, structured rewrite suggestions for resume fit, not null
- `created_at`: timestamptz, not null

#### session_cover_letters
Table representing generated cover letter versions:
- `id`: uuid, pk
- `session_id`: uuid, fk -> `application_sessions.id`, not null
- `version`: int, version number per session, not null
- `content`: text, cover letter markdown content, not null
- `created_at`: timestamptz, not null

**Note**: cover letter is generated in markdown format, but stored as text.
Regeneration creates a new version row instead of overwriting old content.
