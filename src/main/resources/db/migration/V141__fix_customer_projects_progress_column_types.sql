-- ===========================================================================
-- V141 — Coerce customer_projects progress/weight columns to NUMERIC
--
-- Production was provisioned before the V1_50 → V1 baseline squash. The
-- progress and weight columns landed as DOUBLE PRECISION (float8) on that
-- older path, while the JPA entity declares them as NUMERIC(5,2) / NUMERIC(3,2).
-- That mismatch trips Hibernate schema-validation on startup:
--   "wrong column type ... found [float8], but expecting [numeric(5,2)]"
--
-- Cast is value-preserving: progress is 0–100 and weights are 0.0–1.0,
-- both well inside the target precision.
--
-- Idempotent: ALTER COLUMN ... TYPE T USING x::T is a no-op when the
-- column already has type T (already true on dev), so this is safe to run
-- against any environment.
-- ===========================================================================

ALTER TABLE customer_projects
    ALTER COLUMN overall_progress   TYPE NUMERIC(5,2) USING overall_progress::NUMERIC(5,2),
    ALTER COLUMN milestone_progress TYPE NUMERIC(5,2) USING milestone_progress::NUMERIC(5,2),
    ALTER COLUMN task_progress      TYPE NUMERIC(5,2) USING task_progress::NUMERIC(5,2),
    ALTER COLUMN budget_progress    TYPE NUMERIC(5,2) USING budget_progress::NUMERIC(5,2),
    ALTER COLUMN milestone_weight   TYPE NUMERIC(3,2) USING milestone_weight::NUMERIC(3,2),
    ALTER COLUMN task_weight        TYPE NUMERIC(3,2) USING task_weight::NUMERIC(3,2),
    ALTER COLUMN budget_weight      TYPE NUMERIC(3,2) USING budget_weight::NUMERIC(3,2);
