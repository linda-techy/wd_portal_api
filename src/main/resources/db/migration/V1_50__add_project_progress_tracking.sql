-- V1_40: Add Project Progress Tracking Infrastructure
-- Purpose: Implement hybrid progress tracking system with milestone templates for different project types
-- Author: System Enhancement
-- Date: 2026-01-14

-- =====================================================
-- 1. Project Type Templates
-- =====================================================
-- Stores predefined project types with their characteristics
CREATE TABLE IF NOT EXISTS project_type_templates (
    id BIGSERIAL PRIMARY KEY,
    project_type VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    category VARCHAR(50), -- RESIDENTIAL, COMMERCIAL, INFRASTRUCTURE, INTERIOR
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE project_type_templates IS 'Master templates for different construction project types (Residential Villa, Commercial Complex, etc.)';
COMMENT ON COLUMN project_type_templates.project_type IS 'Unique identifier for project type (e.g., RESIDENTIAL_VILLA, COMMERCIAL_COMPLEX)';
COMMENT ON COLUMN project_type_templates.category IS 'High-level category grouping';

-- =====================================================
-- 2. Milestone Templates
-- =====================================================
-- Predefined milestones for each project type with default percentages
CREATE TABLE IF NOT EXISTS milestone_templates (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES project_type_templates(id) ON DELETE CASCADE,
    milestone_name VARCHAR(255) NOT NULL,
    milestone_order INTEGER NOT NULL,
    default_percentage NUMERIC(5,2) NOT NULL CHECK (default_percentage >= 0 AND default_percentage <= 100),
    description TEXT,
    phase VARCHAR(50), -- DESIGN, PLANNING, EXECUTION, COMPLETION, HANDOVER, WARRANTY
    estimated_duration_days INTEGER, -- Typical duration in days
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_milestone_per_template UNIQUE (template_id, milestone_order)
);

COMMENT ON TABLE milestone_templates IS 'Default milestone breakdown for each project type';
COMMENT ON COLUMN milestone_templates.default_percentage IS 'Default weight of this milestone towards overall completion (sum should be 100)';
COMMENT ON COLUMN milestone_templates.milestone_order IS 'Sequential order of milestone execution';
COMMENT ON COLUMN milestone_templates.phase IS 'Project phase this milestone belongs to';

CREATE INDEX idx_milestone_templates_template ON milestone_templates(template_id);
CREATE INDEX idx_milestone_templates_phase ON milestone_templates(phase);

-- =====================================================
-- 3. Enhance customer_projects Table (if it exists)
-- =====================================================
-- Add progress tracking columns only if customer_projects table exists
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'customer_projects') THEN
        -- Add columns one by one to avoid errors if they already exist
        ALTER TABLE customer_projects 
        ADD COLUMN IF NOT EXISTS overall_progress NUMERIC(5,2) DEFAULT 0.00;
        
        ALTER TABLE customer_projects 
        ADD COLUMN IF NOT EXISTS milestone_progress NUMERIC(5,2) DEFAULT 0.00;
        
        ALTER TABLE customer_projects 
        ADD COLUMN IF NOT EXISTS task_progress NUMERIC(5,2) DEFAULT 0.00;
        
        ALTER TABLE customer_projects 
        ADD COLUMN IF NOT EXISTS budget_progress NUMERIC(5,2) DEFAULT 0.00;
        
        ALTER TABLE customer_projects 
        ADD COLUMN IF NOT EXISTS last_progress_update TIMESTAMP;
        
        ALTER TABLE customer_projects 
        ADD COLUMN IF NOT EXISTS progress_calculation_method VARCHAR(50) DEFAULT 'HYBRID';
        
        ALTER TABLE customer_projects 
        ADD COLUMN IF NOT EXISTS milestone_weight NUMERIC(3,2) DEFAULT 0.40;
        
        ALTER TABLE customer_projects 
        ADD COLUMN IF NOT EXISTS task_weight NUMERIC(3,2) DEFAULT 0.30;
        
        ALTER TABLE customer_projects 
        ADD COLUMN IF NOT EXISTS budget_weight NUMERIC(3,2) DEFAULT 0.30;
        
        RAISE NOTICE 'Successfully added progress tracking columns to customer_projects';
    ELSE
        RAISE NOTICE 'Table customer_projects does not exist - skipping column additions';
    END IF;
END $$;

-- Add comments on customer_projects columns (only if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'customer_projects') THEN
        COMMENT ON COLUMN customer_projects.overall_progress IS 'Overall project completion percentage (0-100) calculated using hybrid method';
        COMMENT ON COLUMN customer_projects.milestone_progress IS 'Progress based on milestone completion';
        COMMENT ON COLUMN customer_projects.task_progress IS 'Progress based on task completion';
        COMMENT ON COLUMN customer_projects.budget_progress IS 'Progress based on budget utilization';
        COMMENT ON COLUMN customer_projects.progress_calculation_method IS 'Method used to calculate overall progress';
        COMMENT ON COLUMN customer_projects.milestone_weight IS 'Weight of milestone completion in overall progress (default 40%)';
        COMMENT ON COLUMN customer_projects.task_weight IS 'Weight of task completion in overall progress (default 30%)';
        COMMENT ON COLUMN customer_projects.budget_weight IS 'Weight of budget utilization in overall progress (default 30%)';
    END IF;
END $$;

-- Add constraint to ensure weights sum to 1.0 (only if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'customer_projects') THEN
        -- Drop constraint if it exists, then add it
        ALTER TABLE customer_projects DROP CONSTRAINT IF EXISTS chk_progress_weights_sum;
        ALTER TABLE customer_projects ADD CONSTRAINT chk_progress_weights_sum 
        CHECK (milestone_weight + task_weight + budget_weight = 1.00);
        
        -- Create indexes if they don't exist
        CREATE INDEX IF NOT EXISTS idx_customer_projects_overall_progress ON customer_projects(overall_progress);
        CREATE INDEX IF NOT EXISTS idx_customer_projects_project_type ON customer_projects(project_type);
        
        RAISE NOTICE 'Successfully added constraints and indexes to customer_projects';
    END IF;
END $$;

-- =====================================================
-- 4. Project Progress Logs (Audit Trail)
-- =====================================================
-- Track all progress updates with reasons for audit purposes
CREATE TABLE IF NOT EXISTS project_progress_logs (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES customer_projects(id) ON DELETE CASCADE,
    previous_progress NUMERIC(5,2),
    new_progress NUMERIC(5,2),
    previous_milestone_progress NUMERIC(5,2),
    new_milestone_progress NUMERIC(5,2),
    previous_task_progress NUMERIC(5,2),
    new_task_progress NUMERIC(5,2),
    previous_budget_progress NUMERIC(5,2),
    new_budget_progress NUMERIC(5,2),
    change_reason TEXT,
    change_type VARCHAR(50), -- MILESTONE_UPDATE, TASK_UPDATE, BUDGET_UPDATE, MANUAL_ADJUSTMENT, RECALCULATION
    changed_by BIGINT REFERENCES portal_users(id),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE project_progress_logs IS 'Audit trail for all project progress updates';
COMMENT ON COLUMN project_progress_logs.change_type IS 'What triggered the progress update';
COMMENT ON COLUMN project_progress_logs.change_reason IS 'Explanation for progress change (required for manual adjustments)';

CREATE INDEX idx_progress_logs_project ON project_progress_logs(project_id);
CREATE INDEX idx_progress_logs_changed_at ON project_progress_logs(changed_at DESC);
CREATE INDEX idx_progress_logs_changed_by ON project_progress_logs(changed_by);

-- =====================================================
-- 5. Enhance project_milestones Table (if it exists)
-- =====================================================
-- Link milestones to templates and add completion tracking
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'project_milestones') THEN
        -- Add columns one by one
        ALTER TABLE project_milestones
        ADD COLUMN IF NOT EXISTS template_id BIGINT REFERENCES milestone_templates(id);
        
        ALTER TABLE project_milestones
        ADD COLUMN IF NOT EXISTS completion_percentage NUMERIC(5,2) DEFAULT 0.00;
        
        ALTER TABLE project_milestones
        ADD COLUMN IF NOT EXISTS weight_percentage NUMERIC(5,2);
        
        ALTER TABLE project_milestones
        ADD COLUMN IF NOT EXISTS actual_start_date DATE;
        
        ALTER TABLE project_milestones
        ADD COLUMN IF NOT EXISTS actual_end_date DATE;
        
        -- Note: Generated columns cannot be added with IF NOT EXISTS in old PostgreSQL
        -- So we check if the column exists first
        IF NOT EXISTS (
            SELECT FROM information_schema.columns 
            WHERE table_name = 'project_milestones' AND column_name = 'delay_days'
        ) THEN
            ALTER TABLE project_milestones
            ADD COLUMN delay_days INTEGER GENERATED ALWAYS AS (
                CASE 
                    WHEN actual_end_date IS NOT NULL AND due_date IS NOT NULL 
                    THEN EXTRACT(DAY FROM actual_end_date - due_date)::INTEGER
                    WHEN due_date IS NOT NULL AND due_date < CURRENT_DATE AND actual_end_date IS NULL
                    THEN EXTRACT(DAY FROM CURRENT_DATE - due_date)::INTEGER
                    ELSE 0
                END
            ) STORED;
        END IF;
        
        RAISE NOTICE 'Successfully enhanced project_milestones table';
    ELSE
        RAISE NOTICE 'Table project_milestones does not exist - skipping enhancements';
    END IF;
END $$;

-- Add comments only if table exists
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'project_milestones') THEN
        COMMENT ON COLUMN project_milestones.template_id IS 'Reference to milestone template if created from template';
        COMMENT ON COLUMN project_milestones.completion_percentage IS 'Actual completion percentage of this milestone (0-100)';
        COMMENT ON COLUMN project_milestones.weight_percentage IS 'Weight of this milestone in overall project completion';
        COMMENT ON COLUMN project_milestones.actual_start_date IS 'When work on this milestone actually started';
        COMMENT ON COLUMN project_milestones.actual_end_date IS 'When this milestone was actually completed';
        COMMENT ON COLUMN project_milestones.delay_days IS 'Number of days delayed (auto-calculated)';
    END IF;
END $$;

-- Create indexes if table exists
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'project_milestones') THEN
        CREATE INDEX IF NOT EXISTS idx_project_milestones_template ON project_milestones(template_id);
        CREATE INDEX IF NOT EXISTS idx_project_milestones_completion ON project_milestones(completion_percentage);
    END IF;
END $$;

-- =====================================================
-- 6. Update Database Schema Documentation
-- =====================================================
COMMENT ON DATABASE wdtestdb IS 'WallDot Construction Management System - Enhanced with Hybrid Progress Tracking';

-- =====================================================
-- 7. Create Function to Auto-Calculate Progress (only if tables exist)
-- =====================================================
-- Only create function if customer_projects table exists
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'customer_projects') THEN
        -- Function creation happens in next block
        RAISE NOTICE 'customer_projects table exists, will create progress calculation function';
    ELSE
        RAISE NOTICE 'customer_projects table does not exist - skipping function creation';
        RETURN;
    END IF;
END $$;

CREATE OR REPLACE FUNCTION calculate_project_progress(p_project_id BIGINT)
RETURNS TABLE (
    overall_progress NUMERIC,
    milestone_progress NUMERIC,
    task_progress NUMERIC,
    budget_progress NUMERIC
) AS $$
DECLARE
    v_milestone_progress NUMERIC := 0;
    v_task_progress NUMERIC := 0;
    v_budget_progress NUMERIC := 0;
    v_overall_progress NUMERIC := 0;
    v_milestone_weight NUMERIC;
    v_task_weight NUMERIC;
    v_budget_weight NUMERIC;
    v_total_milestones INTEGER;
    v_total_tasks INTEGER;
    v_completed_tasks INTEGER;
    v_budget NUMERIC;
    v_total_spent NUMERIC;
BEGIN
    -- Get weights from project
    SELECT 
        COALESCE(p.milestone_weight, 0.40),
        COALESCE(p.task_weight, 0.30),
        COALESCE(p.budget_weight, 0.30)
    INTO v_milestone_weight, v_task_weight, v_budget_weight
    FROM customer_projects p
    WHERE p.id = p_project_id;
    
    -- Calculate milestone progress (weighted average of completed milestones)
    -- Only if project_milestones table exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'project_milestones') THEN
        SELECT 
            COALESCE(SUM(pm.completion_percentage * pm.weight_percentage) / NULLIF(SUM(pm.weight_percentage), 0), 0)
        INTO v_milestone_progress
        FROM project_milestones pm
        WHERE pm.project_id = p_project_id;
    ELSE
        v_milestone_progress := 0;
    END IF;
    
    -- Calculate task progress (percentage of completed tasks)
    -- Only if tasks table exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'tasks') THEN
        SELECT 
            COUNT(*),
            COUNT(*) FILTER (WHERE status IN ('COMPLETED', 'DONE'))
        INTO v_total_tasks, v_completed_tasks
        FROM tasks t
        WHERE t.project_id = p_project_id;
    ELSE
        v_total_tasks := 0;
        v_completed_tasks := 0;
    END IF;
    
    v_task_progress := CASE 
        WHEN v_total_tasks > 0 THEN (v_completed_tasks::NUMERIC / v_total_tasks::NUMERIC) * 100
        ELSE 0
    END;
    
    -- Calculate budget progress (aligned with phase expectations)
    -- This is simplified - in production, you'd compare spent vs expected at current phase
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'payment_transactions') THEN
        SELECT 
            p.budget,
            COALESCE(SUM(pt.amount), 0)
        INTO v_budget, v_total_spent
        FROM customer_projects p
        LEFT JOIN payment_transactions pt ON pt.project_id = p.id
        WHERE p.id = p_project_id
        GROUP BY p.budget;
    ELSE
        SELECT budget INTO v_budget FROM customer_projects WHERE id = p_project_id;
        v_total_spent := 0;
    END IF;
    
    v_budget_progress := CASE
        WHEN v_budget > 0 THEN LEAST((v_total_spent / v_budget) * 100, 100)
        ELSE 0
    END;
    
    -- Calculate overall progress using weighted average
    v_overall_progress := (
        (v_milestone_progress * v_milestone_weight) +
        (v_task_progress * v_task_weight) +
        (v_budget_progress * v_budget_weight)
    );
    
    -- Return results
    RETURN QUERY SELECT 
        v_overall_progress,
        v_milestone_progress,
        v_task_progress,
        v_budget_progress;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION calculate_project_progress IS 'Calculate hybrid project progress based on milestones, tasks, and budget';

-- =====================================================
-- 8. Create Trigger to Auto-Update Progress
-- =====================================================
CREATE OR REPLACE FUNCTION trigger_update_project_progress()
RETURNS TRIGGER AS $$
DECLARE
    v_progress_data RECORD;
    v_old_overall_progress NUMERIC;
BEGIN
    -- Get current progress before update
    SELECT overall_progress INTO v_old_overall_progress
    FROM customer_projects
    WHERE id = COALESCE(NEW.project_id, OLD.project_id);
    
    -- Calculate new progress
    SELECT * INTO v_progress_data
    FROM calculate_project_progress(COALESCE(NEW.project_id, OLD.project_id));
    
    -- Update project with new progress
    UPDATE customer_projects
    SET 
        overall_progress = v_progress_data.overall_progress,
        milestone_progress = v_progress_data.milestone_progress,
        task_progress = v_progress_data.task_progress,
        budget_progress = v_progress_data.budget_progress,
        last_progress_update = CURRENT_TIMESTAMP
    WHERE id = COALESCE(NEW.project_id, OLD.project_id);
    
    -- Log the change if progress actually changed
    IF v_old_overall_progress IS DISTINCT FROM v_progress_data.overall_progress THEN
        INSERT INTO project_progress_logs (
            project_id, 
            previous_progress, 
            new_progress,
            change_type,
            change_reason
        ) VALUES (
            COALESCE(NEW.project_id, OLD.project_id),
            v_old_overall_progress,
            v_progress_data.overall_progress,
            'RECALCULATION',
            'Auto-calculated after milestone/task/budget update'
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers only if tables exist
DO $$
BEGIN
    -- Trigger for project_milestones
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'project_milestones') THEN
        DROP TRIGGER IF EXISTS update_progress_on_milestone_change ON project_milestones;
        CREATE TRIGGER update_progress_on_milestone_change
            AFTER INSERT OR UPDATE OF completion_percentage, status ON project_milestones
            FOR EACH ROW
            EXECUTE FUNCTION trigger_update_project_progress();
        RAISE NOTICE 'Created trigger on project_milestones';
    END IF;
    
    -- Trigger for tasks
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'tasks') THEN
        DROP TRIGGER IF EXISTS update_progress_on_task_change ON tasks;
        CREATE TRIGGER update_progress_on_task_change
            AFTER INSERT OR UPDATE OF status ON tasks
            FOR EACH ROW
            WHEN (NEW.project_id IS NOT NULL)
            EXECUTE FUNCTION trigger_update_project_progress();
        RAISE NOTICE 'Created trigger on tasks';
    END IF;
END $$;

-- Add comments on triggers (only if tables/triggers exist)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'project_milestones') THEN
        IF EXISTS (SELECT FROM information_schema.triggers WHERE trigger_name = 'update_progress_on_milestone_change') THEN
            COMMENT ON TRIGGER update_progress_on_milestone_change ON project_milestones IS 'Auto-update project progress when milestones change';
        END IF;
    END IF;
    
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'tasks') THEN
        IF EXISTS (SELECT FROM information_schema.triggers WHERE trigger_name = 'update_progress_on_task_change') THEN
            COMMENT ON TRIGGER update_progress_on_task_change ON tasks IS 'Auto-update project progress when tasks change';
        END IF;
    END IF;
END $$;

