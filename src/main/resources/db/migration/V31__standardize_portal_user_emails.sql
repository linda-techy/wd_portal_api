-- =============================================================================
-- V31 — Standardize portal_users.email + role integrity safety-net
-- =============================================================================
-- Purpose
--   1. Guarantee the 27 required portal_roles exist (idempotent safety-net
--      copied from V5 — no-op on a healthy DB where V5 already ran).
--   2. Reassign any portal_users row with NULL or invalid role_id to EMPLOYEE.
--   3. Rewrite every portal_users.email to the form
--        <role-prefix>walldot@outlook.com
--      where <role-prefix> = lower(replace(portal_roles.code, '_', '')).
--      Duplicates within the same role are disambiguated by appending a
--      numeric suffix (2, 3, …) ordered by portal_users.id ASC.
--   4. Add the missing FK-side index on portal_users.role_id.
--
-- Target
--   Shared test DB first (46.202.164.251/wdTestDB). Promote to production
--   only after the verify script (docs/sql/verify_V31_email_standardization.sql)
--   returns the expected zero-row / 27-count results.
--
-- Idempotency
--   Every statement is safe to re-run.
--     * Role INSERT uses ON CONFLICT (code) DO UPDATE.
--     * Orphan fix is a conditional UPDATE (no-op when no orphans).
--     * Email rewrite uses IS DISTINCT FROM so compliant rows are skipped.
--     * Index uses CREATE INDEX IF NOT EXISTS.
--
-- Impact warning
--   This migration CHANGES LOGIN EMAILS for portal_users. Password hashes
--   survive, but any user relying on their old email as the login key must
--   be told the new address. Existing JWT tokens remain valid until expiry.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- STEP 0 — Safety-net: ensure all 27 required roles exist
-- -----------------------------------------------------------------------------
-- Mirrors V5's role seed. No-op on a DB where V5 already ran. Present so
-- Step 1 can always resolve 'EMPLOYEE' and Step 2 can always join to a
-- valid role_code.
INSERT INTO portal_roles (name, description, code)
VALUES
    ('Administrator',                'Full system access — all modules, all actions',                                          'ADMIN'),
    ('Project Manager',              'Manages project lifecycle, team, BOQ, progress and quality',                             'PROJECT_MANAGER'),
    ('Site Engineer',                'On-site technical supervision, site reports, observations, attendance',                  'SITE_ENGINEER'),
    ('Procurement Officer',          'Procurement approvals, purchase orders, vendor quotes, GRN',                             'PROCUREMENT_OFFICER'),
    ('Inventory Manager',            'Stock management, GRN reconciliation, material tracking',                                'INVENTORY_MANAGER'),
    ('Finance Officer',              'Payments, invoices, financial reports, budget tracking',                                 'FINANCE_OFFICER'),
    ('HR Manager',                   'Staff management, attendance oversight, payroll coordination',                           'HR_MANAGER'),
    ('Sales Team Member',            'Lead management, customer acquisition, quotations',                                      'SALES'),
    ('Quality & Safety Officer',     'QC inspections, safety observations, snag management, audits',                           'QUALITY_SAFETY'),
    ('Employee',                     'Limited access — tasks and documents only',                                              'EMPLOYEE'),
    ('Site Supervisor',              'Site oversight, daily reports, attendance, gallery updates',                             'SITE_SUPERVISOR'),
    ('Estimator / Quantity Surveyor','BOQ creation, quantity take-offs, cost estimation',                                      'ESTIMATOR'),
    ('Architect / Designer',         'Design documents, architectural drawings, BOQ reference',                                'ARCHITECT_DESIGNER'),
    ('3D Visualizer / VAR Specialist','3D renders, visual presentations, gallery management',                                  'VISUALIZER'),
    ('Structural Engineer',          'Structural drawings, site reports, QC reference',                                        'STRUCTURAL_ENGINEER'),
    ('Interior Designer',            'Interior design documents, snag tracking, gallery',                                      'INTERIOR_DESIGNER'),
    ('Purchase Assistant',           'Supports procurement — raises indent requests, tracks orders',                           'PURCHASE_ASSISTANT'),
    ('Accounts Assistant',           'Supports finance — data entry, payment tracking, BOQ reference',                         'ACCOUNTS_ASSISTANT'),
    ('Admin Executive',              'Office administration, user onboarding, document filing',                                'ADMIN_EXECUTIVE'),
    ('Client Coordinator',           'Client communication, lead follow-up, query management',                                 'CLIENT_COORDINATOR'),
    ('Marketing Executive',          'Lead generation, marketing campaigns, reports',                                          'MARKETING'),
    ('CRM Executive',                'Full CRM — leads, customers, queries, pipeline management',                              'CRM'),
    ('IT / Systems Administrator',   'User account management, system configuration',                                          'IT_ADMIN'),
    ('Draftsman',                    'CAD drawings, document preparation, gallery uploads',                                    'DRAFTSMAN'),
    ('Foreman',                      'Field team management, labour attendance, task execution',                               'FOREMAN'),
    ('MEP Supervisor',               'MEP systems oversight, site reports, QC, task management',                               'MEP_SUPERVISOR'),
    ('Intern / Trainee',             'Read-only access to dashboard, documents and gallery',                                   'INTERN')
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;


-- -----------------------------------------------------------------------------
-- STEP 1 — Reassign orphan users to EMPLOYEE
-- -----------------------------------------------------------------------------
-- MUST run before Step 2 so every user has a valid role to derive its email
-- prefix from.
UPDATE portal_users pu
SET role_id = (SELECT id FROM portal_roles WHERE code = 'EMPLOYEE')
WHERE pu.role_id IS NULL
   OR NOT EXISTS (
       SELECT 1 FROM portal_roles pr WHERE pr.id = pu.role_id
   );


-- -----------------------------------------------------------------------------
-- STEP 2 — Standardize email addresses
-- -----------------------------------------------------------------------------
-- Prefix rule:
--   lower(replace(portal_roles.code, '_', ''))
--   e.g. SITE_ENGINEER → siteengineer
--
-- Uniqueness rule:
--   ROW_NUMBER() OVER (PARTITION BY portal_roles.code ORDER BY portal_users.id)
--   First user in a role → no suffix.
--   Second → '2', third → '3', …
--
-- Idempotency:
--   IS DISTINCT FROM leaves already-compliant rows untouched.
WITH ranked AS (
    SELECT pu.id,
           LOWER(REPLACE(pr.code, '_', '')) AS base,
           ROW_NUMBER() OVER (
               PARTITION BY pr.code
               ORDER BY pu.id
           ) AS rn
    FROM portal_users pu
    JOIN portal_roles pr ON pr.id = pu.role_id
),
target AS (
    SELECT id,
           base
             || CASE WHEN rn = 1 THEN '' ELSE rn::text END
             || 'walldot@outlook.com' AS new_email
    FROM ranked
)
UPDATE portal_users pu
SET email = t.new_email
FROM target t
WHERE pu.id = t.id
  AND pu.email IS DISTINCT FROM t.new_email;


-- -----------------------------------------------------------------------------
-- STEP 3 — Index the FK side of portal_users.role_id
-- -----------------------------------------------------------------------------
-- Postgres does not automatically index the child side of a foreign key.
-- portal_users.role_id is joined on every authenticated request that loads
-- user permissions, so the missing index is a real gap.
CREATE INDEX IF NOT EXISTS idx_portal_users_role_id
    ON portal_users(role_id);
