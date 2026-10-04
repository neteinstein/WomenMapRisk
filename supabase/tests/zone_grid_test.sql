begin;
create extension if not exists pgtap with schema extensions;
select plan(4);

-- Shared vector with LocationAnonymizerTest.known_vector_matches_sql_implementation (Kotlin). Keep in sync.
select is((select zone_id from public.snap_to_zone(41.1496, -8.6110)), 'z45721_-7205', 'zone id matches Kotlin vector');

select ok(
    (select abs(lat - 41.1496) < 0.0009 and abs(lng - (-8.6110)) < 0.0013 from public.snap_to_zone(41.1496, -8.6110)),
    'snapped point is within one cell of the original');

select is(
    (select zone_id from public.snap_to_zone(z.lat, z.lng)),
    z.zone_id,
    'snapping is idempotent')
  from public.snap_to_zone(41.1496, -8.6110) z;

select isnt(
    (select zone_id from public.snap_to_zone(41.1496, -8.6110)),
    (select zone_id from public.snap_to_zone(41.1580, -8.6291)),
    'distant points get different zones');

select * from finish();
rollback;
