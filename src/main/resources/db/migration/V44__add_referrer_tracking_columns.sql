-- V36: Add referrer tracking columns for queryable referral attribution
-- Currently referrer info is stored only in the notes field as free text.
-- These columns allow the customer API to query "leads I referred" efficiently.

ALTER TABLE leads ADD COLUMN IF NOT EXISTS referred_by_email VARCHAR(255);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS referred_by_name VARCHAR(255);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS referred_by_phone VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_leads_referred_by_email
    ON leads(referred_by_email) WHERE referred_by_email IS NOT NULL AND deleted_at IS NULL;
