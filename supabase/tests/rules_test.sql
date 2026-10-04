-- Business rules (spec §5, §6, §7) and invite addendum, exercised through the public API as real roles.
begin;
create extension if not exists pgtap with schema extensions;
select plan(32);

-- ---------- helpers (in a throwaway schema; the whole file rolls back) ----------
create schema th;
grant usage on schema th to authenticated, anon;

create function th.mk_user(p_email text, p_confirmed boolean default true, p_meta jsonb default '{}'::jsonb, p_active boolean default true)
returns uuid language plpgsql as $$
declare uid uuid := gen_random_uuid();
begin
    insert into auth.users (id, email, email_confirmed_at, raw_user_meta_data, aud, role)
    values (uid, p_email, case when p_confirmed then now() end, p_meta, 'authenticated', 'authenticated');
    if p_active then update public.profiles set status = 'active' where id = uid; end if;
    return uid;
end;
$$;

create function th.login(p_uid uuid) returns void language plpgsql as $$
begin
    perform set_config('request.jwt.claims', json_build_object('sub', p_uid, 'role', 'authenticated')::text, true);
    perform set_config('role', 'authenticated', true);
end;
$$;

create function th.logout() returns void language plpgsql as $$
begin
    perform set_config('role', 'postgres', true);
    perform set_config('request.jwt.claims', '', true);
end;
$$;

create function th.report(p_lat double precision, p_lng double precision, p_est boolean default false)
returns jsonb language sql as $$
    select public.submit_report(p_lat, p_lng, 'verbal_harassment', 'today', 'night', null, p_est)
$$;

grant execute on all functions in schema th to authenticated, anon;

create table th.ids (name text primary key, id uuid);
grant all on th.ids to authenticated, anon;
create table th.r (id uuid);
create table th.inv (code text);
grant all on th.r, th.inv to authenticated, anon;
insert into th.ids values
    ('ana', th.mk_user('ana@test.local')),
    ('bea', th.mk_user('bea@test.local')),
    ('cris', th.mk_user('cris@test.local')),
    ('dina', th.mk_user('dina@test.local')),
    ('unconfirmed', th.mk_user('u@test.local', false)),
    ('mod', th.mk_user('mod@test.local'));
update public.profiles set role = 'moderator' where id = (select id from th.ids where name = 'mod');

-- ---------- direct table access is denied ----------
select th.login((select id from th.ids where name = 'ana'));
select throws_ok($$ select * from public.reports $$, '42501', null, 'authenticated cannot read reports table directly');
select throws_ok($$ insert into public.reports (zone_id, lat, lng, type, occurred_when, day_period) values ('x', 0, 0, 'other', 'now', 'day') $$,
                 '42501', null, 'authenticated cannot insert into reports directly');

-- ---------- submit + privacy ----------
select ok((th.report(41.14961, -8.61102) ->> 'lat')::double precision <> 41.14961, 'stored latitude is snapped, not exact');
select ok(not (th.report(41.20, -8.60) ? 'user_id'), 'report json never exposes user_id');
select is((public.reports_in_bbox(41.0, -8.8, 41.3, -8.5) -> 0 ->> 'is_mine')::boolean, true, 'author sees is_mine');

-- same place same day blocked
select throws_ok($$ select th.report(41.14961, -8.61102) $$, 'P0001', 'duplicate_report', 'same place same day is blocked');

-- daily limit 5 (already 2 today)
select lives_ok($$ select th.report(41.10, -8.60); select th.report(41.11, -8.60); select th.report(41.12, -8.60) $$, 'up to 5 per day allowed');
select throws_ok($$ select th.report(41.13, -8.60) $$, 'P0001', 'daily_report_limit', '6th report in a day is blocked');

-- establishment reports wait for review
select th.login((select id from th.ids where name = 'bea'));
select is(th.report(41.15, -8.62, true) ->> 'status', 'pending', 'establishment report is pending');

-- unconfirmed email cannot report
select th.login((select id from th.ids where name = 'unconfirmed'));
select throws_ok($$ select th.report(41.15, -8.62) $$, 'P0001', 'email_not_confirmed', 'unconfirmed email cannot report');

-- visitors (anon) can read the map but not report
select th.logout();
select set_config('role', 'anon', true);
select ok(jsonb_array_length(public.reports_in_bbox(41.0, -8.8, 41.3, -8.5)) >= 5, 'anon can read published reports');
select throws_ok($$ select th.report(41.15, -8.62) $$, '42501', null, 'anon cannot submit');
select th.logout();

-- ---------- confirmations ----------
insert into th.r select ((public.reports_in_bbox(41.149, -8.612, 41.151, -8.610) -> 0) ->> 'id')::uuid;

select th.login((select id from th.ids where name = 'ana'));
select throws_ok($$ select public.confirm_report((select id from th.r)) $$, 'P0001', 'cannot_confirm_own', 'cannot confirm own report');

select th.login((select id from th.ids where name = 'bea'));
select lives_ok($$ select public.confirm_report((select id from th.r)) $$, 'other woman can confirm');
select throws_ok($$ select public.confirm_report((select id from th.r)) $$, 'P0001', 'already_confirmed', 'confirm only once');
select is((public.reports_in_zone((select zone_id from public.snap_to_zone(41.14961, -8.61102))) -> 0 ->> 'confirmations')::int, 1, 'counter went up by 1');
select is((public.reports_in_zone((select zone_id from public.snap_to_zone(41.14961, -8.61102))) -> 0 ->> 'confirmed_by_me')::boolean, true, 'confirmed_by_me true');

-- ---------- flags hide at 3 ----------
select public.flag_report((select id from th.r), 'false_information');
select th.login((select id from th.ids where name = 'cris'));
select public.flag_report((select id from th.r), 'offensive');
select th.login((select id from th.ids where name = 'dina'));
select public.flag_report((select id from th.r), 'spam');
select is(jsonb_array_length(public.reports_in_zone((select zone_id from public.snap_to_zone(41.14961, -8.61102)))), 0, '3 flags hide the report');

-- ---------- moderation ----------
select throws_ok($$ select public.moderation_queue() $$, 'P0001', 'not_allowed', 'non-moderators cannot see the queue');
select th.login((select id from th.ids where name = 'mod'));
select ok(jsonb_array_length(public.moderation_queue()) >= 2, 'queue has flagged + establishment items');
select lives_ok($$ select public.moderate_report((select id from th.r), 'approve', 'Reviewed, legitimate') $$, 'moderator approves');
select is(jsonb_array_length(public.reports_in_zone((select zone_id from public.snap_to_zone(41.14961, -8.61102)))), 1, 'approved report is visible again');
select th.logout();
select is((select count(*)::int from public.moderation_actions where reason = 'Reviewed, legitimate'), 1, 'action recorded with reason');

-- ---------- edit window ----------
update public.reports set created_at = now() - interval '25 hours' where id = (select id from th.r);
select th.login((select id from th.ids where name = 'ana'));
select throws_ok($$ select public.delete_report((select id from th.r)) $$, 'P0001', 'edit_window_expired', 'cannot delete after 24h');

-- ---------- invites ----------
select throws_ok($$ select public.create_invite() $$, 'P0001', 'invites_locked', 'invites locked for new users');
select th.logout();
insert into public.usage_days (user_id, day)
select (select id from th.ids where name = 'ana'), public.policy_date() - g from generate_series(0, 2) g;
select th.login((select id from th.ids where name = 'ana'));
insert into th.inv select public.create_invite() ->> 'code';
select ok((select code from th.inv) ~ '^[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{8}$', '3 consecutive days unlock invites; code well-formed');
select lives_ok($$ select public.create_invite(); select public.create_invite(); select public.create_invite(); select public.create_invite() $$, 'up to 5 invites');
select throws_ok($$ select public.create_invite() $$, 'P0001', 'invite_limit_reached', '6th invite blocked');

-- sign-up with invite code in metadata activates the account (handle_new_user)
select th.logout();
insert into th.ids values ('eva', th.mk_user('eva@test.local', true, jsonb_build_object('invite_code', (select code from th.inv), 'declared_woman', true), false));
select is((select status::text from public.profiles where id = (select id from th.ids where name = 'eva')), 'active', 'invite in sign-up metadata activates account');
select is(public.validate_invite((select code from th.inv)), false, 'used invite is no longer valid');

-- ---------- account deletion ----------
select th.login((select id from th.ids where name = 'ana'));
select public.delete_my_account();
select th.logout();
select is((select count(*)::int from public.profiles where id = (select id from th.ids where name = 'ana')), 0, 'profile deleted');
select ok((select count(*) from public.reports where id = (select id from th.r)) = 1
          and (select user_id from public.reports where id = (select id from th.r)) is null,
          'reports stay on the map, unlinked from the deleted account');

select * from finish();
rollback;
