-- V109__estimation_is_current.sql
-- Adds the per-lead "current estimation" invariant. Exactly one non-deleted
-- estimation per lead may carry is_current = true.

ALTER TABLE estimation
    ADD COLUMN is_current BOOLEAN NOT NULL DEFAULT false;

-- Backfill: for each lead, mark the most recent non-deleted estimation as current.
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY lead_id ORDER BY created_at DESC) AS rn
    FROM estimation
    WHERE deleted_at IS NULL
)
UPDATE estimation e SET is_current = true
FROM ranked r
WHERE e.id = r.id AND r.rn = 1;

-- Partial unique index: enforces at most one current estimation per lead among
-- non-deleted rows. Soft-deleted rows are excluded so the index doesn't bloat.
CREATE UNIQUE INDEX estimation_one_current_per_lead_idx
ON estimation (lead_id)
WHERE is_current = true AND deleted_at IS NULL;
