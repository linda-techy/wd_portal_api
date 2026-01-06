-- V1_37: Enhance site_visits for check-in/check-out tracking
-- Business Purpose: Track site engineers and project managers on-site presence
-- with GPS coordinates for duty verification

-- Add check-in/check-out tracking fields
ALTER TABLE site_visits
ADD COLUMN IF NOT EXISTS check_in_time TIMESTAMP WITHOUT TIME ZONE,
ADD COLUMN IF NOT EXISTS check_out_time TIMESTAMP WITHOUT TIME ZONE,
ADD COLUMN IF NOT EXISTS check_in_latitude DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS check_in_longitude DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS check_out_latitude DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS check_out_longitude DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS visit_type VARCHAR(50) DEFAULT 'GENERAL',
ADD COLUMN IF NOT EXISTS visit_status VARCHAR(50) DEFAULT 'PENDING',
ADD COLUMN IF NOT EXISTS duration_minutes INTEGER,
ADD COLUMN IF NOT EXISTS check_out_notes TEXT;

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_site_visits_status ON site_visits(visit_status);
CREATE INDEX IF NOT EXISTS idx_site_visits_type ON site_visits(visit_type);
CREATE INDEX IF NOT EXISTS idx_site_visits_checkin ON site_visits(check_in_time) WHERE check_in_time IS NOT NULL;

-- Comments for documentation
COMMENT ON COLUMN site_visits.visit_type IS 'SITE_ENGINEER, PROJECT_MANAGER, SUPERVISOR, CONTRACTOR, CLIENT, GENERAL';
COMMENT ON COLUMN site_visits.visit_status IS 'PENDING, CHECKED_IN, CHECKED_OUT, CANCELLED';
COMMENT ON COLUMN site_visits.duration_minutes IS 'Auto-calculated on check-out';
