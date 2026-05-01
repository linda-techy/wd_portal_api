-- ===========================================================================
-- V102 — Estimation Settings RBAC permissions
--
-- Adds 3 permissions used by the admin endpoints in B.PR-1:
--   ESTIMATION_SETTINGS_VIEW         — read access to packages/rate-versions/market-index
--   ESTIMATION_SETTINGS_MANAGE       — write access (create/update/soft-delete)
--   ESTIMATION_MARKET_INDEX_PUBLISH  — narrower role for procurement (future);
--                                      ADMIN gets it too so admins can publish
--
-- All 3 are granted to the existing ADMIN role. The MARKET_INDEX_PUBLISH code
-- exists today so a future PROCUREMENT_OFFICER role can be granted just this
-- one without re-running migrations.
--
-- Idempotent via ON CONFLICT DO NOTHING (matches V32 / V46 / V48 / V50 / V52
-- convention).
-- ===========================================================================

INSERT INTO portal_permissions (name, description) VALUES
    ('ESTIMATION_SETTINGS_VIEW',         'View estimation settings (packages, rate cards, market index)'),
    ('ESTIMATION_SETTINGS_MANAGE',       'Create / update / soft-delete estimation settings entities'),
    ('ESTIMATION_MARKET_INDEX_PUBLISH',  'Publish a new market-index snapshot (composite-index recompute)')
ON CONFLICT (name) DO NOTHING;

INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM portal_roles r, portal_permissions p
WHERE r.code = 'ADMIN'
  AND p.name IN (
      'ESTIMATION_SETTINGS_VIEW',
      'ESTIMATION_SETTINGS_MANAGE',
      'ESTIMATION_MARKET_INDEX_PUBLISH')
ON CONFLICT DO NOTHING;
