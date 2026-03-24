# Phase 1 Summary (Completed)

Updated: March 24, 2026

This document is the handoff/onboarding guide for current Phase 1 state.
If you are new to the project, read in this order:
1. `What Is Done`
2. `How It Works`
3. `Run Locally`
4. `Phase 2 Entry`

## What Is Done

### Auth and registration
- Auth/register flows are implemented end-to-end.
- JWT access + refresh session rotation/reuse detection are implemented.
- Register flow (`start -> verify -> confirm -> resend`) uses Redis attempt cache.
- Auth/register controllers and MockMvc documentation tests are in place.
- JWT parse failures (invalid/expired) are handled with standardized JSON error responses.

### Database and repository foundation
- Flyway migrations:
  - `V1__init.sql`
  - `V2__auth.sql`
  - `V3__resume.sql`
- Resume tables and status enum are added:
  - `resumes`
  - `resume_analysis`
- Resume domain and repo layer are implemented:
  - `module.resume.domain`
  - `module.resume.repo` + `repo/query` + jOOQ implementation

### File and storage foundation
- File infrastructure implemented:
  - content type detection
  - document parsing + cleaning
  - SHA-256 hashing
- Storage infrastructure implemented:
  - `FileEngine` abstraction
  - secure local engine with temporary signed URL endpoint

### Resume upload, analysis, and query APIs
- Resume upload service pipeline is implemented:
  - validate file
  - detect content type
  - dedupe by `(account_id, file_hash)`
  - parse content
  - upload file
  - persist resume
  - publish analysis message
- Resume APIs implemented:
  - `POST /api/resumes/upload-and-analyze`
  - `POST /api/resumes/{id}/reanalyze`
  - `GET /api/resumes`
  - `GET /api/resumes/{id}`
  - `GET /api/resumes/{id}/status`
- Resume analysis service is implemented:
  - prompts + structured output parsing
  - insert `resume_analysis`
  - update `resumes.status` (`ANALYZING` -> `COMPLETED` / `FAILED`)
- Ownership checks are applied to resume detail/status/reanalysis.

### Redis Stream MQ foundation
- Redis Stream MQ foundation is implemented:
  - abstract publisher/consumer
  - message wrapper + retry + DLQ flow
  - resume-specific publisher/consumer + real analysis processor wiring
  - consumer group creation made idempotent (`BUSYGROUP` safe handling)
  - pending reclaim implemented in `streamAutoClaim` (XPENDING + XCLAIM flow)

### Tests and API docs
- Resume API documentation test added:
  - upload, list, detail, status, reanalyze
- Resume service coverage added:
  - query service tests
  - reanalysis tests
  - upload duplicate + publish-failure tests
- Redis integration test added for pending reclaim behavior.

## How It Works

### Upload + analysis flow (current)
1. Client uploads file to `/api/resumes/upload-and-analyze`.
2. Service validates and detects content type (`application/pdf`, `text/plain`).
3. Service computes file hash and checks duplicate resume for account.
4. Service parses content and uploads file to storage.
5. Service inserts resume record (`PENDING`) and publishes analysis job.
6. Consumer receives job, invokes analysis service, stores `resume_analysis`, updates resume status.
7. API returns upload result (`resumeId`, `name`, `status`, `duplicate`, `createdAt`).

### Read/query flow (current)
- List page calls `GET /api/resumes` and receives: `id`, `name`, `status`, `score`, `createdAt`.
- Detail page calls `GET /api/resumes/{id}` and receives latest analysis (`summary`, `strengths`, `suggestions`) plus status/error.
- Polling calls `GET /api/resumes/{id}/status` for lightweight status updates.

## Run Locally

### Prerequisites
- JDK 21
- PostgreSQL + Redis
- `.env` created from `.env.example`

### Commands
```bash
set -a
source .env
set +a

./gradlew app:flywayMigrate
./gradlew app:jooqCodegen
./gradlew app:test
./gradlew app:bootRun
```

## Phase 2 Entry
With Phase 1 complete, the next implementation focus is Phase 2:
1. Add application session domain (`company`, `position`, `job description`) and APIs.
2. Generate position-specific advice using resume + position context.
3. Add cover letter generation endpoint and persistence.
4. Introduce minimal history views for session runs and generated outputs.
