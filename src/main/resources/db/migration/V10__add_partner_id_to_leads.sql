-- Add partner_id FK to leads table for proper partner-lead attribution.
-- Replaces fragile regex parsing on notes field.

ALTER TABLE leads ADD COLUMN IF NOT EXISTS partner_id BIGINT;

ALTER TABLE leads ADD CONSTRAINT fk_lead_partner
    FOREIGN KEY (partner_id) REFERENCES partnership_users(id);

CREATE INDEX IF NOT EXISTS idx_leads_partner_id ON leads(partner_id);
