-- ===========================================================================
-- V143 — Add cctv_cameras.display_order (drift fix)
--
-- Both portal-api and customer-api entities declare display_order as a
-- nullable INTEGER. Production schema was missing the column → Hibernate
-- schema-validation aborts startup with:
--   Schema-validation: missing column [display_order] in table [cctv_cameras]
-- Idempotent (ADD COLUMN IF NOT EXISTS).
-- ===========================================================================

ALTER TABLE cctv_cameras
    ADD COLUMN IF NOT EXISTS display_order INTEGER;
