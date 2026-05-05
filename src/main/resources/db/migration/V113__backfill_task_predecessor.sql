-- V113: backfill task_predecessor from existing single-predecessor column.
-- Skips rows where:
--   - depends_on_task_id is NULL (no predecessor)
--   - depends_on_task_id points to a non-existent task (dangling FK; would have
--     been rejected here anyway, but we'd rather log + skip than error out)
--   - depends_on_task_id == id (self-loop in legacy data)
-- Uses INNER JOIN tasks p to filter danglers; PostgreSQL's INSERT ... SELECT
-- silently skips nothing on no-row-match, so dangling rows are dropped by the
-- join.
-- ON CONFLICT (successor_id, predecessor_id) DO NOTHING is defensive in case
-- the table is non-empty when this migration runs (it shouldn't be, but
-- backfill should be idempotent if re-run after a partial failure).
--
-- The RAISE NOTICE counters below are diagnostic-only — they help operators
-- spot-check the migration on first run, but should NOT be parsed by
-- monitoring. On an idempotent re-run "backfilled" will read 0 because
-- ON CONFLICT skipped the already-present rows; that's expected, not a bug.

DO $$
DECLARE
    total_candidates INT;
    backfilled       INT;
    skipped_dangling INT;
    skipped_self     INT;
BEGIN
    -- Count of all rows with a predecessor pointer set (regardless of
    -- whether the pointer is valid, self-loop, or dangling).
    SELECT COUNT(*) INTO total_candidates
      FROM tasks
     WHERE depends_on_task_id IS NOT NULL;

    -- Count self-loops directly.
    SELECT COUNT(*) INTO skipped_self
      FROM tasks
     WHERE depends_on_task_id IS NOT NULL
       AND depends_on_task_id = id;

    -- Count dangling-FK rows directly (pre-INSERT) so the diagnostic stays
    -- accurate even on idempotent re-runs where ROW_COUNT below is 0.
    SELECT COUNT(*) INTO skipped_dangling
      FROM tasks t
      LEFT JOIN tasks p ON p.id = t.depends_on_task_id
     WHERE t.depends_on_task_id IS NOT NULL
       AND t.depends_on_task_id <> t.id
       AND p.id IS NULL;

    INSERT INTO task_predecessor (
        successor_id, predecessor_id, lag_days, dep_type, version
    )
    SELECT t.id, t.depends_on_task_id, 0, 'FS', 1
      FROM tasks t
      JOIN tasks p ON p.id = t.depends_on_task_id
     WHERE t.depends_on_task_id IS NOT NULL
       AND t.depends_on_task_id <> t.id
    ON CONFLICT (successor_id, predecessor_id) DO NOTHING;

    -- Rows actually inserted by THIS run. On a fresh run this equals
    -- (total_candidates - skipped_self - skipped_dangling). On re-run it
    -- is 0 because ON CONFLICT skipped the duplicates.
    GET DIAGNOSTICS backfilled = ROW_COUNT;

    RAISE NOTICE 'V113 backfill_task_predecessor: inserted % new rows; % candidates skipped via ON CONFLICT (already present); % dangling FK rows skipped; % self-loops skipped (total candidates: %)',
        backfilled,
        total_candidates - backfilled - skipped_self - skipped_dangling,
        skipped_dangling,
        skipped_self,
        total_candidates;
END $$;
