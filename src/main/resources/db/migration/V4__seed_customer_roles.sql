-- V4: Seed customer role types
-- These roles control what 3rd-party customer users can see on their project portal.
-- CUSTOMER  = the primary project owner (full visibility)
-- ARCHITECT  = architecture firm with access to technical/structural data (no financials)
-- INTERIOR_DESIGNER = interior team, sees design scope and interior progress (no financials)
-- SITE_ENGINEER = on-site contractor, sees tasks and progress timeline (no financials)
-- VIEWER = read-only access to overall progress and status only

INSERT INTO customer_roles (name, description)
SELECT v.name, v.description
FROM (VALUES
    ('CUSTOMER',          'Primary project owner / main client - full access to their own project data including financials'),
    ('ARCHITECT',         'Architecture firm or professional - views technical plans and construction progress; financial data hidden'),
    ('INTERIOR_DESIGNER', 'Interior design team - views design scope, interior milestones and progress; financial data hidden'),
    ('SITE_ENGINEER',     'On-site engineer or contractor - views tasks, schedules and progress timeline; financial data hidden'),
    ('VIEWER',            'Limited read-only access - views overall project progress and current phase only')
) AS v(name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM customer_roles cr WHERE cr.name = v.name
);
