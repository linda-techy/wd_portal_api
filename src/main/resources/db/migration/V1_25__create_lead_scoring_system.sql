-- Lead Scoring System
-- Migration: V1_25
-- Adds intelligent lead prioritization through automated scoring
-- Tracks score history for analytics and trend analysis

-- Add scoring columns to leads table
ALTER TABLE leads
ADD COLUMN score INTEGER DEFAULT 0,
ADD COLUMN score_category VARCHAR(20) DEFAULT 'COLD',
ADD COLUMN last_scored_at TIMESTAMP,
ADD COLUMN score_factors JSONB;

-- Add constraints for score values
ALTER TABLE leads
ADD CONSTRAINT chk_lead_score CHECK (score >= 0 AND score <= 100),
ADD CONSTRAINT chk_score_category CHECK (score_category IN ('HOT', 'WARM', 'COLD', 'UNQUALIFIED'));

-- Create index for score-based queries
CREATE INDEX idx_leads_score ON leads(score DESC, score_category);
CREATE INDEX idx_leads_score_category ON leads(score_category);

-- Create lead score history table for audit trail
CREATE TABLE lead_score_history (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(lead_id) ON DELETE CASCADE,
    previous_score INTEGER,
    new_score INTEGER NOT NULL,
    previous_category VARCHAR(20),
    new_category VARCHAR(20) NOT NULL,
    score_factors JSONB,
    reason TEXT,
    scored_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scored_by_id BIGINT REFERENCES portal_users(id),
    
    CONSTRAINT chk_score_history_score CHECK (
        (previous_score IS NULL OR (previous_score >= 0 AND previous_score <= 100)) AND
        (new_score >= 0 AND new_score <= 100)
    )
);

-- Create indexes for score history
CREATE INDEX idx_lead_score_history_lead ON lead_score_history(lead_id, scored_at DESC);
CREATE INDEX idx_lead_score_history_date ON lead_score_history(scored_at DESC);

-- Create function to calculate lead score based on multiple factors
CREATE OR REPLACE FUNCTION calculate_lead_score(p_lead_id BIGINT)
RETURNS INTEGER AS $$
DECLARE
    v_score INTEGER := 0;
    v_lead RECORD;
    v_interaction_count INTEGER;
    v_days_since_creation INTEGER;
    v_days_since_last_contact INTEGER;
BEGIN
    -- Get lead details
    SELECT * INTO v_lead FROM leads WHERE lead_id = p_lead_id;
    
    IF NOT FOUND THEN
        RETURN 0;
    END IF;
    
    -- Factor 1: Budget (0-25 points)
    IF v_lead.budget IS NOT NULL THEN
        CASE 
            WHEN v_lead.budget >= 5000000 THEN v_score := v_score + 25;  -- 50L+
            WHEN v_lead.budget >= 2500000 THEN v_score := v_score + 20;  -- 25L-50L
            WHEN v_lead.budget >= 1000000 THEN v_score := v_score + 15;  -- 10L-25L
            WHEN v_lead.budget >= 500000 THEN v_score := v_score + 10;   -- 5L-10L
            ELSE v_score := v_score + 5;
        END CASE;
    END IF;
    
    -- Factor 2: Lead Source Quality (0-15 points)
    CASE v_lead.lead_source
        WHEN 'referral' THEN v_score := v_score + 15;
        WHEN 'website' THEN v_score := v_score + 12;
        WHEN 'partnership' THEN v_score := v_score + 10;
        WHEN 'social_media' THEN v_score := v_score + 8;
        WHEN 'advertisement' THEN v_score := v_score + 5;
        ELSE v_score := v_score + 3;
    END CASE;
    
    -- Factor 3: Response Time / Engagement (0-20 points)
    SELECT COUNT(*) INTO v_interaction_count 
    FROM lead_interactions 
    WHERE lead_id = p_lead_id;
    
    CASE 
        WHEN v_interaction_count >= 5 THEN v_score := v_score + 20;
        WHEN v_interaction_count >= 3 THEN v_score := v_score + 15;
        WHEN v_interaction_count >= 1 THEN v_score := v_score + 10;
        ELSE v_score := v_score + 0;
    END CASE;
    
    -- Factor 4: Project Details Completeness (0-15 points)
    IF v_lead.project_sqft_area IS NOT NULL THEN v_score := v_score + 5; END IF;
    IF v_lead.location IS NOT NULL AND LENGTH(v_lead.location) > 5 THEN v_score := v_score + 5; END IF;
    IF v_lead.project_type IS NOT NULL THEN v_score := v_score + 5; END IF;
    
    -- Factor 5: Lead Age / Freshness (0-15 points)
    v_days_since_creation := EXTRACT(DAY FROM (CURRENT_TIMESTAMP - v_lead.created_at));
    CASE 
        WHEN v_days_since_creation <= 7 THEN v_score := v_score + 15;   -- Fresh leads
        WHEN v_days_since_creation <= 30 THEN v_score := v_score + 10;  -- Recent
        WHEN v_days_since_creation <= 90 THEN v_score := v_score + 5;   -- Aging
        ELSE v_score := v_score + 0;  -- Old leads
    END CASE;
    
    -- Factor 6: Priority Level (0-10 points)
    CASE v_lead.priority
        WHEN 'urgent' THEN v_score := v_score + 10;
        WHEN 'high' THEN v_score := v_score + 8;
        WHEN 'medium' THEN v_score := v_score + 5;
        ELSE v_score := v_score + 2;
    END CASE;
    
    -- Ensure score is within bounds
    v_score := LEAST(100, GREATEST(0, v_score));
    
    RETURN v_score;
END;
$$ LANGUAGE plpgsql;

-- Create function to determine score category
CREATE OR REPLACE FUNCTION get_score_category(p_score INTEGER)
RETURNS VARCHAR(20) AS $$
BEGIN
    CASE 
        WHEN p_score >= 70 THEN RETURN 'HOT';
        WHEN p_score >= 40 THEN RETURN 'WARM';
        WHEN p_score >= 20 THEN RETURN 'COLD';
        ELSE RETURN 'UNQUALIFIED';
    END CASE;
END;
$$ LANGUAGE plpgsql;

-- Create trigger function to auto-update lead score
CREATE OR REPLACE FUNCTION update_lead_score()
RETURNS TRIGGER AS $$
DECLARE
    v_new_score INTEGER;
    v_new_category VARCHAR(20);
    v_old_score INTEGER;
    v_old_category VARCHAR(20);
BEGIN
    -- Calculate new score
    v_new_score := calculate_lead_score(NEW.lead_id);
    v_new_category := get_score_category(v_new_score);
    
    -- Store old values for history
    v_old_score := OLD.score;
    v_old_category := OLD.score_category;
    
    -- Update lead with new score
    NEW.score := v_new_score;
    NEW.score_category := v_new_category;
    NEW.last_scored_at := CURRENT_TIMESTAMP;
    
    -- Insert into history if score changed significantly (> 5 points)
    IF v_old_score IS NULL OR ABS(v_new_score - v_old_score) >= 5 OR v_new_category != v_old_category THEN
        INSERT INTO lead_score_history (lead_id, previous_score, new_score, previous_category, new_category, scored_at)
        VALUES (NEW.lead_id, v_old_score, v_new_score, v_old_category, v_new_category, CURRENT_TIMESTAMP);
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-score leads on update
CREATE TRIGGER trg_auto_score_lead
    BEFORE UPDATE ON leads
    FOR EACH ROW
    WHEN (
        OLD.budget IS DISTINCT FROM NEW.budget OR
        OLD.lead_source IS DISTINCT FROM NEW.lead_source OR
        OLD.priority IS DISTINCT FROM NEW.priority OR
        OLD.project_sqft_area IS DISTINCT FROM NEW.project_sqft_area OR
        OLD.location IS DISTINCT FROM NEW.location OR
        OLD.project_type IS DISTINCT FROM NEW.project_type
    )
    EXECUTE FUNCTION update_lead_score();

-- Initial scoring for existing leads
UPDATE leads SET last_scored_at = CURRENT_TIMESTAMP WHERE score IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN leads.score IS 'Calculated lead score (0-100) based on multiple factors';
COMMENT ON COLUMN leads.score_category IS 'Lead temperature: HOT (70+), WARM (40-69), COLD (20-39), UNQUALIFIED (<20)';
COMMENT ON COLUMN leads.score_factors IS 'JSON object with individual scoring factor breakdown';
COMMENT ON TABLE lead_score_history IS 'Audit trail for lead score changes over time';
