-- ===========================================================================
-- V95 — estimation_site_fee
--
-- Site-specific fees: soil-type surcharges, road-access factors, excavation, etc.
-- Either LUMP (fixed amount) or PER_SQFT (multiplied by chargeable area).
-- Per spec §1.1 row 8.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation_site_fee (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(150)    NOT NULL,
    mode                 VARCHAR(20)     NOT NULL,
    lump_amount          NUMERIC(14, 2),
    per_sqft_rate        NUMERIC(10, 2),
    is_active            BOOLEAN         NOT NULL DEFAULT TRUE,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estimation_site_fee_mode') THEN
        ALTER TABLE estimation_site_fee
            ADD CONSTRAINT chk_estimation_site_fee_mode
            CHECK (mode IN ('LUMP', 'PER_SQFT'));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estimation_site_fee_amount_xor') THEN
        ALTER TABLE estimation_site_fee
            ADD CONSTRAINT chk_estimation_site_fee_amount_xor
            CHECK (
                (mode = 'LUMP'     AND lump_amount   IS NOT NULL AND per_sqft_rate IS NULL) OR
                (mode = 'PER_SQFT' AND per_sqft_rate IS NOT NULL AND lump_amount   IS NULL)
            );
    END IF;
END $$;
