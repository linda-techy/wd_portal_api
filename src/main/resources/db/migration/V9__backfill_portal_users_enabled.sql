-- V9: Backfill portal_users.enabled column
--
-- The 'enabled' column was added to PortalUser entity after the initial data
-- seed, so existing rows have enabled = NULL.  NULL rows cause isEnabled()
-- to return false → all logins fail with 401.
--
-- Fix:
--   1. Set NULL → true  (all existing users should be active)
--   2. Add NOT NULL constraint with DEFAULT true so future INSERTs can't leave it null

-- Step 1: backfill NULLs
UPDATE portal_users
SET    enabled = true
WHERE  enabled IS NULL;

-- Step 2: set column default so ORM-created rows are always enabled
ALTER TABLE portal_users
    ALTER COLUMN enabled SET DEFAULT true;

-- Step 3: enforce NOT NULL (safe now that no NULLs remain)
ALTER TABLE portal_users
    ALTER COLUMN enabled SET NOT NULL;
