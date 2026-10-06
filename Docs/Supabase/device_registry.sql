-- ==============================================================================
-- Fotara Device Registry - Supabase SQL Migration
-- Purpose: Anonymous active device count backed by secure RPC functions.
-- File: Docs/Supabase/device_registry.sql
-- ==============================================================================

-- 1. Create table app_devices
create table if not exists public.app_devices (
    device_id uuid primary key,
    app_version text not null,
    channel text not null,
    first_seen timestamptz not null default now(),
    last_seen timestamptz not null default now()
);

-- Enable Row Level Security (RLS)
alter table public.app_devices enable row level security;

-- Do NOT add any policies for anon on app_devices table directly.
-- Direct SELECT, INSERT, UPDATE, and DELETE operations by anon/authenticated are denied by default.

-- 2. Create rate limiting tracking table for register_device floods
create table if not exists public.device_registration_rate_limit (
    window_minute timestamptz primary key,
    registration_count integer not null default 1
);

alter table public.device_registration_rate_limit enable row level security;

-- 3. Secure RPC: register_device
create or replace function public.register_device(
    p_device_id uuid,
    p_app_version text,
    p_channel text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_clean_version text;
    v_clean_channel text;
    v_current_minute timestamptz;
    v_current_count integer;
    v_max_per_minute integer := 120; -- Configurable rate limit: max 120 new devices/min globally
    v_existing_last_seen timestamptz;
begin
    -- Parameter validation: device_id
    if p_device_id is null then
        raise exception 'Invalid device_id: UUID cannot be null';
    end if;

    -- Parameter validation: app_version (strict numeric semver pattern, max 32 chars)
    v_clean_version := trim(p_app_version);
    if length(v_clean_version) = 0 or length(v_clean_version) > 32 then
        raise exception 'Invalid app_version length';
    end if;
    if v_clean_version !~ '^[0-9]+(\.[0-9]+){1,3}(-[A-Za-z0-9.]+)?$' then
        raise exception 'Invalid app_version format: %', v_clean_version;
    end if;

    -- Parameter validation: channel (strict allowlist)
    v_clean_channel := lower(trim(p_channel));
    if v_clean_channel not in ('stable', 'beta') then
        raise exception 'Invalid channel: must be stable or beta';
    end if;

    -- Check if device already exists
    select last_seen into v_existing_last_seen
    from public.app_devices
    where device_id = p_device_id;

    if found then
        -- Update only if last_seen is older than 1 hour (throttles database churn)
        if v_existing_last_seen < now() - interval '1 hour' or public.app_devices.app_version <> v_clean_version then
            update public.app_devices
            set
                app_version = v_clean_version,
                channel = v_clean_channel,
                last_seen = now()
            where device_id = p_device_id;
        end if;
    else
        -- Rate limiting check for new device inserts
        v_current_minute := date_trunc('minute', now());

        insert into public.device_registration_rate_limit (window_minute, registration_count)
        values (v_current_minute, 1)
        on conflict (window_minute)
        do update set registration_count = public.device_registration_rate_limit.registration_count + 1
        returning registration_count into v_current_count;

        if v_current_count > v_max_per_minute then
            raise exception 'Global device registration rate limit exceeded. Please retry later.';
        end if;

        -- Clean up old rate limit tracking rows (older than 1 hour)
        delete from public.device_registration_rate_limit
        where window_minute < now() - interval '1 hour';

        -- Insert new anonymous device record
        insert into public.app_devices (device_id, app_version, channel, first_seen, last_seen)
        values (p_device_id, v_clean_version, v_clean_channel, now(), now());
    end if;
end;
$$;

-- 4. Secure RPC: unregister_device
create or replace function public.unregister_device(
    p_device_id uuid
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if p_device_id is null then
        return;
    end if;

    delete from public.app_devices
    where device_id = p_device_id;
end;
$$;

-- 5. Revoke execute privileges from PUBLIC and grant to anon and authenticated
revoke all on function public.register_device(uuid, text, text) from public;
grant execute on function public.register_device(uuid, text, text) to anon, authenticated;

revoke all on function public.unregister_device(uuid) from public;
grant execute on function public.unregister_device(uuid) to anon, authenticated;

-- 6. Owner analytics view (NOT accessible to anon)
create or replace view public.owner_device_analytics as
select
    count(*) as total_devices,
    count(*) filter (where last_seen >= now() - interval '30 days') as active_last_30_days,
    count(*) filter (where last_seen >= now() - interval '7 days') as active_last_7_days,
    count(*) filter (where last_seen >= now() - interval '24 hours') as active_last_24_hours,
    jsonb_object_agg(app_version, version_count) as devices_by_version,
    jsonb_object_agg(channel, channel_count) as devices_by_channel
from (
    select
        app_version,
        channel,
        count(*) over(partition by app_version) as version_count,
        count(*) over(partition by channel) as channel_count,
        last_seen
    from public.app_devices
) sub;

-- Ensure view is restricted to service_role / owner only
revoke all on public.owner_device_analytics from public;
revoke all on public.owner_device_analytics from anon;
revoke all on public.owner_device_analytics from authenticated;
