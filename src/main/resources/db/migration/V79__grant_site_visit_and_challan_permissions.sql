-- ===========================================================================
-- V79 — Grant orphaned site-visit + challan permissions to roles
--
-- Bug: V39 created the SITE_VISIT_VIEW, SITE_VISIT_CREATE, CHALLAN_VIEW,
-- CHALLAN_CREATE and CHALLAN_DOWNLOAD permissions in `portal_permissions`,
-- but no migration ever inserted the corresponding rows into
-- `portal_role_permissions`. Result: every role — including ADMIN —
-- received 403 Forbidden on every endpoint that uses
-- @PreAuthorize("hasAuthority('SITE_VISIT_VIEW')") or its CHALLAN_*
-- counterparts (e.g. /api/site-visits/active and
-- /api/site-visits/search returning 403 even for the Administrator role).
--
-- Fix: insert role→permission rows for the affected roles, mirroring the
-- existing scope from V5:
--   * SITE_VISIT_VIEW / SITE_VISIT_CREATE follow the SITE_REPORT_*
--     scope (same operational reality — staff who file site reports
--     are the same staff who check in/out).
--   * CHALLAN_* follow the FINANCE_* / PAYMENT_* scope.
--
-- Idempotent: NOT EXISTS guard skips rows already granted, so re-running
-- the migration against a partially-fixed DB is safe.
-- ===========================================================================

WITH role_permission_mapping(role_code, perm_name) AS (
    VALUES
    -- SITE_VISIT_VIEW — operational + supervisory + admin roles
    ('ADMIN',                'SITE_VISIT_VIEW'),
    ('PROJECT_MANAGER',      'SITE_VISIT_VIEW'),
    ('SITE_ENGINEER',        'SITE_VISIT_VIEW'),
    ('SITE_SUPERVISOR',      'SITE_VISIT_VIEW'),
    ('FOREMAN',              'SITE_VISIT_VIEW'),
    ('MEP_SUPERVISOR',       'SITE_VISIT_VIEW'),
    ('QUALITY_SAFETY',       'SITE_VISIT_VIEW'),
    ('STRUCTURAL_ENGINEER',  'SITE_VISIT_VIEW'),
    ('ARCHITECT_DESIGNER',   'SITE_VISIT_VIEW'),
    ('HR_MANAGER',           'SITE_VISIT_VIEW'),
    ('CRM',                  'SITE_VISIT_VIEW'),

    -- SITE_VISIT_CREATE — only roles that physically check in/out at site
    ('ADMIN',                'SITE_VISIT_CREATE'),
    ('PROJECT_MANAGER',      'SITE_VISIT_CREATE'),
    ('SITE_ENGINEER',        'SITE_VISIT_CREATE'),
    ('SITE_SUPERVISOR',      'SITE_VISIT_CREATE'),
    ('FOREMAN',              'SITE_VISIT_CREATE'),
    ('MEP_SUPERVISOR',       'SITE_VISIT_CREATE'),
    ('QUALITY_SAFETY',       'SITE_VISIT_CREATE'),

    -- CHALLAN_VIEW — finance + project oversight
    ('ADMIN',                'CHALLAN_VIEW'),
    ('FINANCE_OFFICER',      'CHALLAN_VIEW'),
    ('ACCOUNTS_ASSISTANT',   'CHALLAN_VIEW'),
    ('PROJECT_MANAGER',      'CHALLAN_VIEW'),

    -- CHALLAN_CREATE — finance issuers only
    ('ADMIN',                'CHALLAN_CREATE'),
    ('FINANCE_OFFICER',      'CHALLAN_CREATE'),

    -- CHALLAN_DOWNLOAD — finance + accounts (PMs read in-app, don't download)
    ('ADMIN',                'CHALLAN_DOWNLOAD'),
    ('FINANCE_OFFICER',      'CHALLAN_DOWNLOAD'),
    ('ACCOUNTS_ASSISTANT',   'CHALLAN_DOWNLOAD')
)
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role_permission_mapping rpm
JOIN portal_roles r        ON r.code = rpm.role_code
JOIN portal_permissions p  ON p.name = rpm.perm_name
WHERE NOT EXISTS (
    SELECT 1 FROM portal_role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
