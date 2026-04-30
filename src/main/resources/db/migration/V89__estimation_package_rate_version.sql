-- ===========================================================================
-- V89 — estimation_package_rate_version
--
-- Versioned three-component rates per package, per project_type. Append-only:
-- a rate change inserts a new row with effective_from = today; the previous
-- row's effective_to is set to (today - 1).
-- Per spec §1.1 row 2.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation_package_rate_version (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id           UUID            NOT NULL REFERENCES estimation_package(id),
    project_type         VARCHAR(20)     NOT NULL,
    material_rate        NUMERIC(10, 2)  NOT NULL,
    labour_rate          NUMERIC(10, 2)  NOT NULL,
    overhead_rate        NUMERIC(10, 2)  NOT NULL,
    effective_from       DATE            NOT NULL,
    effective_to         DATE,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estimation_package_rate_version_project_type') THEN
        ALTER TABLE estimation_package_rate_version
            ADD CONSTRAINT chk_estimation_package_rate_version_project_type
            CHECK (project_type IN ('NEW_BUILD', 'COMMERCIAL'));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estimation_prv_dates') THEN
        ALTER TABLE estimation_package_rate_version
            ADD CONSTRAINT chk_estimation_prv_dates
            CHECK (effective_to IS NULL OR effective_to > effective_from);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_estimation_prv_lookup
    ON estimation_package_rate_version (package_id, project_type, effective_from);

COMMENT ON TABLE estimation_package_rate_version IS
    'Append-only history of per-package per-project-type rates. Pinned by estimation.rate_version_id at quote time.';
