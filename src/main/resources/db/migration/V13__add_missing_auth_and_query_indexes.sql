-- =============================================================================
-- V13: Add Missing Indexes for Auth Hot Path and Frequent Query Columns
--
-- Root cause: EXPLAIN ANALYZE revealed full sequential scans on tables hit
-- on every authenticated request (portal_role_permissions, portal_users,
-- portal_roles) and common filter columns with no indexes.
--
-- Priority ranking by seq_scan count from pg_stat_user_tables:
--   1. portal_role_permissions.role_id       → 6,757 seq scans (CRITICAL)
--   2. portal_users.email                    → auth hot path
--   3. portal_roles.code                     → role resolution
--   4. customer_projects project_status      → project list queries
--   5. leads composite (status, next_follow_up) → lead list/scheduler
--   6. document_categories.reference_type   → document queries
--   7. project_members FK columns            → project member lookups
--   8. site_report_photos.site_report_id    → photo list queries
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. AUTH HOT PATH — called on every authenticated API request
--    Without this, every permission check does a full scan of the permissions table.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id
    ON portal_role_permissions(role_id);

CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id
    ON portal_role_permissions(permission_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. USER EMAIL LOOKUP — every login + JWT token validation
-- ─────────────────────────────────────────────────────────────────────────────
CREATE UNIQUE INDEX IF NOT EXISTS idx_portal_users_email
    ON portal_users(email);

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. ROLE CODE LOOKUP — every role resolution
-- ─────────────────────────────────────────────────────────────────────────────
CREATE UNIQUE INDEX IF NOT EXISTS idx_portal_roles_code
    ON portal_roles(code);

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. CUSTOMER PROJECTS — project list/dashboard queries filter by status
--    Partial index: WHERE deleted_at IS NULL covers the common soft-delete filter
-- ─────────────────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_projects_status_active
    ON customer_projects(project_status)
    WHERE deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. LEADS — composite index for follow-up scheduler and status-filtered lists
--    Covers: WHERE lead_status NOT IN (...) AND next_follow_up < NOW() AND deleted_at IS NULL
-- ─────────────────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_leads_status_followup
    ON leads(lead_status, next_follow_up)
    WHERE deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. DOCUMENT CATEGORIES — reference_type filter used in document list queries
-- ─────────────────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_document_categories_reference_type
    ON document_categories(reference_type);

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. PROJECT MEMBERS — portal_user_id and project_id are both filter columns
-- ─────────────────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_project_members_portal_user_id
    ON project_members(portal_user_id);

CREATE INDEX IF NOT EXISTS idx_project_members_project_id
    ON project_members(project_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 8. SITE REPORT PHOTOS — site_report_id is the primary FK filter column
-- ─────────────────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_site_report_photos_report_id
    ON site_report_photos(site_report_id);
