CREATE TABLE IF NOT EXISTS webhook_event_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    project_id BIGINT,
    customer_id BIGINT,
    reference_id BIGINT,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP,
    delivered_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_webhook_events_status ON webhook_event_log(status);
CREATE INDEX IF NOT EXISTS idx_webhook_events_created ON webhook_event_log(created_at);
