-- Lead Interactions/Communication History Table  
-- Migration: V1_23
-- Tracks all interactions and communications with leads
-- Essential for sales pipeline management and follow-up tracking

CREATE TABLE lead_interactions (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(lead_id) ON DELETE CASCADE,
    
    -- Interaction details
    interaction_type VARCHAR(50) NOT NULL,
    interaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_minutes INTEGER,
    subject VARCHAR(255),
    notes TEXT,
    
    -- Outcome and follow-up
    outcome VARCHAR(100),
    next_action VARCHAR(255),
    next_action_date TIMESTAMP,
    
    -- Audit trail
    created_by_id BIGINT NOT NULL REFERENCES portal_users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_interaction_type CHECK (
        interaction_type IN ('CALL', 'EMAIL', 'MEETING', 'SITE_VISIT', 'WHATSAPP', 'SMS', 'OTHER')
    ),
    CONSTRAINT chk_interaction_duration CHECK (
        duration_minutes IS NULL OR duration_minutes >= 0
    )
);

-- Indexes for performance
CREATE INDEX idx_lead_interactions_lead ON lead_interactions(lead_id);
CREATE INDEX idx_lead_interactions_date ON lead_interactions(interaction_date DESC);
CREATE INDEX idx_lead_interactions_next_action ON lead_interactions(next_action_date) 
    WHERE next_action_date IS NOT NULL;
CREATE INDEX idx_lead_interactions_type ON lead_interactions(interaction_type);
CREATE INDEX idx_lead_interactions_created_by ON lead_interactions(created_by_id);

-- Comment for documentation
COMMENT ON TABLE lead_interactions IS 'Tracks all customer communications and interactions for lead management and sales pipeline';
COMMENT ON COLUMN lead_interactions.outcome IS 'Result of interaction: SCHEDULED_FOLLOWUP, QUOTE_SENT, NEEDS_INFO, NOT_INTERESTED, CONVERTED, HOT_LEAD, COLD_LEAD';
COMMENT ON COLUMN lead_interactions.next_action IS 'Planned next step in sales process';
