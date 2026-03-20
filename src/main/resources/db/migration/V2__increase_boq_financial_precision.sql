-- ============================================================================
-- V2: Increase BOQ Financial Precision to Industry Standard (18,6)
-- Rationale: (15,4) can overflow for large-scale construction projects with
-- high unit rates. (18,6) is the international construction industry standard.
-- ============================================================================

ALTER TABLE boq_items ALTER COLUMN quantity TYPE NUMERIC(18,6);
ALTER TABLE boq_items ALTER COLUMN unit_rate TYPE NUMERIC(18,6);
ALTER TABLE boq_items ALTER COLUMN total_amount TYPE NUMERIC(18,6);
ALTER TABLE boq_items ALTER COLUMN executed_quantity TYPE NUMERIC(18,6);
ALTER TABLE boq_items ALTER COLUMN billed_quantity TYPE NUMERIC(18,6);

COMMENT ON COLUMN boq_items.quantity IS 'NUMERIC(18,6) — international construction industry standard precision';
COMMENT ON COLUMN boq_items.unit_rate IS 'NUMERIC(18,6) — international construction industry standard precision';
COMMENT ON COLUMN boq_items.total_amount IS 'NUMERIC(18,6) — international construction industry standard precision';
COMMENT ON COLUMN boq_items.executed_quantity IS 'NUMERIC(18,6) — international construction industry standard precision';
COMMENT ON COLUMN boq_items.billed_quantity IS 'NUMERIC(18,6) — international construction industry standard precision';
