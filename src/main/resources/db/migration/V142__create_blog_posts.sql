-- ===========================================================================
-- V142 — Create blog_posts (entity lives in customer-api, DDL owned here)
--
-- The BlogPost entity in wd_customer_api shipped without its CREATE TABLE.
-- Customer-api uses ddl-auto=validate, so it crashes on startup with
--   Schema-validation: missing table [blog_posts]
-- until this migration runs.
--
-- Soft-delete pattern (deleted_at IS NULL filter) is enforced at entity
-- level via @SQLRestriction — no partial index needed for correctness, but
-- (published, published_at DESC) covers the public-facing listing query.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS blog_posts (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    slug          VARCHAR(255) NOT NULL UNIQUE,
    excerpt       VARCHAR(500),
    content       TEXT,
    image_url     VARCHAR(500),
    author        VARCHAR(100),
    published     BOOLEAN     NOT NULL DEFAULT FALSE,
    published_at  TIMESTAMP,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL,
    deleted_at    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_blog_posts_published
    ON blog_posts (published, published_at DESC)
    WHERE deleted_at IS NULL;
