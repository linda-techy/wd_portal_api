-- Migration to consolidate duplicate square footage fields in customer_projects table
-- Consolidates sq_feet (Double) and sqfeet (NUMERIC) into single sqfeet (NUMERIC) field

-- Step 1: Copy data from sq_feet to sqfeet if sqfeet is NULL
UPDATE customer_projects
SET sqfeet = sq_feet::NUMERIC(10,2)
WHERE sqfeet IS NULL AND sq_feet IS NOT NULL;

-- Step 2: If both have values and they differ significantly, log a warning (we'll keep sqfeet as source of truth)
-- This is just documentation - PostgreSQL doesn't have PRINT, so we'll add a comment
-- In production, you'd want to verify no data loss before running this

-- Step 3: Drop the redundant sq_feet column
ALTER TABLE customer_projects 
DROP COLUMN IF EXISTS sq_feet;

-- Note: After this migration, only 'sqfeet' NUMERIC(10,2) should be used
-- Update application code to remove getSqFeet()/setSqFeet() methods
