-- Migration: Add Foreign Key CASCADE DELETE Constraints
-- Version: V1_27__add_cascade_delete_constraints.sql
-- Description: Adds ON DELETE CASCADE to foreign keys to automate cascade deletes
--              Replaces manual cascade deletes in service layer for better data integrity

-- =====================================================================
-- TASKS TABLE - CASCADE DELETE when project is deleted
-- =====================================================================

-- Drop existing constraint if it exists
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_project_id_fkey;

-- Add constraint with CASCADE DELETE
ALTER TABLE tasks 
ADD CONSTRAINT tasks_project_id_fkey 
FOREIGN KEY (project_id) 
REFERENCES customer_projects(id) 
ON DELETE CASCADE;

COMMENT ON CONSTRAINT tasks_project_id_fkey ON tasks IS 
'Automatically deletes tasks when parent project is deleted';

-- =====================================================================
-- PROJECT_DOCUMENTS TABLE - CASCADE DELETE when project is deleted
-- =====================================================================

-- Drop existing constraint if it exists  
ALTER TABLE project_documents DROP CONSTRAINT IF EXISTS project_documents_project_id_fkey;

-- Add constraint with CASCADE DELETE
ALTER TABLE project_documents
ADD CONSTRAINT project_documents_project_id_fkey
FOREIGN KEY (project_id)
REFERENCES customer_projects(id)
ON DELETE CASCADE;

COMMENT ON CONSTRAINT project_documents_project_id_fkey ON project_documents IS
'Automatically deletes documents when parent project is deleted';

-- =====================================================================
-- PROJECT_MEMBERS TABLE - CASCADE DELETE when project is deleted
-- =====================================================================

-- Drop existing constraint if it exists
ALTER TABLE project_members DROP CONSTRAINT IF EXISTS project_members_project_id_fkey;

-- Add constraint with CASCADE DELETE
ALTER TABLE project_members
ADD CONSTRAINT project_members_project_id_fkey
FOREIGN KEY (project_id)
REFERENCES customer_projects(id)
ON DELETE CASCADE;

COMMENT ON CONSTRAINT project_members_project_id_fkey ON project_members IS
'Automatically deletes project members when parent project is deleted';

-- =====================================================================
-- CUSTOMER_PROJECTS TABLE - RESTRICT DELETE when customer has projects
-- =====================================================================

-- Drop existing constraint if it exists
ALTER TABLE customer_projects DROP CONSTRAINT IF EXISTS customer_projects_customer_id_fkey;

-- Add constraint with RESTRICT (prevents deletion of customer with projects)
ALTER TABLE customer_projects
ADD CONSTRAINT customer_projects_customer_id_fkey
FOREIGN KEY (customer_id)
REFERENCES customer_users(id)
ON DELETE RESTRICT;

COMMENT ON CONSTRAINT customer_projects_customer_id_fkey ON customer_projects IS
'Prevents deletion of customer if they have associated projects - business rule enforcement';

-- =====================================================================
-- VERIFICATION QUERIES (commented out - for manual testing)
-- =====================================================================

-- Check all FK constraints on customer_projects table:
-- SELECT 
--     tc.constraint_name,
--     tc.table_name,
--     kcu.column_name,
--     ccu.table_name AS foreign_table_name,
--     ccu.column_name AS foreign_column_name,
--     rc.delete_rule
-- FROM 
--     information_schema.table_constraints AS tc
--     JOIN information_schema.key_column_usage AS kcu
--       ON tc.constraint_name = kcu.constraint_name
--     JOIN information_schema.constraint_column_usage AS ccu
--       ON ccu.constraint_name = tc.constraint_name
--     JOIN information_schema.referential_constraints AS rc
--       ON tc.constraint_name = rc.constraint_name
-- WHERE tc.table_name IN ('tasks', 'project_documents', 'project_members', 'customer_projects')
--   AND tc.constraint_type = 'FOREIGN KEY'
-- ORDER BY tc.table_name, tc.constraint_name;
