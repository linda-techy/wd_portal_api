-- ===========================================================================
-- V97 — estimation
--
-- The aggregate root. Pins rate_version_id and market_index_id at quote time
-- so the calculation is reproducible. References leads(lead_id) — column name
-- 'lead_id' on the leads table per Lead.java.
-- Per spec §1.1 row 10.
-- Immutability trigger arrives in V100 (Task 4.1).
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    estimation_no        VARCHAR(30)     NOT NULL UNIQUE,
    lead_id              BIGINT          NOT NULL REFERENCES leads(lead_id),
    project_type         VARCHAR(20)     NOT NULL,
    package_id           UUID            REFERENCES estimation_package(id),
    rate_version_id      UUID            REFERENCES estimation_package_rate_version(id),
    market_index_id      UUID            REFERENCES estimation_market_index_snapshot(id),
    dimensions_json      JSONB           NOT NULL,
    status               VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    subtotal             NUMERIC(14, 2),
    discount_amount      NUMERIC(14, 2),
    gst_amount           NUMERIC(14, 2),
    grand_total          NUMERIC(14, 2),
    valid_until          DATE,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estimation_project_type') THEN
        ALTER TABLE estimation
            ADD CONSTRAINT chk_estimation_project_type
            CHECK (project_type IN ('NEW_BUILD', 'COMMERCIAL', 'RENOVATION', 'INTERIOR', 'COMPOUND'));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estimation_status') THEN
        ALTER TABLE estimation
            ADD CONSTRAINT chk_estimation_status
            CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SENT', 'ACCEPTED', 'REJECTED', 'EXPIRED'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_estimation_lead_id     ON estimation (lead_id);
CREATE INDEX IF NOT EXISTS idx_estimation_status      ON estimation (status);
CREATE INDEX IF NOT EXISTS idx_estimation_created_at  ON estimation (created_at);

COMMENT ON TABLE estimation IS
    'Quote draft/parent record. Pins rate_version_id and market_index_id so historical quotes reproduce exactly. Immutable once status=ACCEPTED (see V100 trigger).';
