-- ============================================================================
-- V1_67: Fix legacy site report photo URLs
-- Old format: /api/files/download/site-reports/...
-- New format: /api/storage/site-reports/...
-- ============================================================================

UPDATE site_report_photos 
SET photo_url = REPLACE(photo_url, '/api/files/download/', '/api/storage/')
WHERE photo_url LIKE '/api/files/download/%';
