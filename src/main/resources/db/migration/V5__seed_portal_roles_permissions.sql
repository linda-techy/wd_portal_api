-- =============================================================================
-- V5 — Seed 27 Portal Roles + Full Permission System
-- =============================================================================
-- Adds the complete org-chart role structure for a construction management
-- company. Existing role rows are preserved (WHERE NOT EXISTS / ON CONFLICT).
-- Old role codes (USER, PM, SALES_MANAGER, PROCUREMENT_MANAGER) remain in
-- the table so existing portal_users rows are unaffected.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- STEP 0 — Fix constraints: drop name constraint, ensure code constraint exists
-- -----------------------------------------------------------------------------
-- Drop the unique constraint on name if it exists (name is just a display value, code is the key)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'portal_roles_name_key' 
        AND conrelid = 'portal_roles'::regclass
    ) THEN
        ALTER TABLE portal_roles DROP CONSTRAINT portal_roles_name_key;
    END IF;
END $$;

-- Ensure unique constraint exists on portal_roles.code
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'portal_roles_code_unique' 
        AND conrelid = 'portal_roles'::regclass
    ) THEN
        ALTER TABLE portal_roles ADD CONSTRAINT portal_roles_code_unique UNIQUE (code);
    END IF;
END $$;

-- Ensure unique constraint exists on portal_permissions.name
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'portal_permissions_name_unique' 
        AND conrelid = 'portal_permissions'::regclass
    ) THEN
        ALTER TABLE portal_permissions ADD CONSTRAINT portal_permissions_name_unique UNIQUE (name);
    END IF;
END $$;

-- Reset sequences BEFORE inserts to prevent conflicts with existing IDs
SELECT setval('portal_roles_id_seq', COALESCE((SELECT MAX(id) FROM portal_roles), 0) + 1, false);
SELECT setval('portal_permissions_id_seq', COALESCE((SELECT MAX(id) FROM portal_permissions), 0) + 1, false);

-- -----------------------------------------------------------------------------
-- STEP 1 — Seed 27 portal roles (idempotent with ON CONFLICT)
-- -----------------------------------------------------------------------------
-- Use DO UPDATE SET to handle conflicts on both code and name constraints
-- If a role with the same code exists, update its name and description
-- This handles the case where both code and name may have unique constraints
INSERT INTO portal_roles (name, description, code)
VALUES
    ('Administrator',               'Full system access — all modules, all actions',                                          'ADMIN'),
    ('Project Manager',             'Manages project lifecycle, team, BOQ, progress and quality',                             'PROJECT_MANAGER'),
    ('Site Engineer',               'On-site technical supervision, site reports, observations, attendance',                  'SITE_ENGINEER'),
    ('Procurement Officer',         'Procurement approvals, purchase orders, vendor quotes, GRN',                             'PROCUREMENT_OFFICER'),
    ('Inventory Manager',           'Stock management, GRN reconciliation, material tracking',                                'INVENTORY_MANAGER'),
    ('Finance Officer',             'Payments, invoices, financial reports, budget tracking',                                 'FINANCE_OFFICER'),
    ('HR Manager',                  'Staff management, attendance oversight, payroll coordination',                           'HR_MANAGER'),
    ('Sales Team Member',           'Lead management, customer acquisition, quotations',                                     'SALES'),
    ('Quality & Safety Officer',    'QC inspections, safety observations, snag management, audits',                          'QUALITY_SAFETY'),
    ('Employee',                    'Limited access — tasks and documents only',                                              'EMPLOYEE'),
    ('Site Supervisor',             'Site oversight, daily reports, attendance, gallery updates',                             'SITE_SUPERVISOR'),
    ('Estimator / Quantity Surveyor','BOQ creation, quantity take-offs, cost estimation',                                    'ESTIMATOR'),
    ('Architect / Designer',        'Design documents, architectural drawings, BOQ reference',                                'ARCHITECT_DESIGNER'),
    ('3D Visualizer / VAR Specialist','3D renders, visual presentations, gallery management',                                 'VISUALIZER'),
    ('Structural Engineer',         'Structural drawings, site reports, QC reference',                                       'STRUCTURAL_ENGINEER'),
    ('Interior Designer',           'Interior design documents, snag tracking, gallery',                                     'INTERIOR_DESIGNER'),
    ('Purchase Assistant',          'Supports procurement — raises indent requests, tracks orders',                           'PURCHASE_ASSISTANT'),
    ('Accounts Assistant',          'Supports finance — data entry, payment tracking, BOQ reference',                        'ACCOUNTS_ASSISTANT'),
    ('Admin Executive',             'Office administration, user onboarding, document filing',                                'ADMIN_EXECUTIVE'),
    ('Client Coordinator',          'Client communication, lead follow-up, query management',                                 'CLIENT_COORDINATOR'),
    ('Marketing Executive',         'Lead generation, marketing campaigns, reports',                                         'MARKETING'),
    ('CRM Executive',               'Full CRM — leads, customers, queries, pipeline management',                              'CRM'),
    ('IT / Systems Administrator',  'User account management, system configuration',                                         'IT_ADMIN'),
    ('Draftsman',                   'CAD drawings, document preparation, gallery uploads',                                   'DRAFTSMAN'),
    ('Foreman',                     'Field team management, labour attendance, task execution',                               'FOREMAN'),
    ('MEP Supervisor',              'MEP systems oversight, site reports, QC, task management',                              'MEP_SUPERVISOR'),
    ('Intern / Trainee',            'Read-only access to dashboard, documents and gallery',                                  'INTERN')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- -----------------------------------------------------------------------------
-- STEP 2 — Seed all portal permissions (ON CONFLICT DO NOTHING = idempotent)
-- -----------------------------------------------------------------------------
INSERT INTO portal_permissions (name, description) VALUES
    -- Dashboard
    ('DASHBOARD_VIEW',          'View the main dashboard and project summaries'),
    ('DASHBOARD_FILTER',        'Apply date/project filters on dashboard'),
    ('DASHBOARD_EXPORT',        'Export dashboard data'),
    -- Leads
    ('LEAD_VIEW',               'View lead list and lead details'),
    ('LEAD_CREATE',             'Create new leads'),
    ('LEAD_EDIT',               'Edit and update leads'),
    ('LEAD_DELETE',             'Delete leads'),
    ('LEAD_EXPORT',             'Export lead data to CSV/Excel'),
    -- Projects
    ('PROJECT_VIEW',            'View project list and project details'),
    ('PROJECT_CREATE',          'Create new projects'),
    ('PROJECT_EDIT',            'Edit project details'),
    ('PROJECT_DELETE',          'Delete projects'),
    -- Customers
    ('CUSTOMER_VIEW',           'View customer records'),
    ('CUSTOMER_CREATE',         'Create new customer records'),
    ('CUSTOMER_EDIT',           'Edit customer records'),
    ('CUSTOMER_DELETE',         'Delete customer records'),
    -- Tasks
    ('TASK_VIEW',               'View tasks'),
    ('TASK_CREATE',             'Create tasks'),
    ('TASK_EDIT',               'Edit and update tasks'),
    ('TASK_DELETE',             'Delete tasks'),
    -- Portal Users
    ('PORTAL_USER_VIEW',        'View portal user accounts'),
    ('PORTAL_USER_CREATE',      'Create portal user accounts'),
    ('PORTAL_USER_EDIT',        'Edit portal user accounts and role assignment'),
    ('PORTAL_USER_DELETE',      'Deactivate or delete portal user accounts'),
    -- Reports
    ('REPORT_VIEW',             'View reports and analytics'),
    ('REPORT_EXPORT',           'Export reports to PDF/Excel'),
    -- Finance
    ('FINANCE_VIEW',            'View financial records, budgets and cost summaries'),
    ('FINANCE_CREATE',          'Create financial entries'),
    ('FINANCE_EDIT',            'Edit financial entries'),
    -- Payments
    ('PAYMENT_VIEW',            'View payment schedules and transactions'),
    ('PAYMENT_CREATE',          'Record payments'),
    ('PAYMENT_EDIT',            'Edit payment records'),
    ('PAYMENT_APPROVE',         'Approve payment disbursements'),
    -- BOQ
    ('BOQ_VIEW',                'View Bill of Quantities'),
    ('BOQ_CREATE',              'Create BOQ items'),
    ('BOQ_EDIT',                'Edit BOQ items and quantities'),
    ('BOQ_DELETE',              'Delete BOQ items'),
    ('BOQ_APPROVE',             'Approve BOQ items and rate changes'),
    -- Documents
    ('DOCUMENT_VIEW',           'View and download documents'),
    ('DOCUMENT_CREATE',         'Upload documents'),
    ('DOCUMENT_DELETE',         'Delete documents'),
    -- Site Reports
    ('SITE_REPORT_VIEW',        'View site progress reports'),
    ('SITE_REPORT_CREATE',      'Create site progress reports'),
    ('SITE_REPORT_EDIT',        'Edit site progress reports'),
    -- Gallery
    ('GALLERY_VIEW',            'View project photo gallery'),
    ('GALLERY_CREATE',          'Upload photos to gallery'),
    ('GALLERY_DELETE',          'Delete gallery photos'),
    -- Labour
    ('LABOUR_VIEW',             'View labour contracts and attendance'),
    ('LABOUR_CREATE',           'Add labour contracts'),
    ('LABOUR_EDIT',             'Edit labour records and payroll'),
    -- Procurement
    ('PROCUREMENT_VIEW',        'View purchase orders, indent requests and vendor quotes'),
    ('PROCUREMENT_CREATE',      'Raise material indent requests and purchase orders'),
    ('PROCUREMENT_EDIT',        'Edit procurement records'),
    ('PROCUREMENT_APPROVE',     'Approve purchase orders and vendor quotes'),
    -- Inventory
    ('INVENTORY_VIEW',          'View inventory stock levels and GRN'),
    ('INVENTORY_CREATE',        'Add inventory entries and GRN'),
    ('INVENTORY_EDIT',          'Edit inventory records'),
    ('INVENTORY_DELETE',        'Delete inventory entries'),
    -- Quality Checks
    ('QC_VIEW',                 'View quality check records'),
    ('QC_CREATE',               'Create quality check inspections'),
    ('QC_EDIT',                 'Edit quality check records'),
    -- Observations
    ('OBSERVATION_VIEW',        'View site observations'),
    ('OBSERVATION_CREATE',      'Log site observations'),
    ('OBSERVATION_EDIT',        'Edit observations and resolution notes'),
    -- Snags
    ('SNAG_VIEW',               'View snag list'),
    ('SNAG_CREATE',             'Add snag items'),
    ('SNAG_EDIT',               'Edit and resolve snags'),
    -- Queries
    ('QUERY_VIEW',              'View project queries'),
    ('QUERY_CREATE',            'Raise project queries'),
    ('QUERY_EDIT',              'Edit and respond to queries'),
    -- Attendance
    ('ATTENDANCE_VIEW',         'View attendance records'),
    ('ATTENDANCE_CREATE',       'Mark attendance'),
    ('ATTENDANCE_EDIT',         'Edit attendance records'),
    -- Notifications
    ('NOTIFICATION_VIEW',       'View in-app notifications')
ON CONFLICT (name) DO NOTHING;

-- -----------------------------------------------------------------------------
-- STEP 3 — Seed portal_role_permissions mappings
--           Uses a VALUES table join — fully idempotent (WHERE NOT EXISTS)
-- -----------------------------------------------------------------------------
WITH role_permission_mapping(role_code, perm_name) AS (
    VALUES
    -- =========================================================================
    -- ADMIN — all permissions
    -- =========================================================================
    ('ADMIN','DASHBOARD_VIEW'),    ('ADMIN','DASHBOARD_FILTER'),  ('ADMIN','DASHBOARD_EXPORT'),
    ('ADMIN','LEAD_VIEW'),         ('ADMIN','LEAD_CREATE'),        ('ADMIN','LEAD_EDIT'),
    ('ADMIN','LEAD_DELETE'),       ('ADMIN','LEAD_EXPORT'),
    ('ADMIN','PROJECT_VIEW'),      ('ADMIN','PROJECT_CREATE'),     ('ADMIN','PROJECT_EDIT'),
    ('ADMIN','PROJECT_DELETE'),
    ('ADMIN','CUSTOMER_VIEW'),     ('ADMIN','CUSTOMER_CREATE'),    ('ADMIN','CUSTOMER_EDIT'),
    ('ADMIN','CUSTOMER_DELETE'),
    ('ADMIN','TASK_VIEW'),         ('ADMIN','TASK_CREATE'),        ('ADMIN','TASK_EDIT'),
    ('ADMIN','TASK_DELETE'),
    ('ADMIN','PORTAL_USER_VIEW'),  ('ADMIN','PORTAL_USER_CREATE'), ('ADMIN','PORTAL_USER_EDIT'),
    ('ADMIN','PORTAL_USER_DELETE'),
    ('ADMIN','REPORT_VIEW'),       ('ADMIN','REPORT_EXPORT'),
    ('ADMIN','FINANCE_VIEW'),      ('ADMIN','FINANCE_CREATE'),     ('ADMIN','FINANCE_EDIT'),
    ('ADMIN','PAYMENT_VIEW'),      ('ADMIN','PAYMENT_CREATE'),     ('ADMIN','PAYMENT_EDIT'),
    ('ADMIN','PAYMENT_APPROVE'),
    ('ADMIN','BOQ_VIEW'),          ('ADMIN','BOQ_CREATE'),         ('ADMIN','BOQ_EDIT'),
    ('ADMIN','BOQ_DELETE'),        ('ADMIN','BOQ_APPROVE'),
    ('ADMIN','DOCUMENT_VIEW'),     ('ADMIN','DOCUMENT_CREATE'),    ('ADMIN','DOCUMENT_DELETE'),
    ('ADMIN','SITE_REPORT_VIEW'),  ('ADMIN','SITE_REPORT_CREATE'), ('ADMIN','SITE_REPORT_EDIT'),
    ('ADMIN','GALLERY_VIEW'),      ('ADMIN','GALLERY_CREATE'),     ('ADMIN','GALLERY_DELETE'),
    ('ADMIN','LABOUR_VIEW'),       ('ADMIN','LABOUR_CREATE'),      ('ADMIN','LABOUR_EDIT'),
    ('ADMIN','PROCUREMENT_VIEW'),  ('ADMIN','PROCUREMENT_CREATE'), ('ADMIN','PROCUREMENT_EDIT'),
    ('ADMIN','PROCUREMENT_APPROVE'),
    ('ADMIN','INVENTORY_VIEW'),    ('ADMIN','INVENTORY_CREATE'),   ('ADMIN','INVENTORY_EDIT'),
    ('ADMIN','INVENTORY_DELETE'),
    ('ADMIN','QC_VIEW'),           ('ADMIN','QC_CREATE'),          ('ADMIN','QC_EDIT'),
    ('ADMIN','OBSERVATION_VIEW'),  ('ADMIN','OBSERVATION_CREATE'), ('ADMIN','OBSERVATION_EDIT'),
    ('ADMIN','SNAG_VIEW'),         ('ADMIN','SNAG_CREATE'),        ('ADMIN','SNAG_EDIT'),
    ('ADMIN','QUERY_VIEW'),        ('ADMIN','QUERY_CREATE'),       ('ADMIN','QUERY_EDIT'),
    ('ADMIN','ATTENDANCE_VIEW'),   ('ADMIN','ATTENDANCE_CREATE'),  ('ADMIN','ATTENDANCE_EDIT'),
    ('ADMIN','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- PROJECT_MANAGER
    -- =========================================================================
    ('PROJECT_MANAGER','DASHBOARD_VIEW'),       ('PROJECT_MANAGER','DASHBOARD_FILTER'),
    ('PROJECT_MANAGER','LEAD_VIEW'),
    ('PROJECT_MANAGER','PROJECT_VIEW'),         ('PROJECT_MANAGER','PROJECT_CREATE'),
    ('PROJECT_MANAGER','PROJECT_EDIT'),
    ('PROJECT_MANAGER','CUSTOMER_VIEW'),
    ('PROJECT_MANAGER','TASK_VIEW'),            ('PROJECT_MANAGER','TASK_CREATE'),
    ('PROJECT_MANAGER','TASK_EDIT'),            ('PROJECT_MANAGER','TASK_DELETE'),
    ('PROJECT_MANAGER','FINANCE_VIEW'),
    ('PROJECT_MANAGER','BOQ_VIEW'),             ('PROJECT_MANAGER','BOQ_CREATE'),
    ('PROJECT_MANAGER','BOQ_EDIT'),             ('PROJECT_MANAGER','BOQ_APPROVE'),
    ('PROJECT_MANAGER','DOCUMENT_VIEW'),        ('PROJECT_MANAGER','DOCUMENT_CREATE'),
    ('PROJECT_MANAGER','DOCUMENT_DELETE'),
    ('PROJECT_MANAGER','SITE_REPORT_VIEW'),     ('PROJECT_MANAGER','SITE_REPORT_CREATE'),
    ('PROJECT_MANAGER','SITE_REPORT_EDIT'),
    ('PROJECT_MANAGER','GALLERY_VIEW'),         ('PROJECT_MANAGER','GALLERY_CREATE'),
    ('PROJECT_MANAGER','GALLERY_DELETE'),
    ('PROJECT_MANAGER','LABOUR_VIEW'),          ('PROJECT_MANAGER','LABOUR_EDIT'),
    ('PROJECT_MANAGER','PROCUREMENT_VIEW'),
    ('PROJECT_MANAGER','QC_VIEW'),              ('PROJECT_MANAGER','QC_CREATE'),
    ('PROJECT_MANAGER','QC_EDIT'),
    ('PROJECT_MANAGER','OBSERVATION_VIEW'),     ('PROJECT_MANAGER','OBSERVATION_CREATE'),
    ('PROJECT_MANAGER','OBSERVATION_EDIT'),
    ('PROJECT_MANAGER','SNAG_VIEW'),            ('PROJECT_MANAGER','SNAG_CREATE'),
    ('PROJECT_MANAGER','SNAG_EDIT'),
    ('PROJECT_MANAGER','QUERY_VIEW'),           ('PROJECT_MANAGER','QUERY_CREATE'),
    ('PROJECT_MANAGER','QUERY_EDIT'),
    ('PROJECT_MANAGER','ATTENDANCE_VIEW'),
    ('PROJECT_MANAGER','REPORT_VIEW'),
    ('PROJECT_MANAGER','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- SITE_ENGINEER
    -- =========================================================================
    ('SITE_ENGINEER','DASHBOARD_VIEW'),
    ('SITE_ENGINEER','PROJECT_VIEW'),
    ('SITE_ENGINEER','TASK_VIEW'),          ('SITE_ENGINEER','TASK_CREATE'),
    ('SITE_ENGINEER','TASK_EDIT'),
    ('SITE_ENGINEER','SITE_REPORT_VIEW'),   ('SITE_ENGINEER','SITE_REPORT_CREATE'),
    ('SITE_ENGINEER','SITE_REPORT_EDIT'),
    ('SITE_ENGINEER','GALLERY_VIEW'),       ('SITE_ENGINEER','GALLERY_CREATE'),
    ('SITE_ENGINEER','OBSERVATION_VIEW'),   ('SITE_ENGINEER','OBSERVATION_CREATE'),
    ('SITE_ENGINEER','OBSERVATION_EDIT'),
    ('SITE_ENGINEER','QC_VIEW'),            ('SITE_ENGINEER','QC_CREATE'),
    ('SITE_ENGINEER','SNAG_VIEW'),          ('SITE_ENGINEER','SNAG_CREATE'),
    ('SITE_ENGINEER','QUERY_VIEW'),         ('SITE_ENGINEER','QUERY_CREATE'),
    ('SITE_ENGINEER','DOCUMENT_VIEW'),
    ('SITE_ENGINEER','ATTENDANCE_VIEW'),    ('SITE_ENGINEER','ATTENDANCE_CREATE'),
    ('SITE_ENGINEER','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- PROCUREMENT_OFFICER
    -- =========================================================================
    ('PROCUREMENT_OFFICER','DASHBOARD_VIEW'),
    ('PROCUREMENT_OFFICER','PROJECT_VIEW'),
    ('PROCUREMENT_OFFICER','BOQ_VIEW'),
    ('PROCUREMENT_OFFICER','PROCUREMENT_VIEW'),     ('PROCUREMENT_OFFICER','PROCUREMENT_CREATE'),
    ('PROCUREMENT_OFFICER','PROCUREMENT_EDIT'),     ('PROCUREMENT_OFFICER','PROCUREMENT_APPROVE'),
    ('PROCUREMENT_OFFICER','INVENTORY_VIEW'),       ('PROCUREMENT_OFFICER','INVENTORY_EDIT'),
    ('PROCUREMENT_OFFICER','REPORT_VIEW'),
    ('PROCUREMENT_OFFICER','DOCUMENT_VIEW'),
    ('PROCUREMENT_OFFICER','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- INVENTORY_MANAGER
    -- =========================================================================
    ('INVENTORY_MANAGER','DASHBOARD_VIEW'),
    ('INVENTORY_MANAGER','INVENTORY_VIEW'),     ('INVENTORY_MANAGER','INVENTORY_CREATE'),
    ('INVENTORY_MANAGER','INVENTORY_EDIT'),     ('INVENTORY_MANAGER','INVENTORY_DELETE'),
    ('INVENTORY_MANAGER','PROCUREMENT_VIEW'),
    ('INVENTORY_MANAGER','REPORT_VIEW'),
    ('INVENTORY_MANAGER','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- FINANCE_OFFICER
    -- =========================================================================
    ('FINANCE_OFFICER','DASHBOARD_VIEW'),   ('FINANCE_OFFICER','DASHBOARD_FILTER'),
    ('FINANCE_OFFICER','FINANCE_VIEW'),     ('FINANCE_OFFICER','FINANCE_CREATE'),
    ('FINANCE_OFFICER','FINANCE_EDIT'),
    ('FINANCE_OFFICER','PAYMENT_VIEW'),     ('FINANCE_OFFICER','PAYMENT_CREATE'),
    ('FINANCE_OFFICER','PAYMENT_EDIT'),     ('FINANCE_OFFICER','PAYMENT_APPROVE'),
    ('FINANCE_OFFICER','BOQ_VIEW'),
    ('FINANCE_OFFICER','REPORT_VIEW'),      ('FINANCE_OFFICER','REPORT_EXPORT'),
    ('FINANCE_OFFICER','PROJECT_VIEW'),
    ('FINANCE_OFFICER','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- HR_MANAGER
    -- =========================================================================
    ('HR_MANAGER','DASHBOARD_VIEW'),
    ('HR_MANAGER','PORTAL_USER_VIEW'),      ('HR_MANAGER','PORTAL_USER_CREATE'),
    ('HR_MANAGER','PORTAL_USER_EDIT'),      ('HR_MANAGER','PORTAL_USER_DELETE'),
    ('HR_MANAGER','ATTENDANCE_VIEW'),       ('HR_MANAGER','ATTENDANCE_CREATE'),
    ('HR_MANAGER','ATTENDANCE_EDIT'),
    ('HR_MANAGER','REPORT_VIEW'),
    ('HR_MANAGER','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- SALES
    -- =========================================================================
    ('SALES','DASHBOARD_VIEW'),     ('SALES','DASHBOARD_FILTER'),
    ('SALES','LEAD_VIEW'),          ('SALES','LEAD_CREATE'),         ('SALES','LEAD_EDIT'),
    ('SALES','CUSTOMER_VIEW'),      ('SALES','CUSTOMER_CREATE'),     ('SALES','CUSTOMER_EDIT'),
    ('SALES','REPORT_VIEW'),        ('SALES','REPORT_EXPORT'),
    ('SALES','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- QUALITY_SAFETY
    -- =========================================================================
    ('QUALITY_SAFETY','DASHBOARD_VIEW'),
    ('QUALITY_SAFETY','PROJECT_VIEW'),
    ('QUALITY_SAFETY','QC_VIEW'),           ('QUALITY_SAFETY','QC_CREATE'),
    ('QUALITY_SAFETY','QC_EDIT'),
    ('QUALITY_SAFETY','OBSERVATION_VIEW'),  ('QUALITY_SAFETY','OBSERVATION_CREATE'),
    ('QUALITY_SAFETY','OBSERVATION_EDIT'),
    ('QUALITY_SAFETY','SNAG_VIEW'),         ('QUALITY_SAFETY','SNAG_CREATE'),
    ('QUALITY_SAFETY','SNAG_EDIT'),
    ('QUALITY_SAFETY','SITE_REPORT_VIEW'),
    ('QUALITY_SAFETY','REPORT_VIEW'),       ('QUALITY_SAFETY','REPORT_EXPORT'),
    ('QUALITY_SAFETY','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- EMPLOYEE
    -- =========================================================================
    ('EMPLOYEE','DASHBOARD_VIEW'),
    ('EMPLOYEE','TASK_VIEW'),
    ('EMPLOYEE','DOCUMENT_VIEW'),
    ('EMPLOYEE','PROJECT_VIEW'),
    ('EMPLOYEE','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- SITE_SUPERVISOR
    -- =========================================================================
    ('SITE_SUPERVISOR','DASHBOARD_VIEW'),
    ('SITE_SUPERVISOR','PROJECT_VIEW'),
    ('SITE_SUPERVISOR','TASK_VIEW'),            ('SITE_SUPERVISOR','TASK_EDIT'),
    ('SITE_SUPERVISOR','SITE_REPORT_VIEW'),     ('SITE_SUPERVISOR','SITE_REPORT_CREATE'),
    ('SITE_SUPERVISOR','SITE_REPORT_EDIT'),
    ('SITE_SUPERVISOR','GALLERY_VIEW'),         ('SITE_SUPERVISOR','GALLERY_CREATE'),
    ('SITE_SUPERVISOR','OBSERVATION_VIEW'),     ('SITE_SUPERVISOR','OBSERVATION_CREATE'),
    ('SITE_SUPERVISOR','OBSERVATION_EDIT'),
    ('SITE_SUPERVISOR','ATTENDANCE_VIEW'),      ('SITE_SUPERVISOR','ATTENDANCE_CREATE'),
    ('SITE_SUPERVISOR','QC_VIEW'),              ('SITE_SUPERVISOR','QC_CREATE'),
    ('SITE_SUPERVISOR','DOCUMENT_VIEW'),
    ('SITE_SUPERVISOR','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- ESTIMATOR
    -- =========================================================================
    ('ESTIMATOR','DASHBOARD_VIEW'),
    ('ESTIMATOR','BOQ_VIEW'),       ('ESTIMATOR','BOQ_CREATE'),
    ('ESTIMATOR','BOQ_EDIT'),       ('ESTIMATOR','BOQ_DELETE'),
    ('ESTIMATOR','PROCUREMENT_VIEW'),
    ('ESTIMATOR','PROJECT_VIEW'),
    ('ESTIMATOR','FINANCE_VIEW'),
    ('ESTIMATOR','DOCUMENT_VIEW'),  ('ESTIMATOR','DOCUMENT_CREATE'),
    ('ESTIMATOR','REPORT_VIEW'),
    ('ESTIMATOR','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- ARCHITECT_DESIGNER
    -- =========================================================================
    ('ARCHITECT_DESIGNER','DASHBOARD_VIEW'),
    ('ARCHITECT_DESIGNER','PROJECT_VIEW'),
    ('ARCHITECT_DESIGNER','DOCUMENT_VIEW'),     ('ARCHITECT_DESIGNER','DOCUMENT_CREATE'),
    ('ARCHITECT_DESIGNER','DOCUMENT_DELETE'),
    ('ARCHITECT_DESIGNER','GALLERY_VIEW'),      ('ARCHITECT_DESIGNER','GALLERY_CREATE'),
    ('ARCHITECT_DESIGNER','BOQ_VIEW'),
    ('ARCHITECT_DESIGNER','SITE_REPORT_VIEW'),
    ('ARCHITECT_DESIGNER','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- VISUALIZER
    -- =========================================================================
    ('VISUALIZER','DASHBOARD_VIEW'),
    ('VISUALIZER','GALLERY_VIEW'),      ('VISUALIZER','GALLERY_CREATE'),
    ('VISUALIZER','GALLERY_DELETE'),
    ('VISUALIZER','DOCUMENT_VIEW'),     ('VISUALIZER','DOCUMENT_CREATE'),
    ('VISUALIZER','PROJECT_VIEW'),
    ('VISUALIZER','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- STRUCTURAL_ENGINEER
    -- =========================================================================
    ('STRUCTURAL_ENGINEER','DASHBOARD_VIEW'),
    ('STRUCTURAL_ENGINEER','PROJECT_VIEW'),
    ('STRUCTURAL_ENGINEER','DOCUMENT_VIEW'),    ('STRUCTURAL_ENGINEER','DOCUMENT_CREATE'),
    ('STRUCTURAL_ENGINEER','BOQ_VIEW'),
    ('STRUCTURAL_ENGINEER','SITE_REPORT_VIEW'),
    ('STRUCTURAL_ENGINEER','QC_VIEW'),          ('STRUCTURAL_ENGINEER','QC_CREATE'),
    ('STRUCTURAL_ENGINEER','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- INTERIOR_DESIGNER
    -- =========================================================================
    ('INTERIOR_DESIGNER','DASHBOARD_VIEW'),
    ('INTERIOR_DESIGNER','PROJECT_VIEW'),
    ('INTERIOR_DESIGNER','DOCUMENT_VIEW'),      ('INTERIOR_DESIGNER','DOCUMENT_CREATE'),
    ('INTERIOR_DESIGNER','GALLERY_VIEW'),       ('INTERIOR_DESIGNER','GALLERY_CREATE'),
    ('INTERIOR_DESIGNER','SNAG_VIEW'),          ('INTERIOR_DESIGNER','SNAG_CREATE'),
    ('INTERIOR_DESIGNER','SNAG_EDIT'),
    ('INTERIOR_DESIGNER','BOQ_VIEW'),
    ('INTERIOR_DESIGNER','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- PURCHASE_ASSISTANT
    -- =========================================================================
    ('PURCHASE_ASSISTANT','DASHBOARD_VIEW'),
    ('PURCHASE_ASSISTANT','PROCUREMENT_VIEW'),   ('PURCHASE_ASSISTANT','PROCUREMENT_CREATE'),
    ('PURCHASE_ASSISTANT','INVENTORY_VIEW'),
    ('PURCHASE_ASSISTANT','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- ACCOUNTS_ASSISTANT
    -- =========================================================================
    ('ACCOUNTS_ASSISTANT','DASHBOARD_VIEW'),
    ('ACCOUNTS_ASSISTANT','FINANCE_VIEW'),   ('ACCOUNTS_ASSISTANT','FINANCE_CREATE'),
    ('ACCOUNTS_ASSISTANT','PAYMENT_VIEW'),
    ('ACCOUNTS_ASSISTANT','BOQ_VIEW'),
    ('ACCOUNTS_ASSISTANT','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- ADMIN_EXECUTIVE
    -- =========================================================================
    ('ADMIN_EXECUTIVE','DASHBOARD_VIEW'),
    ('ADMIN_EXECUTIVE','PORTAL_USER_VIEW'),     ('ADMIN_EXECUTIVE','PORTAL_USER_CREATE'),
    ('ADMIN_EXECUTIVE','ATTENDANCE_VIEW'),
    ('ADMIN_EXECUTIVE','DOCUMENT_VIEW'),        ('ADMIN_EXECUTIVE','DOCUMENT_CREATE'),
    ('ADMIN_EXECUTIVE','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- CLIENT_COORDINATOR
    -- =========================================================================
    ('CLIENT_COORDINATOR','DASHBOARD_VIEW'),
    ('CLIENT_COORDINATOR','LEAD_VIEW'),         ('CLIENT_COORDINATOR','LEAD_CREATE'),
    ('CLIENT_COORDINATOR','LEAD_EDIT'),
    ('CLIENT_COORDINATOR','CUSTOMER_VIEW'),     ('CLIENT_COORDINATOR','CUSTOMER_EDIT'),
    ('CLIENT_COORDINATOR','QUERY_VIEW'),        ('CLIENT_COORDINATOR','QUERY_CREATE'),
    ('CLIENT_COORDINATOR','QUERY_EDIT'),
    ('CLIENT_COORDINATOR','PROJECT_VIEW'),
    ('CLIENT_COORDINATOR','DOCUMENT_VIEW'),
    ('CLIENT_COORDINATOR','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- MARKETING
    -- =========================================================================
    ('MARKETING','DASHBOARD_VIEW'),     ('MARKETING','DASHBOARD_FILTER'),
    ('MARKETING','LEAD_VIEW'),          ('MARKETING','LEAD_CREATE'),
    ('MARKETING','REPORT_VIEW'),        ('MARKETING','REPORT_EXPORT'),
    ('MARKETING','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- CRM
    -- =========================================================================
    ('CRM','DASHBOARD_VIEW'),       ('CRM','DASHBOARD_FILTER'),
    ('CRM','LEAD_VIEW'),            ('CRM','LEAD_CREATE'),           ('CRM','LEAD_EDIT'),
    ('CRM','LEAD_DELETE'),          ('CRM','LEAD_EXPORT'),
    ('CRM','CUSTOMER_VIEW'),        ('CRM','CUSTOMER_CREATE'),       ('CRM','CUSTOMER_EDIT'),
    ('CRM','QUERY_VIEW'),           ('CRM','QUERY_CREATE'),          ('CRM','QUERY_EDIT'),
    ('CRM','REPORT_VIEW'),
    ('CRM','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- IT_ADMIN
    -- =========================================================================
    ('IT_ADMIN','DASHBOARD_VIEW'),
    ('IT_ADMIN','PORTAL_USER_VIEW'),    ('IT_ADMIN','PORTAL_USER_CREATE'),
    ('IT_ADMIN','PORTAL_USER_EDIT'),    ('IT_ADMIN','PORTAL_USER_DELETE'),
    ('IT_ADMIN','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- DRAFTSMAN
    -- =========================================================================
    ('DRAFTSMAN','DASHBOARD_VIEW'),
    ('DRAFTSMAN','DOCUMENT_VIEW'),  ('DRAFTSMAN','DOCUMENT_CREATE'),
    ('DRAFTSMAN','GALLERY_VIEW'),
    ('DRAFTSMAN','PROJECT_VIEW'),
    ('DRAFTSMAN','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- FOREMAN
    -- =========================================================================
    ('FOREMAN','DASHBOARD_VIEW'),
    ('FOREMAN','PROJECT_VIEW'),
    ('FOREMAN','TASK_VIEW'),            ('FOREMAN','TASK_CREATE'),      ('FOREMAN','TASK_EDIT'),
    ('FOREMAN','LABOUR_VIEW'),          ('FOREMAN','LABOUR_EDIT'),
    ('FOREMAN','ATTENDANCE_VIEW'),      ('FOREMAN','ATTENDANCE_CREATE'),
    ('FOREMAN','ATTENDANCE_EDIT'),
    ('FOREMAN','SITE_REPORT_VIEW'),     ('FOREMAN','SITE_REPORT_CREATE'),
    ('FOREMAN','OBSERVATION_VIEW'),     ('FOREMAN','OBSERVATION_CREATE'),
    ('FOREMAN','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- MEP_SUPERVISOR
    -- =========================================================================
    ('MEP_SUPERVISOR','DASHBOARD_VIEW'),
    ('MEP_SUPERVISOR','PROJECT_VIEW'),
    ('MEP_SUPERVISOR','TASK_VIEW'),             ('MEP_SUPERVISOR','TASK_CREATE'),
    ('MEP_SUPERVISOR','TASK_EDIT'),
    ('MEP_SUPERVISOR','SITE_REPORT_VIEW'),      ('MEP_SUPERVISOR','SITE_REPORT_CREATE'),
    ('MEP_SUPERVISOR','SITE_REPORT_EDIT'),
    ('MEP_SUPERVISOR','OBSERVATION_VIEW'),      ('MEP_SUPERVISOR','OBSERVATION_CREATE'),
    ('MEP_SUPERVISOR','QC_VIEW'),               ('MEP_SUPERVISOR','QC_CREATE'),
    ('MEP_SUPERVISOR','DOCUMENT_VIEW'),
    ('MEP_SUPERVISOR','NOTIFICATION_VIEW'),

    -- =========================================================================
    -- INTERN
    -- =========================================================================
    ('INTERN','DASHBOARD_VIEW'),
    ('INTERN','DOCUMENT_VIEW'),
    ('INTERN','GALLERY_VIEW'),
    ('INTERN','NOTIFICATION_VIEW')
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
