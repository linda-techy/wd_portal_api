-- ===========================================================================
-- V119 — S2 PR2 baseline snapshot tables
--
-- Single-baseline-per-project semantics enforced by UNIQUE on
-- project_baseline.project_id. Removing that constraint is the only
-- schema change needed to enable re-baselining (deferred to S3).
--
-- task_baseline rows are immutable snapshots; cascades on baseline
-- delete so a future "discard baseline" admin flow can clean up in
-- one statement.
-- ===========================================================================

CREATE TABLE project_baseline (
    id                    BIGSERIAL    PRIMARY KEY,
    project_id            BIGINT       NOT NULL UNIQUE
                                       REFERENCES customer_projects(id),
    approved_at           TIMESTAMP    NOT NULL,
    approved_by           BIGINT       NOT NULL REFERENCES portal_users(id),
    project_start_date    DATE         NOT NULL,
    project_finish_date   DATE         NOT NULL,
    -- BaseEntity audit columns (matches V112 / V115 convention)
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by_user_id      BIGINT,
    updated_by_user_id      BIGINT,
    deleted_at              TIMESTAMP,
    deleted_by_user_id      BIGINT,
    version                 BIGINT     NOT NULL DEFAULT 1
);

CREATE INDEX idx_project_baseline_project_id ON project_baseline(project_id);

CREATE TABLE task_baseline (
    id                      BIGSERIAL  PRIMARY KEY,
    baseline_id             BIGINT     NOT NULL
                                       REFERENCES project_baseline(id) ON DELETE CASCADE,
    task_id                 BIGINT     NOT NULL REFERENCES tasks(id),
    task_name_at_baseline   VARCHAR(256) NOT NULL,
    baseline_start          DATE       NOT NULL,
    baseline_end            DATE       NOT NULL,
    baseline_duration_days  INT        NOT NULL,
    -- BaseEntity audit columns
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by_user_id      BIGINT,
    updated_by_user_id      BIGINT,
    deleted_at              TIMESTAMP,
    deleted_by_user_id      BIGINT,
    version                 BIGINT     NOT NULL DEFAULT 1,
    CONSTRAINT uq_task_baseline_pair UNIQUE (baseline_id, task_id)
);

CREATE INDEX idx_task_baseline_baseline_id ON task_baseline(baseline_id);
CREATE INDEX idx_task_baseline_task_id     ON task_baseline(task_id);
