-- ===========================================================================
-- V93 — estimation_package_default_customisation
--
-- Join table: which option is the "included" default for which package within a category.
-- Per spec §1.1 row 6.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation_package_default_customisation (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id           UUID         NOT NULL REFERENCES estimation_package(id),
    category_id          UUID         NOT NULL REFERENCES estimation_customisation_category(id),
    option_id            UUID         NOT NULL REFERENCES estimation_customisation_option(id),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT       NOT NULL DEFAULT 1,
    UNIQUE (package_id, category_id)
);
