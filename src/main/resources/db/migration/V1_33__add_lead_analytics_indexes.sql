-- Indexes for Lead Analytics Performance

-- Lead Status Index for filtering and counting
CREATE INDEX IF NOT EXISTS idx_leads_lead_status ON leads(lead_status);

-- Lead Source Index for analytics grouping
CREATE INDEX IF NOT EXISTS idx_leads_lead_source ON leads(lead_source);

-- Priority Index for analytics grouping
CREATE INDEX IF NOT EXISTS idx_leads_priority ON leads(priority);

-- Follow-up Index for dashboard modification
CREATE INDEX IF NOT EXISTS idx_leads_next_followup ON leads(next_follow_up);

-- Composite Index for specific active lead queries (optional but recommended)
CREATE INDEX IF NOT EXISTS idx_leads_status_source ON leads(lead_status, lead_source);
