-- Migration: Create wage_sheets table
-- Version: V1_61__create_wage_sheets.sql
-- Description: Wage sheets for periodic labour payment processing

CREATE TABLE IF NOT EXISTS wage_sheets (
    id BIGSERIAL PRIMARY KEY,
    sheet_number VARCHAR(255) NOT NULL,
    project_id BIGINT NOT NULL REFERENCES customer_projects(id),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_amount NUMERIC(15,2) NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'DRAFT',
    -- BaseEntity audit fields
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    created_by_user_id BIGINT REFERENCES portal_users(id),
    updated_by_user_id BIGINT REFERENCES portal_users(id),
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by_user_id BIGINT REFERENCES portal_users(id),
    version BIGINT NOT NULL DEFAULT 1
);

-- Unique constraint on sheet number
ALTER TABLE wage_sheets ADD CONSTRAINT uk_wage_sheets_sheet_number UNIQUE (sheet_number);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_wage_sheets_project ON wage_sheets(project_id);
CREATE INDEX IF NOT EXISTS idx_wage_sheets_status ON wage_sheets(status);
CREATE INDEX IF NOT EXISTS idx_wage_sheets_period ON wage_sheets(period_start, period_end);

COMMENT ON TABLE wage_sheets IS 'Wage sheets for periodic labour payment processing in construction projects';
