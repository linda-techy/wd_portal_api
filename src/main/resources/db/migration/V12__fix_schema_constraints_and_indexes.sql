-- =============================================================================
-- V12: Fix Schema Constraints and Indexes
--
-- Fixes:
--   1. Drop duplicate FK constraint on tasks.project_id
--   2. Drop incorrect FKs on observations (reported_by_id/resolved_by_id → customer_users)
--   3. Drop duplicate UNIQUE constraint on customer_projects.project_uuid
--   4. Drop 18 duplicate indexes on leads, tasks, customer_projects
--   5. Add 7 missing indexes on frequently-queried columns
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. DUPLICATE FK: tasks.project_id has two constraints pointing to the same table
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS fk_task_project;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. WRONG FK: observations.reported_by_id and resolved_by_id point to
--    customer_users but the entity maps them to portal_users. Having both FK
--    constraints simultaneously is impossible to satisfy (IDs must exist in
--    BOTH tables at once), blocking all observation inserts.
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE observations DROP CONSTRAINT IF EXISTS fkh2iq646mb3751rf8t4mx5tiqg;  -- reported_by_id → customer_users
ALTER TABLE observations DROP CONSTRAINT IF EXISTS fk6ppqi5hu02gtxr6tsvbch7u3w;  -- resolved_by_id → customer_users

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. DUPLICATE UNIQUE: customer_projects has uk_project_uuid AND uk_public_id
--    both enforcing uniqueness on the same project_uuid column.
--    uk_public_id is referenced by project_design_steps FK, so drop uk_project_uuid.
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE customer_projects DROP CONSTRAINT IF EXISTS uk_project_uuid;

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. DUPLICATE INDEXES — drop redundant copies (keeping the more descriptive name)
-- ─────────────────────────────────────────────────────────────────────────────

-- leads: 3 indexes all on next_follow_up
DROP INDEX IF EXISTS idx_leads_follow_up_date;   -- kept: idx_leads_next_follow_up
DROP INDEX IF EXISTS idx_leads_next_followup;    -- typo variant of idx_leads_next_follow_up

-- leads: 2 indexes on lead_source
DROP INDEX IF EXISTS idx_leads_source;           -- kept: idx_leads_lead_source

-- leads: 2 indexes on lead_status
DROP INDEX IF EXISTS idx_leads_status;           -- kept: idx_leads_lead_status

-- tasks: 5 pairs of duplicates (idx_task_* duplicated by idx_tasks_*)
DROP INDEX IF EXISTS idx_task_assigned_to;       -- kept: idx_tasks_assigned_to
DROP INDEX IF EXISTS idx_task_created_by;        -- kept: idx_tasks_created_by
DROP INDEX IF EXISTS idx_task_due_date;          -- kept: idx_tasks_due_date
DROP INDEX IF EXISTS idx_task_project_id;        -- kept: idx_tasks_project_id
DROP INDEX IF EXISTS idx_task_status;            -- kept: idx_tasks_status

-- customer_projects: 3 pairs of duplicates
DROP INDEX IF EXISTS idx_customer_projects_customer_id;  -- kept: idx_projects_customer_active
DROP INDEX IF EXISTS idx_customer_projects_manager;      -- kept: idx_projects_manager
DROP INDEX IF EXISTS idx_customer_projects_status;       -- kept: idx_projects_status

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. MISSING INDEXES — add indexes for frequently-queried columns
-- ─────────────────────────────────────────────────────────────────────────────

-- observations: status is used in every active/resolved query
CREATE INDEX IF NOT EXISTS idx_observations_status
    ON observations(status);

-- observations: reported_date is used in ORDER BY and range filters
CREATE INDEX IF NOT EXISTS idx_observations_reported_date
    ON observations(reported_date DESC);

-- site_visits: visit_date is used in all date-range and "today" queries
CREATE INDEX IF NOT EXISTS idx_site_visits_visit_date
    ON site_visits(project_id, visit_date DESC);

-- portal_notifications: lead_id and project_id used in notification lookups
CREATE INDEX IF NOT EXISTS idx_portal_notifications_lead_id
    ON portal_notifications(lead_id) WHERE lead_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_portal_notifications_project_id
    ON portal_notifications(project_id) WHERE project_id IS NOT NULL;

-- labour: trade_type and is_active are primary filter columns
CREATE INDEX IF NOT EXISTS idx_labour_trade_type
    ON labour(trade_type);

CREATE INDEX IF NOT EXISTS idx_labour_is_active
    ON labour(is_active);

-- boq_items: deleted_at is checked in every soft-delete WHERE clause
CREATE INDEX IF NOT EXISTS idx_boq_items_deleted_at
    ON boq_items(deleted_at) WHERE deleted_at IS NULL;
