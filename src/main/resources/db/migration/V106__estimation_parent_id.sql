-- ===========================================================================
-- V106 — parent_estimation_id for revisions (Sub-project I)
--
-- A NULLable self-FK on the estimation table. New estimations have
-- parent_estimation_id = NULL. Revising an existing estimation creates
-- a new row with parent_estimation_id = original.id.
-- ===========================================================================

ALTER TABLE estimation
    ADD COLUMN IF NOT EXISTS parent_estimation_id UUID
        REFERENCES estimation(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_estimation_parent
    ON estimation (parent_estimation_id)
    WHERE parent_estimation_id IS NOT NULL;
