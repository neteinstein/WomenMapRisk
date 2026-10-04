-- Women Risk Map: core schema.
-- Spec: docs/spec/especificacao-funcional-v1.md (§6 rules, §8 data) + docs/spec/addendum-invites.md.
-- Access model: RLS is ENABLED on every table with NO policies for anon/authenticated, so the API cannot touch
-- tables directly. All reads/writes go through SECURITY DEFINER functions (see *_api.sql), which enforce the
-- business rules and never expose report authorship.

create extension if not exists pgcrypto with schema extensions;

-- Enums (wire values are lowercase; the Kotlin enums map case-insensitively)
create type public.report_type as enum ('verbal_harassment', 'followed', 'poorly_lit', 'deserted', 'robbery', 'assault', 'other');
create type public.occurred_when as enum ('now', 'today', 'this_week', 'earlier');
create type public.day_period as enum ('day', 'night');
create type public.report_status as enum ('pending', 'published', 'hidden', 'removed');
create type public.account_status as enum ('pending_invite', 'active', 'blocked');
create type public.user_role as enum ('user', 'moderator');
create type public.flag_reason as enum ('false_information', 'personal_data', 'offensive', 'spam', 'other');
create type public.moderation_action as enum ('approve', 'remove', 'block_user', 'revoke_invites');

-- Spec §8 Utilizadora. Email lives in auth.users (single source of truth).
create table public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    pseudonym text check (char_length(pseudonym) <= 40),
    country text not null default 'PT' check (char_length(country) = 2),
    status public.account_status not null default 'pending_invite',
    role public.user_role not null default 'user',
    declared_woman boolean not null default false,
    terms_accepted_at timestamptz,
    invited_by uuid references public.profiles (id) on delete set null,
    created_at timestamptz not null default now()
);

-- Spec §8 Reporte. lat/lng are ALWAYS the zone centre (public.snap_to_zone), never the exact point.
-- user_id is nulled when the author deletes her account (spec §7: reports stay, unlinked).
create table public.reports (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references public.profiles (id) on delete set null,
    zone_id text not null,
    lat double precision not null,
    lng double precision not null,
    type public.report_type not null,
    occurred_when public.occurred_when not null,
    day_period public.day_period not null,
    description text check (char_length(description) <= 300),
    is_establishment boolean not null default false,
    status public.report_status not null default 'published',
    confirmations integer not null default 0,
    flags integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index reports_zone_idx on public.reports (zone_id);
create index reports_geo_idx on public.reports (lat, lng) where status = 'published';
create index reports_user_created_idx on public.reports (user_id, created_at desc);
create index reports_status_idx on public.reports (status);

-- Spec §8 Confirmação: one per user per report (spec §5).
create table public.confirmations (
    report_id uuid not null references public.reports (id) on delete cascade,
    user_id uuid not null references public.profiles (id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (report_id, user_id)
);

-- Spec §8 Denúncia.
create table public.flags (
    report_id uuid not null references public.reports (id) on delete cascade,
    user_id uuid not null references public.profiles (id) on delete cascade,
    reason public.flag_reason not null,
    resolved boolean not null default false,
    created_at timestamptz not null default now(),
    primary key (report_id, user_id)
);

-- Spec §8 Zona guardada.
create table public.saved_zones (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles (id) on delete cascade,
    zone_id text not null,
    lat double precision not null,
    lng double precision not null,
    name text not null check (char_length(name) between 1 and 120),
    created_at timestamptz not null default now(),
    unique (user_id, zone_id)
);

-- Invite addendum.
create table public.invites (
    id uuid primary key default gen_random_uuid(),
    code text not null unique check (code ~ '^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{8}$'),
    inviter_id uuid references public.profiles (id) on delete cascade,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null default now() + interval '30 days',
    redeemed_by uuid unique references public.profiles (id) on delete set null,
    redeemed_at timestamptz,
    revoked_at timestamptz
);
create index invites_inviter_idx on public.invites (inviter_id);

-- Invite addendum: only (user, day). No times, no locations.
create table public.usage_days (
    user_id uuid not null references public.profiles (id) on delete cascade,
    day date not null,
    primary key (user_id, day)
);

-- Spec §4 Ecrã 10: actions with a recorded reason.
create table public.moderation_actions (
    id uuid primary key default gen_random_uuid(),
    moderator_id uuid references public.profiles (id) on delete set null,
    report_id uuid references public.reports (id) on delete set null,
    target_user_id uuid references public.profiles (id) on delete set null,
    action public.moderation_action not null,
    reason text not null check (char_length(reason) between 1 and 500),
    created_at timestamptz not null default now()
);

-- Deny-by-default: RLS on, no policies. Functions (SECURITY DEFINER, owned by postgres) bypass RLS.
alter table public.profiles enable row level security;
alter table public.reports enable row level security;
alter table public.confirmations enable row level security;
alter table public.flags enable row level security;
alter table public.saved_zones enable row level security;
alter table public.invites enable row level security;
alter table public.usage_days enable row level security;
alter table public.moderation_actions enable row level security;

revoke all on all tables in schema public from anon, authenticated;
