-- V9: Task Assignment History Tracking
-- Tracks all task assignments for audit trail and UI display

-- Create task_assignment_history table
CREATE TABLE IF NOT EXISTS task_assignment_history (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    assigned_from_id BIGINT REFERENCES portal_users(id) ON DELETE SET NULL,
    assigned_to_id BIGINT REFERENCES portal_users(id) ON DELETE SET NULL,
    assigned_by_id BIGINT NOT NULL REFERENCES portal_users(id),
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    
    CONSTRAINT fk_task_assignment_task FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_task_assignment_from FOREIGN KEY (assigned_from_id) REFERENCES portal_users(id),
    CONSTRAINT fk_task_assignment_to FOREIGN KEY (assigned_to_id) REFERENCES portal_users(id),
    CONSTRAINT fk_task_assignment_by FOREIGN KEY (assigned_by_id) REFERENCES portal_users(id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_task_assignment_history_task 
ON task_assignment_history(task_id);

CREATE INDEX IF NOT EXISTS idx_task_assignment_history_assigned_at 
ON task_assignment_history(assigned_at DESC);

CREATE INDEX IF NOT EXISTS idx_task_assignment_history_assigned_to 
ON task_assignment_history(assigned_to_id);

-- Add project_manager_id to customer_projects if not exists (for project-level permissions)
-- First check if column exists to make migration idempotent
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'customer_projects' 
        AND column_name = 'project_manager_id'
    ) THEN
        ALTER TABLE customer_projects
        ADD COLUMN project_manager_id BIGINT REFERENCES portal_users(id);
    END IF;
END $$;

-- Create index for project manager lookups
CREATE INDEX IF NOT EXISTS idx_customer_projects_manager 
ON customer_projects(project_manager_id);

-- Add comments for documentation
COMMENT ON TABLE task_assignment_history IS 'Audit trail for all task assignments - who assigned what to whom and when';
COMMENT ON COLUMN task_assignment_history.assigned_from_id IS 'Previous assignee (NULL if task was unassigned)';
COMMENT ON COLUMN task_assignment_history.assigned_to_id IS 'New assignee (NULL if task is being unassigned)';
COMMENT ON COLUMN task_assignment_history.assigned_by_id IS 'User who made the assignment change';
COMMENT ON COLUMN task_assignment_history.notes IS 'Optional notes about why assignment changed';
COMMENT ON COLUMN customer_projects.project_manager_id IS 'Project manager who can edit all tasks in this project';
