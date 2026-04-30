-- ===========================================================================
-- V92 — estimation_customisation_option
--
-- Concrete options inside a category (e.g., Vitrified, Italian Marble within Flooring)
-- with their per-unit rate. Per spec §1.1 row 5 / TDD §4.1.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation_customisation_option (
    id                   UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id          UUID            NOT NULL REFERENCES estimation_customisation_category(id),
    name                 VARCHAR(150)    NOT NULL,
    rate                 NUMERIC(10, 2)  NOT NULL,
    display_order        INTEGER         NOT NULL DEFAULT 0,
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT          NOT NULL DEFAULT 1,
    UNIQUE (category_id, name)
);

CREATE INDEX IF NOT EXISTS idx_estimation_customisation_option_category
    ON estimation_customisation_option (category_id);
