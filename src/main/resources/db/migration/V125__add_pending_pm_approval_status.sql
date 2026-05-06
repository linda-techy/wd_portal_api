-- ===========================================================================
-- V125 — S3 PR2 PENDING_PM_APPROVAL TaskStatus + completion-approval permission
--
-- Drops + recreates tasks_status_chk to admit the new PENDING_PM_APPROVAL
-- value alongside the existing PENDING / IN_PROGRESS / COMPLETED / CANCELLED.
-- (No DONE — the production enum has never had it; the parent spec text
-- mentioning DONE is a copy-paste artefact from a sibling table.)
--
-- Adds nullable rejection_reason VARCHAR(500) so PMs can record why they
-- bounced a task back to IN_PROGRESS.
--
-- Seeds TASK_COMPLETION_APPROVE permission and grants it to ADMIN +
-- PROJECT_MANAGER. Mirrors V117 / V120 grant pattern (INNER JOIN drops
-- unseeded role codes silently). Idempotent via ON CONFLICT DO NOTHING.
-- ===========================================================================

-- 1) status CHECK constraint
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_status_chk;
ALTER TABLE tasks ADD CONSTRAINT tasks_status_chk
    CHECK (status IN (
        'PENDING',
        'IN_PROGRESS',
        'PENDING_PM_APPROVAL',
        'COMPLETED',
        'CANCELLED'
    ));

-- 2) rejection_reason column
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500) NULL;

-- 3) permission row
INSERT INTO portal_permissions (name, description) VALUES
    ('TASK_COMPLETION_APPROVE',
     'Approve or reject site engineer task-completion submissions')
ON CONFLICT (name) DO NOTHING;

-- 4) grants
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM portal_roles r
JOIN portal_permissions p ON TRUE
JOIN (VALUES
    ('ADMIN',           'TASK_COMPLETION_APPROVE'),
    ('PROJECT_MANAGER', 'TASK_COMPLETION_APPROVE')
) AS grants(role_code, permission_name)
     ON r.code = grants.role_code AND p.name = grants.permission_name
ON CONFLICT (role_id, permission_id) DO NOTHING;
