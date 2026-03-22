-- Summary: authentication schema (enums, accounts, refresh_sessions, indexes, triggers).

create extension if not exists pgcrypto;

do $$
begin
    if not exists (select 1 from pg_type where typname = 'account_status') then
        create type account_status as enum ('ACTIVE', 'ARCHIVED', 'LOCKED');
    end if;
end
$$;

do $$
begin
    if not exists (select 1 from pg_type where typname = 'account_role') then
        create type account_role as enum ('USER', 'ADMIN');
    end if;
end
$$;

create table if not exists accounts (
    id uuid primary key default gen_random_uuid(),
    email citext not null,
    password text not null,
    name text not null,
    status account_status not null default 'ACTIVE',
    role account_role not null default 'USER',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_accounts_password_not_blank check (length(trim(password)) > 0),
    constraint ck_accounts_name_not_blank check (length(trim(name)) > 0)
);

create unique index if not exists ux_accounts_email on accounts (email);

drop trigger if exists trg_accounts_set_updated_at on accounts;
create trigger trg_accounts_set_updated_at
    before update on accounts
    for each row
execute function update_updated_at_column();

create table if not exists refresh_sessions (
    id uuid primary key default gen_random_uuid(),
    account_id uuid not null references accounts on delete cascade,
    secret text not null,
    prev_secret text,
    last_used_at timestamptz,
    revoked_at timestamptz,
    revoke_reason text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_refresh_sessions_secret_not_blank check (length(trim(secret)) > 0),
    constraint ck_refresh_sessions_prev_secret_not_blank check (
        prev_secret is null or length(trim(prev_secret)) > 0
    ),
    constraint ck_refresh_sessions_revoke_consistency check (
        (revoked_at is null and revoke_reason is null)
        or (revoked_at is not null and revoke_reason is not null)
    )
);

create index if not exists idx_refresh_sessions_account_id
    on refresh_sessions (account_id);

create index if not exists idx_refresh_sessions_revoked_at
    on refresh_sessions (revoked_at)
    where revoked_at is not null;

drop trigger if exists trg_refresh_sessions_set_updated_at on refresh_sessions;
create trigger trg_refresh_sessions_set_updated_at
    before update on refresh_sessions
    for each row
execute function update_updated_at_column();
