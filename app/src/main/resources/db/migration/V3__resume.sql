-- Summary: resume schema (enum, resumes, resume_analysis, indexes, triggers).

do $$
begin
    if not exists (select 1 from pg_type where typname = 'resume_status') then
        create type resume_status as enum ('PENDING', 'ANALYZING', 'COMPLETED', 'FAILED');
    end if;
end
$$;

create table if not exists resumes (
    id uuid primary key default gen_random_uuid(),
    account_id uuid not null references accounts on delete cascade,
    filename text not null,
    content text,
    file_hash text not null,
    size bigint not null,
    storage_engine text not null,
    storage_key text not null,
    status resume_status not null default 'PENDING',
    error text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_resumes_filename_not_blank check (length(trim(filename)) > 0),
    constraint ck_resumes_file_hash_not_blank check (length(trim(file_hash)) > 0),
    constraint ck_resumes_size_non_negative check (size >= 0),
    constraint ck_resumes_storage_engine_not_blank check (length(trim(storage_engine)) > 0),
    constraint ck_resumes_storage_key_not_blank check (length(trim(storage_key)) > 0),
    constraint ck_resumes_error_not_blank check (error is null or length(trim(error)) > 0)
);

create index if not exists idx_resumes_account_id
    on resumes (account_id);

create index if not exists idx_resumes_status
    on resumes (status);

create index if not exists idx_resumes_account_file_hash
    on resumes (account_id, file_hash);

drop trigger if exists trg_resumes_set_updated_at on resumes;
create trigger trg_resumes_set_updated_at
    before update on resumes
    for each row
execute function update_updated_at_column();

create table if not exists resume_analysis (
    id uuid primary key default gen_random_uuid(),
    resume_id uuid not null references resumes on delete cascade,
    score int not null,
    summary text not null,
    strengths_json jsonb not null default '[]'::jsonb,
    suggestions_json jsonb not null default '[]'::jsonb,
    created_at timestamptz not null default now(),
    constraint ck_resume_analysis_score_range check (score >= 0 and score <= 100),
    constraint ck_resume_analysis_summary_not_blank check (length(trim(summary)) > 0),
    constraint ck_resume_analysis_strengths_array check (jsonb_typeof(strengths_json) = 'array'),
    constraint ck_resume_analysis_suggestions_array check (jsonb_typeof(suggestions_json) = 'array')
);

create index if not exists idx_resume_analysis_resume_id_created_at
    on resume_analysis (resume_id, created_at desc);
