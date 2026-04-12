-- =============================================================================
-- V24: Add item_kind to boq_items
-- =============================================================================
-- Distinguishes between scope types on a BOQ line item:
--   BASE        — always included in the contracted scope
--   ADDON       — optional extra charged on top of the base scope
--   OPTIONAL    — customer may select/deselect (similar to ADDON but not pre-priced)
--   EXCLUSION   — explicitly NOT included; listed for clarity only
--
-- Defaults to BASE so all existing rows are unaffected.
-- =============================================================================

ALTER TABLE boq_items
    ADD COLUMN IF NOT EXISTS item_kind VARCHAR(20) NOT NULL DEFAULT 'BASE';

ALTER TABLE boq_items
    ADD CONSTRAINT chk_boq_item_kind
        CHECK (item_kind IN ('BASE', 'ADDON', 'OPTIONAL', 'EXCLUSION'));

CREATE INDEX IF NOT EXISTS idx_boq_items_item_kind ON boq_items(item_kind);
