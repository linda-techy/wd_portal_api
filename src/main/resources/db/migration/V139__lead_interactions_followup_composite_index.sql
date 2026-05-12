-- G-62: lead-interactions "overdue follow-up" surfaces on the SALES
-- dashboard. The existing partial index idx_lead_interactions_next_action_date
-- handles the global query, but a per-user dashboard ("my overdue follow-ups")
-- filters BOTH on created_by_id AND next_action_date. A composite partial
-- index keeps that query off a full table scan as the table grows.

CREATE INDEX IF NOT EXISTS idx_lead_interactions_creator_nextaction
    ON lead_interactions (created_by_id, next_action_date)
    WHERE next_action_date IS NOT NULL;
