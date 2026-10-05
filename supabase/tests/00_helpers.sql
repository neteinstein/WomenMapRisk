-- Shared pgTAP helpers, created inside each test transaction (tests roll back).
-- Usage: \ir 00_helpers.sql is not supported by `supabase test db`, so each test file inlines the
-- same helpers through tests.setup(); this file only verifies the extension is available.
begin;
create extension if not exists pgtap with schema extensions;
select plan(1);
select has_extension('pgtap');
select * from finish();
rollback;
