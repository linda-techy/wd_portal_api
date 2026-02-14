-- Add GPS and location tracking to site reports
-- This enables accountability and duty tracking for Site Engineers

-- Add GPS fields to site_reports table
ALTER TABLE site_reports ADD COLUMN latitude DECIMAL(10, 8);
ALTER TABLE site_reports ADD COLUMN longitude DECIMAL(11, 8);
ALTER TABLE site_reports ADD COLUMN location_accuracy DECIMAL(10, 2);
ALTER TABLE site_reports ADD COLUMN distance_from_project DECIMAL(10, 2);

-- Add photo metadata fields
ALTER TABLE site_report_photos ADD COLUMN caption VARCHAR(255);
ALTER TABLE site_report_photos ADD COLUMN latitude DECIMAL(10, 8);
ALTER TABLE site_report_photos ADD COLUMN longitude DECIMAL(11, 8);
ALTER TABLE site_report_photos ADD COLUMN display_order INTEGER DEFAULT 0;

-- Create index for location-based queries
CREATE INDEX IF NOT EXISTS idx_site_reports_location ON site_reports(latitude, longitude);

-- Create index for photo ordering
CREATE INDEX IF NOT EXISTS idx_site_report_photos_order ON site_report_photos(site_report_id, display_order);

-- Add comments for documentation
COMMENT ON COLUMN site_reports.latitude IS 'GPS latitude where report was submitted';
COMMENT ON COLUMN site_reports.longitude IS 'GPS longitude where report was submitted';
COMMENT ON COLUMN site_reports.location_accuracy IS 'GPS accuracy in meters';
COMMENT ON COLUMN site_reports.distance_from_project IS 'Distance in km from project location';
COMMENT ON COLUMN site_report_photos.caption IS 'Optional caption/description for photo';
COMMENT ON COLUMN site_report_photos.display_order IS 'Display order of photos (0-based)';
