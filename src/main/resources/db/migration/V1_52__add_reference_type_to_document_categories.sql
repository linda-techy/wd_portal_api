-- V1_52__add_reference_type_to_document_categories.sql
-- Add reference_type field to document_categories to filter categories by context (LEAD, PROJECT, or BOTH)

-- Add reference_type column (nullable for backward compatibility)
ALTER TABLE document_categories 
ADD COLUMN IF NOT EXISTS reference_type VARCHAR(50) DEFAULT 'BOTH';

-- Update existing categories to be PROJECT-specific (most are project-related)
-- First, update by exact name matches
UPDATE document_categories 
SET reference_type = 'PROJECT' 
WHERE name IN (
    'Floor Plan Layout',
    '3D Elevation',
    'Detailed Project Costing',
    'Structural Drawings',
    'MEP Drawings',
    'MEP',  -- Handle case where it might be just "MEP"
    'Collaboration Agreement',
    'Site Photos',
    'Permits & Approvals',
    'Safety Reports',
    'Quality Assurance',
    'Handover Documents'
);

-- Also catch any variations using pattern matching (case-insensitive)
-- This ensures any MEP-related or project-related categories are set to PROJECT
UPDATE document_categories 
SET reference_type = 'PROJECT' 
WHERE (UPPER(name) LIKE '%MEP%' 
    OR UPPER(name) LIKE '%FLOOR PLAN%'
    OR UPPER(name) LIKE '%STRUCTURAL%'
    OR UPPER(name) LIKE '%ELEVATION%'
    OR UPPER(name) LIKE '%SAFETY%'
    OR UPPER(name) LIKE '%QUALITY%'
    OR UPPER(name) LIKE '%HANDOVER%'
    OR UPPER(name) LIKE '%PERMIT%'
    OR UPPER(name) LIKE '%COLLABORATION%')
AND (reference_type IS NULL OR reference_type = 'BOTH');

-- Set 'Other' to BOTH (can be used for both leads and projects)
UPDATE document_categories 
SET reference_type = 'BOTH' 
WHERE name = 'Other';

-- Add lead-specific document categories
INSERT INTO document_categories (name, description, display_order, reference_type, created_at)
VALUES 
    ('Quotation/Proposal', 'Quotations, proposals, and estimates for leads', 10, 'LEAD', NOW()),
    ('Client Requirements', 'Client requirements, specifications, and needs documents', 20, 'LEAD', NOW()),
    ('Site Visit Reports', 'Site visit reports, photos, and assessments', 30, 'LEAD', NOW()),
    ('Initial Discussions', 'Meeting notes, call logs, and initial communication', 40, 'LEAD', NOW()),
    ('Budget Estimates', 'Initial budget estimates and rough costing', 50, 'LEAD', NOW()),
    ('Client Documents', 'Client-provided documents, IDs, and references', 60, 'LEAD', NOW())
ON CONFLICT (name) DO NOTHING;

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_document_categories_reference_type 
ON document_categories(reference_type);
