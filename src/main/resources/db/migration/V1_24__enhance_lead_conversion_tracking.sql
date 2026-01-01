-- Enhanced Lead Conversion Tracking
-- Migration: V1_24
-- Adds conversion metadata to customer_projects for audit trail
-- Prevents duplicate conversions with unique constraint

-- Add conversion tracking columns
ALTER TABLE customer_projects
ADD COLUMN converted_by_id BIGINT REFERENCES portal_users(id),
ADD COLUMN converted_at TIMESTAMP;

-- Add unique constraint to prevent duplicate conversions
ALTER TABLE customer_projects
ADD CONSTRAINT uq_project_lead UNIQUE (lead_id);

-- Add index for conversion queries
CREATE INDEX idx_customer_projects_conversion 
ON customer_projects(lead_id, converted_at) 
WHERE lead_id IS NOT NULL;

-- Create trigger function to auto-populate conversion metadata
CREATE OR REPLACE FUNCTION set_lead_conversion_metadata()
RETURNS TRIGGER AS $$
BEGIN
    -- If lead_id is being set for the first time (from NULL to a value)
    IF NEW.lead_id IS NOT NULL AND (OLD.lead_id IS NULL OR OLD.lead_id IS DISTINCT FROM NEW.lead_id) THEN
        NEW.converted_at = CURRENT_TIMESTAMP;
        -- Note: converted_by_id should be set by application logic
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-populate conversion timestamp
CREATE TRIGGER trg_lead_conversion
    BEFORE INSERT OR UPDATE ON customer_projects
    FOR EACH ROW
    EXECUTE FUNCTION set_lead_conversion_metadata();

-- Add comment for documentation
COMMENT ON COLUMN customer_projects.converted_by_id IS 'Portal user who converted the lead to project';
COMMENT ON COLUMN customer_projects.converted_at IS 'Timestamp when lead was converted to project';
COMMENT ON CONSTRAINT uq_project_lead ON customer_projects IS 'Ensures each lead can only be converted to one project';
