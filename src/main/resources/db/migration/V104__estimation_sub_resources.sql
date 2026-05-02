-- ===========================================================================
-- V104 — Estimation sub-resources (Sub-project E)
--
-- Inclusions / Exclusions / Assumptions / Payment Milestones — the contract-
-- shaped content that turns a pure-numeric estimation into a customer-facing
-- quotation.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation_inclusion (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    estimation_id        UUID            NOT NULL REFERENCES estimation(id) ON DELETE CASCADE,
    label                VARCHAR(200)    NOT NULL,
    description          TEXT,
    display_order        INTEGER         NOT NULL DEFAULT 0,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT          NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS estimation_exclusion (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    estimation_id        UUID            NOT NULL REFERENCES estimation(id) ON DELETE CASCADE,
    label                VARCHAR(200)    NOT NULL,
    description          TEXT,
    display_order        INTEGER         NOT NULL DEFAULT 0,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT          NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS estimation_assumption (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    estimation_id        UUID            NOT NULL REFERENCES estimation(id) ON DELETE CASCADE,
    label                VARCHAR(200)    NOT NULL,
    description          TEXT,
    display_order        INTEGER         NOT NULL DEFAULT 0,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT          NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS estimation_payment_milestone (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    estimation_id        UUID            NOT NULL REFERENCES estimation(id) ON DELETE CASCADE,
    label                VARCHAR(200)    NOT NULL,
    percentage           NUMERIC(5, 2)   NOT NULL CHECK (percentage > 0 AND percentage <= 100),
    description          TEXT,
    display_order        INTEGER         NOT NULL DEFAULT 0,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT          NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_est_incl_estimation ON estimation_inclusion (estimation_id);
CREATE INDEX IF NOT EXISTS idx_est_excl_estimation ON estimation_exclusion (estimation_id);
CREATE INDEX IF NOT EXISTS idx_est_assm_estimation ON estimation_assumption (estimation_id);
CREATE INDEX IF NOT EXISTS idx_est_milestone_estimation ON estimation_payment_milestone (estimation_id);
