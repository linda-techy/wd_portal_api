-- V53: Add Gantt/scheduling fields to tasks table
-- Supports project timeline visualization and dependency tracking

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS end_date DATE;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS depends_on_task_id BIGINT REFERENCES tasks(id);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS progress_percent INTEGER DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_tasks_project_schedule ON tasks(project_id, start_date);
