-- --- Arinara Network (c) 2026 ---
-- Exclusive property of Arinara Network.
-- Unauthorized use, reproduction, distribution, or modification of this code,
-- in whole or in part, for any purpose, is strictly prohibited without prior
-- written consent from Arinara Network as sole legal owner of this codebase.

-- ============================================================================
-- Fotara - Supabase Database Schema & Security Configuration
-- Target Table: public.suggestions
--
-- Instructions:
-- 1. Open your Supabase Dashboard: https://supabase.com/dashboard/project/nrvnhbizyvubcdqvzabv
-- 2. Navigate to "SQL Editor" in the left sidebar.
-- 3. Click "New Query", paste this entire script, and click "Run".
-- 4. Your suggestions table is now securely provisioned with:
--    - Row Level Security (RLS) allowing mobile anon clients to INSERT only.
--    - Zero SELECT permissions for anon (user feedback & email remain private).
--    - Postgres trigger enforcing max 5 submissions/24-hours and 60-second cooldown per installation UUID.
-- ============================================================================

-- 1. Enable UUID Extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Create the Suggestions Table
CREATE TABLE IF NOT EXISTS public.suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uuid TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN (
        'Bug Report', 'Suggestion', 'Feature Idea', 'General',
        'BUG_REPORT', 'SUGGESTION', 'FEATURE_IDEA', 'GENERAL'
    )),
    content TEXT NOT NULL,
    email TEXT,
    diagnostic_info TEXT,
    submitted_at BIGINT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::TEXT, NOW()) NOT NULL
);

-- 3. Indexes for High Performance and Fast Rate-Limit Lookups
CREATE INDEX IF NOT EXISTS idx_suggestions_uuid ON public.suggestions(uuid);
CREATE INDEX IF NOT EXISTS idx_suggestions_created_at ON public.suggestions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_suggestions_category ON public.suggestions(category);

-- 4. Enable Row Level Security (RLS)
ALTER TABLE public.suggestions ENABLE ROW LEVEL SECURITY;

-- 5. Drop existing policies if re-running
DROP POLICY IF EXISTS "Allow anonymous feedback insertion" ON public.suggestions;
DROP POLICY IF EXISTS "Deny public feedback read" ON public.suggestions;
DROP POLICY IF EXISTS "Service role full access" ON public.suggestions;

-- Policy 1: Allow mobile apps (using anon / publishable key) to INSERT feedback
CREATE POLICY "Allow anonymous feedback insertion"
ON public.suggestions
FOR INSERT
TO anon, authenticated
WITH CHECK (
    char_length(content) > 0 AND
    char_length(content) <= 5000 AND
    char_length(uuid) > 0
);

-- Policy 2: Prevent anonymous users from reading other users' feedback/emails
CREATE POLICY "Deny public feedback read"
ON public.suggestions
FOR SELECT
TO anon
USING (false);

-- Policy 3: Allow service_role (Admin / Dashboard) full CRUD access
CREATE POLICY "Service role full access"
ON public.suggestions
FOR ALL
TO service_role
USING (true)
WITH CHECK (true);

-- 6. Rate Limiting Function & Trigger (Database Level)
-- Enforces:
-- 1) 60-second cooldown per installation UUID
-- 2) Maximum of 5 submissions per rolling 24 hours per installation UUID
CREATE OR REPLACE FUNCTION public.check_feedback_rate_limit()
RETURNS TRIGGER AS $$
DECLARE
    recent_submissions_count INT;
    last_submission_time TIMESTAMPTZ;
BEGIN
    -- 1. Enforce 60-second cooldown per installation UUID
    SELECT created_at
    INTO last_submission_time
    FROM public.suggestions
    WHERE uuid = NEW.uuid
    ORDER BY created_at DESC
    LIMIT 1;

    IF last_submission_time IS NOT NULL AND last_submission_time > (NOW() - INTERVAL '60 seconds') THEN
        RAISE EXCEPTION 'Rate limit exceeded: Please wait 60 seconds between feedback submissions.'
            USING ERRCODE = 'P0001';
    END IF;

    -- 2. Enforce rolling 24-hour limit (max 5 submissions per installation UUID)
    SELECT COUNT(*)
    INTO recent_submissions_count
    FROM public.suggestions
    WHERE uuid = NEW.uuid
      AND created_at > (NOW() - INTERVAL '24 hours');

    IF recent_submissions_count >= 5 THEN
        RAISE EXCEPTION 'Rate limit exceeded: A maximum of 5 submissions per 24 hours per installation is permitted.'
            USING ERRCODE = 'P0001';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Drop existing trigger if re-running
DROP TRIGGER IF EXISTS trg_feedback_rate_limit ON public.suggestions;

-- Attach trigger to run before insert
CREATE TRIGGER trg_feedback_rate_limit
BEFORE INSERT ON public.suggestions
FOR EACH ROW
EXECUTE FUNCTION public.check_feedback_rate_limit();

-- 7. Grant Permissions to PostgREST roles
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT INSERT ON TABLE public.suggestions TO anon, authenticated;
GRANT ALL ON TABLE public.suggestions TO service_role;

-- ============================================================================
-- 8. Automated Email Forwarding to arinaranetwork@gmail.com
--
-- For full step-by-step instructions, see:
-- Docs/Supabase_Email_Notification_Guide.md and Docs/SupabaseEmailBridge.gs
--
-- Recommended setup: Use Supabase Dashboard > Database > Webhooks with Google Apps Script.
-- ============================================================================
