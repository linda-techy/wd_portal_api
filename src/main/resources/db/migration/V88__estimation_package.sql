-- ===========================================================================
-- V88 — estimation_package
--
-- Three-tier package definitions (BASIC/STANDARD/PREMIUM) with marketing names.
-- Per spec docs/superpowers/specs/2026-05-01-estimation-engine-foundation-design.md §1.1 row 1.
-- Idempotent: CREATE TABLE IF NOT EXISTS + DO-block constraint guards.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation_package (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    internal_name        VARCHAR(50)  NOT NULL UNIQUE,
    marketing_name       VARCHAR(100) NOT NULL,
    tagline              VARCHAR(255),
    description          TEXT,
    display_order        INTEGER      NOT NULL DEFAULT 0,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    -- BaseEntity-inherited columns
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT       NOT NULL DEFAULT 1
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_estimation_package_internal_name') THEN
        ALTER TABLE estimation_package
            ADD CONSTRAINT chk_estimation_package_internal_name
            CHECK (internal_name IN ('BASIC', 'STANDARD', 'PREMIUM'));
    END IF;
END $$;

COMMENT ON TABLE estimation_package IS
    'Three-tier package definitions (Basic/Standard/Premium). Marketing names are editable; internal_name is the stable code referenced by rate versions and customisation defaults.';
