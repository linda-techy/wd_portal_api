-- =============================================================================
-- V6 — Portal Notifications + FCM Token Support
-- =============================================================================
-- Adds push notification infrastructure for the portal (staff) application.
-- Mirrors the customer_notifications / fcm_token design from V1001.
-- =============================================================================

-- FCM device token on portal staff users (one active token per user)
ALTER TABLE portal_users
    ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(512);

-- In-app notification store for portal staff notifications
CREATE TABLE IF NOT EXISTS portal_notifications (
    id                 BIGSERIAL PRIMARY KEY,
    portal_user_id     BIGINT NOT NULL REFERENCES portal_users(id) ON DELETE CASCADE,
    project_id         BIGINT,           -- optional FK to customer_projects(id)
    lead_id            BIGINT,           -- optional: for lead-related notifications
    title              VARCHAR(255) NOT NULL,
    body               TEXT,
    notification_type  VARCHAR(50),
        -- LEAD_NEW        — new lead submitted from website/referral
        -- LEAD_ASSIGNED   — lead assigned to this staff member
        -- TASK_ASSIGNED   — task assigned to this staff member
        -- TASK_OVERDUE    — task past due date
        -- GENERAL         — ad-hoc notification
    reference_id       BIGINT,           -- ID of the linked entity (lead_id, task_id, etc.)
    is_read            BOOLEAN NOT NULL DEFAULT false,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index for fast unread-count badge query per user
CREATE INDEX IF NOT EXISTS idx_portal_notif_user_unread
    ON portal_notifications(portal_user_id, is_read, created_at DESC);
