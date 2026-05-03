-- V108__estimation_pricing_mode.sql
-- Adds budgetary mode support to estimation. Existing rows keep LINE_ITEM behaviour.

ALTER TABLE estimation
    ADD COLUMN pricing_mode VARCHAR(20) NOT NULL DEFAULT 'LINE_ITEM',
    ADD COLUMN estimated_area_sqft NUMERIC(10,2),
    ADD COLUMN grand_total_min NUMERIC(14,2),
    ADD COLUMN grand_total_max NUMERIC(14,2);

ALTER TABLE estimation
    ADD CONSTRAINT estimation_pricing_mode_chk
    CHECK (pricing_mode IN ('BUDGETARY', 'LINE_ITEM'));

ALTER TABLE estimation
    ADD CONSTRAINT estimation_budgetary_area_chk
    CHECK (pricing_mode = 'LINE_ITEM' OR estimated_area_sqft IS NOT NULL);

ALTER TABLE estimation
    ADD CONSTRAINT estimation_budgetary_range_chk
    CHECK (
        pricing_mode = 'LINE_ITEM'
        OR (grand_total_min IS NOT NULL AND grand_total_max IS NOT NULL)
    );
