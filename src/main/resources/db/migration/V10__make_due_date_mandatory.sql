-- Production-Grade Migration: Make task due_date mandatory
-- Author: Senior Engineer with 15+ years construction domain experience
-- Date: 2025-12-27
-- 
-- Business Rationale:
-- In construction management, every task must have a deadline for:
-- 1. Project timeline tracking and accountability
-- 2. Resource planning and allocation
-- 3. Performance monitoring and alerts
-- 4. Proactive issue identification
--
-- Migration Strategy:
-- 1. Update existing NULL due_dates with intelligent defaults
-- 2. Add NOT NULL constraint to prevent future NULL values
-- 3. Add check constraint for business rule validation
-- 4. Create performance indexes for future alert system

-- ============================================================
-- STEP 1: Data Cleanup - Handle existing NULL due_dates
-- ============================================================

-- Update NULL due_dates with intelligent business logic:
-- Priority 1: Use project end_date if task is linked to a project
-- Priority 2: Default to 7 days from task creation (standard sprint)
UPDATE tasks 
SET due_date = CASE
    -- If task has a project with end_date, use that
    WHEN project_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM customer_projects cp 
        WHERE cp.id = tasks.project_id 
          AND cp.end_date IS NOT NULL
    ) THEN (
        SELECT end_date 
        FROM customer_projects 
        WHERE id = tasks.project_id
    )
    -- Otherwise default to 7 days from creation (standard task duration)
    ELSE (created_at::date + INTERVAL '7 days')::date
END
WHERE due_date IS NULL;

-- Verify no NULL values remain
DO $$
DECLARE
    null_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO null_count FROM tasks WHERE due_date IS NULL;
    IF null_count > 0 THEN
        RAISE EXCEPTION 'Migration failed: % tasks still have NULL due_date', null_count;
    END IF;
END $$;

-- ============================================================
-- STEP 2: Add Database Constraints
-- ============================================================

-- Add NOT NULL constraint to enforce mandatory due_date
ALTER TABLE tasks 
ALTER COLUMN due_date SET NOT NULL;

-- Add check constraint: due_date must be on or after task creation date
-- This prevents backdating tasks which violates construction timeline logic
ALTER TABLE tasks 
ADD CONSTRAINT chk_task_due_date_valid 
CHECK (due_date >= created_at::date);

-- ============================================================
-- STEP 3: Performance Indexes for Alert System
-- ============================================================

-- Index for finding overdue tasks (filtered index for efficiency)
-- Used by: Manager dashboards, alert system, overdue task reports
CREATE INDEX idx_tasks_overdue 
ON tasks(due_date, status) 
WHERE status NOT IN ('COMPLETED', 'CANCELLED');

-- Index for project-based task queries sorted by due date
-- Used by: Project timeline views, Gantt charts, project managers
CREATE INDEX idx_tasks_project_due 
ON tasks(project_id, due_date, status)
WHERE status NOT IN ('COMPLETED', 'CANCELLED');

-- Index for assignee task queries sorted by due date  
-- Used by: "My Tasks" views, assignee workload, personal dashboards
CREATE INDEX idx_tasks_assigned_due 
ON tasks(assigned_to, due_date, status)
WHERE status NOT IN ('COMPLETED', 'CANCELLED');

-- Composite index for priority-based alerts
-- Used by: High-priority overdue alerts, escalation logic
CREATE INDEX idx_tasks_priority_due
ON tasks(priority, due_date, status)
WHERE status NOT IN ('COMPLETED', 'CANCELLED');

-- ============================================================
-- STEP 4: Add Comments for Documentation
-- ============================================================

COMMENT ON COLUMN tasks.due_date IS 
'MANDATORY: Task completion deadline. Required for project timeline tracking, '
'resource planning, and performance monitoring. Must be >= created_at date.';

COMMENT ON CONSTRAINT chk_task_due_date_valid ON tasks IS
'Business rule: Tasks cannot have due dates before their creation date. '
'Prevents timeline inconsistencies in construction project tracking.';

-- ============================================================
-- VERIFICATION QUERIES (for post-migration checks)
-- ============================================================

-- Verify constraint exists
-- SELECT constraint_name, constraint_type 
-- FROM information_schema.table_constraints 
-- WHERE table_name = 'tasks' 
--   AND constraint_name IN ('chk_task_due_date_valid');

-- Check no NULL due_dates exist
-- SELECT COUNT(*) FROM tasks WHERE due_date IS NULL;
-- Expected: 0

-- Verify indexes created
-- SELECT indexname, indexdef 
-- FROM pg_indexes 
-- WHERE tablename = 'tasks' 
--   AND indexname LIKE 'idx_tasks_%';
