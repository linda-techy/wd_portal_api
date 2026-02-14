-- ============================================================================
-- V1_66: Standardize project phases to 5 values
-- Target phases: PLANNING, DESIGN, CONSTRUCTION, COMPLETED, ON_HOLD
-- ============================================================================

-- Map old enum values (SCREAMING_CASE) to new ones
UPDATE customer_projects SET project_phase = 'CONSTRUCTION' WHERE project_phase = 'EXECUTION';
UPDATE customer_projects SET project_phase = 'COMPLETED'    WHERE project_phase = 'COMPLETION';
UPDATE customer_projects SET project_phase = 'COMPLETED'    WHERE project_phase = 'HANDOVER';
UPDATE customer_projects SET project_phase = 'COMPLETED'    WHERE project_phase = 'WARRANTY';

-- Map mixed-case values that were stored from portal hardcoded lists
UPDATE customer_projects SET project_phase = 'PLANNING'     WHERE UPPER(project_phase) = 'PLANNING'     AND project_phase != 'PLANNING';
UPDATE customer_projects SET project_phase = 'DESIGN'       WHERE UPPER(project_phase) = 'DESIGN'       AND project_phase != 'DESIGN';
UPDATE customer_projects SET project_phase = 'CONSTRUCTION' WHERE UPPER(project_phase) = 'CONSTRUCTION' AND project_phase != 'CONSTRUCTION';
UPDATE customer_projects SET project_phase = 'COMPLETED'    WHERE UPPER(project_phase) = 'COMPLETED'    AND project_phase != 'COMPLETED';
UPDATE customer_projects SET project_phase = 'ON_HOLD'      WHERE UPPER(REPLACE(project_phase, ' ', '_')) = 'ON_HOLD' AND project_phase != 'ON_HOLD';

-- Map legacy portal values (Foundation, Finishing)
UPDATE customer_projects SET project_phase = 'CONSTRUCTION' WHERE UPPER(project_phase) = 'FOUNDATION';
UPDATE customer_projects SET project_phase = 'CONSTRUCTION' WHERE UPPER(project_phase) = 'FINISHING';

-- Catch-all: any remaining unrecognized values default to PLANNING
UPDATE customer_projects SET project_phase = 'PLANNING'
WHERE project_phase NOT IN ('PLANNING', 'DESIGN', 'CONSTRUCTION', 'COMPLETED', 'ON_HOLD');

-- Update column comment
COMMENT ON COLUMN customer_projects.project_phase IS 'Current lifecycle phase: PLANNING, DESIGN, CONSTRUCTION, COMPLETED, ON_HOLD';
