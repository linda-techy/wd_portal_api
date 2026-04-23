-- V59__add_share_with_customer_to_project_members.sql
-- Visibility flag: when TRUE, customer sees this team member's phone + email.
-- Defaults FALSE for new rows; backfill PM and Site Engineer roles to TRUE
-- so existing customer projects retain operational contact channels.

ALTER TABLE project_members
    ADD COLUMN IF NOT EXISTS share_with_customer BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE project_members
   SET share_with_customer = TRUE
 WHERE role_in_project IN ('PROJECT_MANAGER', 'SITE_ENGINEER');
