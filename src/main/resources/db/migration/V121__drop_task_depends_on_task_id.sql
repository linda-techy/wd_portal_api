-- ===========================================================================
-- V121 — Drop legacy task.depends_on_task_id column.
--
-- S1 PR1 introduced task_predecessor with a dual-write shim that kept
-- this column in sync with the join table. After one production
-- release of S1 PR1 we drop the legacy column. The join table is the
-- canonical source of predecessor edges from this point on.
--
-- This migration:
--   * drops the column
--   * does NOT touch task_predecessor (the join table holds all the data)
--
-- Pre-merge gate: confirmed S1 PR1 has been live in production for
-- at least one release. If you are reading this and S1 PR1 has not
-- yet soaked, REVERT this migration and the corresponding code
-- changes in TaskPredecessorService / GanttController / Task.java
-- and ship them in a trailing PR after the soak.
-- ===========================================================================

ALTER TABLE tasks DROP COLUMN IF EXISTS depends_on_task_id;
