-- Capture the lead_interactions table in version control.
-- The table has been created by Hibernate auto-DDL in earlier deployments;
-- IF NOT EXISTS keeps this migration safe to run against existing databases.

CREATE TABLE IF NOT EXISTS lead_interactions (
    id                BIGSERIAL    PRIMARY KEY,
    lead_id           BIGINT       NOT NULL,
    interaction_type  VARCHAR(50)  NOT NULL,
    interaction_date  TIMESTAMP    NOT NULL,
    duration_minutes  INTEGER,
    subject           VARCHAR(255),
    notes             TEXT,
    outcome           VARCHAR(100),
    next_action       VARCHAR(255),
    next_action_date  TIMESTAMP,
    created_by_id     BIGINT       NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    location          VARCHAR(255),
    metadata          TEXT
);

CREATE INDEX IF NOT EXISTS idx_lead_interactions_lead_id
    ON lead_interactions (lead_id);

CREATE INDEX IF NOT EXISTS idx_lead_interactions_lead_interaction_date
    ON lead_interactions (lead_id, interaction_date DESC);

CREATE INDEX IF NOT EXISTS idx_lead_interactions_next_action_date
    ON lead_interactions (next_action_date)
    WHERE next_action_date IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_lead_interactions_created_by_id
    ON lead_interactions (created_by_id);
