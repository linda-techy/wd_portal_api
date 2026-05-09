-- S5 PR1: server-side idempotency support
--
-- Adds an idempotency_key column to the two mutation tables exposed by S5,
-- and a generic idempotency_responses cache table consulted by the new
-- IdempotencyFilter. Cache rows expire after 24h; a weekly sweeper deletes
-- expired rows.

ALTER TABLE site_reports
    ADD COLUMN idempotency_key VARCHAR(64) NULL;

ALTER TABLE site_reports
    ADD CONSTRAINT uk_site_reports_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE delay_logs
    ADD COLUMN idempotency_key VARCHAR(64) NULL;

ALTER TABLE delay_logs
    ADD CONSTRAINT uk_delay_logs_idempotency_key UNIQUE (idempotency_key);

CREATE TABLE idempotency_responses (
    idempotency_key       VARCHAR(64)  PRIMARY KEY,
    request_method        VARCHAR(8)   NOT NULL,
    request_path          VARCHAR(255) NOT NULL,
    response_status       INT          NOT NULL,
    response_body         TEXT         NOT NULL,
    response_content_type VARCHAR(80)  NOT NULL DEFAULT 'application/json',
    cached_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at            TIMESTAMP    NOT NULL
);

CREATE INDEX idx_idempotency_responses_expires_at
    ON idempotency_responses (expires_at);
