# Repository Guidelines

## Project Structure & Module Organization
- `app/src/main/kotlin/dev/haomin/resumer/app`: Spring Boot Kotlin source.
- `auth/`: authentication and registration modules (domain, repo, service, API).
- `module/resume/`: resume domain and repository layer.
- `infra/`: infrastructure adapters (`file`, `storage`, `mq`).
- `framework/`: shared framework config/helpers (Redis, advice, config).
- `app/src/main/resources/db/migration`: Flyway SQL migrations (`V1__...`, `V2__...`, `V3__...`).
- `app/src/test/kotlin`: unit/integration tests, MockMvc docs tests, Testcontainers config.
- `docs/`: architecture notes, database design, roadmap, examples.

## Build, Test, and Development Commands
- `set -a; source .env; set +a`: export local env vars.
- `./gradlew app:flywayMigrate`: apply DB migrations.
- `./gradlew app:jooqCodegen`: regenerate jOOQ classes from schema.
- `./gradlew app:compileKotlin`: fast compile check.
- `./gradlew app:test`: run all tests.
- `./gradlew app:test --tests <ClassName>`: run a single test class.
- `./gradlew app:bootRun`: run the application locally.

## Coding Style & Naming Conventions
- Kotlin style: 4-space indentation, immutable `data class` by default, clear nullability.
- Domain/repo style follows `docs/style/domain-repo-style.md`.
- Repository query objects are top-level under `repo/query` (for example, `ResumeInsertQuery`).
- jOOQ repo implementations live under `repo/impl` and use explicit `toDomain`/`toJooq` mapping helpers.
- Use `snake_case` for DB columns and migration constraints/index names.

## Testing Guidelines
- Frameworks: JUnit 5, Mockito-Kotlin, Spring Boot Test, MockMvc, Testcontainers.
- Naming: `*Test.kt`; test names should describe behavior (backtick style is preferred).
- Add unit tests for service/infra logic and integration tests for API + Redis/DB behavior.
- For API documentation flows, use existing MockMvc REST Docs tests as reference.

## Commit & Pull Request Guidelines
- Commit history uses Conventional Commit style, primarily `feat: ...` (also use `fix:`, `refactor:`, `test:`, `docs:`).
- Keep commits focused and atomic; include migration/codegen updates when schema changes.
- PRs should include:
- scope summary and rationale
- linked issue/task (if available)
- test evidence (commands run)
- API/doc updates when behavior changes

## Security & Configuration Tips
- Never hardcode secrets; use `.env` and environment placeholders in `application.yaml`.
- Keep storage/object URLs temporary (presigned/signed), not permanently public.
- For auth/storage changes, update `.env.example` and relevant docs together.
