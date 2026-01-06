-- Migration: Enhance Site Reports
-- Version: V1_39
-- Description: Adds report_type and site_visit_id to site_reports,
--              and creates site_report_photos table for attachments.

-- ============================================================================
-- 1. Create site_reports if it doesn't already exist (supporting baseline)
-- ============================================================================
CREATE TABLE IF NOT EXISTS site_reports (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    report_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'SUBMITTED',
    submitted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_site_reports_project FOREIGN KEY (project_id) REFERENCES customer_projects(id),
    CONSTRAINT fk_site_reports_user FOREIGN KEY (submitted_by) REFERENCES portal_users(id)
);

-- ============================================================================
-- 2. Add New Columns to site_reports
-- ============================================================================
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='site_reports' AND column_name='report_type') THEN
        ALTER TABLE site_reports ADD COLUMN report_type VARCHAR(50) DEFAULT 'DAILY_PROGRESS';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='site_reports' AND column_name='site_visit_id') THEN
        ALTER TABLE site_reports ADD COLUMN site_visit_id BIGINT;
        ALTER TABLE site_reports ADD CONSTRAINT fk_site_reports_visit FOREIGN KEY (site_visit_id) REFERENCES site_visits(id);
    END IF;
END $$;

-- ============================================================================
-- 3. Create site_report_photos Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS site_report_photos (
    id BIGSERIAL PRIMARY KEY,
    site_report_id BIGINT NOT NULL,
    photo_url TEXT NOT NULL,
    storage_path TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_photos_site_report FOREIGN KEY (site_report_id) REFERENCES site_reports(id) ON DELETE CASCADE
);

-- ============================================================================
-- 4. Create Indexes
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_site_reports_project ON site_reports(project_id);
CREATE INDEX IF NOT EXISTS idx_site_reports_visit ON site_reports(site_visit_id);
CREATE INDEX IF NOT EXISTS idx_site_report_photos_report ON site_report_photos(site_report_id);
