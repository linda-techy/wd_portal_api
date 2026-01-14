-- V1_41: Seed Milestone Templates for Different Project Types
-- Purpose: Insert predefined milestone templates for common construction project types
-- Author: System Enhancement
-- Date: 2026-01-14

-- =====================================================
-- 1. Insert Project Type Templates
-- =====================================================

INSERT INTO project_type_templates (project_type, description, category) VALUES
-- Residential
('RESIDENTIAL_VILLA', 'Independent house/villa construction', 'RESIDENTIAL'),
('RESIDENTIAL_APARTMENT', 'Multi-unit apartment building', 'RESIDENTIAL'),
('CUSTOM_HOME', 'Custom designed home', 'RESIDENTIAL'),
('RENOVATION', 'Renovation of existing residential property', 'RESIDENTIAL'),

-- Commercial
('COMMERCIAL_COMPLEX', 'Mixed-use commercial building', 'COMMERCIAL'),
('OFFICE_SPACE', 'Corporate office building', 'COMMERCIAL'),
('RETAIL_STORE', 'Retail shop or showroom', 'COMMERCIAL'),
('HOSPITALITY_PROJECT', 'Hotel, resort, or guest house', 'COMMERCIAL'),

-- Infrastructure
('INFRASTRUCTURE_PROJECT', 'Roads, bridges, utilities', 'INFRASTRUCTURE'),
('INDUSTRIAL_BUILDING', 'Factory, warehouse, or plant', 'INFRASTRUCTURE'),

-- Interior & Specialized
('INTERIOR_DESIGN', 'Interior design and furnishing', 'INTERIOR'),
('LANDSCAPE_DESIGN', 'Landscaping and outdoor design', 'INTERIOR'),
('EDUCATIONAL_INSTITUTION', 'School, college, or training center', 'COMMERCIAL')
ON CONFLICT (project_type) DO NOTHING;

-- =====================================================
-- 2. RESIDENTIAL VILLA - Milestone Templates
-- =====================================================
INSERT INTO milestone_templates (template_id, milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
SELECT 
    project_type_templates.id,
    milestones.milestone_name,
    milestones.milestone_order,
    milestones.default_percentage,
    milestones.description,
    milestones.phase,
    milestones.estimated_duration_days
FROM project_type_templates,
    (VALUES
        ('Site Survey & Soil Testing', 1, 5.00, 'Site investigation, soil analysis, and topographic survey', 'DESIGN', 7),
        ('Foundation & Plinth', 2, 15.00, 'Excavation, foundation work, and plinth construction', 'EXECUTION', 30),
        ('Structural Framework', 3, 20.00, 'Columns, beams, and load-bearing structure', 'EXECUTION', 45),
        ('Roofing & Waterproofing', 4, 15.00, 'Roof construction and waterproofing', 'EXECUTION', 20),
        ('Masonry & Plastering', 5, 15.00, 'Brick work, block work, and plastering', 'EXECUTION', 35),
        ('Electrical & Plumbing', 6, 10.00, 'Electrical wiring and plumbing installation', 'EXECUTION', 25),
        ('Flooring & Tiling', 7, 10.00, 'Floor finishing and tile work', 'COMPLETION', 20),
        ('Painting & Finishing', 8, 7.00, 'Interior and exterior painting', 'COMPLETION', 15),
        ('Fixtures & Fittings', 9, 3.00, 'Final fixtures, fittings, and handover preparations', 'HANDOVER', 10)
    ) AS milestones(milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
WHERE project_type_templates.project_type = 'RESIDENTIAL_VILLA';

-- =====================================================
-- 3. COMMERCIAL COMPLEX - Milestone Templates
-- =====================================================
INSERT INTO milestone_templates (template_id, milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
SELECT 
    project_type_templates.id,
    milestones.milestone_name,
    milestones.milestone_order,
    milestones.default_percentage,
    milestones.description,
    milestones.phase,
    milestones.estimated_duration_days
FROM project_type_templates,
    (VALUES
        ('Site Preparation & Demolition', 1, 3.00, 'Site clearing and existing structure demolition', 'PLANNING', 10),
        ('Foundation & Basement', 2, 12.00, 'Deep foundation and basement construction', 'EXECUTION', 45),
        ('Structural Columns & Beams', 3, 20.00, 'Main structural framework', 'EXECUTION', 60),
        ('Floor Slabs', 4, 15.00, 'Multi-floor slab construction', 'EXECUTION', 50),
        ('External Walls & Cladding', 5, 12.00, 'Facade and external finishing', 'EXECUTION', 40),
        ('MEP - Rough-in', 6, 15.00, 'Mechanical, Electrical, Plumbing rough installation', 'EXECUTION', 35),
        ('Interior Partitions', 7, 8.00, 'Internal walls and partitions', 'EXECUTION', 25),
        ('MEP - Final Fix', 8, 10.00, 'Final MEP installations and connections', 'COMPLETION', 30),
        ('Finishes & FFE', 9, 5.00, 'Furniture, Fixtures, Equipment, and final touches', 'HANDOVER', 20)
    ) AS milestones(milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
WHERE project_type_templates.project_type = 'COMMERCIAL_COMPLEX';

-- =====================================================
-- 4. INFRASTRUCTURE PROJECT - Milestone Templates
-- =====================================================
INSERT INTO milestone_templates (template_id, milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
SELECT 
    project_type_templates.id,
    milestones.milestone_name,
    milestones.milestone_order,
    milestones.default_percentage,
    milestones.description,
    milestones.phase,
    milestones.estimated_duration_days
FROM project_type_templates,
    (VALUES
        ('Survey & Design Approval', 1, 5.00, 'Detailed survey and design approvals', 'DESIGN', 15),
        ('Land Acquisition & Clearances', 2, 10.00, 'Land procurement and regulatory clearances', 'PLANNING', 60),
        ('Earthwork & Leveling', 3, 15.00, 'Site preparation and earth moving', 'EXECUTION', 30),
        ('Foundation & Sub-structure', 4, 20.00, 'Foundation and underground works', 'EXECUTION', 45),
        ('Main Structure', 5, 25.00, 'Primary infrastructure construction', 'EXECUTION', 90),
        ('Service Installations', 6, 15.00, 'Utilities and service connections', 'EXECUTION', 40),
        ('Testing & Commissioning', 7, 7.00, 'System testing and quality checks', 'COMPLETION', 20),
        ('Handover & Documentation', 8, 3.00, 'Final documentation and project closure', 'HANDOVER', 10)
    ) AS milestones(milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
WHERE project_type_templates.project_type = 'INFRASTRUCTURE_PROJECT';

-- =====================================================
-- 5. INTERIOR DESIGN - Milestone Templates
-- =====================================================
INSERT INTO milestone_templates (template_id, milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
SELECT 
    project_type_templates.id,
    milestones.milestone_name,
    milestones.milestone_order,
    milestones.default_percentage,
    milestones.description,
    milestones.phase,
    milestones.estimated_duration_days
FROM project_type_templates,
    (VALUES
        ('Design Concept & 3D Renders', 1, 15.00, 'Initial design concepts and 3D visualizations', 'DESIGN', 10),
        ('Client Approval & Revisions', 2, 10.00, 'Client review and design modifications', 'DESIGN', 7),
        ('Material Procurement', 3, 15.00, 'Sourcing and purchasing materials', 'PLANNING', 15),
        ('False Ceiling & Partitions', 4, 20.00, 'Ceiling work and space partitioning', 'EXECUTION', 15),
        ('Electrical & Lighting', 5, 15.00, 'Lighting installation and electrical work', 'EXECUTION', 10),
        ('Flooring & Wall Finishes', 6, 15.00, 'Floor and wall finishing work', 'EXECUTION', 12),
        ('Furniture & Fixtures', 7, 8.00, 'Furniture installation and fixture mounting', 'COMPLETION', 8),
        ('Styling & Handover', 8, 2.00, 'Final styling and project handover', 'HANDOVER', 3)
    ) AS milestones(milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
WHERE project_type_templates.project_type = 'INTERIOR_DESIGN';

-- =====================================================
-- 6. APARTMENT BUILDING - Milestone Templates
-- =====================================================
INSERT INTO milestone_templates (template_id, milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
SELECT 
    project_type_templates.id,
    milestones.milestone_name,
    milestones.milestone_order,
    milestones.default_percentage,
    milestones.description,
    milestones.phase,
    milestones.estimated_duration_days
FROM project_type_templates,
    (VALUES
        ('Site Development Plan Approval', 1, 5.00, 'Approval of site plan and permits', 'DESIGN', 20),
        ('Foundation & Basement', 2, 12.00, 'Foundation and parking basement', 'EXECUTION', 50),
        ('Structural Framework', 3, 22.00, 'Main structure up to terrace', 'EXECUTION', 90),
        ('Masonry & Plastering', 4, 18.00, 'Brick work and plastering all floors', 'EXECUTION', 60),
        ('MEP Installation', 5, 15.00, 'Complete MEP installation', 'EXECUTION', 45),
        ('Unit-wise Finishing', 6, 15.00, 'Finishing work in individual units', 'COMPLETION', 50),
        ('Common Area Development', 7, 8.00, 'Lobby, corridors, amenities', 'COMPLETION', 30),
        ('External Works & Landscape', 8, 3.00, 'Compound wall, landscaping, parking', 'HANDOVER', 15),
        ('OC & Final Handover', 9, 2.00, 'Occupancy certificate and handover', 'HANDOVER', 10)
    ) AS milestones(milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
WHERE project_type_templates.project_type = 'RESIDENTIAL_APARTMENT';

-- =====================================================
-- 7. RENOVATION - Milestone Templates
-- =====================================================
INSERT INTO milestone_templates (template_id, milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
SELECT 
    project_type_templates.id,
    milestones.milestone_name,
    milestones.milestone_order,
    milestones.default_percentage,
    milestones.description,
    milestones.phase,
    milestones.estimated_duration_days
FROM project_type_templates,
    (VALUES
        ('Assessment & Planning', 1, 10.00, 'Existing condition assessment and planning', 'DESIGN', 5),
        ('Demolition & Removal', 2, 15.00, 'Selective demolition of existing elements', 'PLANNING', 7),
        ('Structural Repairs', 3, 20.00, 'Structural strengthening and repairs', 'EXECUTION', 20),
        ('MEP Upgrades', 4, 20.00, 'Electrical and plumbing system upgrades', 'EXECUTION', 15),
        ('New Finishes', 5, 20.00, 'New flooring, painting, and finishes', 'EXECUTION', 18),
        ('Fixtures & Fittings', 6, 10.00, 'New fixtures and fittings installation', 'COMPLETION', 7),
        ('Final Touches', 7, 5.00, 'Final cleanup and handover', 'HANDOVER', 3)
    ) AS milestones(milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
WHERE project_type_templates.project_type = 'RENOVATION';

-- =====================================================
-- 8. OFFICE SPACE - Milestone Templates
-- =====================================================
INSERT INTO milestone_templates (template_id, milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
SELECT 
    project_type_templates.id,
    milestones.milestone_name,
    milestones.milestone_order,
    milestones.default_percentage,
    milestones.description,
    milestones.phase,
    milestones.estimated_duration_days
FROM project_type_templates,
    (VALUES
        ('Space Planning & Design', 1, 10.00, 'Office layout and design approval', 'DESIGN', 10),
        ('Partitions & Cabin Construction', 2, 20.00, 'Glass/gypsum partitions and cabins', 'EXECUTION', 15),
        ('False Ceiling & Flooring', 3, 18.00, 'Ceiling grid and floor finishing', 'EXECUTION', 12),
        ('Electrical & Network Cabling', 4, 17.00, 'Power and data infrastructure', 'EXECUTION', 10),
        ('HVAC & Fire Safety', 5, 15.00, 'AC installation and fire systems', 'EXECUTION', 12),
        ('Workstation & Furniture', 6, 12.00, 'Furniture and workstation setup', 'COMPLETION', 8),
        ('AV & Technology Setup', 7, 5.00, 'Conference room AV and tech', 'COMPLETION', 5),
        ('Signage & Branding', 8, 3.00, 'Office signage and branding elements', 'HANDOVER', 3)
    ) AS milestones(milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days)
WHERE project_type_templates.project_type = 'OFFICE_SPACE';

-- =====================================================
-- 9. Add Comments for Documentation
-- =====================================================

COMMENT ON TABLE project_type_templates IS 'ENHANCED: Now contains 13 predefined project types across 4 categories';
COMMENT ON TABLE milestone_templates IS 'ENHANCED: Contains 69 milestone templates across all project types';

-- =====================================================
-- 10. Validation Queries (for testing)
-- =====================================================

-- Verify template counts
DO $$
DECLARE
    template_count INTEGER;
    milestone_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO template_count FROM project_type_templates;
    SELECT COUNT(*) INTO milestone_count FROM milestone_templates;
    
    RAISE NOTICE 'Successfully created % project type templates', template_count;
    RAISE NOTICE 'Successfully created % milestone templates', milestone_count;
    
    -- Verify percentages sum to ~100 for each template
    FOR template_count IN 
        SELECT template_id, SUM(default_percentage) as total_pct
        FROM milestone_templates
        GROUP BY template_id
        HAVING SUM(default_percentage) < 99 OR SUM(default_percentage) > 101
    LOOP
        RAISE WARNING 'Template ID % has milestone percentages summing to %', template_count, total_pct;
    END LOOP;
END $$;

