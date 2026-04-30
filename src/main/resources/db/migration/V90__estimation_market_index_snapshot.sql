-- ===========================================================================
-- V90 — estimation_market_index_snapshot
--
-- Append-only history of commodity prices + the weighted composite index.
-- Only one row is is_active=true at a time (partial unique index).
-- Per spec §1.1 row 3.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation_market_index_snapshot (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_date        DATE            NOT NULL,
    steel_rate           NUMERIC(10, 2)  NOT NULL,
    cement_rate          NUMERIC(10, 2)  NOT NULL,
    sand_rate            NUMERIC(10, 2)  NOT NULL,
    aggregate_rate       NUMERIC(10, 2)  NOT NULL,
    tiles_rate           NUMERIC(10, 2)  NOT NULL,
    electrical_rate      NUMERIC(10, 2)  NOT NULL,
    paints_rate          NUMERIC(10, 2)  NOT NULL,
    weights_json         JSONB           NOT NULL,
    composite_index      NUMERIC(6, 4)   NOT NULL,
    is_active            BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT          NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_estimation_market_index_active
    ON estimation_market_index_snapshot (is_active)
    WHERE is_active;

COMMENT ON TABLE estimation_market_index_snapshot IS
    'Commodity prices + computed weighted composite_index. Only one row is_active at a time. Pinned by estimation.market_index_id at quote time.';
