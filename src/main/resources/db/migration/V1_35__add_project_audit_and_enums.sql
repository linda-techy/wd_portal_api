-- Migration: Add audit trail fields, optimistic locking, and project status enum
-- Version: V1_35
-- Description: Enhances customer_projects table with enterprise-grade audit trail,
--              soft delete capability, optimistic locking, and project status tracking

-- ============================================================================
-- 1. Add Audit Trail Columns
-- ============================================================================

-- Add audit trail foreign keys to portal_users
ALTER TABLE customer_projects 
    ADD COLUMN created_by_user_id BIGINT,
    ADD COLUMN updated_by_user_id BIGINT,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN deleted_by_user_id BIGINT;

-- Add foreign key constraints for audit trail
ALTER TABLE customer_projects
    ADD CONSTRAINT fk_projects_created_by_user 
        FOREIGN KEY (created_by_user_id) REFERENCES portal_users(id),
    ADD CONSTRAINT fk_projects_updated_by_user 
        FOREIGN KEY (updated_by_user_id) REFERENCES portal_users(id),
    ADD CONSTRAINT fk_projects_deleted_by_user 
        FOREIGN KEY (deleted_by_user_id) REFERENCES portal_users(id);

-- ============================================================================
-- 2. Add Optimistic Locking
-- ============================================================================

ALTER TABLE customer_projects 
    ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- ============================================================================
-- 3. Add Project Status Column
-- ============================================================================

ALTER TABLE customer_projects 
    ADD COLUMN project_status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL;

-- Add check constraint for valid enum values
ALTER TABLE customer_projects
    ADD CONSTRAINT chk_project_status 
        CHECK (project_status IN ('ACTIVE', 'COMPLETED', 'SUSPENDED', 'CANCELLED', 'ON_HOLD'));

-- ============================================================================
-- 4. Convert Existing Data to Enum-Compatible Format
-- ============================================================================

-- Standardize project_phase to uppercase for enum compatibility
UPDATE customer_projects 
    SET project_phase = UPPER(REPLACE(project_phase, ' ', '_'))
    WHERE project_phase IS NOT NULL;

-- Ensure project_phase values match enum constants
UPDATE customer_projects 
    SET project_phase = 
        CASE 
            WHEN project_phase IN ('DESIGN', 'DESIGNING') THEN 'DESIGN'
            WHEN project_phase IN ('PLANNING', 'PLAN') THEN 'PLANNING'
            WHEN project_phase IN ('EXECUTION', 'EXECUTING', 'IN_PROGRESS') THEN 'EXECUTION'
            WHEN project_phase IN ('COMPLETION', 'COMPLETING') THEN 'COMPLETION'
            WHEN project_phase IN ('HANDOVER', 'HANDOVER_COMPLETE') THEN 'HANDOVER'
            WHEN project_phase IN ('WARRANTY', 'WARRANTY_PERIOD') THEN 'WARRANTY'
            ELSE 'PLANNING' -- Default fallback
        END
    WHERE project_phase IS NOT NULL;

-- Standardize permit_status to uppercase if exists
UPDATE customer_projects 
    SET permit_status = UPPER(REPLACE(permit_status, ' ', '_'))
    WHERE permit_status IS NOT NULL;

-- Ensure permit_status values match enum constants
UPDATE customer_projects 
    SET permit_status = 
        CASE 
            WHEN permit_status IN ('NOT_REQUIRED', 'NOT REQUIRED', 'NA', 'N/A') THEN 'NOT_REQUIRED'
            WHEN permit_status IN ('APPLIED', 'PENDING', 'SUBMITTED') THEN 'APPLIED'
            WHEN permit_status IN ('APPROVED', 'SANCTIONED') THEN 'APPROVED'
            WHEN permit_status IN ('REJECTED', 'DENIED') THEN 'REJECTED'
            WHEN permit_status IN ('UNDER_REVIEW', 'REVIEWING', 'IN_REVIEW') THEN 'UNDER_REVIEW'
            WHEN permit_status IN ('EXPIRED', 'LAPSED') THEN 'EXPIRED'
            ELSE NULL
        END
    WHERE permit_status IS NOT NULL;

-- ============================================================================
-- 5. Add Indexes for Performance
-- ============================================================================

-- Index for soft delete queries (active projects only)
CREATE INDEX idx_projects_deleted_at 
    ON customer_projects(deleted_at)
    WHERE deleted_at IS NULL;

-- Index for active projects composite query
CREATE INDEX idx_projects_active_phase 
    ON customer_projects(id, project_phase, project_status)
    WHERE deleted_at IS NULL;

-- Index for project manager queries
CREATE INDEX idx_projects_manager 
    ON customer_projects(project_manager_id)
    WHERE deleted_at IS NULL;

-- Index for customer queries (already might exist, but ensuring it's optimized for soft delete)
CREATE INDEX idx_projects_customer_active 
    ON customer_projects(customer_id)
    WHERE deleted_at IS NULL;

-- Index for project status queries
CREATE INDEX idx_projects_status 
    ON customer_projects(project_status);

-- Index for version (optimistic locking lookups)
CREATE INDEX idx_projects_version 
    ON customer_projects(id, version);

-- ============================================================================
-- 6. Add Comments for Documentation
-- ============================================================================

COMMENT ON COLUMN customer_projects.created_by_user_id IS 
    'Portal user who created this project (for audit trail)';
    
COMMENT ON COLUMN customer_projects.updated_by_user_id IS 
    'Portal user who last updated this project (for audit trail)';
    
COMMENT ON COLUMN customer_projects.deleted_at IS 
    'Timestamp when project was soft-deleted (NULL = active)';
    
COMMENT ON COLUMN customer_projects.deleted_by_user_id IS 
    'Portal user who soft-deleted this project';
    
COMMENT ON COLUMN customer_projects.version IS 
    'Version counter for optimistic locking (prevents lost updates)';
    
COMMENT ON COLUMN customer_projects.project_status IS 
    'Overall operational status: ACTIVE, COMPLETED, SUSPENDED, CANCELLED, ON_HOLD';

COMMENT ON COLUMN customer_projects.project_phase IS 
    'Current lifecycle phase: DESIGN, PLANNING, EXECUTION, COMPLETION, HANDOVER, WARRANTY';
    
COMMENT ON COLUMN customer_projects.permit_status IS 
    'Regulatory approval status: NOT_REQUIRED, APPLIED, APPROVED, REJECTED, UNDER_REVIEW, EXPIRED';

-- ============================================================================
-- Migration Complete
-- ============================================================================
