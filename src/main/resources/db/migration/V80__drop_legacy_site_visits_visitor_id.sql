-- ===========================================================================
-- V80 — Drop legacy site_visits.visitor_id column
--
-- Bug: the SiteVisit entity holds the visitor as `visited_by` (column
-- `visited_by`), but the site_visits table also carries a legacy
-- `visitor_id BIGINT NOT NULL` column from a much earlier schema version.
-- Hibernate doesn't write to it, so every check-in INSERT was failing
-- with:
--   ERROR: null value in column "visitor_id" of relation "site_visits"
--          violates not-null constraint
-- mapped to 500 by GlobalExceptionHandler#handleRuntimeException.
--
-- A repo-wide grep confirms zero references to `visitor_id` in the Java
-- source tree — the column is fully orphaned. Dropping it outright is
-- safer than carrying a NULLable but-still-present duplicate that future
-- contributors might accidentally re-couple to.
--
-- Idempotent: every step uses IF EXISTS so re-running the migration
-- against a partially-applied schema is safe.
-- ===========================================================================

DO $$
DECLARE
    fk_name text;
BEGIN
    -- 1. Drop any FK constraint that pinned visitor_id to portal_users
    --    (or customer_users — the older variants used either). Constraint
    --    name varies by Hibernate seed version, so look it up dynamically.
    FOR fk_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class      rel ON rel.oid = con.conrelid
        JOIN pg_attribute  att ON att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey)
        WHERE rel.relname = 'site_visits'
          AND att.attname = 'visitor_id'
          AND con.contype = 'f'
    LOOP
        EXECUTE format('ALTER TABLE site_visits DROP CONSTRAINT %I', fk_name);
    END LOOP;

    -- 2. Drop NOT NULL so inserts succeed even if the column survives this
    --    migration for any reason (e.g. dependent view we don't yet know about).
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'site_visits' AND column_name = 'visitor_id'
    ) THEN
        ALTER TABLE site_visits ALTER COLUMN visitor_id DROP NOT NULL;
    END IF;

    -- 3. Drop the column entirely.
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'site_visits' AND column_name = 'visitor_id'
    ) THEN
        ALTER TABLE site_visits DROP COLUMN visitor_id;
    END IF;
END $$;
