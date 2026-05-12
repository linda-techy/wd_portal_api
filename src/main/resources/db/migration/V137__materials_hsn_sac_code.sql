-- G-15 (partial): HSN/SAC code on materials, mirroring the V136 change for
-- boq_items. Indian GST law requires the HSN code on every tax-invoice line,
-- and the construction industry leans on the material master to populate that
-- code consistently (otherwise each BOQ author re-enters it and drift occurs).
--
-- Column is nullable so existing rows survive the migration; the application
-- layer enforces format validation when a value is set. A follow-up
-- data-quality pass can backfill legacy rows and make the column NOT NULL.

ALTER TABLE materials
    ADD COLUMN IF NOT EXISTS hsn_sac_code VARCHAR(10);

CREATE INDEX IF NOT EXISTS ix_materials_hsn_sac_code
    ON materials (hsn_sac_code)
    WHERE hsn_sac_code IS NOT NULL;
