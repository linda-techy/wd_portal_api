-- ===========================================================================
-- V117 — S1 roles + permissions
--
-- Adds:
--   * 3 new role rows (SCHEDULER, QUANTITY_SURVEYOR, MANAGEMENT)
--   * 8 new permission rows (S1-scoped: WBS templates, holidays, schedule, monsoon)
--   * Role-permission grants per the matrix in the S1 design spec.
--
-- Idempotent: ON CONFLICT DO NOTHING throughout (matches V32/V46/V48/V50/V52
-- convention).
--
-- Note on grant cardinality: V5 baseline seeds these roles:
-- ADMIN, PROJECT_MANAGER, SITE_ENGINEER, FINANCE_OFFICER,
-- ARCHITECT_DESIGNER, INTERIOR_DESIGNER, PROCUREMENT_OFFICER.
-- The S1 spec matrix references SUPER_ADMIN and ARCHITECT (not
-- ARCHITECT_DESIGNER); both are no-ops here because the INNER JOIN
-- below drops them.
--
-- SUPER_ADMIN is declared in PortalRoleCode and used by isAdmin();
-- seeding it is out of scope for S1 PR2 (cross-cutting role-taxonomy
-- change) and tracked separately. ARCHITECT in the spec matrix is a
-- stale reference to ARCHITECT_DESIGNER — see the amended spec.
--
-- Today's reachable grant count from the matrix below: 34 (matrix
-- cells minus the missing roles). When SUPER_ADMIN is seeded in a
-- future migration (and the spec is re-aligned), an updated R__
-- companion or a new seed migration can backfill the dropped grants.
-- ===========================================================================

-- 1) Role rows
INSERT INTO portal_roles (name, description, code) VALUES
    ('Scheduler',          'Plans and maintains project schedules; can edit WBS templates and holidays', 'SCHEDULER'),
    ('Quantity Surveyor',  'Reviews quantities and pricing; read-only access to scheduling data',         'QUANTITY_SURVEYOR'),
    ('Management',         'Executive read-only role with monsoon and schedule visibility',               'MANAGEMENT')
ON CONFLICT (code) DO NOTHING;

-- 2) Permission rows
INSERT INTO portal_permissions (name, description) VALUES
    ('WBS_TEMPLATE_VIEW',           'View WBS templates'),
    ('WBS_TEMPLATE_MANAGE',         'Create / edit / version WBS templates'),
    ('PROJECT_WBS_CLONE',           'Clone a WBS template into a project at creation time'),
    ('HOLIDAY_VIEW',                'View holiday calendar (national/state/district/project)'),
    ('HOLIDAY_MANAGE',              'Create / edit / delete holiday rows in the holiday catalog'),
    ('PROJECT_HOLIDAY_OVERRIDE',    'Add or exclude a holiday for a single project'),
    ('PROJECT_SCHEDULE_CONFIG_EDIT','Edit per-project schedule config (Sunday-working, monsoon window, district)'),
    ('MONSOON_WARNING_VIEW',        'View monsoon warnings on the project schedule view')
ON CONFLICT (name) DO NOTHING;

-- 3) Grants. One INSERT per matrix cell. The unioned SELECTs yield one row
--    per (role_code, permission_name) pair; the join hydrates IDs.
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM portal_roles r
JOIN portal_permissions p ON TRUE
JOIN (VALUES
    -- WBS_TEMPLATE_VIEW: all 11 roles
    ('SUPER_ADMIN',        'WBS_TEMPLATE_VIEW'),
    ('ADMIN',              'WBS_TEMPLATE_VIEW'),
    ('SCHEDULER',          'WBS_TEMPLATE_VIEW'),
    ('PROJECT_MANAGER',    'WBS_TEMPLATE_VIEW'),
    ('SITE_ENGINEER',      'WBS_TEMPLATE_VIEW'),
    ('QUANTITY_SURVEYOR',  'WBS_TEMPLATE_VIEW'),
    ('MANAGEMENT',         'WBS_TEMPLATE_VIEW'),
    ('ARCHITECT',          'WBS_TEMPLATE_VIEW'),
    ('INTERIOR_DESIGNER',  'WBS_TEMPLATE_VIEW'),
    ('PROCUREMENT_OFFICER','WBS_TEMPLATE_VIEW'),
    ('FINANCE_OFFICER',    'WBS_TEMPLATE_VIEW'),
    -- WBS_TEMPLATE_MANAGE: 3
    ('SUPER_ADMIN','WBS_TEMPLATE_MANAGE'),
    ('ADMIN',      'WBS_TEMPLATE_MANAGE'),
    ('SCHEDULER',  'WBS_TEMPLATE_MANAGE'),
    -- PROJECT_WBS_CLONE: 3
    ('SUPER_ADMIN','PROJECT_WBS_CLONE'),
    ('ADMIN',      'PROJECT_WBS_CLONE'),
    ('SCHEDULER',  'PROJECT_WBS_CLONE'),
    -- HOLIDAY_VIEW: all 11
    ('SUPER_ADMIN',        'HOLIDAY_VIEW'),
    ('ADMIN',              'HOLIDAY_VIEW'),
    ('SCHEDULER',          'HOLIDAY_VIEW'),
    ('PROJECT_MANAGER',    'HOLIDAY_VIEW'),
    ('SITE_ENGINEER',      'HOLIDAY_VIEW'),
    ('QUANTITY_SURVEYOR',  'HOLIDAY_VIEW'),
    ('MANAGEMENT',         'HOLIDAY_VIEW'),
    ('ARCHITECT',          'HOLIDAY_VIEW'),
    ('INTERIOR_DESIGNER',  'HOLIDAY_VIEW'),
    ('PROCUREMENT_OFFICER','HOLIDAY_VIEW'),
    ('FINANCE_OFFICER',    'HOLIDAY_VIEW'),
    -- HOLIDAY_MANAGE: 2
    ('SUPER_ADMIN','HOLIDAY_MANAGE'),
    ('ADMIN',      'HOLIDAY_MANAGE'),
    -- PROJECT_HOLIDAY_OVERRIDE: 4
    ('SUPER_ADMIN',     'PROJECT_HOLIDAY_OVERRIDE'),
    ('ADMIN',           'PROJECT_HOLIDAY_OVERRIDE'),
    ('SCHEDULER',       'PROJECT_HOLIDAY_OVERRIDE'),
    ('PROJECT_MANAGER', 'PROJECT_HOLIDAY_OVERRIDE'),
    -- PROJECT_SCHEDULE_CONFIG_EDIT: 4
    ('SUPER_ADMIN',     'PROJECT_SCHEDULE_CONFIG_EDIT'),
    ('ADMIN',           'PROJECT_SCHEDULE_CONFIG_EDIT'),
    ('SCHEDULER',       'PROJECT_SCHEDULE_CONFIG_EDIT'),
    ('PROJECT_MANAGER', 'PROJECT_SCHEDULE_CONFIG_EDIT'),
    -- MONSOON_WARNING_VIEW: 5
    ('SUPER_ADMIN',     'MONSOON_WARNING_VIEW'),
    ('ADMIN',           'MONSOON_WARNING_VIEW'),
    ('SCHEDULER',       'MONSOON_WARNING_VIEW'),
    ('PROJECT_MANAGER', 'MONSOON_WARNING_VIEW'),
    ('SITE_ENGINEER',   'MONSOON_WARNING_VIEW'),
    ('MANAGEMENT',      'MONSOON_WARNING_VIEW')
) AS grants(role_code, permission_name)
     ON r.code = grants.role_code AND p.name = grants.permission_name
ON CONFLICT (role_id, permission_id) DO NOTHING;
