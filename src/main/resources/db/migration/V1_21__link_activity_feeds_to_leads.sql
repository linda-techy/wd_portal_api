-- Link Activity Feeds to Leads
-- Migration: V1_21 (continuing the sequence after V1_20)
-- This allows activity feeds to be associated with either projects or leads
-- Essential for tracking lead-related activities before conversion to project

ALTER TABLE activity_feeds
ADD COLUMN lead_id BIGINT REFERENCES leads(lead_id);

-- Add index for performance on lead activity queries
CREATE INDEX idx_activity_feeds_lead ON activity_feeds(lead_id) WHERE lead_id IS NOT NULL;

-- Add check constraint to ensure proper reference integrity
-- Either project_id OR lead_id should be set  (but not both), or neither for system-level activities
ALTER TABLE activity_feeds
ADD CONSTRAINT chk_activity_reference CHECK (
    (project_id IS NOT NULL AND lead_id IS NULL) OR
    (project_id IS NULL AND lead_id IS NOT NULL) OR
    (reference_type IN ('SYSTEM', 'GLOBAL') AND project_id IS NULL AND lead_id IS NULL)
);

-- Insert activity types for lead-related events (if not already present from V1_16)
INSERT INTO activity_types (name, description, icon, color)
VALUES
    ('LEAD_CREATED', 'New lead created', 'person_add', '#4CAF50'),
    ('LEAD_UPDATED', 'Lead information updated', 'edit', '#2196F3'),
    ('LEAD_STATUS_CHANGED', 'Lead status changed', 'swap_horiz', '#FF9800'),
    ('LEAD_CONVERTED', 'Lead converted to project', 'check_circle', '#9C27B0'),
    ('LEAD_ASSIGNED', 'Lead assigned to team member', 'person', '#00BCD4'),
    ('LEAD_INTERACTION', 'Interaction with lead (call, email, meeting)', 'phone', '#795548')
ON CONFLICT (name) DO NOTHING;
