-- =============================================================================
-- V17: Add performance indexes for BOQ queries
--
-- Root cause: boq_items has no indexes on project_id or deleted_at.
-- Every BOQ list/summary query does a full table scan.
-- These indexes cover the two most frequent access patterns:
--   1. GET /boq/project/:id  → WHERE project_id = ? AND deleted_at IS NULL
--   2. Financial summary aggregate → WHERE project_id = ? AND deleted_at IS NULL
-- =============================================================================

-- Primary filter: project + soft-delete (covers both list and aggregate queries)
CREATE INDEX IF NOT EXISTS idx_boq_items_project_id_deleted_at
    ON boq_items(project_id, deleted_at);

-- Category FK for JOIN in category breakdown aggregate query
CREATE INDEX IF NOT EXISTS idx_boq_items_category_id
    ON boq_items(category_id)
    WHERE deleted_at IS NULL;

-- Work type FK for JOIN in work-type breakdown aggregate query
CREATE INDEX IF NOT EXISTS idx_boq_items_work_type_id
    ON boq_items(work_type_id)
    WHERE deleted_at IS NULL;

-- Status filter (used by portal list/search screens)
CREATE INDEX IF NOT EXISTS idx_boq_items_status
    ON boq_items(status)
    WHERE deleted_at IS NULL;
