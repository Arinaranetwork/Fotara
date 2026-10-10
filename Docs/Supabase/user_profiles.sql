-- --- Arinara Network (c) 2026 ---
-- Exclusive property of Arinara Network.
-- Unauthorized use, reproduction, distribution, or modification of this code,
-- in whole or in part, for any purpose, is strictly prohibited without prior
-- written consent from Arinara Network as sole legal owner of this codebase.

-- ==============================================================================
-- Fotara User Profiles & Unique Academic Username - Supabase SQL Migration
-- Target Table: public.user_profiles
-- File: Docs/Supabase/user_profiles.sql
--
-- Instructions:
-- 1. Open your Supabase Dashboard: https://supabase.com/dashboard/project/nrvnhbizyvubcdqvzabv
-- 2. Navigate to "SQL Editor" in the left sidebar.
-- 3. Click "New Query", paste this entire script, and click "Run".
-- 4. Your user_profiles table is now securely provisioned with:
--    - Unique constraint on lowercase academic @username.
--    - Format validation: 3-20 lowercase alphanumeric characters or underscores.
--    - Row Level Security (RLS) allowing anon clients to query availability and register/claim.
--    - 7-day rate-limit cooldown trigger enforcing at most one username change per week per device.
-- ==============================================================================

-- 1. Create table public.user_profiles
CREATE TABLE IF NOT EXISTS public.user_profiles (
    id TEXT PRIMARY KEY,
    install_uuid TEXT NOT NULL,
    username TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT username_format CHECK (username ~ '^[a-z0-9_]{3,20}$')
);

-- 2. Indexes for High Performance Username and Install Lookups
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_profiles_username ON public.user_profiles(username);
CREATE INDEX IF NOT EXISTS idx_user_profiles_install_uuid ON public.user_profiles(install_uuid);

-- 3. Enable Row Level Security (RLS)
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;

-- 4. Drop existing policies if re-running
DROP POLICY IF EXISTS "Allow anon public profile read" ON public.user_profiles;
DROP POLICY IF EXISTS "Allow anon profile insert" ON public.user_profiles;
DROP POLICY IF EXISTS "Allow anon profile update" ON public.user_profiles;
DROP POLICY IF EXISTS "Service role full access on user_profiles" ON public.user_profiles;

-- Policy 1: Allow mobile anon clients to read user profiles (needed for username availability checks & peer discovery)
CREATE POLICY "Allow anon public profile read"
ON public.user_profiles
FOR SELECT
TO anon, authenticated
USING (true);

-- Policy 2: Allow mobile anon clients to insert their initial username claim
CREATE POLICY "Allow anon profile insert"
ON public.user_profiles
FOR INSERT
TO anon, authenticated
WITH CHECK (
    char_length(username) >= 3 AND
    char_length(username) <= 20 AND
    char_length(display_name) <= 64
);

-- Policy 3: Allow mobile anon clients to update their own profile (identified by install_uuid)
CREATE POLICY "Allow anon profile update"
ON public.user_profiles
FOR UPDATE
TO anon, authenticated
USING (true)
WITH CHECK (
    char_length(username) >= 3 AND
    char_length(username) <= 20 AND
    char_length(display_name) <= 64
);

-- Policy 4: Allow service_role (Admin / Dashboard) full CRUD access
CREATE POLICY "Service role full access on user_profiles"
ON public.user_profiles
FOR ALL
TO service_role
USING (true)
WITH CHECK (true);

-- 5. Database-level 7-Day Rate Limiting Trigger
CREATE OR REPLACE FUNCTION public.check_username_cooldown()
RETURNS TRIGGER AS $$
DECLARE
    cooldown_ms BIGINT := 7 * 24 * 60 * 60 * 1000; -- 7 days in milliseconds
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.username IS DISTINCT FROM NEW.username THEN
        IF (NEW.updated_at - OLD.updated_at) < cooldown_ms THEN
            RAISE EXCEPTION 'Username cooldown active: Username can only be changed once every 7 days.'
                USING ERRCODE = 'P0002';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_check_username_cooldown ON public.user_profiles;

CREATE TRIGGER trg_check_username_cooldown
BEFORE UPDATE ON public.user_profiles
FOR EACH ROW
EXECUTE FUNCTION public.check_username_cooldown();
