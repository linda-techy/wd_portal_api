-- S6 PR2: dedup table for daily payment-milestone reminders.
-- Unique (stage_id, reminder_kind) is the idempotency key — running the
-- job twice on the same day is a no-op for any (stage, kind) already sent.
-- ON DELETE CASCADE so a deleted payment_stage cleans up its reminder rows.
CREATE TABLE payment_stage_reminder_sent (
    id              BIGSERIAL PRIMARY KEY,
    stage_id        BIGINT NOT NULL REFERENCES payment_stages(id) ON DELETE CASCADE,
    reminder_kind   VARCHAR(16) NOT NULL,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_psrs_stage_kind UNIQUE (stage_id, reminder_kind),
    CONSTRAINT chk_psrs_kind CHECK (reminder_kind IN ('T_MINUS_3', 'DUE_TODAY', 'OVERDUE'))
);

CREATE INDEX idx_psrs_stage ON payment_stage_reminder_sent(stage_id);
