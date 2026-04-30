-- ===========================================================================
-- V98 — estimation_line_item
--
-- Per-line breakdown for an estimation. ON DELETE CASCADE — line items live
-- and die with their parent estimation. Per spec §1.1 row 11.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation_line_item (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    estimation_id        UUID            NOT NULL REFERENCES estimation(id) ON DELETE CASCADE,
    line_type            VARCHAR(30)     NOT NULL,
    description          VARCHAR(255)    NOT NULL,
    source_ref_id        UUID,
    quantity             NUMERIC(12, 2),
    unit                 VARCHAR(20),
    unit_rate            NUMERIC(10, 2),
    amount               NUMERIC(14, 2)  NOT NULL,
    display_order        INTEGER         NOT NULL,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT          NOT NULL DEFAULT 1
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estimation_line_item_line_type') THEN
        ALTER TABLE estimation_line_item
            ADD CONSTRAINT chk_estimation_line_item_line_type
            CHECK (line_type IN ('BASE', 'CUSTOMISATION', 'SITE', 'ADDON', 'FLUCTUATION', 'FEE', 'DISCOUNT', 'GST'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_estimation_line_item_estimation
    ON estimation_line_item (estimation_id);
