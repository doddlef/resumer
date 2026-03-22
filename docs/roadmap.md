# Resume Helper Roadmap

## Vision
Build a full-stack interview guidance platform for students using Spring Boot + React. The system helps users upload resumes, receive structured analysis and targeted advice, prepare application materials, and practice interviews with progressive intelligence.

## Progress Snapshot (Updated: March 22, 2026)
### Completed in Current Phase Slice
- Authentication foundation (Spring Security + JWT + refresh session rotation/revocation).
- Registration flow (`start -> verify -> confirm -> resend`) with Redis-backed attempts.
- Token facade architecture (`TokenService` as entry, delegating to `AccessService` and `RefreshService`).
- API controllers:
  - `/api/auth` (login)
  - `/api/auth/refresh`
  - `/api/auth/logout`
  - `/api/register`
  - `/api/register/start|verify|resend`
- Flyway migrations for auth schema (`accounts`, `refresh_sessions`).
- jOOQ repository layer with extracted query DTOs (`auth/repo/query/*`).
- Integration/unit test coverage for auth/register/token/redis flows.
- Spring REST Docs snippets generation through MockMvc tests.
- GitHub Actions CI:
  - export env
  - `app:flywayMigrate`
  - `app:jooqCodegen`
  - `app:test`

### Remaining for Full Phase 1 Completion
- Resume upload/storage module.
- Resume analysis/scoring pipeline and result persistence.
- Resume base listing/detail APIs and UI integration.

## Product Principles
- Start with production-grade foundations (auth, modular backend, persistent data model).
- Prioritize maintainability and extensibility over fast but brittle shortcuts.
- Keep AI outputs explainable with score breakdowns and rubric-backed suggestions.
- Design early for async processing and provider abstraction (file storage + LLM).
- Build features as vertical slices that are demo-ready and testable.

## Current Scope Decisions
- Authentication starts in Phase 1 (`register/login/refresh/logout`).
- Admin is deferred to a future phase.
- Resume/documents stored in S3-compatible object storage via abstract `FileEngine`.
- PostgreSQL stores metadata, analysis records, sessions, and tracking data.
- LLM access is abstracted with `LLMClient`/`AgentClient`.
- Development provider is local Ollama; architecture supports other providers.
- Mock interview starts as text-only; voice is future scope.

## Target Architecture (High Level)
### Frontend (React)
- Authentication pages and token lifecycle handling.
- Resume dashboard: upload, list, analysis detail view.
- Job targeting workspace: company + position + JD session.
- Application tracker UI (status, deadline, notes).
- Interview practice UI (text chat style).

### Backend (Spring Boot)
- `auth`: user accounts, JWT access token, refresh token rotation.
- `resume`: upload metadata, file references, versioning.
- `analysis`: rubric scoring + LLM summary/suggestions.
- `job-session`: JD ingestion and resume-job fit analysis.
- `generation`: cover letter and tailored output generation.
- `knowledge`: company/position knowledge and user-uploaded materials.
- `mock-interview`: question generation, answer scoring, coaching.
- `application-tracker`: lifecycle and status timeline.

### Core Infrastructure
- `FileEngine` abstraction:
  - `S3FileEngine` (primary)
  - `LocalFileEngine` (dev fallback)
- `LLMClient` abstraction (provider-level chat/completion)
- `AgentClient` abstraction (task-level workflows)
- Async processing for long-running AI jobs (queue/job table)
- Observability: structured logs + job/error traces

## Data Model Baseline (Early, Cross-Phase)
- `users`
- `refresh_tokens`
- `resumes`
- `resume_versions`
- `resume_analyses`
- `job_sessions`
- `tailored_advices`
- `cover_letters`
- `applications`
- `application_status_history`
- `interview_sessions`
- `interview_questions`
- `interview_answers`
- `interview_scores`
- `knowledge_documents`
- `knowledge_chunks`

Note: all domain tables should include `user_id` from the beginning.

## Phase Roadmap

## Phase 1: Foundation + Authentication + Resume Analysis MVP
### Goals
- Deliver secure user onboarding and login.
- Support resume upload/storage and analysis with score breakdown.
- Provide a resume base view (history of uploads and analyses).

### Functional Scope
- Auth
  - Register, login, refresh, logout
  - JWT access token + DB-backed refresh token rotation
- Resume
  - Upload resume file to S3 via `FileEngine`
  - Persist resume metadata and version records in PostgreSQL
- Analysis
  - Trigger analysis job for uploaded resume
  - Save:
    - `score` (overall)
    - rubric scores (e.g., `structure_score`, `impact_score`, `clarity_score`)
    - `summary`
    - `strengths` (jsonb)
    - `suggestions` (jsonb)
    - provider/model/prompt version metadata
- Resume base view
  - List resumes and analysis status/results
  - Open analysis details for each record

### Non-Functional Scope
- Service interfaces for `FileEngine`, `LLMClient`, `AgentClient`
- Input validation and robust error handling
- Basic rate limiting for auth endpoints
- Integration tests for auth and one protected feature endpoint

### Acceptance Criteria
- User can register and login successfully.
- Protected endpoints reject invalid/expired tokens.
- Resume file is uploaded and retrievable via stored key.
- Analysis result is persisted with score + explanation fields.
- User can view historical resume uploads and analyses.

### Implementation Status (March 22, 2026)
- Done:
  - Register/login/refresh/logout backend flows.
  - JWT access token issue/parse and refresh session rotation/reuse handling.
  - DB migrations + jOOQ codegen integration.
  - Auth/register controllers and cookie handling.
  - MockMvc integration tests and REST Docs for auth/register endpoints.
- In progress / pending:
  - Resume upload/storage
  - Resume analysis and scoring
  - Resume base query/view endpoints

## Phase 2: Job Targeting + Cover Letter
### Goals
- Make advice specific to company/position/JD.
- Generate tailored outputs that increase role fit.

### Functional Scope
- Create job session with:
  - company
  - position
  - job description
- Generate role-targeted advice based on resume + JD
- Identify resume-job gaps and actionable rewrites
- Generate cover letter from resume + job context

### Acceptance Criteria
- Job session can be created and linked to a resume.
- Tailored advice clearly references JD requirements.
- Cover letter is generated and stored as a versioned artifact.

## Phase 3: External Context Enrichment + Enhanced Coaching
### Goals
- Improve personalization using company/position trends/news.
- Introduce interview practice with scoring in text mode.

### Functional Scope
- Fetch and store company/role trend/news context
- Enrich resume advice and cover letters with external context
- Mock phone/chat simulation (text only in this phase)
- Generate interview questions and score user answers
- Return answer feedback and improvement suggestions

### Acceptance Criteria
- Generated outputs cite/use relevant external context.
- Interview session stores prompts, answers, scores, and feedback.
- Users receive clear, actionable feedback on each answer.

## Phase 4: Knowledge Base + Full Mock Interview Layer
### Goals
- Allow user-managed knowledge ingestion.
- Improve quality/consistency of interview and advice outputs.

### Functional Scope
- User uploads documents into personal knowledge base
- Chunk/index knowledge for retrieval-assisted generation
- Advanced mock interview sessions with structured rubrics
- Better answer coaching with competency-level breakdown

### Acceptance Criteria
- Uploaded knowledge can be used to improve output relevance.
- Mock interview scoring follows visible rubric dimensions.
- Historical sessions are browseable and comparable.

## Phase 5 (Continuous): Application Tracking
### Goals
- Track application lifecycle while other phases are developed.

### Functional Scope
- Create application entries with company/position/deadline/status
- Update timeline events (applied, OA, interview, offer, reject)
- Attach notes, linked resumes, and generated cover letters
- Dashboard for upcoming deadlines and pipeline status

### Acceptance Criteria
- Application lifecycle is persisted and queryable.
- Resume/job-session assets can be linked to applications.
- User can monitor deadlines and status progression.

## Cross-Cutting Technical Standards
- Keep business logic in service layer; controllers remain thin.
- Use DTOs + mappers for API boundaries.
- Version prompts/templates and record model/provider metadata.
- Keep deterministic score logic separate from free-form LLM advice.
- Add migration scripts for all schema changes.
- Maintain API contract docs (OpenAPI/Swagger).

## Security and Privacy Baseline
- Hash passwords with BCrypt.
- Store refresh tokens hashed and revocable.
- Restrict CORS to known frontend origins.
- Scan and validate upload file types/sizes.
- Avoid logging sensitive resume content and PII.
- Define retention/deletion strategy for user documents.

## Risks and Mitigations
- AI output inconsistency
  - Mitigation: rubric + deterministic checks + prompt versioning
- Long-running analysis latency
  - Mitigation: async jobs + status polling
- Vendor lock-in for LLM or storage
  - Mitigation: strict abstractions (`LLMClient`, `FileEngine`)
- Scope expansion
  - Mitigation: hard phase gates and acceptance criteria

## Suggested Milestones
1. Milestone A (Phase 1 core): Auth + resume upload + list view
2. Milestone B (Phase 1 complete): Resume analysis + scoring + tests
3. Milestone C (Phase 2): Job session + tailored advice + cover letter
4. Milestone D (Phase 3): External context + interview text simulation
5. Milestone E (Phase 4): Knowledge base + advanced mock interview
6. Milestone F (Continuous): Application tracker matured across phases

## Out of Scope for Initial MVP
- Social login and enterprise SSO
- Full RBAC admin portal
- Voice interview processing
- Real-time collaborative editing
- Mobile app clients
