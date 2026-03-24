-- Summary: add session task status columns for async advice jobs.

do $$
begin
    if not exists (select 1 from pg_type where typname = 'session_task_status') then
        create type session_task_status as enum ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED');
    end if;
end
$$;

alter table if exists application_sessions
    add column if not exists position_advice_status session_task_status not null default 'PENDING',
    add column if not exists position_advice_error text,
    add column if not exists resume_fit_status session_task_status not null default 'PENDING',
    add column if not exists resume_fit_error text;

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'ck_application_sessions_position_advice_error_not_blank'
    ) then
        alter table application_sessions
            add constraint ck_application_sessions_position_advice_error_not_blank
            check (position_advice_error is null or length(trim(position_advice_error)) > 0);
    end if;
end
$$;

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'ck_application_sessions_resume_fit_error_not_blank'
    ) then
        alter table application_sessions
            add constraint ck_application_sessions_resume_fit_error_not_blank
            check (resume_fit_error is null or length(trim(resume_fit_error)) > 0);
    end if;
end
$$;

create index if not exists idx_application_sessions_position_advice_status
    on application_sessions (position_advice_status);

create index if not exists idx_application_sessions_resume_fit_status
    on application_sessions (resume_fit_status);
