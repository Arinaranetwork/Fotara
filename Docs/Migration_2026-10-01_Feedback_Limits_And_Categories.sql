-- --- Arinara Network (c) 2026 ---
-- Exclusive property of Arinara Network.
-- Unauthorized use, reproduction, distribution, or modification of this code,
-- in whole or in part, for any purpose, is strictly prohibited without prior
-- written consent from Arinara Network as sole legal owner of this codebase.

-- ============================================================================
-- Migration: 2026-10-01_Feedback_Limits_And_Categories.sql
-- Description:
-- 1. Updates category check constraint to accept both Title Case and UPPER_CASE
--    ('Bug Report', 'Suggestion', 'Feature Idea', 'General', 'BUG_REPORT', etc.)
-- 2. Tightens database trigger rate limit to:
--    - 60-second cooldown per installation UUID
--    - 5 submissions per rolling 24-hour window per installation UUID
--    - Returns custom error code P0001 for reliable client identification
-- ============================================================================

-- Step 1: Update category check constraint
ALTER TABLE public.suggestions DROP CONSTRAINT IF EXISTS suggestions_category_check;
ALTER TABLE public.suggestions ADD CONSTRAINT suggestions_category_check CHECK (
    category IN (
        'Bug Report', 'Suggestion', 'Feature Idea', 'General',
        'BUG_REPORT', 'SUGGESTION', 'FEATURE_IDEA', 'GENERAL'
    )
);

-- Step 2: Replace rate limiting function
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

-- Step 3: Re-attach trigger
DROP TRIGGER IF EXISTS trg_feedback_rate_limit ON public.suggestions;
CREATE TRIGGER trg_feedback_rate_limit
BEFORE INSERT ON public.suggestions
FOR EACH ROW
EXECUTE FUNCTION public.check_feedback_rate_limit();
