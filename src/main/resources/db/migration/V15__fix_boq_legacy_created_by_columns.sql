-- ============================================================================
-- V15: Fix legacy created_by_id column on boq_items (NOT NULL constraint)
-- ============================================================================
-- The boq_items table has a legacy created_by_id column (NOT NULL) that was
-- created by Hibernate before BaseEntity was introduced. BaseEntity writes to
-- created_by_user_id instead, leaving created_by_id null and triggering a
-- NOT NULL constraint violation on every insert.
-- PostgreSQL 14 does not support ALTER COLUMN IF EXISTS, so we use DO blocks
-- to safely drop NOT NULL only when the column actually exists.
-- ============================================================================

DO $$
BEGIN
    -- boq_items.created_by_id (confirmed to exist, NOT NULL)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'boq_items' AND column_name = 'created_by_id'
    ) THEN
        ALTER TABLE boq_items ALTER COLUMN created_by_id DROP NOT NULL;
    END IF;

    -- boq_categories legacy columns
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'boq_categories' AND column_name = 'created_by_id'
    ) THEN
        ALTER TABLE boq_categories ALTER COLUMN created_by_id DROP NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'boq_categories' AND column_name = 'updated_by_id'
    ) THEN
        ALTER TABLE boq_categories ALTER COLUMN updated_by_id DROP NOT NULL;
    END IF;

    -- boq_audit_logs legacy columns
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'boq_audit_logs' AND column_name = 'created_by_id'
    ) THEN
        ALTER TABLE boq_audit_logs ALTER COLUMN created_by_id DROP NOT NULL;
    END IF;
END $$;
