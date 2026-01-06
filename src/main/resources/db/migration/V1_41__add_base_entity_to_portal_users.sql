-- V1_41__add_base_entity_to_portal_users.sql
-- Add BaseEntity audit fields to portal_users table

ALTER TABLE portal_users
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;

-- Add foreign key constraints
ALTER TABLE portal_users
    ADD CONSTRAINT fk_portal_users_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES portal_users(id),
    ADD CONSTRAINT fk_portal_users_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES portal_users(id),
    ADD CONSTRAINT fk_portal_users_deleted_by
        FOREIGN KEY (deleted_by_user_id) REFERENCES portal_users(id);

-- Add index for soft delete queries
CREATE INDEX IF NOT EXISTS idx_portal_users_deleted_at ON portal_users(deleted_at);
