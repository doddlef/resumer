# Phase 1 Summary (Auth + Resume Upload Foundation)

Updated: March 24, 2026

This document is the handoff/onboarding guide for current Phase 1 state.
If you are new to the project, read in this order:
1. `What Is Done`
2. `How It Works`
3. `Run Locally`
4. `Next Steps`

## What Is Done

### Auth and registration
- Auth/register flows are implemented end-to-end.
- JWT access + refresh session rotation/reuse detection are implemented.
- Register flow (`start -> verify -> confirm -> resend`) uses Redis attempt cache.
- Auth/register controllers and MockMvc documentation tests are in place.

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

### Resume upload and queue foundation
- Resume upload service pipeline is implemented:
  - validate file
  - detect content type
  - dedupe by `(account_id, file_hash)`
  - parse content
  - upload file
  - persist resume
  - publish analysis message
- Resume upload API endpoint is implemented:
  - `POST /api/resumes/upload-and-analyze`
- Redis Stream MQ foundation is implemented:
  - abstract publisher/consumer
  - message wrapper + retry + DLQ flow
  - resume-specific publisher/consumer + placeholder analysis processor

## How It Works

### Upload flow (current)
1. Client uploads file to `/api/resumes/upload-and-analyze`.
2. Service validates and detects content type (`application/pdf`, `text/plain`).
3. Service computes file hash and checks duplicate resume for account.
4. Service parses content and uploads file to storage.
5. Service inserts resume record (`PENDING`) and publishes analysis job.
6. API returns upload result (`resumeId`, `name`, `status`, `duplicate`, `createdAt`).

### Analysis flow (current)
- Resume analysis consumer receives stream message and calls placeholder processor.
- Real analysis scoring/persistence is not implemented yet.

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

## Next Steps (Phase 1 Completion)
1. Implement real `ResumeAnalysisJobProcessor`:
   - run analysis logic
   - insert `resume_analysis`
   - update `resumes.status` (`COMPLETED` / `FAILED`)
2. Add resume detail/list APIs:
   - `GET /api/resumes/{id}`
   - `GET /api/resumes`
   - include latest analysis summary/score
3. Add upload + analysis integration tests:
   - happy path
   - duplicate path
   - MQ publish failure path
4. Add API docs for resume endpoints (MockMvc REST Docs).
5. Finalize retry/DLQ operations and implement stream pending reclaim (`XAUTOCLAIM`) in Redis client.
