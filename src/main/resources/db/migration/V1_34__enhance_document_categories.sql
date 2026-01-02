-- V1_34__enhance_document_categories.sql
-- Add enterprise-grade document categories

INSERT INTO document_categories (name, description, display_order, created_at)
VALUES 
    ('Permits & Approvals', 'Building permits, NOCs, and regulatory approvals', 15, NOW()),
    ('Safety Reports', 'HSE reports, incident logs, and safety manuals', 80, NOW()),
    ('Quality Assurance', 'Test reports, material certifications, and audit logs', 85, NOW()),
    ('Handover Documents', 'As-built drawings, warranties, and manuals', 90, NOW())
ON CONFLICT (name) DO NOTHING;
