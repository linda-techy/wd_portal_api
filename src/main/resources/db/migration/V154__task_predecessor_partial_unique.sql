-- ===========================================================================
-- V154 — Make task_predecessor uniqueness honour soft-delete.
--
-- The table was created by V112 with a plain table-level UNIQUE constraint:
--     CONSTRAINT uq_task_predecessor_pair UNIQUE (successor_id, predecessor_id)
-- but the entity uses @SQLDelete (soft-delete via deleted_at). So when a row
-- is soft-deleted, Postgres still treats the (successor, predecessor) pair as
-- present in the unique index — every subsequent INSERT of the same pair
-- (e.g. the very common "change a predecessor's lag-days" flow which is
-- implemented as delete+insert by TaskPredecessorService.replacePredecessors)
-- fails with:
--   ERROR: duplicate key value violates unique constraint "uq_task_predecessor_pair"
--          Key (successor_id, predecessor_id)=(5, 4) already exists.
--
-- Fix: drop the full-table constraint and replace it with a PARTIAL unique
-- index that only applies to rows where deleted_at IS NULL. Same semantics for
-- live data, no longer collides with the soft-delete tombstones.
--
-- Idempotent: DROP CONSTRAINT IF EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS.
-- ===========================================================================

ALTER TABLE task_predecessor
    DROP CONSTRAINT IF EXISTS uq_task_predecessor_pair;

CREATE UNIQUE INDEX IF NOT EXISTS uq_task_predecessor_pair_live
    ON task_predecessor (successor_id, predecessor_id)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX uq_task_predecessor_pair_live IS
    'Partial unique index — enforces one live (deleted_at IS NULL) edge per (successor, predecessor) pair, leaving soft-deleted tombstones alone.';
