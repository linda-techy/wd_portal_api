-- Create tasks table for PostgreSQL
CREATE TABLE IF NOT EXISTS tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    assigned_to BIGINT,
    created_by BIGINT,
    project_id BIGINT,
    due_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_task_assigned_to FOREIGN KEY (assigned_to) REFERENCES portal_users(id) ON DELETE SET NULL,
    CONSTRAINT fk_task_created_by FOREIGN KEY (created_by) REFERENCES portal_users(id) ON DELETE SET NULL,
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_task_assigned_to ON tasks(assigned_to);
CREATE INDEX IF NOT EXISTS idx_task_created_by ON tasks(created_by);
CREATE INDEX IF NOT EXISTS idx_task_project_id ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_task_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_task_due_date ON tasks(due_date);

-- Add trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tasks_updated_at BEFORE UPDATE ON tasks
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
