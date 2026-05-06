-- ===========================================================================
-- V120 — S2 PR2 baseline-approval permission
--
-- Adds:
--   * 1 new permission row: PROJECT_BASELINE_APPROVE
--   * Grants to ADMIN, PROJECT_MANAGER, SCHEDULER (3 grants)
--
-- Mirrors V117's INNER-JOIN seed pattern: roles that aren't seeded yet
-- (e.g. SUPER_ADMIN — declared in PortalRoleCode but not in V5 baseline)
-- are silently dropped by the join. Re-running this migration after a
-- future SUPER_ADMIN seed migration would not retroactively grant; a
-- companion migration would do that.
--
-- Idempotent: ON CONFLICT DO NOTHING throughout (matches V117 / V32 /
-- V46 / V48 / V50 / V52 convention).
-- ===========================================================================

-- 1) Permission row.
INSERT INTO portal_permissions (name, description) VALUES
    ('PROJECT_BASELINE_APPROVE',
     'Approve the kickoff schedule baseline for a project (single-shot, no re-baselining)')
ON CONFLICT (name) DO NOTHING;

-- 2) Grants.
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM portal_roles r
JOIN portal_permissions p ON TRUE
JOIN (VALUES
    ('ADMIN',           'PROJECT_BASELINE_APPROVE'),
    ('PROJECT_MANAGER', 'PROJECT_BASELINE_APPROVE'),
    ('SCHEDULER',       'PROJECT_BASELINE_APPROVE')
) AS grants(role_code, permission_name)
     ON r.code = grants.role_code AND p.name = grants.permission_name
ON CONFLICT (role_id, permission_id) DO NOTHING;
