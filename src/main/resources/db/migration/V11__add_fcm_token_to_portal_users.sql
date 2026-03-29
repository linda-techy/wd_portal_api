-- V11 — Ensure fcm_token column exists on portal_users
-- Guard in case V6 was applied before this column was added to V6.
ALTER TABLE portal_users
    ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(512);
