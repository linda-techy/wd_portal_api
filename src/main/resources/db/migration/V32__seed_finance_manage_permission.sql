-- V32: Seed FINANCE_MANAGE permission and assign to appropriate roles.
--
-- FinanceController.java:89 checks hasAnyAuthority('FINANCE_MANAGE', 'ADMIN').
-- The ADMIN fallback means admins can access it, but no non-admin role had this
-- permission assigned. Finance Officer and Accounts Assistant need it.

INSERT INTO portal_permissions (name, description)
VALUES ('FINANCE_MANAGE', 'Access finance dashboard and manage financial records')
ON CONFLICT (name) DO NOTHING;

INSERT INTO portal_role_permissions (role_code, permission_name)
VALUES
    ('ADMIN',              'FINANCE_MANAGE'),
    ('FINANCE_OFFICER',    'FINANCE_MANAGE'),
    ('ACCOUNTS_ASSISTANT', 'FINANCE_MANAGE')
ON CONFLICT DO NOTHING;
