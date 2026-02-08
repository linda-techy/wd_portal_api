-- Add distance tracking columns to site_visits for GPS proximity verification
-- Stores the distance (in km) between the user's check-in/check-out location
-- and the project's GPS coordinates

ALTER TABLE site_visits ADD COLUMN IF NOT EXISTS distance_from_project_checkin DOUBLE PRECISION;
ALTER TABLE site_visits ADD COLUMN IF NOT EXISTS distance_from_project_checkout DOUBLE PRECISION;

COMMENT ON COLUMN site_visits.distance_from_project_checkin IS 'Distance in km between check-in GPS and project site GPS';
COMMENT ON COLUMN site_visits.distance_from_project_checkout IS 'Distance in km between check-out GPS and project site GPS';
