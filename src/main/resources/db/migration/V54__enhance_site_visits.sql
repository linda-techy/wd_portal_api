-- V54: Enhance site_visits table with purpose field
-- purpose: categorise visits (INSPECTION, CLIENT_MEETING, MATERIAL_DELIVERY,
--          QUALITY_CHECK, PROGRESS_REVIEW, OTHER)
-- notes column already exists (mapped by Hibernate from entity field `notes`)

ALTER TABLE site_visits ADD COLUMN IF NOT EXISTS purpose VARCHAR(50);
