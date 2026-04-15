-- =============================================================================
-- V25: Extend change_orders with VO classification/approval fields
--      Extend payment_stages with certification/retention fields
-- =============================================================================

-- ---- change_orders (created in V21) -----------------------------------------
-- vo_category      — cost-split category (drives advance/progress/completion %)
-- revises_co_id    — REVISION type: FK to the CO being superseded
-- scope_notes      — detailed scope text
-- mapped_stage_ids — JSON array of payment_stage IDs this VO spans
-- approved_cost    — final approved amount (separate from net_amount_incl_gst)
-- advance_collected — has the advance invoice been paid?
ALTER TABLE change_orders
    ADD COLUMN IF NOT EXISTS vo_category        VARCHAR(30)
        CONSTRAINT chk_co_vo_category CHECK (
            vo_category IS NULL OR vo_category IN (
                'MATERIAL_HEAVY','LABOUR_HEAVY','MIXED','CUSTOM'
            )
        ),
    ADD COLUMN IF NOT EXISTS revises_co_id      BIGINT REFERENCES change_orders(id),
    ADD COLUMN IF NOT EXISTS scope_notes        TEXT,
    ADD COLUMN IF NOT EXISTS mapped_stage_ids   JSONB,
    ADD COLUMN IF NOT EXISTS approved_cost      NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS advance_collected  BOOLEAN NOT NULL DEFAULT FALSE;

-- ---- payment_stages (created in V20) ----------------------------------------
-- certified_by   — name of the PM/Director who certified the stage
-- retention_pct  — % held as retention (default 5%)
-- retention_held — computed retention amount
-- certified_at   — timestamp of certification
ALTER TABLE payment_stages
    ADD COLUMN IF NOT EXISTS certified_by    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS retention_pct   NUMERIC(5,4) NOT NULL DEFAULT 0.0500,
    ADD COLUMN IF NOT EXISTS retention_held  NUMERIC(18,6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS certified_at    TIMESTAMP;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_co_vo_category
    ON change_orders(vo_category) WHERE vo_category IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_co_revises
    ON change_orders(revises_co_id) WHERE revises_co_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ps_certified_at
    ON payment_stages(certified_at) WHERE certified_at IS NOT NULL;
