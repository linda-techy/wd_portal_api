-- V1_43__add_base_entity_to_tasks.sql
-- Add BaseEntity audit fields to tasks table

-- Add columns
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;

-- Add foreign key constraints (using DO blocks for conditional creation)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_created_by') THEN
        ALTER TABLE tasks ADD CONSTRAINT fk_tasks_created_by FOREIGN KEY (created_by_user_id) REFERENCES portal_users(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_updated_by') THEN
        ALTER TABLE tasks ADD CONSTRAINT fk_tasks_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES portal_users(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_deleted_by') THEN
        ALTER TABLE tasks ADD CONSTRAINT fk_tasks_deleted_by FOREIGN KEY (deleted_by_user_id) REFERENCES portal_users(id);
    END IF;
END $$;

-- Add index for soft delete queries
CREATE INDEX IF NOT EXISTS idx_tasks_deleted_at ON tasks(deleted_at);
