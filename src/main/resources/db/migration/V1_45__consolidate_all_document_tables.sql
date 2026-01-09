-- Migration: Consolidate all specialized document tables into a unified 'project_documents' table
-- Version: V1_45__consolidate_all_document_tables.sql
-- Description: 
-- 1. Upgrades 'project_documents' to support polymorphic attachments (reference_id, reference_type).
-- 2. Migrates data from 'portal_project_documents' and 'lead_documents' into 'project_documents'.
-- 3. Harmonizes schema with BaseEntity audit fields.
-- 4. Cleans up legacy tables.

-- =====================================================================
-- 1. PREPARE project_documents FOR POLYMORPHIC USE
-- =====================================================================

-- Add polymorphic columns
ALTER TABLE project_documents 
ADD COLUMN IF NOT EXISTS reference_id BIGINT,
ADD COLUMN IF NOT EXISTS reference_type VARCHAR(50);

-- Rename or add audit columns to match BaseEntity/Unified Document standard
DO $$ 
BEGIN 
    -- Rename upload_date to created_at if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='project_documents' AND column_name='upload_date') THEN
        ALTER TABLE project_documents RENAME COLUMN upload_date TO created_at;
    END IF;

    -- Rename uploaded_by_id to created_by_user_id if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='project_documents' AND column_name='uploaded_by_id') THEN
        ALTER TABLE project_documents RENAME COLUMN uploaded_by_id TO created_by_user_id;
    END IF;
END $$;

-- Add remaining audit/BaseEntity columns
ALTER TABLE project_documents
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE,
ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE,
ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT;

-- Initialize polymorphic data for existing project documents
UPDATE project_documents 
SET reference_id = project_id, 
    reference_type = 'PROJECT'
WHERE reference_id IS NULL AND project_id IS NOT NULL;

-- =====================================================================
-- 2. MIGRATE DATA FROM legacy tables
-- =====================================================================

-- From portal_project_documents
INSERT INTO project_documents (
    id, description, file_path, file_size, file_type, filename, is_active, 
    created_at, version, category_id, reference_id, reference_type, created_by_user_id
)
SELECT 
    id, description, file_path, file_size, file_type, filename, COALESCE(is_active, true), 
    upload_date, COALESCE(version, 1), category_id, project_id, 'PROJECT', uploaded_by_id
FROM portal_project_documents
ON CONFLICT (id) DO NOTHING;

-- From lead_documents (if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'lead_documents') THEN
        INSERT INTO project_documents (
            description, file_path, file_size, file_type, filename, is_active, 
            created_at, version, reference_id, reference_type, created_by_user_id
        )
        SELECT 
            description, file_path, file_size, file_type, filename, COALESCE(is_active, true), 
            uploaded_at, 1, lead_id, 'LEAD', uploaded_by_id
        FROM lead_documents;
    END IF;
END $$;

-- =====================================================================
-- 3. FINAL CLEANUP & CONSTRAINTS
-- =====================================================================

-- Drop legacy tables
DROP TABLE IF EXISTS portal_project_documents;
DROP TABLE IF EXISTS lead_documents;
DROP TABLE IF EXISTS wd_documents; -- Trash from previous failed attempts/proposals

-- Cleanup redundant columns in project_documents after safe migration
ALTER TABLE project_documents DROP COLUMN IF EXISTS project_id;

-- Add indexes for common polymorphic queries
CREATE INDEX IF NOT EXISTS idx_project_documents_ref 
ON project_documents(reference_id, reference_type);

CREATE INDEX IF NOT EXISTS idx_project_documents_category 
ON project_documents(category_id);

-- Ensure primary key sequence is correct (important if we manually inserted IDs)
SELECT setval(pg_get_serial_sequence('project_documents', 'id'), COALESCE(MAX(id), 1)) FROM project_documents;

COMMENT ON TABLE project_documents IS 'Unified polymorphic document table for all project, lead, and system attachments.';
