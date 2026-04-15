-- =============================================================================
-- V27: Add permissions for Variation Orders, Stage Certification,
--      Deduction Register, and Final Account modules
-- =============================================================================

-- ---- New permissions --------------------------------------------------------
INSERT INTO portal_permissions (name, description)
VALUES
    -- Variation Orders
    ('VO_VIEW',             'View variation orders and their approval history'),
    ('VO_CREATE',           'Create new variation order drafts'),
    ('VO_EDIT',             'Edit draft variation orders'),
    ('VO_SUBMIT',           'Submit a draft VO for approval'),
    ('VO_APPROVE',          'Approve a submitted variation order (role-threshold enforced)'),
    ('VO_REJECT',           'Reject a variation order with mandatory reason'),
    ('VO_CANCEL',           'Cancel a variation order'),
    ('VO_PAYMENT_EDIT',     'Mark VO advance/progress/completion payments as paid'),
    -- Stage Certification
    ('STAGE_VIEW',          'View payment stages and retention summary'),
    ('STAGE_CERTIFY',       'Certify a payment stage (triggers progress payments)'),
    ('STAGE_MARK_PAID',     'Mark a certified stage payment as received'),
    -- Deduction Register
    ('DEDUCTION_VIEW',      'View deduction register entries'),
    ('DEDUCTION_CREATE',    'Raise an omission deduction'),
    ('DEDUCTION_DECIDE',    'Accept, partially accept, or reject a deduction'),
    ('DEDUCTION_ESCALATE',  'Escalate a deduction to the next approval level'),
    -- Final Account
    ('FINAL_ACCOUNT_VIEW',  'View the final account for a project'),
    ('FINAL_ACCOUNT_CREATE','Generate a draft final account'),
    ('FINAL_ACCOUNT_SUBMIT','Submit final account to client'),
    ('FINAL_ACCOUNT_AGREE', 'Mark final account as agreed by client'),
    ('FINAL_ACCOUNT_CLOSE', 'Close a final account after DLP'),
    ('FINAL_ACCOUNT_RELEASE_RETENTION', 'Release retention after DLP expiry')
ON CONFLICT (name) DO NOTHING;

-- ---- Role-permission mappings (idempotent) ----------------------------------
WITH rpm(role_code, perm_name) AS (
    VALUES
    -- ADMIN — full access to all new modules
    ('ADMIN','VO_VIEW'),            ('ADMIN','VO_CREATE'),          ('ADMIN','VO_EDIT'),
    ('ADMIN','VO_SUBMIT'),          ('ADMIN','VO_APPROVE'),         ('ADMIN','VO_REJECT'),
    ('ADMIN','VO_CANCEL'),          ('ADMIN','VO_PAYMENT_EDIT'),
    ('ADMIN','STAGE_VIEW'),         ('ADMIN','STAGE_CERTIFY'),      ('ADMIN','STAGE_MARK_PAID'),
    ('ADMIN','DEDUCTION_VIEW'),     ('ADMIN','DEDUCTION_CREATE'),   ('ADMIN','DEDUCTION_DECIDE'),
    ('ADMIN','DEDUCTION_ESCALATE'),
    ('ADMIN','FINAL_ACCOUNT_VIEW'), ('ADMIN','FINAL_ACCOUNT_CREATE'),
    ('ADMIN','FINAL_ACCOUNT_SUBMIT'),('ADMIN','FINAL_ACCOUNT_AGREE'),
    ('ADMIN','FINAL_ACCOUNT_CLOSE'),('ADMIN','FINAL_ACCOUNT_RELEASE_RETENTION'),

    -- DIRECTOR — full approve authority, can decide high-value deductions
    ('DIRECTOR','VO_VIEW'),         ('DIRECTOR','VO_APPROVE'),      ('DIRECTOR','VO_REJECT'),
    ('DIRECTOR','VO_CANCEL'),       ('DIRECTOR','VO_PAYMENT_EDIT'),
    ('DIRECTOR','STAGE_VIEW'),      ('DIRECTOR','STAGE_CERTIFY'),   ('DIRECTOR','STAGE_MARK_PAID'),
    ('DIRECTOR','DEDUCTION_VIEW'),  ('DIRECTOR','DEDUCTION_DECIDE'),('DIRECTOR','DEDUCTION_ESCALATE'),
    ('DIRECTOR','FINAL_ACCOUNT_VIEW'),('DIRECTOR','FINAL_ACCOUNT_AGREE'),
    ('DIRECTOR','FINAL_ACCOUNT_CLOSE'),('DIRECTOR','FINAL_ACCOUNT_RELEASE_RETENTION'),

    -- COMMERCIAL_MANAGER — approve mid-range VOs, manage deductions
    ('COMMERCIAL_MANAGER','VO_VIEW'),   ('COMMERCIAL_MANAGER','VO_CREATE'),
    ('COMMERCIAL_MANAGER','VO_EDIT'),   ('COMMERCIAL_MANAGER','VO_SUBMIT'),
    ('COMMERCIAL_MANAGER','VO_APPROVE'),('COMMERCIAL_MANAGER','VO_REJECT'),
    ('COMMERCIAL_MANAGER','VO_CANCEL'), ('COMMERCIAL_MANAGER','VO_PAYMENT_EDIT'),
    ('COMMERCIAL_MANAGER','STAGE_VIEW'),('COMMERCIAL_MANAGER','STAGE_CERTIFY'),
    ('COMMERCIAL_MANAGER','STAGE_MARK_PAID'),
    ('COMMERCIAL_MANAGER','DEDUCTION_VIEW'), ('COMMERCIAL_MANAGER','DEDUCTION_CREATE'),
    ('COMMERCIAL_MANAGER','DEDUCTION_DECIDE'),('COMMERCIAL_MANAGER','DEDUCTION_ESCALATE'),
    ('COMMERCIAL_MANAGER','FINAL_ACCOUNT_VIEW'),('COMMERCIAL_MANAGER','FINAL_ACCOUNT_CREATE'),
    ('COMMERCIAL_MANAGER','FINAL_ACCOUNT_SUBMIT'),

    -- PROJECT_MANAGER — create/submit VOs, certify stages, view deductions
    ('PROJECT_MANAGER','VO_VIEW'),    ('PROJECT_MANAGER','VO_CREATE'),
    ('PROJECT_MANAGER','VO_EDIT'),    ('PROJECT_MANAGER','VO_SUBMIT'),
    ('PROJECT_MANAGER','VO_APPROVE'), ('PROJECT_MANAGER','VO_REJECT'),
    ('PROJECT_MANAGER','STAGE_VIEW'), ('PROJECT_MANAGER','STAGE_CERTIFY'),
    ('PROJECT_MANAGER','DEDUCTION_VIEW'),('PROJECT_MANAGER','DEDUCTION_CREATE'),
    ('PROJECT_MANAGER','DEDUCTION_ESCALATE'),
    ('PROJECT_MANAGER','FINAL_ACCOUNT_VIEW'),

    -- FINANCE — payment marking, final account, view-only on VOs/stages
    ('FINANCE','VO_VIEW'),            ('FINANCE','VO_PAYMENT_EDIT'),
    ('FINANCE','STAGE_VIEW'),         ('FINANCE','STAGE_MARK_PAID'),
    ('FINANCE','DEDUCTION_VIEW'),
    ('FINANCE','FINAL_ACCOUNT_VIEW'), ('FINANCE','FINAL_ACCOUNT_CREATE'),
    ('FINANCE','FINAL_ACCOUNT_SUBMIT'),

    -- SITE_ENGINEER — view only
    ('SITE_ENGINEER','VO_VIEW'), ('SITE_ENGINEER','STAGE_VIEW'), ('SITE_ENGINEER','DEDUCTION_VIEW')
)
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM rpm
JOIN portal_roles       r ON r.code = rpm.role_code
JOIN portal_permissions p ON p.name = rpm.perm_name
WHERE NOT EXISTS (
    SELECT 1 FROM portal_role_permissions x
    WHERE x.role_id = r.id AND x.permission_id = p.id
);
