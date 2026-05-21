-- ===========================================================================
-- V153 — Drop orphaned `calculate_project_progress` trigger & function.
--
-- Background:
--   Pre-V1 (the V1.50 baseline squash), some environments had a Postgres
--   trigger `trigger_update_project_progress` on the `tasks` table that
--   called a PL/pgSQL function `calculate_project_progress(bigint)`. That
--   function referenced a column `pt.project_id` (a join alias `pt`)
--   that no longer exists in the post-V141 schema — Postgres now hints
--   "Perhaps you meant to reference the column 'p.project_uuid'".
--
--   The trigger is NOT defined by any committed Flyway migration in this
--   repo — search proves it (grep for `calculate_project_progress` returns
--   zero hits across src/main/resources/db/migration/). It's orphan code
--   from an out-of-tree manual SQL apply.
--
--   Effect: EVERY INSERT INTO tasks fails with `column pt.project_id does
--   not exist`, blocking:
--     • POST /api/tasks (manual create / Gantt FAB)
--     • POST /api/projects/{id}/wbs/clone-from-template
--     • Change-request merge
--     • Any other task-creation path
--
-- What replaces it:
--   Project progress aggregation is handled in application code by
--   ProjectAggregationService + ProjectProgressService — the trigger has no
--   load-bearing role.
--
-- Idempotency:
--   DROP ... IF EXISTS is a no-op on environments that never had the
--   orphan. Safe to apply everywhere (dev, test, staging, prod).
-- ===========================================================================

-- CASCADE because the same `trigger_update_project_progress()` function is
-- wired to triggers on multiple tables (tasks, project_milestones, and
-- likely customer_projects too). CASCADE drops all dependent triggers in
-- one go — which is exactly what we want, since they ALL call the same
-- broken function. Application code (ProjectAggregationService /
-- ProjectProgressService) recomputes progress without DB triggers.

DROP FUNCTION IF EXISTS trigger_update_project_progress() CASCADE;
DROP FUNCTION IF EXISTS calculate_project_progress(bigint) CASCADE;
DROP FUNCTION IF EXISTS calculate_project_progress(int8) CASCADE;
