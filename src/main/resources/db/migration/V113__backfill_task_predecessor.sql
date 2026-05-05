-- V113: backfill task_predecessor from existing single-predecessor column.
-- Skips rows where:
--   - depends_on_task_id is NULL (no predecessor)
--   - depends_on_task_id points to a non-existent task (dangling FK; would have
--     been rejected here anyway, but we'd rather log + skip than error out)
--   - depends_on_task_id == id (self-loop in legacy data)
-- Uses LEFT JOIN to filter; PostgreSQL's INSERT ... SELECT silently skips
-- nothing on no-row-match, so dangling rows are dropped by the join.
-- ON CONFLICT (successor_id, predecessor_id) DO NOTHING is defensive in case
-- the table is non-empty when this migration runs (it shouldn't be, but
-- backfill should be idempotent if re-run after a partial failure).

DO $$
DECLARE
    total_candidates INT;
    backfilled       INT;
    skipped_dangling INT;
    skipped_self     INT;
BEGIN
    SELECT COUNT(*) INTO total_candidates
      FROM tasks
     WHERE depends_on_task_id IS NOT NULL;

    INSERT INTO task_predecessor (
        successor_id, predecessor_id, lag_days, dep_type, version
    )
    SELECT t.id, t.depends_on_task_id, 0, 'FS', 1
      FROM tasks t
      JOIN tasks p ON p.id = t.depends_on_task_id
     WHERE t.depends_on_task_id IS NOT NULL
       AND t.depends_on_task_id <> t.id
    ON CONFLICT (successor_id, predecessor_id) DO NOTHING;

    GET DIAGNOSTICS backfilled = ROW_COUNT;

    SELECT COUNT(*) INTO skipped_self
      FROM tasks
     WHERE depends_on_task_id = id;

    skipped_dangling := total_candidates - backfilled - skipped_self;

    RAISE NOTICE 'V113 backfill_task_predecessor: candidates=%, backfilled=%, skipped_self=%, skipped_dangling=%',
        total_candidates, backfilled, skipped_self, skipped_dangling;
END $$;
