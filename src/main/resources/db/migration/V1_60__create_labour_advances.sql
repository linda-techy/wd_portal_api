-- Migration: Create labour_advances table
-- Version: V1_60__create_labour_advances.sql
-- Description: Advance payments given to labour workers, tracked for deduction from wages

CREATE TABLE IF NOT EXISTS labour_advances (
    id BIGSERIAL PRIMARY KEY,
    labour_id BIGINT NOT NULL REFERENCES labour(id),
    advance_date DATE NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    recovered_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    -- BaseEntity audit fields
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    created_by_user_id BIGINT REFERENCES portal_users(id),
    updated_by_user_id BIGINT REFERENCES portal_users(id),
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by_user_id BIGINT REFERENCES portal_users(id),
    version BIGINT NOT NULL DEFAULT 1
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_labour_advances_labour ON labour_advances(labour_id);
CREATE INDEX IF NOT EXISTS idx_labour_advances_date ON labour_advances(advance_date);

COMMENT ON TABLE labour_advances IS 'Tracks advance payments given to labour workers for construction projects';
