-- =============================================================================
-- Verification script for V31 — standardize portal_users emails
-- =============================================================================
-- Run AFTER V31__standardize_portal_user_emails.sql is applied. This file is
-- NOT a Flyway migration — run it manually in your DB tool.
--
-- Every query below is a SELECT. Nothing here mutates data.
-- Expected results are documented in the comment above each query.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. Non-compliant emails
-- -----------------------------------------------------------------------------
-- Expect: 0 rows.
-- Any row returned means V31 did not rewrite that user's email.
SELECT id, email, role_id
FROM portal_users
WHERE email NOT LIKE '%walldot@outlook.com'
ORDER BY id;


-- -----------------------------------------------------------------------------
-- 2. Duplicate emails
-- -----------------------------------------------------------------------------
-- Expect: 0 rows.
-- The UNIQUE constraint on portal_users.email would have rejected the
-- migration if this ever produced duplicates, but this query is the
-- belt-and-braces check.
SELECT email, COUNT(*) AS duplicate_count
FROM portal_users
GROUP BY email
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, email;


-- -----------------------------------------------------------------------------
-- 3. Users with NULL role or invalid role reference
-- -----------------------------------------------------------------------------
-- Expect: 0 rows.
-- Any row returned means Step 1 (orphan reassignment) missed a user.
SELECT pu.id, pu.email, pu.role_id
FROM portal_users pu
LEFT JOIN portal_roles pr ON pr.id = pu.role_id
WHERE pu.role_id IS NULL
   OR pr.id IS NULL
ORDER BY pu.id;


-- -----------------------------------------------------------------------------
-- 4. Required-role coverage
-- -----------------------------------------------------------------------------
-- Expect: present_count = 27, missing_count = 0.
-- Confirms every required role exists in portal_roles.
WITH required(code) AS (
    VALUES
        ('ADMIN'),               ('PROJECT_MANAGER'),     ('SITE_ENGINEER'),
        ('PROCUREMENT_OFFICER'), ('INVENTORY_MANAGER'),   ('FINANCE_OFFICER'),
        ('HR_MANAGER'),          ('SALES'),               ('QUALITY_SAFETY'),
        ('EMPLOYEE'),            ('SITE_SUPERVISOR'),     ('ESTIMATOR'),
        ('ARCHITECT_DESIGNER'),  ('VISUALIZER'),          ('STRUCTURAL_ENGINEER'),
        ('INTERIOR_DESIGNER'),   ('PURCHASE_ASSISTANT'),  ('ACCOUNTS_ASSISTANT'),
        ('ADMIN_EXECUTIVE'),     ('CLIENT_COORDINATOR'),  ('MARKETING'),
        ('CRM'),                 ('IT_ADMIN'),            ('DRAFTSMAN'),
        ('FOREMAN'),             ('MEP_SUPERVISOR'),      ('INTERN')
)
SELECT
    COUNT(pr.id)                             AS present_count,
    COUNT(*) FILTER (WHERE pr.id IS NULL)    AS missing_count,
    ARRAY_AGG(r.code ORDER BY r.code) FILTER (WHERE pr.id IS NULL) AS missing_codes
FROM required r
LEFT JOIN portal_roles pr ON pr.code = r.code;


-- -----------------------------------------------------------------------------
-- 5. Per-role user distribution
-- -----------------------------------------------------------------------------
-- Sanity check. No specific expected numbers — this is for eyeballing that
-- EMPLOYEE absorbed any orphan users and no role has unexpected zero/huge
-- populations.
SELECT pr.code,
       pr.name,
       COUNT(pu.id) AS user_count
FROM portal_roles pr
LEFT JOIN portal_users pu ON pu.role_id = pr.id
GROUP BY pr.code, pr.name
ORDER BY user_count DESC, pr.code;


-- -----------------------------------------------------------------------------
-- 6. Spot-check: show the final email shape for each user
-- -----------------------------------------------------------------------------
-- Visual inspection aid. Joins portal_users to portal_roles so you can
-- verify that the email prefix matches the role code in every row.
SELECT pu.id,
       pu.email,
       pr.code AS role_code,
       pr.name AS role_name
FROM portal_users pu
JOIN portal_roles pr ON pr.id = pu.role_id
ORDER BY pr.code, pu.id;
