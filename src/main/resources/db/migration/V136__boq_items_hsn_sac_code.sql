-- G-21: GST compliance — capture HSN (goods) or SAC (services) code per line.
--
-- Indian GST invoices must show the HSN code for goods (4–8 digits) or SAC
-- code for services (6 digits, typically prefixed 99...). Without this we
-- cannot produce a legally compliant tax invoice or file GSTR-1 correctly.
--
-- Column is nullable for now so existing rows survive the migration;
-- the application layer enforces format validation when a value is set, and
-- @NotBlank on new items so all incoming BOQ items carry it from launch.
-- A follow-up data-quality pass will backfill legacy rows from material/
-- category mappings, after which the column can be made NOT NULL in a
-- separate migration.

ALTER TABLE boq_items
    ADD COLUMN IF NOT EXISTS hsn_sac_code VARCHAR(10);

CREATE INDEX IF NOT EXISTS ix_boq_items_hsn_sac_code
    ON boq_items (hsn_sac_code)
    WHERE hsn_sac_code IS NOT NULL;
