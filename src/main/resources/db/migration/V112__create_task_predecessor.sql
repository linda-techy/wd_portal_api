-- V112: multi-predecessor graph table for S1 scheduling foundation.
-- Replaces single tasks.depends_on_task_id (kept in place during S1 for back-compat;
-- dropped in S2 after dual-write soak).

CREATE TABLE task_predecessor (
    id              BIGSERIAL PRIMARY KEY,
    successor_id    BIGINT       NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    predecessor_id  BIGINT       NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    lag_days        INT          NOT NULL DEFAULT 0,
    dep_type        VARCHAR(2)   NOT NULL DEFAULT 'FS',

    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by_user_id      BIGINT,
    updated_by_user_id      BIGINT,
    deleted_at              TIMESTAMP,
    deleted_by_user_id      BIGINT,
    version                 BIGINT NOT NULL DEFAULT 1,

    CONSTRAINT uq_task_predecessor_pair UNIQUE (successor_id, predecessor_id),
    CONSTRAINT chk_task_predecessor_not_self CHECK (successor_id <> predecessor_id),
    CONSTRAINT chk_task_predecessor_dep_type CHECK (dep_type IN ('FS','SS','FF','SF'))
);

CREATE INDEX idx_succ ON task_predecessor (successor_id);
CREATE INDEX idx_pred ON task_predecessor (predecessor_id);
