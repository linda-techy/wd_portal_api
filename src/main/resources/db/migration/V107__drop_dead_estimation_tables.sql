-- ===========================================================================
-- V107 — Drop unused estimation tables (Sub-project J)
--
-- estimation_settings_audit and estimation_package_default_customisation
-- were scaffolded in sub-project A (Estimation Engine Foundation) but never
-- wired to any code path. Zero queries hit them. Dropping per the post-
-- cutover code hygiene pass.
--
-- Drop order: package_default_customisation first (it has an FK to
-- estimation_package which we keep). Settings audit has no FKs.
-- ===========================================================================

DROP TABLE IF EXISTS estimation_package_default_customisation CASCADE;
DROP TABLE IF EXISTS estimation_settings_audit CASCADE;
