-- Docker-free Supabase emulation for running supabase/migrations + supabase/tests on plain Postgres.
-- Used by scripts/sql-harness/run.sh. NOT the real Supabase: CI (supabase start + supabase test db) is authoritative.
-- Local harness ONLY: emulates the bits of Supabase the migrations rely on.
do $$ begin create role anon nologin; exception when duplicate_object then null; end $$;
do $$ begin create role authenticated nologin; exception when duplicate_object then null; end $$;
create schema auth; create schema extensions; create schema realtime;
grant usage on schema extensions, auth to anon, authenticated;
create table auth.users (
  instance_id uuid, id uuid primary key, aud text, role text, email text unique, encrypted_password text,
  email_confirmed_at timestamptz, raw_app_meta_data jsonb, raw_user_meta_data jsonb,
  created_at timestamptz default now(), updated_at timestamptz default now(),
  confirmation_token text, recovery_token text, email_change_token_new text, email_change text);
create table auth.identities (id uuid primary key, user_id uuid references auth.users on delete cascade, identity_data jsonb,
  provider text, provider_id text, last_sign_in_at timestamptz, created_at timestamptz, updated_at timestamptz);
create function auth.uid() returns uuid language sql stable as $$
  select nullif(coalesce(current_setting('request.jwt.claim.sub', true), (nullif(current_setting('request.jwt.claims', true), '')::jsonb ->> 'sub')), '')::uuid $$;
grant execute on function auth.uid() to anon, authenticated;
create function extensions.gen_random_bytes(n int) returns bytea language sql volatile as $$
  select substring(decode(md5(random()::text) || md5(random()::text), 'hex') from 1 for n) $$;
create function extensions.gen_salt(t text) returns text language sql as $$ select 'salt' $$;
create function extensions.crypt(p text, s text) returns text language sql as $$ select md5(p || s) $$;

-- Minimal pgTAP shim
create table extensions.tap (n serial, ok boolean, descr text, diag text);
create table extensions.tap_plan (planned int);
grant all on extensions.tap, extensions.tap_plan to anon, authenticated;
grant usage on sequence extensions.tap_n_seq to anon, authenticated;
create function extensions.plan(n int) returns text language plpgsql as $$
begin truncate extensions.tap restart identity; truncate extensions.tap_plan; insert into extensions.tap_plan values (n); return '1..' || n; end $$;
create function extensions._rec(b boolean, d text, diag text default null) returns text language plpgsql as $$
begin insert into extensions.tap (ok, descr, diag) values (coalesce(b,false), d, diag);
return case when coalesce(b,false) then 'ok - ' else 'NOT OK - ' end || d || coalesce(' # ' || diag, ''); end $$;
create function extensions.ok(b boolean, d text default '') returns text language sql as $$ select extensions._rec(b, d) $$;
create function extensions.is(a anyelement, b anyelement, d text default '') returns text language sql as $$
  select extensions._rec(a is not distinct from b, d, case when a is distinct from b then 'got ' || coalesce(a::text,'NULL') || ' expected ' || coalesce(b::text,'NULL') end) $$;
create function extensions.isnt(a anyelement, b anyelement, d text default '') returns text language sql as $$ select extensions._rec(a is distinct from b, d) $$;
create function extensions.has_extension(e text) returns text language sql as $$ select extensions._rec(true, 'has_extension ' || e) $$;
create function extensions.lives_ok(q text, d text default '') returns text language plpgsql as $$
begin execute q; return extensions._rec(true, d);
exception when others then return extensions._rec(false, d, sqlstate || ' ' || sqlerrm); end $$;
create function extensions.throws_ok(q text, code char(5), msg text, d text default '') returns text language plpgsql as $$
begin execute q; return extensions._rec(false, d, 'no exception');
exception when others then
  return extensions._rec(sqlstate = code and (msg is null or sqlerrm = msg), d, 'got ' || sqlstate || ' ' || sqlerrm); end $$;
create function extensions.finish() returns setof text language plpgsql as $$
declare planned int; total int; failed int;
begin select p.planned into planned from extensions.tap_plan p; select count(*), count(*) filter (where not ok) into total, failed from extensions.tap;
  return next format('# %s/%s passed, planned %s', total - failed, total, planned);
  if failed > 0 or total <> planned then raise exception 'TAP FAILED: % failed, % run, % planned', failed, total, planned; end if; end $$;
grant execute on all functions in schema extensions to anon, authenticated;
alter database postgres set search_path = "$user", public, extensions;
