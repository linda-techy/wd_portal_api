-- ===========================================================================
-- V105 — Public view token for estimations (Sub-project H)
--
-- Adds a UUID column that uniquely identifies an estimation for unauthenticated
-- customer access via /public/estimations/{token}. Backfill existing rows
-- with random UUIDs so admins can immediately share existing estimations.
-- ===========================================================================

ALTER TABLE estimation
    ADD COLUMN IF NOT EXISTS public_view_token UUID DEFAULT gen_random_uuid();

-- Backfill: any existing rows that came in via INSERT before this column
-- existed get a token now via the DEFAULT. This is idempotent because new
-- rows already have one.
UPDATE estimation
   SET public_view_token = gen_random_uuid()
 WHERE public_view_token IS NULL;

-- Make NOT NULL + uniquely indexed so token-lookups are O(1) and rotations
-- can't accidentally collide.
ALTER TABLE estimation ALTER COLUMN public_view_token SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_estimation_public_view_token
    ON estimation (public_view_token);
