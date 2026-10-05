-- LOCAL DEVELOPMENT SEED ONLY (supabase db reset). Never run against production.
-- Porto pilot sample data so the map isn't empty. Users (password for all: womenriskmap-dev-1):
--   mod@womenriskmap.local (moderator), ana@womenriskmap.local, bea@womenriskmap.local
-- Invite code for signing up locally: SEGURA26

create or replace function pg_temp.seed_user(p_id uuid, p_email text, p_role public.user_role)
returns void language plpgsql as $$
begin
    insert into auth.users (instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
                            raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
                            confirmation_token, recovery_token, email_change_token_new, email_change)
    values ('00000000-0000-0000-0000-000000000000', p_id, 'authenticated', 'authenticated', p_email,
            extensions.crypt('womenriskmap-dev-1', extensions.gen_salt('bf')), now(),
            '{"provider":"email","providers":["email"]}', '{"country":"PT","declared_woman":true}', now() - interval '60 days', now(),
            '', '', '', '');
    insert into auth.identities (id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at)
    values (gen_random_uuid(), p_id, jsonb_build_object('sub', p_id::text, 'email', p_email), 'email', p_id::text, now(), now(), now());
    update public.profiles set status = 'active', role = p_role where id = p_id;
end;
$$;

select pg_temp.seed_user('00000000-0000-4000-8000-000000000001', 'mod@womenriskmap.local', 'moderator');
select pg_temp.seed_user('00000000-0000-4000-8000-000000000002', 'ana@womenriskmap.local', 'user');
select pg_temp.seed_user('00000000-0000-4000-8000-000000000003', 'bea@womenriskmap.local', 'user');

insert into public.invites (code, inviter_id, expires_at)
values ('SEGURA26', '00000000-0000-4000-8000-000000000001', now() + interval '365 days');

-- Sample reports around Porto (lat/lng get snapped to zone centres by trigger).
insert into public.reports (user_id, zone_id, lat, lng, type, occurred_when, day_period, description, created_at, confirmations)
values
    ('00000000-0000-4000-8000-000000000002', '', 41.1456, -8.6109, 'poorly_lit', 'this_week', 'night', 'Rua muito escura depois das 21h.', now() - interval '3 days', 4),
    ('00000000-0000-4000-8000-000000000003', '', 41.1457, -8.6111, 'verbal_harassment', 'today', 'night', null, now() - interval '10 hours', 2),
    ('00000000-0000-4000-8000-000000000002', '', 41.1455, -8.6108, 'followed', 'earlier', 'night', 'Fui seguida até à estação.', now() - interval '40 days', 1),
    ('00000000-0000-4000-8000-000000000003', '', 41.1497, -8.6062, 'deserted', 'earlier', 'night', 'Zona sem movimento à noite.', now() - interval '20 days', 0),
    ('00000000-0000-4000-8000-000000000002', '', 41.1409, -8.6136, 'robbery', 'earlier', 'day', null, now() - interval '100 days', 3),
    ('00000000-0000-4000-8000-000000000003', '', 41.1580, -8.6291, 'verbal_harassment', 'this_week', 'day', 'Comentários na paragem de autocarro.', now() - interval '5 days', 0),
    ('00000000-0000-4000-8000-000000000002', '', 41.1621, -8.5835, 'poorly_lit', 'earlier', 'night', null, now() - interval '200 days', 0);
