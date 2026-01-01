-- V1_13__add_default_document_categories.sql
-- Add default document categories for construction projects

INSERT INTO document_categories (name, description, display_order, created_at)
VALUES 
    ('Floor Plan Layout', 'Architectural floor plans and layouts', 10, NOW()),
    ('3D Elevation', '3D visualisations and elevation drawings', 20, NOW()),
    ('Detailed Project Costing', 'Detailed BOQ and costing sheets', 30, NOW()),
    ('Structural Drawings', 'Structural engineering drawings and details', 40, NOW()),
    ('MEP Drawings', 'Mechanical, Electrical, and Plumbing drawings', 50, NOW()),
    ('Collaboration Agreement', 'Legal agreements and contracts', 60, NOW()),
    ('Site Photos', 'Progress photos and site documentation', 70, NOW()),
    ('Other', 'Miscellaneous documents', 100, NOW())
ON CONFLICT (name) DO NOTHING;
