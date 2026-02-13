-- Performance Indexes for Portal API
-- These indexes improve query performance for frequently accessed data

-- Site Reports Indexes
CREATE INDEX IF NOT EXISTS idx_site_reports_project_date 
ON site_reports(project_id, report_date DESC);

CREATE INDEX IF NOT EXISTS idx_site_reports_submitted_by_date 
ON site_reports(submitted_by_id, report_date DESC);

CREATE INDEX IF NOT EXISTS idx_site_reports_status
ON site_reports(status);

CREATE INDEX IF NOT EXISTS idx_site_reports_report_type
ON site_reports(report_type);

-- Gallery Images Indexes
CREATE INDEX IF NOT EXISTS idx_gallery_images_project_date
ON gallery_images(project_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_gallery_images_uploaded_by
ON gallery_images(uploaded_by_id);

-- Observations Indexes  
CREATE INDEX IF NOT EXISTS idx_observations_project_status
ON observations(project_id, status);

CREATE INDEX IF NOT EXISTS idx_observations_created_date
ON observations(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_observations_assigned_to
ON observations(assigned_to_id);

-- Site Visits Indexes
CREATE INDEX IF NOT EXISTS idx_site_visits_project_date
ON site_visits(project_id, check_in_time DESC);

CREATE INDEX IF NOT EXISTS idx_site_visits_user
ON site_visits(user_id);

CREATE INDEX IF NOT EXISTS idx_site_visits_status
ON site_visits(status);

-- Site Report Photos Indexes
CREATE INDEX IF NOT EXISTS idx_site_report_photos_report
ON site_report_photos(site_report_id);

-- View360 Indexes
CREATE INDEX IF NOT EXISTS idx_view_360_project_date
ON view_360(project_id, created_at DESC);

-- Customer Projects Indexes (if not already exists)
CREATE INDEX IF NOT EXISTS idx_customer_projects_status
ON customer_projects(status);

CREATE INDEX IF NOT EXISTS idx_customer_projects_start_date
ON customer_projects(start_date DESC);

-- Portal Users Indexes
CREATE INDEX IF NOT EXISTS idx_portal_users_email
ON portal_users(email);

CREATE INDEX IF NOT EXISTS idx_portal_users_active
ON portal_users(active);
