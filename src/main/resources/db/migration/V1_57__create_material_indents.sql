-- Migration: Create material_indents table
-- Version: V1_57__create_material_indents.sql
-- Description: Material indent requests for procurement workflow

CREATE TABLE IF NOT EXISTS material_indents (
    id BIGSERIAL PRIMARY KEY,
    indent_number VARCHAR(255) NOT NULL,
    project_id BIGINT NOT NULL REFERENCES customer_projects(id),
    request_date DATE NOT NULL,
    required_date DATE NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'DRAFT',
    priority VARCHAR(255) DEFAULT 'MEDIUM',
    notes TEXT,
    requested_by_id BIGINT,
    approved_by_id BIGINT,
    approved_at TIMESTAMP WITHOUT TIME ZONE,
    rejection_reason TEXT,
    -- BaseEntity audit fields
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    created_by_user_id BIGINT REFERENCES portal_users(id),
    updated_by_user_id BIGINT REFERENCES portal_users(id),
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by_user_id BIGINT REFERENCES portal_users(id),
    version BIGINT NOT NULL DEFAULT 1
);

-- Unique constraint on indent number
ALTER TABLE material_indents ADD CONSTRAINT uk_material_indents_number UNIQUE (indent_number);

-- Foreign key for requested_by and approved_by
ALTER TABLE material_indents ADD CONSTRAINT fk_material_indents_requested_by FOREIGN KEY (requested_by_id) REFERENCES portal_users(id);
ALTER TABLE material_indents ADD CONSTRAINT fk_material_indents_approved_by FOREIGN KEY (approved_by_id) REFERENCES portal_users(id);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_material_indents_project ON material_indents(project_id);
CREATE INDEX IF NOT EXISTS idx_material_indents_status ON material_indents(status);
CREATE INDEX IF NOT EXISTS idx_material_indents_requested_by ON material_indents(requested_by_id);

COMMENT ON TABLE material_indents IS 'Material indent/requisition requests for construction procurement workflow';
