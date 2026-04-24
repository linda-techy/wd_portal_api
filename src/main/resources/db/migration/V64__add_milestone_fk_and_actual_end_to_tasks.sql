-- V64__add_milestone_fk_and_actual_end_to_tasks.sql
-- Adds the missing task -> milestone FK so the customer Timeline can
-- show milestone names and the portal can roll up milestone progress
-- when a task's progressPercent changes.
-- Also adds actual_end_date so marking a task COMPLETED doesn't clobber
-- the originally-planned end_date (Gantt scheduling field).

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS milestone_id BIGINT
        REFERENCES project_milestones(id);

CREATE INDEX IF NOT EXISTS idx_tasks_milestone_id ON tasks(milestone_id);

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS actual_end_date DATE;
