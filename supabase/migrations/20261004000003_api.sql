-- Public API (PostgREST RPC). Every function is SECURITY DEFINER with an empty search_path and enforces
-- its own authorisation. Error codes raised here are mapped in core/data/.../remote/ErrorMapping.kt.
-- Wire format must match core/data/.../remote/dto/Dtos.kt.

-- Report row as seen by the caller. NEVER includes user_id: only "is it mine".
create or replace function public.report_json(r public.reports)
returns jsonb language sql stable security definer set search_path = ''
as $$
    select jsonb_build_object(
        'id', r.id,
        'zone_id', r.zone_id,
        'lat', r.lat,
        'lng', r.lng,
        'type', r.type,
        'occurred_when', r.occurred_when,
        'day_period', r.day_period,
        'description', r.description,
        'confirmations', r.confirmations,
        'status', r.status,
        'created_at', r.created_at,
        'is_establishment', r.is_establishment,
        'is_mine', auth.uid() is not null and r.user_id = auth.uid(),
        'confirmed_by_me', exists (select 1 from public.confirmations c where c.report_id = r.id and c.user_id = auth.uid())
    )
$$;

-- ---------- Map (visitors included) ----------

-- Published reports from the last 12 months inside a bounding box (spec §6: only last 12 months count).
create or replace function public.reports_in_bbox(p_south double precision, p_west double precision,
                                                  p_north double precision, p_east double precision)
returns jsonb language sql stable security definer set search_path = ''
as $$
    select coalesce(jsonb_agg(public.report_json(r) order by r.created_at desc), '[]'::jsonb)
      from (
        select * from public.reports
         where status = 'published'
           and created_at > now() - interval '12 months'
           and lat between p_south and p_north
           and lng between p_west and p_east
         order by created_at desc
         limit 2000
      ) r
$$;

-- Spec §4 Ecrã 4: zone reports, newest first.
create or replace function public.reports_in_zone(p_zone_id text)
returns jsonb language sql stable security definer set search_path = ''
as $$
    select coalesce(jsonb_agg(public.report_json(r) order by r.created_at desc), '[]'::jsonb)
      from public.reports r
     where r.zone_id = p_zone_id and r.status = 'published' and r.created_at > now() - interval '12 months'
$$;

-- ---------- Reports (enabled users) ----------

create or replace function public.my_reports()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
    if uid is null then raise exception 'not_allowed'; end if;
    return (select coalesce(jsonb_agg(public.report_json(r) order by r.created_at desc), '[]'::jsonb)
              from public.reports r where r.user_id = uid);
end;
$$;

-- Spec §4 Ecrã 5 + §6 Prevenção de abusos.
create or replace function public.submit_report(
    p_lat double precision, p_lng double precision, p_type text, p_occurred_when text, p_day_period text,
    p_description text default null, p_is_establishment boolean default false)
returns jsonb language plpgsql security definer set search_path = ''
as $$
declare
    uid uuid := public.require_enabled();
    z record;
    v_report public.reports;
    today date := public.policy_date();
begin
    -- Serialise this user's submissions so concurrent requests cannot bypass the limits.
    perform pg_advisory_xact_lock(hashtextextended(uid::text, 42));
    select * into z from public.snap_to_zone(p_lat, p_lng);

    if (select count(*) from public.reports where user_id = uid and public.policy_date(created_at) = today) >= 5 then
        raise exception 'daily_report_limit';
    end if;
    if exists (select 1 from public.reports
                where user_id = uid and zone_id = z.zone_id and public.policy_date(created_at) = today) then
        raise exception 'duplicate_report';
    end if;

    insert into public.reports (user_id, zone_id, lat, lng, type, occurred_when, day_period, description, is_establishment, status)
    values (uid, z.zone_id, z.lat, z.lng, p_type::public.report_type, p_occurred_when::public.occurred_when,
            p_day_period::public.day_period, nullif(btrim(p_description), ''), coalesce(p_is_establishment, false),
            case when coalesce(p_is_establishment, false) then 'pending' else 'published' end::public.report_status)
    returning * into v_report;

    if v_report.status = 'published' then perform public.broadcast_reports_changed(v_report.zone_id); end if;
    return public.report_json(v_report);
end;
$$;

-- Spec §4 Ecrã 5: editable within 24 hours (own reports only).
create or replace function public.update_report(
    p_id uuid, p_lat double precision, p_lng double precision, p_type text, p_occurred_when text, p_day_period text,
    p_description text default null, p_is_establishment boolean default false)
returns jsonb language plpgsql security definer set search_path = ''
as $$
declare
    uid uuid := public.require_enabled();
    v_report public.reports;
    old_zone text;
begin
    select * into v_report from public.reports where id = p_id and user_id = uid for update;
    if not found then raise exception 'not_allowed'; end if;
    if v_report.created_at < now() - interval '24 hours' then raise exception 'edit_window_expired'; end if;
    if v_report.status = 'removed' then raise exception 'not_allowed'; end if;
    old_zone := v_report.zone_id;

    update public.reports
       set lat = p_lat, lng = p_lng,
           type = p_type::public.report_type,
           occurred_when = p_occurred_when::public.occurred_when,
           day_period = p_day_period::public.day_period,
           description = nullif(btrim(p_description), ''),
           is_establishment = coalesce(p_is_establishment, false),
           status = case
               when coalesce(p_is_establishment, false) then 'pending'::public.report_status
               when status = 'pending' then 'published'::public.report_status
               else status end
     where id = p_id
     returning * into v_report;

    perform public.broadcast_reports_changed(old_zone);
    if v_report.zone_id <> old_zone then perform public.broadcast_reports_changed(v_report.zone_id); end if;
    return public.report_json(v_report);
end;
$$;

create or replace function public.delete_report(p_id uuid)
returns void language plpgsql security definer set search_path = ''
as $$
declare
    uid uuid := public.require_enabled();
    v_report public.reports;
begin
    select * into v_report from public.reports where id = p_id and user_id = uid;
    if not found then raise exception 'not_allowed'; end if;
    if v_report.created_at < now() - interval '24 hours' then raise exception 'edit_window_expired'; end if;
    delete from public.reports where id = p_id;
    perform public.broadcast_reports_changed(v_report.zone_id);
end;
$$;

-- Spec §5: "Também senti isto": once per user, never your own.
create or replace function public.confirm_report(p_id uuid)
returns void language plpgsql security definer set search_path = ''
as $$
declare
    uid uuid := public.require_enabled();
    v_report public.reports;
begin
    select * into v_report from public.reports where id = p_id and status = 'published';
    if not found then raise exception 'not_allowed'; end if;
    if v_report.user_id = uid then raise exception 'cannot_confirm_own'; end if;
    insert into public.confirmations (report_id, user_id) values (p_id, uid)
    on conflict do nothing;
    if not found then raise exception 'already_confirmed'; end if;
    perform public.broadcast_reports_changed(v_report.zone_id);
end;
$$;

-- Spec §4 Ecrã 4 "Denunciar reporte". 3+ open flags hide the report (trigger).
create or replace function public.flag_report(p_id uuid, p_reason text)
returns void language plpgsql security definer set search_path = ''
as $$
declare
    uid uuid := public.require_enabled();
    v_report public.reports;
begin
    select * into v_report from public.reports where id = p_id and status in ('published', 'hidden');
    if not found then raise exception 'not_allowed'; end if;
    if v_report.user_id = uid then raise exception 'not_allowed'; end if;
    insert into public.flags (report_id, user_id, reason) values (p_id, uid, p_reason::public.flag_reason)
    on conflict (report_id, user_id) do nothing;
    perform public.broadcast_reports_changed(v_report.zone_id);
end;
$$;

-- ---------- Profile ----------

create or replace function public.my_profile()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare
    uid uuid := auth.uid();
    result jsonb;
begin
    if uid is null then raise exception 'not_allowed'; end if;
    select jsonb_build_object(
        'id', p.id, 'email', u.email, 'pseudonym', p.pseudonym, 'country', p.country,
        'created_at', p.created_at, 'status', p.status, 'role', p.role,
        'email_confirmed', u.email_confirmed_at is not null)
      into result
      from public.profiles p join auth.users u on u.id = p.id
     where p.id = uid;
    if result is null then raise exception 'not_allowed'; end if;
    return result;
end;
$$;

create or replace function public.update_my_profile(p_pseudonym text, p_country text)
returns void language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
    if uid is null then raise exception 'not_allowed'; end if;
    update public.profiles
       set pseudonym = nullif(btrim(p_pseudonym), ''),
           country = case when char_length(p_country) = 2 then upper(p_country) else country end
     where id = uid;
end;
$$;

-- ---------- Saved zones (spec §4 Ecrã 7) ----------

create or replace function public.my_saved_zones()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
    if uid is null then raise exception 'not_allowed'; end if;
    return (select coalesce(jsonb_agg(jsonb_build_object('id', s.id, 'zone_id', s.zone_id, 'lat', s.lat, 'lng', s.lng, 'name', s.name)
                                      order by s.created_at desc), '[]'::jsonb)
              from public.saved_zones s where s.user_id = uid);
end;
$$;

create or replace function public.save_zone(p_zone_id text, p_lat double precision, p_lng double precision, p_name text)
returns jsonb language plpgsql security definer set search_path = ''
as $$
declare
    uid uuid := public.require_enabled();
    z record;
    v public.saved_zones;
begin
    select * into z from public.snap_to_zone(p_lat, p_lng);
    insert into public.saved_zones (user_id, zone_id, lat, lng, name)
    values (uid, z.zone_id, z.lat, z.lng, left(coalesce(nullif(btrim(p_name), ''), z.zone_id), 120))
    on conflict (user_id, zone_id) do update set name = excluded.name
    returning * into v;
    return jsonb_build_object('id', v.id, 'zone_id', v.zone_id, 'lat', v.lat, 'lng', v.lng, 'name', v.name);
end;
$$;

create or replace function public.delete_saved_zone(p_id uuid)
returns void language plpgsql security definer set search_path = ''
as $$
begin
    if auth.uid() is null then raise exception 'not_allowed'; end if;
    delete from public.saved_zones where id = p_id and user_id = auth.uid();
end;
$$;

-- ---------- Invites (docs/spec/addendum-invites.md) ----------

create or replace function public.touch_usage()
returns void language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
    if uid is null then return; end if;
    if exists (select 1 from public.profiles where id = uid and status = 'active') then
        insert into public.usage_days (user_id, day) values (uid, public.policy_date()) on conflict do nothing;
    end if;
end;
$$;

create or replace function public.invite_state(i public.invites)
returns text language sql stable set search_path = '' as $$
    select case
        when i.revoked_at is not null then 'revoked'
        when i.redeemed_by is not null then 'used'
        when i.expires_at <= now() then 'expired'
        else 'pending' end
$$;

create or replace function public.invite_json(i public.invites)
returns jsonb language sql stable set search_path = '' as $$
    select jsonb_build_object('id', i.id, 'code', i.code, 'created_at', i.created_at, 'expires_at', i.expires_at,
                              'state', public.invite_state(i))
$$;

create or replace function public.my_invite_data()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
    if uid is null then raise exception 'not_allowed'; end if;
    return jsonb_build_object(
        'usage_days', (select coalesce(jsonb_agg(to_char(d.day, 'YYYY-MM-DD') order by d.day), '[]'::jsonb)
                         from public.usage_days d where d.user_id = uid),
        'invites', (select coalesce(jsonb_agg(public.invite_json(i) order by i.created_at desc), '[]'::jsonb)
                      from public.invites i where i.inviter_id = uid));
end;
$$;

create or replace function public.create_invite()
returns jsonb language plpgsql security definer set search_path = ''
as $$
declare
    uid uuid := public.require_enabled();
    alphabet constant text := 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
    e record;
    v_code text;
    v public.invites;
    bytes bytea;
begin
    perform pg_advisory_xact_lock(hashtextextended(uid::text, 7));
    if not exists (select 1 from public.profiles where id = uid and role = 'moderator') then
        select * into e from public.invite_eligibility(uid);
        if not e.unlocked then raise exception 'invites_locked'; end if;
        if e.active_invites >= 5 then raise exception 'invite_limit_reached'; end if;
    end if;
    loop
        bytes := extensions.gen_random_bytes(8);
        v_code := '';
        for i in 0..7 loop
            v_code := v_code || substr(alphabet, 1 + (get_byte(bytes, i) % 31), 1);
        end loop;
        begin
            insert into public.invites (code, inviter_id) values (v_code, uid) returning * into v;
            exit;
        exception when unique_violation then
            -- extremely unlikely collision: try another code
        end;
    end loop;
    return public.invite_json(v);
end;
$$;

create or replace function public.revoke_invite(p_id uuid)
returns void language plpgsql security definer set search_path = ''
as $$
begin
    if auth.uid() is null then raise exception 'not_allowed'; end if;
    update public.invites set revoked_at = now()
     where id = p_id and inviter_id = auth.uid() and redeemed_by is null and revoked_at is null;
    if not found then raise exception 'not_allowed'; end if;
end;
$$;

-- Callable by visitors (anon) before sign-up. Reveals only valid/invalid.
create or replace function public.validate_invite(p_code text)
returns boolean language sql stable security definer set search_path = ''
as $$
    select exists (select 1 from public.invites
                    where code = upper(regexp_replace(p_code, '[\s.-]', '', 'g'))
                      and redeemed_by is null and revoked_at is null and expires_at > now())
$$;

-- For accounts created without a code (e.g. Google sign-in): activates the account.
create or replace function public.redeem_invite(p_code text)
returns void language plpgsql security definer set search_path = ''
as $$
declare
    uid uuid := auth.uid();
    v public.invites;
    p public.profiles;
begin
    if uid is null then raise exception 'not_allowed'; end if;
    select * into p from public.profiles where id = uid for update;
    if not found then raise exception 'not_allowed'; end if;
    if p.status = 'blocked' then raise exception 'account_blocked'; end if;
    if p.status = 'active' then return; end if;
    select * into v from public.invites
     where code = upper(regexp_replace(p_code, '[\s.-]', '', 'g'))
       and redeemed_by is null and revoked_at is null and expires_at > now()
     for update;
    if not found then raise exception 'invalid_invite'; end if;
    update public.invites set redeemed_by = uid, redeemed_at = now() where id = v.id;
    update public.profiles
       set status = 'active', invited_by = v.inviter_id, declared_woman = true, terms_accepted_at = coalesce(terms_accepted_at, now())
     where id = uid;
end;
$$;

-- ---------- Privacy (spec §4 Ecrã 9) ----------

create or replace function public.export_my_data()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
    if uid is null then raise exception 'not_allowed'; end if;
    return jsonb_build_object(
        'exported_at', now(),
        'profile', public.my_profile(),
        'reports', public.my_reports(),
        'confirmations', (select coalesce(jsonb_agg(jsonb_build_object('report_id', report_id, 'created_at', created_at)), '[]'::jsonb)
                            from public.confirmations where user_id = uid),
        'flags', (select coalesce(jsonb_agg(jsonb_build_object('report_id', report_id, 'reason', reason, 'created_at', created_at)), '[]'::jsonb)
                    from public.flags where user_id = uid),
        'saved_zones', public.my_saved_zones(),
        'invites', (public.my_invite_data() -> 'invites'),
        'usage_days', (public.my_invite_data() -> 'usage_days'));
end;
$$;

-- Spec §7/§12: deleting the account removes all personal data; reports stay on the map, unlinked
-- (reports.user_id ON DELETE SET NULL). Everything else cascades from auth.users -> profiles.
create or replace function public.delete_my_account()
returns void language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
    if uid is null then raise exception 'not_allowed'; end if;
    delete from auth.users where id = uid;
end;
$$;

-- ---------- Moderation (spec §4 Ecrã 10) ----------

create or replace function public.moderation_counters()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
begin
    perform public.require_moderator();
    return jsonb_build_object(
        'new_reports', (select count(*) from public.reports where created_at > now() - interval '24 hours'),
        'pending_flags', (select count(distinct report_id) from public.flags where not resolved),
        'pending_establishments', (select count(*) from public.reports where status = 'pending'));
end;
$$;

create or replace function public.moderation_queue()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
begin
    perform public.require_moderator();
    return (
        select coalesce(jsonb_agg(item order by (item ->> 'created_at') desc), '[]'::jsonb) from (
            select public.report_json(r) || jsonb_build_object(
                       'kind', case when r.status = 'pending' then 'establishment' else 'flagged' end,
                       'open_flags', (select count(*) from public.flags f where f.report_id = r.id and not f.resolved),
                       'flag_reasons', (select coalesce(jsonb_agg(distinct f.reason), '[]'::jsonb)
                                          from public.flags f where f.report_id = r.id and not f.resolved),
                       'author_status', (select p.status from public.profiles p where p.id = r.user_id)) as item
              from public.reports r
             where r.status = 'pending'
                or (r.status in ('published', 'hidden') and exists (select 1 from public.flags f where f.report_id = r.id and not f.resolved))
        ) q
    );
end;
$$;

create or replace function public.moderate_report(p_id uuid, p_action text, p_reason text)
returns void language plpgsql security definer set search_path = ''
as $$
declare
    mod uuid := public.require_moderator();
    v_report public.reports;
begin
    if coalesce(btrim(p_reason), '') = '' then raise exception 'not_allowed'; end if;
    select * into v_report from public.reports where id = p_id for update;
    if not found then raise exception 'not_allowed'; end if;
    if p_action = 'approve' then
        update public.flags set resolved = true where report_id = p_id;
        update public.reports set status = 'published' where id = p_id;
    elsif p_action = 'remove' then
        update public.flags set resolved = true where report_id = p_id;
        update public.reports set status = 'removed' where id = p_id;
    else
        raise exception 'not_allowed';
    end if;
    insert into public.moderation_actions (moderator_id, report_id, target_user_id, action, reason)
    values (mod, p_id, v_report.user_id, p_action::public.moderation_action, btrim(p_reason));
    perform public.broadcast_reports_changed(v_report.zone_id);
end;
$$;

-- Spec §4 Ecrã 10 "bloquear utilizadora": moderators act via a report; they never see who the author is.
create or replace function public.block_author(p_report_id uuid, p_reason text)
returns void language plpgsql security definer set search_path = ''
as $$
declare
    mod uuid := public.require_moderator();
    author uuid;
begin
    if coalesce(btrim(p_reason), '') = '' then raise exception 'not_allowed'; end if;
    select user_id into author from public.reports where id = p_report_id;
    if author is null or author = mod then raise exception 'not_allowed'; end if;
    update public.profiles set status = 'blocked' where id = author;
    update public.invites set revoked_at = now() where inviter_id = author and redeemed_by is null and revoked_at is null;
    insert into public.moderation_actions (moderator_id, report_id, target_user_id, action, reason)
    values (mod, p_report_id, author, 'block_user', btrim(p_reason));
end;
$$;

create or replace function public.revoke_author_invites(p_report_id uuid, p_reason text)
returns integer language plpgsql security definer set search_path = ''
as $$
declare
    mod uuid := public.require_moderator();
    author uuid;
    n integer;
begin
    if coalesce(btrim(p_reason), '') = '' then raise exception 'not_allowed'; end if;
    select user_id into author from public.reports where id = p_report_id;
    if author is null then raise exception 'not_allowed'; end if;
    update public.invites set revoked_at = now() where inviter_id = author and redeemed_by is null and revoked_at is null;
    get diagnostics n = row_count;
    insert into public.moderation_actions (moderator_id, report_id, target_user_id, action, reason)
    values (mod, p_report_id, author, 'revoke_invites', btrim(p_reason));
    return n;
end;
$$;

-- Invite chain of a report's author (up to 5 levels): pseudonymous, for spotting abuse rings.
create or replace function public.inviter_chain(p_report_id uuid)
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
begin
    perform public.require_moderator();
    return (
        with recursive chain as (
            select p.id, p.invited_by, p.status, p.pseudonym, 0 as depth
              from public.profiles p join public.reports r on r.user_id = p.id where r.id = p_report_id
            union all
            select p.id, p.invited_by, p.status, p.pseudonym, c.depth + 1
              from public.profiles p join chain c on p.id = c.invited_by where c.depth < 5
        )
        select coalesce(jsonb_agg(jsonb_build_object(
                   'depth', depth,
                   'alias', coalesce(pseudonym, 'user-' || left(md5(id::text), 6)),
                   'status', status,
                   'invited_count', (select count(*) from public.profiles x where x.invited_by = chain.id),
                   'blocked_invitees', (select count(*) from public.profiles x where x.invited_by = chain.id and x.status = 'blocked'))
               order by depth), '[]'::jsonb)
          from chain
    );
end;
$$;

-- ---------- Grants: deny by default, then expose only the API ----------
revoke execute on all functions in schema public from public, anon, authenticated;

grant execute on function public.reports_in_bbox(double precision, double precision, double precision, double precision) to anon, authenticated;
grant execute on function public.reports_in_zone(text) to anon, authenticated;
grant execute on function public.validate_invite(text) to anon, authenticated;
grant execute on function public.snap_to_zone(double precision, double precision) to anon, authenticated;

grant execute on function public.my_reports() to authenticated;
grant execute on function public.submit_report(double precision, double precision, text, text, text, text, boolean) to authenticated;
grant execute on function public.update_report(uuid, double precision, double precision, text, text, text, text, boolean) to authenticated;
grant execute on function public.delete_report(uuid) to authenticated;
grant execute on function public.confirm_report(uuid) to authenticated;
grant execute on function public.flag_report(uuid, text) to authenticated;
grant execute on function public.my_profile() to authenticated;
grant execute on function public.update_my_profile(text, text) to authenticated;
grant execute on function public.my_saved_zones() to authenticated;
grant execute on function public.save_zone(text, double precision, double precision, text) to authenticated;
grant execute on function public.delete_saved_zone(uuid) to authenticated;
grant execute on function public.touch_usage() to authenticated;
grant execute on function public.my_invite_data() to authenticated;
grant execute on function public.create_invite() to authenticated;
grant execute on function public.revoke_invite(uuid) to authenticated;
grant execute on function public.redeem_invite(text) to authenticated;
grant execute on function public.export_my_data() to authenticated;
grant execute on function public.delete_my_account() to authenticated;
grant execute on function public.moderation_counters() to authenticated;
grant execute on function public.moderation_queue() to authenticated;
grant execute on function public.moderate_report(uuid, text, text) to authenticated;
grant execute on function public.block_author(uuid, text) to authenticated;
grant execute on function public.revoke_author_invites(uuid, text) to authenticated;
grant execute on function public.inviter_chain(uuid) to authenticated;

-- New functions created later must be granted explicitly.
alter default privileges in schema public revoke execute on functions from public, anon, authenticated;
