-- ===========================================================================
-- V91 — estimation_customisation_category
--
-- Categories like Flooring, Walls, Kitchen, etc. Per spec §1.1 row 4 / TDD §4.1.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation_customisation_category (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(100) NOT NULL UNIQUE,
    pricing_mode         VARCHAR(20)  NOT NULL,
    display_order        INTEGER      NOT NULL DEFAULT 0,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT       NOT NULL DEFAULT 1
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estimation_customisation_category_pricing_mode') THEN
        ALTER TABLE estimation_customisation_category
            ADD CONSTRAINT chk_estimation_customisation_category_pricing_mode
            CHECK (pricing_mode IN ('PER_SQFT', 'PER_UNIT', 'PER_RFT'));
    END IF;
END $$;
