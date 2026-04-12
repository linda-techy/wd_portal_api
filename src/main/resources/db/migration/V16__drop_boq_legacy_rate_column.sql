-- ============================================================================
-- V16: Drop legacy 'rate' and 'amount' columns from boq_items
-- ============================================================================
-- The boq_items table has legacy 'rate' and 'amount' columns (NOT NULL) that
-- were created before the column rename to 'unit_rate' / 'total_amount'.
-- Portal API's BoqItem entity writes to 'unit_rate' and 'total_amount'.
-- The legacy columns are never written to, causing NOT NULL constraint
-- violations on every insert.
-- Using DO blocks for PostgreSQL 14 compatibility (no ALTER COLUMN IF EXISTS).
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'boq_items' AND column_name = 'rate'
    ) THEN
        ALTER TABLE boq_items DROP COLUMN rate;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'boq_items' AND column_name = 'amount'
    ) THEN
        ALTER TABLE boq_items DROP COLUMN amount;
    END IF;
END $$;
