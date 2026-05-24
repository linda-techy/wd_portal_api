-- V158: Per-project GST rate (portal-editable) + flip GST defaults 18% -> 0%.
--
-- The company runs GST-free for now (startup); GST may be enabled later. GST is
-- therefore a per-project setting (customer_projects.gst_rate, default 0%) that
-- new BoQ documents inherit. Existing APPROVED BoQ documents snapshot their own
-- rate and are NOT touched here, so enabling GST later cannot alter already
-- signed contracts. The hard-coded 18% column defaults on the financial tables
-- are flipped to 0% so nothing silently adds GST until a rate is explicitly set.

-- 1. Per-project GST rate. Existing rows backfill to 0% via the DEFAULT.
ALTER TABLE customer_projects
    ADD COLUMN IF NOT EXISTS gst_rate NUMERIC(5,4) NOT NULL DEFAULT 0.0000;

-- 2. Flip the 18% defaults to 0% on the financial tables. This affects NEW rows
--    only; existing rows keep whatever rate they were created/approved with.
ALTER TABLE boq_documents ALTER COLUMN gst_rate SET DEFAULT 0.0000;
ALTER TABLE change_orders ALTER COLUMN gst_rate SET DEFAULT 0.0000;
ALTER TABLE boq_invoices  ALTER COLUMN gst_rate SET DEFAULT 0.0000;
ALTER TABLE credit_notes  ALTER COLUMN gst_rate SET DEFAULT 0.0000;
