-- ============================================================================
-- V37: Align customer_users schema across Portal API and Customer API
--
-- Fixes:
--   1. Merges spurious 'whatsapp' column (created by Customer API's ddl-auto=update)
--      into canonical 'whatsapp_number' column (Portal API's mapping).
--   2. Adds 'customer_type' column for explicit type tracking instead of
--      inferring from company_name presence.
-- ============================================================================

-- 1. Merge whatsapp columns: keep whatsapp_number as canonical.
--    Copy data from 'whatsapp' only where whatsapp_number is empty.
UPDATE customer_users SET whatsapp_number = whatsapp
  WHERE whatsapp IS NOT NULL AND whatsapp != ''
  AND (whatsapp_number IS NULL OR whatsapp_number = '');

ALTER TABLE customer_users DROP COLUMN IF EXISTS whatsapp;

-- 2. Add customer_type column with sensible backfill.
ALTER TABLE customer_users ADD COLUMN IF NOT EXISTS customer_type VARCHAR(50) DEFAULT 'individual';

UPDATE customer_users SET customer_type = 'business'
  WHERE company_name IS NOT NULL AND company_name != ''
  AND (customer_type IS NULL OR customer_type = 'individual');
