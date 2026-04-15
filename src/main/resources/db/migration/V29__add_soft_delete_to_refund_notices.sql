-- V29: Add soft-delete columns to refund_notices (missing from V22)
ALTER TABLE refund_notices
    ADD COLUMN IF NOT EXISTS deleted_at         TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT REFERENCES portal_users(id);

CREATE INDEX IF NOT EXISTS idx_refund_notices_deleted_at ON refund_notices(deleted_at)
    WHERE deleted_at IS NULL;
