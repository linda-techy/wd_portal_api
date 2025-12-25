-- Database Indexes for Performance Optimization
-- Created: 2025-12-24
-- Purpose: Add indexes on frequently queried foreign keys and filter columns

-- ==============================================================
-- FOREIGN KEY INDEXES
-- ==============================================================

-- Customer Projects
CREATE INDEX IF NOT EXISTS idx_customer_projects_customer_id 
ON customer_projects(customer_id);

CREATE INDEX IF NOT EXISTS idx_customer_projects_lead_id 
ON customer_projects(lead_id);

-- Tasks
CREATE INDEX IF NOT EXISTS idx_tasks_project_id 
ON tasks(project_id);

CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to 
ON tasks(assigned_to);

CREATE INDEX IF NOT EXISTS idx_tasks_created_by 
ON tasks(created_by);

-- Project Members
CREATE INDEX IF NOT EXISTS idx_project_members_project_id 
ON project_members(project_id);

CREATE INDEX IF NOT EXISTS idx_project_members_portal_user_id 
ON project_members(portal_user_id);

CREATE INDEX IF NOT EXISTS idx_project_members_customer_user_id 
 ON project_members(customer_user_id);

-- Activity Feeds
CREATE INDEX IF NOT EXISTS idx_activity_feeds_project_id 
ON activity_feeds(project_id);

CREATE INDEX IF NOT EXISTS idx_activity_feeds_created_by_id 
ON activity_feeds(created_by_id);

-- Site Reports
CREATE INDEX IF NOT EXISTS idx_site_reports_project_id 
ON site_reports(project_id);

CREATE INDEX IF NOT EXISTS idx_site_reports_submitted_by_id 
ON site_reports(submitted_by_id);

-- Site Visits
CREATE INDEX IF NOT EXISTS idx_site_visits_project_id 
ON site_visits(project_id);

CREATE INDEX IF NOT EXISTS idx_site_visits_visited_by_id 
ON site_visits(visited_by_id);

-- BOQ Items
CREATE INDEX IF NOT EXISTS idx_boq_items_project_id 
ON boq_items(project_id);

CREATE INDEX IF NOT EXISTS idx_boq_items_work_type_id 
ON boq_items(work_type_id);

-- Documents
CREATE INDEX IF NOT EXISTS idx_portal_project_documents_project_id 
ON portal_project_documents(project_id);

CREATE INDEX IF NOT EXISTS idx_portal_project_documents_category_id 
ON portal_project_documents(category_id);

CREATE INDEX IF NOT EXISTS idx_project_documents_project_id 
ON project_documents(project_id);

CREATE INDEX IF NOT EXISTS idx_project_documents_category_id 
ON project_documents(document_category_id);

-- Gallery Images
CREATE INDEX IF NOT EXISTS idx_gallery_images_project_id 
ON gallery_images(project_id);

CREATE INDEX IF NOT EXISTS idx_gallery_images_site_report_id 
ON gallery_images(site_report_id);

-- Observations
CREATE INDEX IF NOT EXISTS idx_observations_project_id 
ON observations(project_id);

CREATE INDEX IF NOT EXISTS idx_observations_reported_by_id 
ON observations(reported_by_id);

-- Quality Checks
CREATE INDEX IF NOT EXISTS idx_quality_checks_project_id 
ON quality_checks(project_id);

CREATE INDEX IF NOT EXISTS idx_quality_checks_inspector_id 
ON quality_checks(inspector_id);

-- Feedback
CREATE INDEX IF NOT EXISTS idx_feedback_forms_project_id 
ON feedback_forms(project_id);

CREATE INDEX IF NOT EXISTS idx_feedback_responses_form_id 
ON feedback_responses(form_id);

CREATE INDEX IF NOT EXISTS idx_feedback_responses_customer_id 
ON feedback_responses(customer_id);

-- Project Queries
CREATE INDEX IF NOT EXISTS idx_project_queries_project_id 
ON project_queries(project_id);

CREATE INDEX IF NOT EXISTS idx_project_queries_customer_id 
ON project_queries(customer_id);

-- ==============================================================
-- QUERY OPTIMIZATION INDEXES
-- ==============================================================

-- Lead status filtering
CREATE INDEX IF NOT EXISTS idx_leads_status 
ON leads(lead_status);

CREATE INDEX IF NOT EXISTS idx_leads_assigned_team 
ON leads(assigned_team);

CREATE INDEX IF NOT EXISTS idx_leads_source 
ON leads(source);

-- Project phase filtering
CREATE INDEX IF NOT EXISTS idx_customer_projects_phase 
ON customer_projects(project_phase);

CREATE INDEX IF NOT EXISTS idx_customer_projects_state 
ON customer_projects(state);

-- Task filtering
CREATE INDEX IF NOT EXISTS idx_tasks_status 
ON tasks(status);

CREATE INDEX IF NOT EXISTS idx_tasks_priority 
ON tasks(priority);

-- ==============================================================
-- DATE-BASED INDEXES FOR SORTING AND FILTERING
-- ==============================================================

-- Activity feeds - frequently sorted by date DESC
CREATE INDEX IF NOT EXISTS idx_activity_feeds_created_at 
ON activity_feeds(created_at DESC);

-- Tasks - due date queries
CREATE INDEX IF NOT EXISTS idx_tasks_due_date 
ON tasks(due_date);

-- Leads - follow up date
CREATE INDEX IF NOT EXISTS idx_leads_follow_up_date 
ON leads(follow_up_date);

-- Projects - start/end dates
CREATE INDEX IF NOT EXISTS idx_customer_projects_start_date 
ON customer_projects(start_date);

CREATE INDEX IF NOT EXISTS idx_customer_projects_end_date 
ON customer_projects(end_date);

-- ==============================================================
-- COMPOSITE INDEXES FOR COMMON QUERIES
-- ==============================================================

-- Find tasks by project and status
CREATE INDEX IF NOT EXISTS idx_tasks_project_status 
ON tasks(project_id, status);

-- Find tasks by user and status
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_status 
ON tasks(assigned_to, status);

-- Find leads by team and status
CREATE INDEX IF NOT EXISTS idx_leads_team_status 
ON leads(assigned_team, lead_status);

-- Find project members by project and type
CREATE INDEX IF NOT EXISTS idx_project_members_project_user 
ON project_members(project_id, portal_user_id, customer_user_id);
