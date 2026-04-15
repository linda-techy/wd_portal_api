-- V18: Add BOQ_CORRECT and BOQ_EXPORT permissions
-- BOQ_CORRECT: allows correcting recorded execution data (previously reused BOQ_APPROVE)
-- BOQ_EXPORT:  allows downloading the BOQ Excel export (previously reused BOQ_VIEW)

INSERT INTO portal_permissions (name, description) VALUES
    ('BOQ_CORRECT', 'Correct recorded BOQ execution quantities'),
    ('BOQ_EXPORT',  'Export BOQ data to Excel');

-- Assign permissions to roles via ID lookup (matches V5 pattern)
WITH role_permission_mapping (role_code, perm_name) AS (VALUES
    -- BOQ_CORRECT: roles that hold BOQ_APPROVE
    ('ADMIN',           'BOQ_CORRECT'),
    ('PROJECT_MANAGER', 'BOQ_CORRECT'),
    -- BOQ_EXPORT: roles that hold BOQ_VIEW
    ('ADMIN',                'BOQ_EXPORT'),
    ('PROJECT_MANAGER',      'BOQ_EXPORT'),
    ('PROCUREMENT_OFFICER',  'BOQ_EXPORT'),
    ('FINANCE_OFFICER',      'BOQ_EXPORT'),
    ('ESTIMATOR',            'BOQ_EXPORT'),
    ('ARCHITECT_DESIGNER',   'BOQ_EXPORT'),
    ('STRUCTURAL_ENGINEER',  'BOQ_EXPORT'),
    ('INTERIOR_DESIGNER',    'BOQ_EXPORT'),
    ('ACCOUNTS_ASSISTANT',   'BOQ_EXPORT')
)
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role_permission_mapping rpm
JOIN portal_roles r       ON r.code = rpm.role_code
JOIN portal_permissions p ON p.name = rpm.perm_name
WHERE NOT EXISTS (
    SELECT 1 FROM portal_role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
