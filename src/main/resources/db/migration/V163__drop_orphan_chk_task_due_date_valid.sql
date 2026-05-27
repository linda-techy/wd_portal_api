-- ============================================================================
-- V163: Drop the orphan chk_task_due_date_valid constraint on tasks
-- ============================================================================
-- The shared DB carries  CHECK (due_date >= created_at::date)  on tasks, but this
-- constraint is defined in NO migration (untracked schema drift). It is also
-- semantically wrong: it forbids creating a task whose due_date is earlier than
-- its creation timestamp, which blocks legitimate cases —
--   * recording already-COMPLETED work,
--   * backfilling / importing a historical schedule,
--   * any task that is genuinely already due.
-- Result: POST /api/tasks 500s ("violates check constraint chk_task_due_date_valid")
-- for any past due_date. The application validates task dates in code and does not
-- rely on this DB constraint.
--
-- Drop it (idempotent). We deliberately do NOT re-add a date invariant here — the
-- meaningful one (end_date >= start_date) is enforced in the service layer, and
-- adding a DB CHECK risks rejecting pre-existing rows.
-- ============================================================================

ALTER TABLE tasks DROP CONSTRAINT IF EXISTS chk_task_due_date_valid;
