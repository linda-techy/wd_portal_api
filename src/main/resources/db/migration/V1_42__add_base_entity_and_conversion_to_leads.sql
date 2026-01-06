-- V1_42__add_base_entity_and_conversion_to_leads.sql
-- Add BaseEntity audit fields and conversion tracking to leads table

-- Add columns
ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS converted_by_id BIGINT,
    ADD COLUMN IF NOT EXISTS converted_at TIMESTAMP;

-- Add foreign key constraints for BaseEntity audit fields (separate statements)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_leads_created_by') THEN
        ALTER TABLE leads ADD CONSTRAINT fk_leads_created_by FOREIGN KEY (created_by_user_id) REFERENCES portal_users(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_leads_updated_by') THEN
        ALTER TABLE leads ADD CONSTRAINT fk_leads_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES portal_users(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_leads_deleted_by') THEN
        ALTER TABLE leads ADD CONSTRAINT fk_leads_deleted_by FOREIGN KEY (deleted_by_user_id) REFERENCES portal_users(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_leads_converted_by') THEN
        ALTER TABLE leads ADD CONSTRAINT fk_leads_converted_by FOREIGN KEY (converted_by_id) REFERENCES portal_users(id);
    END IF;
END $$;

-- Add indexes for soft delete and conversion queries
CREATE INDEX IF NOT EXISTS idx_leads_deleted_at ON leads(deleted_at);
CREATE INDEX IF NOT EXISTS idx_leads_converted_at ON leads(converted_at);
