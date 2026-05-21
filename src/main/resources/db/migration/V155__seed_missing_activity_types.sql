-- ===========================================================================
-- V155 — Seed missing activity_types rows.
--
-- TaskProgressUpdateService.updateProgress logs every task-status transition
-- via ActivityFeedService.logProjectActivity("TASK_STATUS_CHANGED", ...). But
-- no migration ever inserted that row into activity_types, so the call
-- throws RuntimeException("Activity Type not found: TASK_STATUS_CHANGED")
-- AFTER the task save and CPM recompute. The wrapping @Transactional rolls
-- back the entire update, including the just-saved progressPercent and
-- status — meaning the Gantt slider has been silently broken for every
-- status transition since this code shipped.
--
-- Seeding the row repairs the path without changing the calling code.
-- Idempotent via NOT EXISTS — safe to apply on any environment.
-- ===========================================================================

INSERT INTO activity_types (name, description)
SELECT 'TASK_STATUS_CHANGED', 'Task status auto-derived from progress update'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'TASK_STATUS_CHANGED');
