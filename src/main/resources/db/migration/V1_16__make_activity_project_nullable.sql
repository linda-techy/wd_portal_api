ALTER TABLE activity_feeds ALTER COLUMN project_id DROP NOT NULL;

INSERT INTO activity_types (name, description, icon) VALUES 
('LEAD_CREATED', 'New lead created', 'add_circle'),
('LEAD_UPDATED', 'Lead details updated', 'edit'),
('LEAD_STATUS_CHANGED', 'Lead status changed', 'trending_up'),
('LEAD_ASSIGNED', 'Lead assigned to team member', 'person_add'),
('LEAD_CONVERTED', 'Lead converted to project', 'check_circle')
ON CONFLICT (name) DO NOTHING;

