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
