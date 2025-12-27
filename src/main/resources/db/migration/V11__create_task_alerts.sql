-- Production-Grade Task Alert System
-- Migration V11: Create task_alerts table for deadline monitoring
-- Author: Senior Engineer with 15+ years construction domain experience
-- Date: 2025-12-27
--
-- Business Purpose:
-- Track all task deadline alerts sent to admins for:
-- 1. Audit trail and compliance
-- 2. Prevent duplicate alert spam (24-hour window)
-- 3. Analytics on overdue task trends
-- 4. Email delivery status tracking
--
-- Alert Types:
-- - OVERDUE: Task past due date (CRITICAL)
-- - DUE_TODAY: Task due today (HIGH priority)
-- - DUE_SOON: Task due within 3 days (MEDIUM priority - early warning)

-- ============================================================
-- STEP 1: Create task_alerts table
-- ============================================================

CREATE TABLE task_alerts (
    id BIGSERIAL PRIMARY KEY,
    
    -- Task reference (cascading delete - alert history follows task lifecycle)
    task_id BIGINT NOT NULL,
    
    -- Alert classification
    alert_type VARCHAR(20) NOT NULL 
        CHECK (alert_type IN ('OVERDUE', 'DUE_TODAY', 'DUE_SOON')),
    
    -- Priority for UI display and filtering
    severity VARCHAR(20) NOT NULL 
        CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    
    -- Alert content (formatted message sent to user)
    alert_message TEXT NOT NULL,
    
    -- Delivery tracking
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_to_user_id BIGINT,
    sent_to_email VARCHAR(255),
    
    -- Email delivery status (for failure retry logic)
    delivery_status VARCHAR(20) DEFAULT 'SENT' 
        CHECK (delivery_status IN ('SENT', 'FAILED', 'PENDING')),
    
    -- Audit timestamp
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_task_alerts_task 
        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_alerts_user 
        FOREIGN KEY (sent_to_user_id) REFERENCES portal_users(id) ON DELETE SET NULL
);

-- ============================================================
-- STEP 2: Performance indexes
-- ============================================================

-- Index for duplicate alert prevention query
-- Used by: existsRecentAlert() check (24-hour window)
CREATE INDEX idx_task_alerts_task_recent 
ON task_alerts(task_id, alert_type, sent_at DESC);

-- Index for alert history by type (analytics)
-- Used by: Management reports, trending analysis
CREATE INDEX idx_task_alerts_type_date 
ON task_alerts(alert_type, sent_at DESC);

-- Index for user notification history
-- Used by: User-specific alert preferences, notification settings
CREATE INDEX idx_task_alerts_user 
ON task_alerts(sent_to_user_id, sent_at DESC) 
WHERE sent_to_user_id IS NOT NULL;

-- Index for failed delivery retry
-- Used by: Email retry job, delivery monitoring
CREATE INDEX idx_task_alerts_delivery_status 
ON task_alerts(delivery_status, sent_at DESC) 
WHERE delivery_status = 'FAILED';

-- ============================================================
-- STEP 3: Add table comments for documentation
-- ============================================================

COMMENT ON TABLE task_alerts IS 
'Audit trail for all task deadline alerts sent to users. '
'Enables duplicate prevention, delivery tracking, and trend analysis.';

COMMENT ON COLUMN task_alerts.alert_type IS 
'Classification: OVERDUE (critical), DUE_TODAY (high), DUE_SOON (medium, 3-day warning)';

COMMENT ON COLUMN task_alerts.severity IS 
'Alert priority for UI display and notification urgency';

COMMENT ON COLUMN task_alerts.delivery_status IS 
'Email delivery tracking: SENT (delivered), FAILED (retry needed), PENDING (queued)';

-- ============================================================
-- VERIFICATION QUERIES (for post-migration checks)
-- ============================================================

-- Verify table created
-- SELECT table_name, table_type 
-- FROM information_schema.tables 
-- WHERE table_name = 'task_alerts';

-- Verify indexes created
-- SELECT indexname, indexdef 
-- FROM pg_indexes 
-- WHERE tablename = 'task_alerts';

-- Expected indexes: 4 (idx_task_alerts_task_recent, idx_task_alerts_type_date, 
--                      idx_task_alerts_user, idx_task_alerts_delivery_status)
