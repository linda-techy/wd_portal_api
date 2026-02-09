-- Migration: Create wage_sheet_entries table
-- Version: V1_62__create_wage_sheet_entries.sql
-- Description: Individual worker entries within a wage sheet

CREATE TABLE IF NOT EXISTS wage_sheet_entries (
    id BIGSERIAL PRIMARY KEY,
    wage_sheet_id BIGINT NOT NULL REFERENCES wage_sheets(id) ON DELETE CASCADE,
    labour_id BIGINT NOT NULL REFERENCES labour(id),
    days_worked NUMERIC(4,1) NOT NULL,
    daily_wage NUMERIC(10,2) NOT NULL,
    total_wage NUMERIC(15,2) NOT NULL,
    advances_deducted NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    net_payable NUMERIC(15,2) NOT NULL,
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
CREATE INDEX IF NOT EXISTS idx_wage_sheet_entries_sheet ON wage_sheet_entries(wage_sheet_id);
CREATE INDEX IF NOT EXISTS idx_wage_sheet_entries_labour ON wage_sheet_entries(labour_id);

COMMENT ON TABLE wage_sheet_entries IS 'Individual labour worker payment entries within a wage sheet';
