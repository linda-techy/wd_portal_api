-- ===========================================================================
-- V124 — S3 PR2 per-project PM-approval toggle
--
-- requires_pm_approval=false (default) means TaskCompletionService.markComplete
-- transitions IN_PROGRESS → COMPLETED in one shot. requires_pm_approval=true
-- routes through PENDING_PM_APPROVAL until a PM with TASK_COMPLETION_APPROVE
-- calls /approve-completion or /reject-completion.
-- ===========================================================================

ALTER TABLE project_schedule_config
    ADD COLUMN IF NOT EXISTS requires_pm_approval BOOLEAN NOT NULL DEFAULT FALSE;
