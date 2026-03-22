create extension if not exists citext;

-- updated_at trigger function

create or replace function update_updated_at_column()
    returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;