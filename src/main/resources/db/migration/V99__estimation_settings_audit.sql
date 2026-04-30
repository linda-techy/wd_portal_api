-- ===========================================================================
-- V99 — estimation_settings_audit
--
-- Generic Settings change log. One row per (entity, field) change. Wired by
-- @AuditedSetting / @Aspect in a later sub-project; Sub-project A only
-- creates the schema. Per spec §1.1 row 12.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS estimation_settings_audit (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_name          VARCHAR(80)  NOT NULL,
    entity_id            UUID,
    field_name           VARCHAR(80)  NOT NULL,
    old_value            TEXT,
    new_value            TEXT,
    reason               VARCHAR(500),
    actor_user_id        BIGINT,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT       NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_estimation_settings_audit_entity
    ON estimation_settings_audit (entity_name, entity_id);
