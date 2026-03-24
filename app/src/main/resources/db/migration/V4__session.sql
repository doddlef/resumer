-- Summary: phase 2 session schema (application_sessions, session_advice, session_cover_letters, indexes, triggers).

create table if not exists application_sessions (
    id uuid primary key default gen_random_uuid(),
    account_id uuid not null references accounts on delete cascade,
    resume_id uuid references resumes on delete set null,
    company text not null,
    position text not null,
    job_description text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_application_sessions_company_not_blank check (length(trim(company)) > 0),
    constraint ck_application_sessions_position_not_blank check (length(trim(position)) > 0),
    constraint ck_application_sessions_job_description_not_blank check (length(trim(job_description)) > 0)
);

create index if not exists idx_application_sessions_account_created_at
    on application_sessions (account_id, created_at desc);

create index if not exists idx_application_sessions_account_resume
    on application_sessions (account_id, resume_id);

drop trigger if exists trg_application_sessions_set_updated_at on application_sessions;
create trigger trg_application_sessions_set_updated_at
    before update on application_sessions
    for each row
execute function update_updated_at_column();

create table if not exists session_advice (
    id uuid primary key default gen_random_uuid(),
    session_id uuid not null references application_sessions on delete cascade,
    position_summary text not null,
    key_requirements_json jsonb not null default '[]'::jsonb,
    likely_interview_focus_json jsonb not null default '[]'::jsonb,
    red_flags_json jsonb not null default '[]'::jsonb,
    fit_score int not null,
    gaps_json jsonb not null default '[]'::jsonb,
    rewrite_suggestions_json jsonb not null default '[]'::jsonb,
    created_at timestamptz not null default now(),
    constraint ck_session_advice_position_summary_not_blank check (length(trim(position_summary)) > 0),
    constraint ck_session_advice_fit_score_range check (fit_score >= 0 and fit_score <= 100),
    constraint ck_session_advice_key_requirements_array check (jsonb_typeof(key_requirements_json) = 'array'),
    constraint ck_session_advice_likely_interview_focus_array check (jsonb_typeof(likely_interview_focus_json) = 'array'),
    constraint ck_session_advice_red_flags_array check (jsonb_typeof(red_flags_json) = 'array'),
    constraint ck_session_advice_gaps_array check (jsonb_typeof(gaps_json) = 'array'),
    constraint ck_session_advice_rewrite_suggestions_array check (jsonb_typeof(rewrite_suggestions_json) = 'array')
);

create index if not exists idx_session_advice_session_created_at
    on session_advice (session_id, created_at desc);

create table if not exists session_cover_letters (
    id uuid primary key default gen_random_uuid(),
    session_id uuid not null references application_sessions on delete cascade,
    version int not null,
    content text not null,
    created_at timestamptz not null default now(),
    constraint ck_session_cover_letters_version_positive check (version > 0),
    constraint ck_session_cover_letters_content_not_blank check (length(trim(content)) > 0)
);

create unique index if not exists ux_session_cover_letters_session_version
    on session_cover_letters (session_id, version);

create index if not exists idx_session_cover_letters_session_created_at
    on session_cover_letters (session_id, created_at desc);
