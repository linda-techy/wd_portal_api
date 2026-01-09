-- Migration: Standardize Procurement and Inventory Audit Trails and Enums
-- Version: V1_47
-- Description: Adds BaseEntity audit columns, optimistic locking, and enum constraints 
--              to all procurement and inventory related tables.

-- ============================================================================
-- 1. Create Missing Tables
-- ============================================================================

CREATE TABLE IF NOT EXISTS material_budgets (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES customer_projects(id),
    material_id BIGINT NOT NULL REFERENCES materials(id),
    budgeted_quantity NUMERIC(15,2) NOT NULL,
    estimated_rate NUMERIC(15,2),
    total_budget NUMERIC(15,2),
    
    -- Audit Trail
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by_user_id BIGINT REFERENCES portal_users(id),
    updated_by_user_id BIGINT REFERENCES portal_users(id),
    deleted_at TIMESTAMP,
    deleted_by_user_id BIGINT REFERENCES portal_users(id),
    version BIGINT DEFAULT 0 NOT NULL
);

-- ============================================================================
-- 2. Standardize Vendors
-- ============================================================================

ALTER TABLE vendors 
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- Standardize vendor_type for enum compatibility
UPDATE vendors SET vendor_type = UPPER(REPLACE(vendor_type, ' ', '_')) WHERE vendor_type IS NOT NULL;
UPDATE vendors SET vendor_type = 'MATERIAL' WHERE vendor_type NOT IN ('MATERIAL', 'LABOUR', 'BOTH', 'CONSULTANT', 'SERVICE_PROVIDER') OR vendor_type IS NULL;
ALTER TABLE vendors ADD CONSTRAINT chk_vendor_type CHECK (vendor_type IN ('MATERIAL', 'LABOUR', 'BOTH', 'CONSULTANT', 'SERVICE_PROVIDER'));

-- ============================================================================
-- 3. Standardize Materials
-- ============================================================================

ALTER TABLE materials 
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- Standardize strings for enum compatibility
UPDATE materials SET unit = UPPER(REPLACE(unit, ' ', '_')) WHERE unit IS NOT NULL;
UPDATE materials SET category = UPPER(REPLACE(category, ' ', '_')) WHERE category IS NOT NULL;

-- Normalize invalid values before adding constraints
UPDATE materials SET unit = 'NOS' WHERE unit NOT IN ('BAG', 'KG', 'MT', 'CFT', 'SQFT', 'NOS', 'CUM', 'LTR', 'PKT', 'BUNDLE', 'TRUCK', 'LOAD') OR unit IS NULL;
UPDATE materials SET category = 'OTHERS' WHERE category NOT IN ('CEMENT', 'STEEL', 'AGGREGATE', 'SAND', 'BRICKS_BLOCKS', 'TILES_MARBLE', 'ELECTRICAL', 'PLUMBING', 'PAINTING', 'WOOD_CARPENTRY', 'HARDWARE', 'OTHERS') OR category IS NULL;

-- Add constraints
ALTER TABLE materials ADD CONSTRAINT chk_material_unit CHECK (unit IN ('BAG', 'KG', 'MT', 'CFT', 'SQFT', 'NOS', 'CUM', 'LTR', 'PKT', 'BUNDLE', 'TRUCK', 'LOAD'));
ALTER TABLE materials ADD CONSTRAINT chk_material_category CHECK (category IN ('CEMENT', 'STEEL', 'AGGREGATE', 'SAND', 'BRICKS_BLOCKS', 'TILES_MARBLE', 'ELECTRICAL', 'PLUMBING', 'PAINTING', 'WOOD_CARPENTRY', 'HARDWARE', 'OTHERS'));

-- ============================================================================
-- 4. Standardize Purchase Orders
-- ============================================================================

-- Status constraint
UPDATE purchase_orders SET status = UPPER(status) WHERE status IS NOT NULL;
UPDATE purchase_orders SET status = 'DRAFT' WHERE status NOT IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SENT_TO_VENDOR', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED', 'CLOSED') OR status IS NULL;

DO $$ 
BEGIN 
    -- Rename if old exists and new doesn't
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='purchase_orders' AND column_name='created_by_id') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='purchase_orders' AND column_name='created_by_user_id') THEN
        ALTER TABLE purchase_orders RENAME COLUMN created_by_id TO created_by_user_id;
    -- If both exist, migrate data and drop old
    ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='purchase_orders' AND column_name='created_by_id') 
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='purchase_orders' AND column_name='created_by_user_id') THEN
        UPDATE purchase_orders SET created_by_user_id = created_by_id WHERE created_by_user_id IS NULL;
        ALTER TABLE purchase_orders DROP COLUMN created_by_id;
    END IF;
END $$;

ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

ALTER TABLE purchase_orders DROP CONSTRAINT IF EXISTS chk_po_status;
ALTER TABLE purchase_orders ADD CONSTRAINT chk_po_status CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SENT_TO_VENDOR', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED', 'CLOSED'));

-- ============================================================================
-- 5. Standardize Purchase Order Items
-- ============================================================================

ALTER TABLE purchase_order_items 
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

UPDATE purchase_order_items SET unit = UPPER(REPLACE(unit, ' ', '_')) WHERE unit IS NOT NULL;
ALTER TABLE purchase_order_items ADD CONSTRAINT chk_po_item_unit CHECK (unit IN ('BAG', 'KG', 'MT', 'CFT', 'SQFT', 'NOS', 'CUM', 'LTR', 'PKT', 'BUNDLE', 'TRUCK', 'LOAD'));

-- ============================================================================
-- 6. Standardize Goods Received Notes
-- ============================================================================

ALTER TABLE goods_received_notes 
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(), -- BaseEntity uses created_at
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- Map existing received_date to created_at if appropriate
UPDATE goods_received_notes SET created_at = received_date WHERE created_at IS NULL;

-- ============================================================================
-- 7. Standardize Inventory Stock
-- ============================================================================

ALTER TABLE inventory_stock 
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- Map last_updated to updated_at
UPDATE inventory_stock SET updated_at = last_updated WHERE updated_at IS NULL;
ALTER TABLE inventory_stock DROP COLUMN IF EXISTS last_updated;

-- ============================================================================
-- 8. Standardize Stock Adjustments
-- ============================================================================

DO $$ 
BEGIN 
    -- Rename if old exists and new doesn't
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='stock_adjustments' AND column_name='adjusted_by_id') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='stock_adjustments' AND column_name='created_by_user_id') THEN
        ALTER TABLE stock_adjustments RENAME COLUMN adjusted_by_id TO created_by_user_id;
    -- If both exist, migrate data and drop old
    ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='stock_adjustments' AND column_name='adjusted_by_id') 
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='stock_adjustments' AND column_name='created_by_user_id') THEN
        UPDATE stock_adjustments SET created_by_user_id = adjusted_by_id WHERE created_by_user_id IS NULL;
        ALTER TABLE stock_adjustments DROP COLUMN adjusted_by_id;
    END IF;
END $$;

ALTER TABLE stock_adjustments 
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- Map adjusted_at to created_at and updated_at
DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='stock_adjustments' AND column_name='adjusted_at') THEN
        UPDATE stock_adjustments SET created_at = adjusted_at, updated_at = adjusted_at WHERE created_at IS NULL;
        ALTER TABLE stock_adjustments DROP COLUMN adjusted_at;
    END IF;
END $$;

-- ============================================================================
-- 9. Standardize Subcontract Work Orders
-- ============================================================================

DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='subcontract_work_orders' AND column_name='created_by_id') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='subcontract_work_orders' AND column_name='created_by_user_id') THEN
        ALTER TABLE subcontract_work_orders RENAME COLUMN created_by_id TO created_by_user_id;
    ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='subcontract_work_orders' AND column_name='created_by_id') 
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='subcontract_work_orders' AND column_name='created_by_user_id') THEN
        UPDATE subcontract_work_orders SET created_by_user_id = created_by_id WHERE created_by_user_id IS NULL;
        ALTER TABLE subcontract_work_orders DROP COLUMN created_by_id;
    END IF;
END $$;

ALTER TABLE subcontract_work_orders
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- ============================================================================
-- 10. Standardize Vendor Payments
-- ============================================================================

DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vendor_payments' AND column_name='paid_by_id') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vendor_payments' AND column_name='created_by_user_id') THEN
        ALTER TABLE vendor_payments RENAME COLUMN paid_by_id TO created_by_user_id;
    ELSIF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vendor_payments' AND column_name='paid_by_id') 
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vendor_payments' AND column_name='created_by_user_id') THEN
        UPDATE vendor_payments SET created_by_user_id = paid_by_id WHERE created_by_user_id IS NULL;
        ALTER TABLE vendor_payments DROP COLUMN paid_by_id;
    END IF;
END $$;

ALTER TABLE vendor_payments 
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- ============================================================================
-- Indexes for Audit Trails
-- ============================================================================

CREATE INDEX idx_vendors_deleted_at ON vendors(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_materials_deleted_at ON materials(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_purchase_orders_deleted_at ON purchase_orders(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_grn_deleted_at ON goods_received_notes(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_stock_deleted_at ON inventory_stock(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_stock_adjustments_deleted_at ON stock_adjustments(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_subcontract_wo_deleted_at ON subcontract_work_orders(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_vendor_payments_deleted_at ON vendor_payments(deleted_at) WHERE deleted_at IS NULL;

-- ============================================================================
-- Migration Complete
-- ============================================================================
