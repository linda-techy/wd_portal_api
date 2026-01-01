-- Link Tasks to Leads
ALTER TABLE tasks ADD COLUMN lead_id BIGINT REFERENCES leads(lead_id);
CREATE INDEX idx_tasks_lead ON tasks(lead_id);
