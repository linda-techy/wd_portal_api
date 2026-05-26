-- V160: Enforce dependency-type enum values on task_predecessor and
--        wbs_template_task_predecessor (audit P2-2).
--
-- Background:
--   dep_type was added as a free VARCHAR column with a Java-layer default of 'FS'.
--   This migration:
--     1. Backfills any NULL or blank rows to 'FS' (safe: the engine treats NULL as FS).
--     2. Adds a CHECK constraint so only the four valid values ('FS','SS','FF','SF')
--        are accepted at the DB layer, matching the DependencyType Java enum.
--
-- Both tables receive the same treatment.

-- ── task_predecessor ─────────────────────────────────────────────────────────

UPDATE task_predecessor
   SET dep_type = 'FS'
 WHERE dep_type IS NULL OR dep_type = '';

-- Idempotent: the shared wdTestDB already carried this constraint outside Flyway's
-- tracked history (v160 was never recorded as applied), so a bare ADD aborts with
-- 42710 ("constraint already exists"). Drop-if-exists first so the migration is
-- self-healing and re-asserts the intended definition.
ALTER TABLE task_predecessor
    DROP CONSTRAINT IF EXISTS chk_task_predecessor_dep_type;
ALTER TABLE task_predecessor
    ADD CONSTRAINT chk_task_predecessor_dep_type
        CHECK (dep_type IN ('FS', 'SS', 'FF', 'SF'));

-- ── wbs_template_task_predecessor ────────────────────────────────────────────

UPDATE wbs_template_task_predecessor
   SET dep_type = 'FS'
 WHERE dep_type IS NULL OR dep_type = '';

ALTER TABLE wbs_template_task_predecessor
    DROP CONSTRAINT IF EXISTS chk_wbs_tmpl_task_pred_dep_type;
ALTER TABLE wbs_template_task_predecessor
    ADD CONSTRAINT chk_wbs_tmpl_task_pred_dep_type
        CHECK (dep_type IN ('FS', 'SS', 'FF', 'SF'));
