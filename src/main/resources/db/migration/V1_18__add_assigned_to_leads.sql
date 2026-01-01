ALTER TABLE leads
ADD COLUMN assigned_to_id BIGINT;

ALTER TABLE leads
ADD CONSTRAINT fk_leads_assigned_to
FOREIGN KEY (assigned_to_id)
REFERENCES portal_users(id);

CREATE INDEX idx_leads_assigned_to ON leads(assigned_to_id);
