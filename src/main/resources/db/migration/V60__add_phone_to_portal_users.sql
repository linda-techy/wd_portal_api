-- V60__add_phone_to_portal_users.sql
-- Adds phone column to portal_users so customer team-contact feature
-- can show PM/SE/Architect phones (gated by share_with_customer on
-- project_members, see V59).

ALTER TABLE portal_users
    ADD COLUMN IF NOT EXISTS phone VARCHAR(32);
