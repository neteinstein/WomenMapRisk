-- Business-rule helpers and triggers. The source of truth for every rule mirrored in core:domain.

-- Mirror of LocationAnonymizer (core/domain/.../rules/LocationAnonymizer.kt). ~100 m cells; returns the cell
-- centre. MUST stay numerically identical (shared vector: z45721_-7205 for 41.1496,-8.6110).
create or replace function public.snap_to_zone(p_lat double precision, p_lng double precision,
                                               out zone_id text, out lat double precision, out lng double precision)
language plpgsql immutable strict
set search_path = ''
as $$
declare
    lat_step constant double precision := 0.0009;
    lat_idx bigint;
    lng_idx bigint;
    lng_step double precision;
begin
    lat_idx := floor(p_lat / lat_step)::bigint;
    lng_step := lat_step / greatest(cos(radians((lat_idx + 0.5) * lat_step)), 0.01);
    lng_idx := floor(p_lng / lng_step)::bigint;
    zone_id := 'z' || lat_idx || '_' || lng_idx;
    lat := (lat_idx + 0.5) * lat_step;
    lng := (lng_idx + 0.5) * lng_step;
end;
$$;

-- "Day" for rate limits and usage days follows the pilot city's timezone (ReportPolicy.policyTimeZone).
create or replace function public.policy_date(p_ts timestamptz default now())
returns date language sql stable set search_path = '' as $$
    select (p_ts at time zone 'Europe/Lisbon')::date
$$;

-- Raises a stable error code (mapped in core/data/.../remote/ErrorMapping.kt) unless the caller may contribute.
create or replace function public.require_enabled()
returns uuid language plpgsql stable security definer
set search_path = ''
as $$
declare
    uid uuid := auth.uid();
    p public.profiles;
    confirmed timestamptz;
begin
    if uid is null then raise exception 'not_allowed'; end if;
    select * into p from public.profiles where id = uid;
    if not found then raise exception 'not_allowed'; end if;
    if p.status = 'blocked' then raise exception 'account_blocked'; end if;
    if p.status = 'pending_invite' then raise exception 'invalid_invite'; end if;
    select email_confirmed_at into confirmed from auth.users where id = uid;
    if confirmed is null then raise exception 'email_not_confirmed'; end if;
    return uid;
end;
$$;

create or replace function public.require_moderator()
returns uuid language plpgsql stable security definer
set search_path = ''
as $$
declare uid uuid := public.require_enabled();
begin
    if not exists (select 1 from public.profiles where id = uid and role = 'moderator') then
        raise exception 'not_allowed';
    end if;
    return uid;
end;
$$;

-- Notify clients that published reports changed. Payload has no personal data. Never fails the transaction.
create or replace function public.broadcast_reports_changed(p_zone_id text)
returns void language plpgsql security definer
set search_path = ''
as $$
begin
    perform realtime.send(jsonb_build_object('zone_id', p_zone_id), 'changed', 'reports-feed', false);
exception when others then
    null; -- realtime unavailable (e.g. plain Postgres in tests): clients fall back to polling
end;
$$;

-- Counter maintenance + auto-hide at 3 flags (spec §6: "Um reporte com 3 ou mais denúncias fica oculto até revisão").
create or replace function public.trg_confirmations_count()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
    if tg_op = 'INSERT' then
        update public.reports set confirmations = confirmations + 1 where id = new.report_id;
    elsif tg_op = 'DELETE' then
        update public.reports set confirmations = greatest(confirmations - 1, 0) where id = old.report_id;
    end if;
    return null;
end;
$$;
create trigger confirmations_count after insert or delete on public.confirmations
    for each row execute function public.trg_confirmations_count();

create or replace function public.trg_flags_count()
returns trigger language plpgsql security definer set search_path = '' as $$
declare open_flags integer;
begin
    select count(*) into open_flags from public.flags
     where report_id = coalesce(new.report_id, old.report_id) and not resolved;
    update public.reports
       set flags = open_flags,
           status = case when open_flags >= 3 and status = 'published' then 'hidden'::public.report_status else status end
     where id = coalesce(new.report_id, old.report_id);
    return null;
end;
$$;
create trigger flags_count after insert or update or delete on public.flags
    for each row execute function public.trg_flags_count();

create or replace function public.trg_touch_updated_at()
returns trigger language plpgsql set search_path = '' as $$
begin
    new.updated_at := now();
    return new;
end;
$$;
create trigger reports_updated_at before update on public.reports
    for each row execute function public.trg_touch_updated_at();

-- Defence in depth: whatever path inserts/updates a report, the stored point is the zone centre.
create or replace function public.trg_reports_snap()
returns trigger language plpgsql set search_path = '' as $$
declare z record;
begin
    select * into z from public.snap_to_zone(new.lat, new.lng);
    new.zone_id := z.zone_id;
    new.lat := z.lat;
    new.lng := z.lng;
    return new;
end;
$$;
create trigger reports_snap before insert or update of lat, lng on public.reports
    for each row execute function public.trg_reports_snap();

-- Invite eligibility (mirror of InviteEligibility.kt): longest consecutive-day streak and distinct days.
create or replace function public.invite_eligibility(p_user uuid,
    out longest_streak integer, out distinct_days integer, out active_invites integer, out unlocked boolean)
language plpgsql stable security definer set search_path = ''
as $$
begin
    select coalesce(max(run), 0) into longest_streak from (
        select count(*) as run from (
            select day - (row_number() over (order by day))::integer as grp
              from public.usage_days where user_id = p_user and day <= public.policy_date()
        ) islands group by grp
    ) runs;
    select count(*) into distinct_days from public.usage_days where user_id = p_user and day <= public.policy_date();
    -- Active = not revoked and (redeemed, or still valid). Expired unused invites give the slot back.
    select count(*) into active_invites from public.invites
     where inviter_id = p_user and revoked_at is null and (redeemed_by is not null or expires_at > now());
    unlocked := longest_streak >= 3 or distinct_days >= 5;
end;
$$;

-- New auth user -> profile. If sign-up metadata carries a valid invite code, redeem it immediately.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = ''
as $$
declare
    meta jsonb := coalesce(new.raw_user_meta_data, '{}'::jsonb);
    v_code text := upper(regexp_replace(coalesce(meta ->> 'invite_code', ''), '[\s.-]', '', 'g'));
    v_invite public.invites;
    v_country text := upper(coalesce(nullif(meta ->> 'country', ''), 'PT'));
    v_declared boolean := coalesce((meta ->> 'declared_woman')::boolean, false);
begin
    insert into public.profiles (id, country, declared_woman, terms_accepted_at)
    values (new.id, case when char_length(v_country) = 2 then v_country else 'XX' end, v_declared,
            case when v_declared then now() end);

    if v_code <> '' then
        select * into v_invite from public.invites
         where code = v_code and redeemed_by is null and revoked_at is null and expires_at > now()
         for update;
        if found then
            update public.invites set redeemed_by = new.id, redeemed_at = now() where id = v_invite.id;
            update public.profiles set status = 'active', invited_by = v_invite.inviter_id where id = new.id;
        end if;
    end if;
    return new;
end;
$$;
create trigger on_auth_user_created after insert on auth.users
    for each row execute function public.handle_new_user();
