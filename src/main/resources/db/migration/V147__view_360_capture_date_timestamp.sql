-- ===========================================================================
-- V147 — Coerce view_360.capture_date to TIMESTAMP
--
-- The View360 entity declares capture_date as LocalDateTime (TIMESTAMP), but
-- the production schema stores it as DATE. Schema-validation aborts on
-- portal-api startup with "wrong column type ... found [date], expecting
-- [timestamp(6)]". Date values lift to TIMESTAMP cleanly (midnight UTC of
-- the stored date), so no data loss.
-- ===========================================================================

ALTER TABLE view_360
    ALTER COLUMN capture_date TYPE TIMESTAMP
        USING capture_date::TIMESTAMP;
