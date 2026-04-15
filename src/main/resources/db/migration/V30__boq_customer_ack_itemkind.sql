-- =============================================================================
-- V30: BOQ customer acknowledgement + itemKind check constraint
-- =============================================================================

-- 1. Customer acknowledgement columns on boq_documents
ALTER TABLE boq_documents
    ADD COLUMN IF NOT EXISTS customer_acknowledged_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS customer_acknowledged_by  BIGINT REFERENCES customer_users(id);

CREATE INDEX IF NOT EXISTS idx_boq_doc_acknowledged
    ON boq_documents(customer_acknowledged_by)
    WHERE customer_acknowledged_by IS NOT NULL;

-- 2. Normalise any legacy free-text item_kind values before constraining
UPDATE boq_items
SET item_kind = 'BASE'
WHERE item_kind IS NULL
   OR item_kind NOT IN ('BASE', 'ADDON', 'OPTIONAL', 'EXCLUSION');

