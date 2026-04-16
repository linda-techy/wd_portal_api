-- V32: Seed FINANCE_MANAGE permission and assign to appropriate roles.
--
-- FinanceController.java:89 checks hasAnyAuthority('FINANCE_MANAGE', 'ADMIN').
-- The ADMIN fallback means admins can access it, but no non-admin role had this
-- permission assigned. Finance Officer and Accounts Assistant need it.

INSERT INTO portal_permissions (name, description)
VALUES ('FINANCE_MANAGE', 'Access finance dashboard and manage financial records')
ON CONFLICT (name) DO NOTHING;

-- Join table uses FK IDs: role_id → portal_roles.id, permission_id → portal_permissions.id
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM portal_roles r, portal_permissions p
WHERE r.code IN ('ADMIN', 'FINANCE_OFFICER', 'ACCOUNTS_ASSISTANT')
  AND p.name = 'FINANCE_MANAGE'
ON CONFLICT DO NOTHING;
