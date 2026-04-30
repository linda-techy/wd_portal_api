-- =============================================================================
-- V76: Fix Site Reports and Photos Schema
-- 
-- Root cause: SiteReport entity was updated to extend BaseEntity and include 
-- GPS tracking fields, but the database schema was not updated to match.
-- This caused BadSqlGrammarException (500 Internal Server Error) on every POST.
-- 
-- Fixes:
--   1. Add auditing columns to site_reports (from BaseEntity)
--   2. Add GPS and accountability columns to site_reports
--   3. Add missing columns to site_report_photos
--   4. Handle legacy created_by_id constraint on site_reports
-- =============================================================================

-- 1. Update site_reports table
ALTER TABLE site_reports 
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS location_accuracy DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS distance_from_project DOUBLE PRECISION;

-- 2. Update site_report_photos table
ALTER TABLE site_report_photos
    ADD COLUMN IF NOT EXISTS caption VARCHAR(255),
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS display_order INTEGER DEFAULT 0;

-- 3. Handle legacy created_by_id on site_reports
-- This column was created by Hibernate before BaseEntity and has a NOT NULL 
-- constraint + FK to customer_users, both of which are incorrect for portal site reports.
DO $$
BEGIN
    -- Drop FK constraint if it exists (name from schema dump)
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints 
               WHERE constraint_name = 'fkh0794dh3vnvjymnydp25psui7' AND table_name = 'site_reports') THEN
        ALTER TABLE site_reports DROP CONSTRAINT fkh0794dh3vnvjymnydp25psui7;
    END IF;

    -- Drop NOT NULL constraint
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'site_reports' AND column_name = 'created_by_id') THEN
        ALTER TABLE site_reports ALTER COLUMN created_by_id DROP NOT NULL;
    END IF;
END $$;
