-- ===========================================================================
-- V96 — estimation_govt_fee
--
-- Government fees: building permit, electricity/water connection, plinth and
-- occupancy certificates. Always lump-sum. Per spec §1.1 row 9.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation_govt_fee (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(150)    NOT NULL UNIQUE,
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
