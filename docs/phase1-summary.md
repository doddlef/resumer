# Phase 1 Summary (Auth + Registration Foundation)

Updated: March 22, 2026

## Scope Completed
- Authentication domain and persistence foundation.
- Registration workflow with email-code verification state in Redis.
- Token issuance architecture with clear service boundaries.
- API endpoints for auth and registration.
- Integration/unit test coverage and API docs generation.
- CI workflow for migration + jOOQ codegen + tests.

## Implemented Features

## 1) Database and Migrations
- Flyway migrations:
  - `V1__init.sql` (extensions and shared trigger function)
  - `V2__auth.sql` (auth enums/tables/indexes/triggers)
- Core auth tables:
  - `accounts`
  - `refresh_sessions`

## 2) Auth Services
- `AccessService`: issue and parse access JWT tokens.
- `RefreshService`: create/rotate/revoke refresh sessions with replay detection.
- `TokenService`: single entrypoint facade for login/refresh/access/logout.
- `RegisterService`: start/verify/confirm/resend flow backed by Redis attempts.

## 3) Controllers
- Auth APIs:
  - `POST /api/auth`
  - `POST /api/auth/refresh`
  - `POST /api/auth/logout`
- Register APIs:
  - `POST /api/register/start`
  - `POST /api/register/verify`
  - `POST /api/register`
  - `POST /api/register/resend`

Registration confirmation (`POST /api/register`) returns:
- token pair (access + refresh in response payload)
- refresh cookie (`Set-Cookie` header)

## 4) Security and Session Behavior
- Password hashing via BCrypt.
- Refresh secret hashing and constant-time comparison.
- Refresh rotation with:
  - previous secret grace window handling
  - reuse detection and automatic session revocation
- Cookie generation centralized in `RefreshCookieManager`.

## 5) Testing and Documentation
- Unit tests:
  - `AccessServiceImplTest`
  - `RefreshServiceImplTest`
  - `TokenServiceImplTest`
  - `RegisterServiceImplTest`
- Integration tests:
  - `RedisClientIntegrationTest` (Testcontainers Redis)
  - `RegisterControllerDocumentationTest` (MockMvc + REST Docs)
  - `AuthControllerDocumentationTest` (MockMvc + REST Docs)
- REST Docs snippets generated for:
  - register start/verify/confirm/resend
  - auth login/refresh/logout

## 6) CI Automation
- GitHub Actions workflow runs on push/PR.
- Pipeline sequence:
  1. export env from `.env`
  2. run `app:flywayMigrate`
  3. run `app:jooqCodegen`
  4. run `app:test`
- PostgreSQL service is provisioned in CI for migration/codegen.

## Remaining to Complete Full Phase 1
- Resume upload and object storage integration.
- Resume analysis/scoring pipeline.
- Resume base/history APIs and tests.
