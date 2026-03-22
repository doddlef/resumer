# Phase 1 Summary (Auth + Registration Foundation)

Updated: March 22, 2026

This document is the handoff and onboarding guide for Phase 1.
If you are new to the project, follow the sections in this order:
1. `Getting Started`
2. `How It Works`
3. `Where to Change`
4. `Phase 1 Remaining Work`

## Scope and Outcome
Phase 1 objective: establish authentication and registration as the project foundation.

Implemented outcomes:
- Auth schema and migrations are in place.
- Auth/register backend flows are implemented end-to-end.
- Token architecture is in place (`TokenService` entry + delegated services).
- Auth/register API endpoints are implemented.
- Unit + integration tests are implemented.
- REST Docs snippets are generated from MockMvc integration tests.
- CI runs migration + jOOQ codegen + tests.

## Architecture Snapshot

### Service layer
- `AuthService`: email/password authentication via Spring Security `AuthenticationManager`.
- `AccessService`: JWT access token issue and parse.
- `RefreshService`: refresh session create/rotate/revoke with replay detection.
- `TokenService`: single entry point for login/refresh/access/logout.
- `RegisterService`: registration flow (`start -> verify -> confirm -> resend`), backed by Redis.

### Controller layer
- `POST /api/auth`: login.
- `POST /api/auth/refresh`: refresh with cookie token.
- `POST /api/auth/logout`: revoke + clear cookie.
- `POST /api/register/start`
- `POST /api/register/verify`
- `POST /api/register`: confirmation endpoint.
- `POST /api/register/resend`

`POST /api/register` returns:
- `token_pair` payload (`access_token` + `refresh_token`)
- refresh cookie via `Set-Cookie`

### Persistence and state
- PostgreSQL stores durable auth data (`accounts`, `refresh_sessions`).
- Redis stores temporary registration attempts and resend cooldown state.

### Database and migrations
- `V1__init.sql`: extensions + common trigger function.
- `V2__auth.sql`: auth enums, tables, indexes, update triggers.
- Core tables:
  - `accounts`
  - `refresh_sessions`

### Security behavior
- Passwords hashed with BCrypt.
- Refresh secret is hashed before persistence.
- Refresh rotation supports a short grace window for the previous token.
- Reuse detection revokes session (`refresh_token_reuse_detected`).
- Refresh cookie construction is centralized in `RefreshCookieManager`.

## Getting Started (Local)

### 1) Prerequisites
- JDK 21
- Docker (if running local Postgres/Redis with containers) or manually running Postgres/Redis
- Gradle wrapper (`./gradlew`)

### 2) Prepare environment
Copy `.env.example` to `.env`, then set values:
- DB:
  - `POSTGRES_URL`
  - `POSTGRES_DB`
  - `POSTGRES_USER`
  - `POSTGRES_PASSWORD`
- Auth:
  - `AUTH_ACCESS_SECRET`
  - `AUTH_REFRESH_SECRET`
  - `REGISTER_CODE_HASH_SECRET`

### 3) Build schema and generate jOOQ code
Run from project root:

```bash
set -a
source .env
set +a

./gradlew app:flywayMigrate
./gradlew app:jooqCodegen
```

### 4) Run tests
```bash
./gradlew app:test
```

### 5) Run application
```bash
set -a
source .env
set +a
./gradlew app:bootRun
```

## How It Works

### Register flow
1. `POST /api/register/start`: create registration attempt and send verification code.
2. `POST /api/register/verify`: validate code and mark attempt verified.
3. `POST /api/register`: create account from verified attempt and issue token pair + cookie.
4. `POST /api/register/resend`: resend/rotate code (bounded by attempt policy).

### Auth flow
1. `POST /api/auth`: login with email/password.
2. `POST /api/auth/refresh`: rotate refresh session and issue new token pair.
3. `POST /api/auth/logout`: revoke refresh session and clear cookie.

## Testing and API Docs

### Unit tests
- `AccessServiceImplTest`
- `RefreshServiceImplTest`
- `TokenServiceImplTest`
- `RegisterServiceImplTest`

### Integration tests
- `RedisClientIntegrationTest`
- `RegisterControllerDocumentationTest`
- `AuthControllerDocumentationTest`

### REST Docs snippets
Generated under `app/build/generated-snippets`:
- `register-start`
- `register-verify`
- `register-confirm`
- `register-resend`
- `auth-login`
- `auth-refresh`
- `auth-logout`

Generate docs artifact:

```bash
./gradlew app:asciidoctor
```

## CI Behavior
GitHub Actions workflow: `.github/workflows/ci.yml`

Pipeline steps:
1. Start PostgreSQL service.
2. Export environment variables.
3. Run `./gradlew app:flywayMigrate`.
4. Run `./gradlew app:jooqCodegen`.
5. Run `./gradlew app:test`.

Required CI configuration:
- `secrets.POSTGRES_PASSWORD`
- `secrets.AUTH_ACCESS_SECRET`
- `secrets.AUTH_REFRESH_SECRET`
- `secrets.REGISTER_CODE_HASH_SECRET`
- Optional: `vars.POSTGRES_DB`, `vars.POSTGRES_USER`

## Where to Change
- Auth API request/response: `app/src/main/kotlin/dev/haomin/resumer/app/auth/api` and `.../auth/api/dto`
- Token/session logic: `app/src/main/kotlin/dev/haomin/resumer/app/auth/service` and `.../auth/service/impl`
- DB schema: `app/src/main/resources/db/migration`
- Repository query style: `app/src/main/kotlin/dev/haomin/resumer/app/auth/repo` and `.../auth/repo/query`
- Endpoint tests/docs: `app/src/test/kotlin/dev/haomin/resumer/app/auth/api/*DocumentationTest.kt`

## Phase 1 Remaining Work
- Resume upload + object storage integration.
- Resume analysis/scoring pipeline.
- Resume base/history APIs + tests.
