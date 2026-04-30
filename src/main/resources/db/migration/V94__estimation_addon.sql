-- ===========================================================================
-- V94 — estimation_addon
--
-- Lift, solar, smart home, landscaping, pool, etc. Lump-sum priced.
-- Per spec §1.1 row 7.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation_addon (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(150)    NOT NULL UNIQUE,
    description          TEXT,
    lump_amount          NUMERIC(14, 2)  NOT NULL,
    is_active            BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT          NOT NULL DEFAULT 1
);
