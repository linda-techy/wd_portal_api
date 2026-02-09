-- Migration: Create project_milestones table
-- Version: V1_55__create_project_milestones.sql
-- Description: Project milestones for tracking construction stages, payment milestones, and progress

CREATE TABLE IF NOT EXISTS project_milestones (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES customer_projects(id),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    milestone_percentage NUMERIC(5,2),
    amount NUMERIC(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    due_date DATE,
    completed_date DATE,
    invoice_id BIGINT REFERENCES project_invoices(id),
    template_id BIGINT REFERENCES milestone_templates(id),
    completion_percentage NUMERIC(5,2) DEFAULT 0.00,
    weight_percentage NUMERIC(5,2),
    actual_start_date DATE,
    actual_end_date DATE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_project_milestones_project ON project_milestones(project_id);
CREATE INDEX IF NOT EXISTS idx_project_milestones_status ON project_milestones(status);
CREATE INDEX IF NOT EXISTS idx_project_milestones_template ON project_milestones(template_id);

COMMENT ON TABLE project_milestones IS 'Tracks construction project milestones for progress and payment scheduling';
