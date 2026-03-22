# Phase 1 Summary (Auth + Registration Foundation)

Updated: March 22, 2026

This document is the handoff/onboarding guide for current Phase 1 state.  
If you are new to the project, start from `Getting Started`, then read `How It Works` and `Where to Change`.

## What Is Done in This Phase
- Auth schema and migrations are implemented.
- Auth/register backend flows are implemented end-to-end.
- Token architecture is in place (`TokenService` entry + delegated services).
- Auth/register API endpoints are implemented.
- Unit + integration tests are in place.
- REST Docs snippets are generated from MockMvc integration tests.
- CI runs migration + jOOQ codegen + tests.

## Architecture Snapshot

## Service Layer
- `AuthService`: email/password authentication via Spring Security `AuthenticationManager`.
- `AccessService`: JWT access token issue and parse.
- `RefreshService`: refresh session create/rotate/revoke with replay detection.
- `TokenService`: single entry point for login/refresh/access/logout.
- `RegisterService`: registration flow (`start -> verify -> confirm -> resend`), backed by Redis.

## Controller Layer
- `POST /api/auth` login.
- `POST /api/auth/refresh` refresh with cookie token.
- `POST /api/auth/logout` revoke + clear cookie.
- `POST /api/register/start`
- `POST /api/register/verify`
- `POST /api/register` (confirmation endpoint)
- `POST /api/register/resend`

`/api/register` confirmation returns:
- `token_pair` in payload (access + refresh)
- refresh cookie via `Set-Cookie`

## Persistence and State
- PostgreSQL stores durable auth data (`accounts`, `refresh_sessions`).
- Redis stores temporary registration attempts and cooldown state.

## Database and Migrations
- `V1__init.sql`: extensions + common trigger function.
- `V2__auth.sql`: auth enums, tables, indexes, update triggers.
- Core tables:
  - `accounts`
  - `refresh_sessions`

## Security Behavior
- Passwords hashed with BCrypt.
- Refresh secret is hashed before persistence.
- Refresh rotation supports brief grace for previous token.
- Reuse detection revokes session (`refresh_token_reuse_detected`).
- Refresh cookie construction is centralized in `RefreshCookieManager`.

## Getting Started (Local)

## 1) Prerequisites
- JDK 21
- Docker (for local Postgres/Redis via your setup) or manually running Postgres/Redis
- Gradle wrapper (`./gradlew`)

## 2) Prepare `.env`
Copy `.env.example` and fill values:
- DB:
  - `POSTGRES_URL`
  - `POSTGRES_DB`
  - `POSTGRES_USER`
  - `POSTGRES_PASSWORD`
- Auth:
  - `AUTH_ACCESS_SECRET`
  - `AUTH_REFRESH_SECRET`
  - `REGISTER_CODE_HASH_SECRET`

## 3) Build DB Schema + jOOQ
Run in project root:

```bash
set -a
source .env
set +a

./gradlew app:flywayMigrate
./gradlew app:jooqCodegen
```

## 4) Run Tests
```bash
./gradlew app:test
```

## 5) Run Application
```bash
set -a
source .env
set +a
./gradlew app:bootRun
```

## How to Use Current APIs

## Register flow
1. `POST /api/register/start`
2. `POST /api/register/verify`
3. `POST /api/register` (confirm account and receive token pair + cookie)
4. `POST /api/register/resend` (if needed before verify)

## Auth flow
1. `POST /api/auth` (email/password login)
2. `POST /api/auth/refresh` (requires refresh cookie)
3. `POST /api/auth/logout` (revoke + clear refresh cookie)

## Testing and API Docs

## Unit tests
- `AccessServiceImplTest`
- `RefreshServiceImplTest`
- `TokenServiceImplTest`
- `RegisterServiceImplTest`

## Integration tests
- `RedisClientIntegrationTest`
- `RegisterControllerDocumentationTest`
- `AuthControllerDocumentationTest`

## REST Docs snippets
Generated under `app/build/generated-snippets` for:
- `register-start`
- `register-verify`
- `register-confirm`
- `register-resend`
- `auth-login`
- `auth-refresh`
- `auth-logout`

## Generate docs artifact
```bash
./gradlew app:asciidoctor
```

## CI Behavior
GitHub Actions (`.github/workflows/ci.yml`) does:
1. start PostgreSQL service
2. export env
3. `app:flywayMigrate`
4. `app:jooqCodegen`
5. `app:test`

CI requires repository configuration:
- `secrets.POSTGRES_PASSWORD`
- `secrets.AUTH_ACCESS_SECRET` (or fallback used in workflow)
- `secrets.AUTH_REFRESH_SECRET` (or fallback used in workflow)
- `secrets.REGISTER_CODE_HASH_SECRET` (or fallback used in workflow)
- optional `vars.POSTGRES_DB`, `vars.POSTGRES_USER`

## Where to Change Next
- Auth API request/response shape: `auth/api` and `auth/api/dto`.
- Token/session logic: `auth/service` + `auth/service/impl`.
- DB schema changes: `app/src/main/resources/db/migration`.
- Repository query style: `auth/repo` + `auth/repo/query`.
- Test/docs for new endpoints: `auth/api/*DocumentationTest.kt`.

## Remaining for Full Phase 1 Completion
- Resume upload and object storage integration.
- Resume analysis/scoring pipeline.
- Resume base/history APIs and tests.
