-- Add GPS coordinates to customer_projects table for site location tracking
ALTER TABLE customer_projects ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE customer_projects ADD COLUMN longitude DOUBLE PRECISION;

-- Add index for location-based queries
CREATE INDEX idx_customer_projects_location ON customer_projects(latitude, longitude);

COMMENT ON COLUMN customer_projects.latitude IS 'Site location latitude coordinate';
COMMENT ON COLUMN customer_projects.longitude IS 'Site location longitude coordinate';
