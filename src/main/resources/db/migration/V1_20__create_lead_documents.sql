-- Lead Documents Table
CREATE TABLE lead_documents (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(lead_id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    description TEXT,
    category VARCHAR(50), -- e.g., 'REQUIREMENTS', 'SITE_PHOTO', 'PROPOSAL'
    uploaded_by_id BIGINT REFERENCES portal_users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_lead_documents_lead ON lead_documents(lead_id);
