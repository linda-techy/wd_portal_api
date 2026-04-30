-- ===========================================================================
-- V82 — Customer projects: GPS lock-and-override columns
--
-- Site-visit check-in already enforces a 2km proximity guard against
-- (customer_projects.latitude, .longitude) — but until now the portal had
-- no UI to *set* the project's site GPS, so for most projects those
-- columns were null and the proximity guard silently no-op'd
-- (`if (project.hasLocation())`). This migration adds the two columns
-- needed to make the "first user with PROJECT_EDIT to set the location
-- locks it" flow work, with admin override.
--
-- Columns:
--   * gps_locked_at        — TIMESTAMP, NULL = unset, NOT NULL = locked.
--                            Service treats any non-null value as
--                            "already-set, requires admin to override".
--   * gps_locked_by_user_id — BIGINT FK to portal_users, who first set
--                            (or last overrode) the GPS. Surfaced on the
--                            UI so staff know who to ask if it looks wrong.
--
-- Idempotent: every ALTER uses IF NOT EXISTS / guarded DO-block so
-- re-running the migration against a partially-applied schema is safe.
-- ===========================================================================

ALTER TABLE customer_projects
    ADD COLUMN IF NOT EXISTS gps_locked_at        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS gps_locked_by_user_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_customer_projects_gps_locked_by'
    ) THEN
        ALTER TABLE customer_projects
            ADD CONSTRAINT fk_customer_projects_gps_locked_by
            FOREIGN KEY (gps_locked_by_user_id) REFERENCES portal_users(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_customer_projects_gps_locked
    ON customer_projects(gps_locked_at)
    WHERE gps_locked_at IS NOT NULL;

COMMENT ON COLUMN customer_projects.gps_locked_at IS
    'When the project site GPS coordinates were first stamped. NULL = '
    'GPS not yet captured. Once non-null, only ADMIN can re-set the '
    'coordinates (any user with PROJECT_EDIT can set them the first time).';

COMMENT ON COLUMN customer_projects.gps_locked_by_user_id IS
    'PortalUser who first stamped (or last overrode) the project GPS. '
    'Displayed on the GPS card so staff can challenge a wrong location.';
