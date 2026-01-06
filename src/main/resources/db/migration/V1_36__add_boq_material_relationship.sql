-- V1_36: Add optional material relationship to BOQ items for cost tracking
-- This enables linking BOQ line items to material masters for:
-- 1. Accurate cost estimation
-- 2. Material consumption tracking
-- 3. Variance analysis (budgeted vs actual)

ALTER TABLE boq_items 
ADD COLUMN IF NOT EXISTS material_id BIGINT;

-- Add foreign key constraint
ALTER TABLE boq_items 
ADD CONSTRAINT fk_boq_items_material 
FOREIGN KEY (material_id) REFERENCES materials(id) ON DELETE SET NULL;

-- Index for efficient lookups
CREATE INDEX IF NOT EXISTS idx_boq_items_material ON boq_items(material_id) WHERE material_id IS NOT NULL;

COMMENT ON COLUMN boq_items.material_id IS 'Optional link to materials table for cost tracking. Nullable as not all BOQ items may be material-based (e.g., labor items).';
