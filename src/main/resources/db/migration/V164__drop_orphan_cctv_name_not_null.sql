-- ============================================================================
-- V164: Drop legacy NOT NULL on cctv_cameras.name
-- ============================================================================
-- cctv_cameras carries a legacy `name` column (NOT NULL, no default) left from an
-- early Hibernate ddl-auto mapping. The current CctvCamera entity (both portal and
-- customer APIs) maps `camera_name`, not `name`, and never populates `name`. So
--   POST /api/projects/{id}/cctv-cameras  ->  500
--   ERROR: null value in column "name" of relation "cctv_cameras" violates not-null
-- Drop the orphan NOT NULL (idempotent). Same legacy-column class as V15
-- (boq_items.created_by_id) and V161 (boq_items.work_type_id).
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'cctv_cameras'
          AND column_name = 'name'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE cctv_cameras ALTER COLUMN name DROP NOT NULL;
    END IF;
END $$;
