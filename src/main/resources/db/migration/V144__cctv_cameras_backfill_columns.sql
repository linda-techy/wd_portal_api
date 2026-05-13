-- ===========================================================================
-- V144 — Backfill cctv_cameras columns missing on production
--
-- Schema-validation on customer-api startup reported missing column [password]
-- in table [cctv_cameras]. V47 declares the full column set, but the prod DB
-- evidently received an older revision of V47 (or was provisioned manually),
-- so an unknown subset of columns is missing.
--
-- Rather than guess which columns drifted, this migration is the union of all
-- columns the portal-api and customer-api CctvCamera entities declare. Every
-- ADD COLUMN uses IF NOT EXISTS so columns that already exist are no-ops.
--
-- NOT NULL columns include a DEFAULT or are added nullable then constrained
-- so adding them against populated rows doesn't fail.
-- ===========================================================================

ALTER TABLE cctv_cameras
    ADD COLUMN IF NOT EXISTS camera_name        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS location           VARCHAR(255),
    ADD COLUMN IF NOT EXISTS provider           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS stream_protocol    VARCHAR(20)   DEFAULT 'HLS',
    ADD COLUMN IF NOT EXISTS stream_url         VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS snapshot_url       VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS username           VARCHAR(255),
    ADD COLUMN IF NOT EXISTS password           VARCHAR(255),
    ADD COLUMN IF NOT EXISTS port               INTEGER,
    ADD COLUMN IF NOT EXISTS is_active          BOOLEAN       DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS resolution         VARCHAR(50),
    ADD COLUMN IF NOT EXISTS installation_date  DATE,
    ADD COLUMN IF NOT EXISTS display_order      INTEGER       DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted_at         TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_at         TIMESTAMP     DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at         TIMESTAMP     DEFAULT NOW();

-- Tighten NOT NULL constraints to match the entity declarations.
-- (Postgres allows SET NOT NULL repeatedly; no-op if already NOT NULL.)
ALTER TABLE cctv_cameras
    ALTER COLUMN camera_name     SET NOT NULL,
    ALTER COLUMN stream_protocol SET NOT NULL,
    ALTER COLUMN is_active       SET NOT NULL,
    ALTER COLUMN created_at      SET NOT NULL,
    ALTER COLUMN updated_at      SET NOT NULL;
