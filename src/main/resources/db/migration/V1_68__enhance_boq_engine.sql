-- ============================================================================
-- V1_68: Enhance BOQ Engine - Production-Grade Financial Tracking
-- ============================================================================
-- Adds financial tracking fields, audit columns, constraints, and indexes
-- to the boq_items table for construction ERP compliance.
-- ============================================================================

-- 1. Core fields that Customer API model already uses but Portal model doesn't map
ALTER TABLE boq_items ADD COLUMN IF NOT EXISTS item_code VARCHAR(50);
ALTER TABLE boq_items ADD COLUMN IF NOT EXISTS specifications TEXT;
ALTER TABLE boq_items ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- 2. Financial tracking fields (NEW)
ALTER TABLE boq_items ADD COLUMN IF NOT EXISTS executed_quantity NUMERIC(15,4) NOT NULL DEFAULT 0;
ALTER TABLE boq_items ADD COLUMN IF NOT EXISTS billed_quantity NUMERIC(15,4) NOT NULL DEFAULT 0;
ALTER TABLE boq_items ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

-- 3. Audit fields from BaseEntity pattern
ALTER TABLE boq_items ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES portal_users(id);
ALTER TABLE boq_items ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES portal_users(id);
ALTER TABLE boq_items ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE boq_items ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT REFERENCES portal_users(id);
ALTER TABLE boq_items ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 1;

-- 4. Add category_id for hierarchy (will be linked in V1_69)
ALTER TABLE boq_items ADD COLUMN IF NOT EXISTS category_id BIGINT;

-- 5. Increase decimal precision for money/quantity columns
ALTER TABLE boq_items ALTER COLUMN quantity TYPE NUMERIC(15,4);
ALTER TABLE boq_items ALTER COLUMN unit_rate TYPE NUMERIC(15,4);
ALTER TABLE boq_items ALTER COLUMN total_amount TYPE NUMERIC(15,4);

-- 6. Constraints for financial safety
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_boq_quantity_non_negative') THEN
        ALTER TABLE boq_items ADD CONSTRAINT chk_boq_quantity_non_negative CHECK (quantity >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_boq_unit_rate_non_negative') THEN
        ALTER TABLE boq_items ADD CONSTRAINT chk_boq_unit_rate_non_negative CHECK (unit_rate >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_boq_executed_lte_planned') THEN
        ALTER TABLE boq_items ADD CONSTRAINT chk_boq_executed_lte_planned CHECK (executed_quantity <= quantity);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_boq_billed_lte_executed') THEN
        ALTER TABLE boq_items ADD CONSTRAINT chk_boq_billed_lte_executed CHECK (billed_quantity <= executed_quantity);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_boq_status') THEN
        ALTER TABLE boq_items ADD CONSTRAINT chk_boq_status CHECK (status IN ('DRAFT','APPROVED','LOCKED','COMPLETED','ARCHIVED'));
    END IF;
END
$$;

-- 7. Indexes for performance
CREATE INDEX IF NOT EXISTS idx_boq_items_item_code ON boq_items(item_code) WHERE item_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_boq_items_status ON boq_items(status);
CREATE INDEX IF NOT EXISTS idx_boq_items_active ON boq_items(is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_boq_items_category ON boq_items(category_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_boq_items_project_item_code
    ON boq_items(project_id, item_code)
    WHERE item_code IS NOT NULL AND deleted_at IS NULL;

-- 8. Set existing rows to sensible defaults
UPDATE boq_items SET is_active = TRUE WHERE is_active IS NULL;
UPDATE boq_items SET executed_quantity = 0 WHERE executed_quantity IS NULL;
UPDATE boq_items SET billed_quantity = 0 WHERE billed_quantity IS NULL;
UPDATE boq_items SET status = 'DRAFT' WHERE status IS NULL;
UPDATE boq_items SET version = 1 WHERE version IS NULL;

COMMENT ON COLUMN boq_items.executed_quantity IS 'Quantity physically executed/completed on site. Must be <= quantity (planned).';
COMMENT ON COLUMN boq_items.billed_quantity IS 'Quantity already billed to client. Must be <= executed_quantity.';
COMMENT ON COLUMN boq_items.status IS 'Workflow status: DRAFT (editable) -> APPROVED -> LOCKED -> COMPLETED';
