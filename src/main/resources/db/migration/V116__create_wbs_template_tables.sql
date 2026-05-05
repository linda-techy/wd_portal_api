-- ===========================================================================
-- V116 — WBS template tables (S1 PR2)
--
-- 4 tables holding the parameterized WBS templates that are cloned into a
-- project at creation time. Snapshot semantics: a project never references
-- these rows after clone — the cloner copies values into project_milestones
-- and tasks. Templates are versioned via (code, version) with one is_active
-- per code.
-- ===========================================================================

CREATE TABLE wbs_template (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    project_type    VARCHAR(32)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    version         INT          NOT NULL DEFAULT 1,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    source_hash     VARCHAR(64),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(128),
    updated_by      VARCHAR(128),
    CONSTRAINT uk_wbs_template_code_version UNIQUE (code, version)
);

-- Only one active version per code (partial unique index)
CREATE UNIQUE INDEX uk_wbs_template_one_active_per_code
    ON wbs_template (code) WHERE is_active = TRUE;

CREATE TABLE wbs_template_phase (
    id                BIGSERIAL PRIMARY KEY,
    template_id       BIGINT       NOT NULL REFERENCES wbs_template(id) ON DELETE CASCADE,
    sequence          INT          NOT NULL,
    name              VARCHAR(128) NOT NULL,
    role_hint         VARCHAR(64),
    monsoon_sensitive BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_wbs_phase_template_seq UNIQUE (template_id, sequence)
);
CREATE INDEX idx_wbs_phase_template ON wbs_template_phase(template_id);

CREATE TABLE wbs_template_task (
    id                   BIGSERIAL PRIMARY KEY,
    phase_id             BIGINT       NOT NULL REFERENCES wbs_template_phase(id) ON DELETE CASCADE,
    sequence             INT          NOT NULL,
    name                 VARCHAR(128) NOT NULL,
    role_hint            VARCHAR(64),
    duration_days        INT          NOT NULL,
    weight_factor        INT,
    monsoon_sensitive    BOOLEAN      NOT NULL DEFAULT FALSE,
    is_payment_milestone BOOLEAN      NOT NULL DEFAULT FALSE,
    floor_loop           VARCHAR(16)  NOT NULL DEFAULT 'NONE',
    optional_cost        NUMERIC(14,2),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_wbs_task_phase_seq UNIQUE (phase_id, sequence),
    CONSTRAINT chk_wbs_task_floor_loop CHECK (floor_loop IN ('NONE','PER_FLOOR')),
    CONSTRAINT chk_wbs_task_duration  CHECK (duration_days >= 1)
);
CREATE INDEX idx_wbs_task_phase ON wbs_template_task(phase_id);

CREATE TABLE wbs_template_task_predecessor (
    id                            BIGSERIAL PRIMARY KEY,
    successor_template_task_id    BIGINT NOT NULL REFERENCES wbs_template_task(id) ON DELETE CASCADE,
    predecessor_template_task_id  BIGINT NOT NULL REFERENCES wbs_template_task(id) ON DELETE CASCADE,
    lag_days                      INT    NOT NULL DEFAULT 0,
    dep_type                      VARCHAR(2) NOT NULL DEFAULT 'FS',
    created_at                    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_wbs_pred_pair UNIQUE (successor_template_task_id, predecessor_template_task_id),
    CONSTRAINT chk_wbs_pred_self CHECK (successor_template_task_id <> predecessor_template_task_id)
);
CREATE INDEX idx_wbs_pred_succ ON wbs_template_task_predecessor(successor_template_task_id);
CREATE INDEX idx_wbs_pred_pred ON wbs_template_task_predecessor(predecessor_template_task_id);

-- Marker table for the R__seed_wbs_templates_marker.sql repeatable migration.
CREATE TABLE wbs_template_seed_marker (
    id          INT PRIMARY KEY DEFAULT 1,
    last_applied_at TIMESTAMP NOT NULL DEFAULT NOW(),
    note        TEXT,
    CONSTRAINT chk_wbs_seed_marker_singleton CHECK (id = 1)
);
