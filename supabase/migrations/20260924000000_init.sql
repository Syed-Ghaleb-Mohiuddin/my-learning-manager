-- Written to be re-runnable: deploy.ps1 applies every migration on each run.

create table if not exists public.sessions (
    id         uuid primary key default gen_random_uuid(),
    date       date not null,          -- Dubai-local date, computed by the Edge Function, never UTC
    subject    text not null,
    topics     text[] not null default '{}',
    summary    text,
    light      boolean not null default false,
    created_at timestamptz not null default now()
);

create index if not exists sessions_date_idx on public.sessions (date);

create table if not exists public.mastery (
    subject    text not null,
    topic      text not null,
    level      text not null check (level in ('seen', 'held', 'solid', 'dissolved')),
    updated_at timestamptz not null default now(),
    primary key (subject, topic)
);

alter table public.sessions enable row level security;
alter table public.mastery  enable row level security;

-- Anon (the phone) may only read. Writes go through the Edge Function with the service role.
drop policy if exists "anon read sessions" on public.sessions;
create policy "anon read sessions" on public.sessions for select to anon using (true);

drop policy if exists "anon read mastery" on public.mastery;
create policy "anon read mastery" on public.mastery for select to anon using (true);

-- Belt and braces: even if a permissive policy is added later, anon has no write grants.
revoke insert, update, delete, truncate on public.sessions from anon, authenticated;
revoke insert, update, delete, truncate on public.mastery  from anon, authenticated;
grant select on public.sessions to anon;
grant select on public.mastery  to anon;
