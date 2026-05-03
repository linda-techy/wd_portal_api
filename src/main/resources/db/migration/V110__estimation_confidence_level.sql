-- V110__estimation_confidence_level.sql
-- Adds budgetary confidence to control band width: LOW=±10%, MEDIUM=±5%, HIGH=±3%.

ALTER TABLE estimation
    ADD COLUMN confidence_level VARCHAR(10);

ALTER TABLE estimation
    ADD CONSTRAINT estimation_confidence_chk
    CHECK (
        confidence_level IS NULL
        OR confidence_level IN ('LOW', 'MEDIUM', 'HIGH')
    );

ALTER TABLE estimation
    ADD CONSTRAINT estimation_confidence_only_for_budgetary_chk
    CHECK (
        pricing_mode = 'BUDGETARY' OR confidence_level IS NULL
    );
