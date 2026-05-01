-- ===========================================================================
-- V86 — Add designation + department to portal_users
--
-- The PortalUser entity carries `designation` (VARCHAR(100)) and
-- `department` (VARCHAR(100)) but no migration ever added the columns,
-- so a fresh boot fails immediately with:
--   ERROR: column pu1_0.department does not exist
-- (DataInitializer.seedAdminUser issues the first SELECT, which Hibernate
-- builds from the entity field list — so the missing columns abort
-- startup before any business request can run.)
--
-- Idempotent: IF NOT EXISTS guards both ALTERs so the migration is safe
-- to re-run against a partially-applied schema.
-- ===========================================================================

ALTER TABLE portal_users
    ADD COLUMN IF NOT EXISTS designation VARCHAR(100),
    ADD COLUMN IF NOT EXISTS department  VARCHAR(100);

COMMENT ON COLUMN portal_users.designation IS
    'Free-form job title — e.g. "Senior Site Engineer", "Project Manager". '
    'Surfaced on the user profile and in audit logs.';

COMMENT ON COLUMN portal_users.department IS
    'Free-form org-chart department — e.g. "Operations", "Finance". '
    'Used for filtering and reporting.';
