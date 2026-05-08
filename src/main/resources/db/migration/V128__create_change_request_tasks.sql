-- ===========================================================================
-- V128 — S4 PR2 — Change Request proposed-task tables.
--
-- Two tables that mirror the shape of wbs_template_task /
-- wbs_template_task_predecessor (V116). They hold the structured proposed
-- scope of a CR (ProjectVariation) while it moves through the
-- DRAFT -> SUBMITTED -> COSTED -> CUSTOMER_APPROVAL_PENDING -> APPROVED
-- workflow. On the APPROVED -> SCHEDULED transition (PR1 method
-- ProjectVariationService.schedule), ChangeRequestMergeService clones
-- these rows into `tasks` + `task_predecessor` at a scheduler-supplied
-- anchor.
--
-- FK to project_variations(id) ON DELETE CASCADE: deleting a CR also
-- deletes its proposed-task rows. After merge, the cloned rows live on
-- in `tasks` and are not touched.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS change_request_tasks (
    id                   BIGSERIAL PRIMARY KEY,
    change_request_id    BIGINT       NOT NULL REFERENCES project_variations(id) ON DELETE CASCADE,
    sequence             INT          NOT NULL,
    name                 VARCHAR(256) NOT NULL,
    role_hint            VARCHAR(64),
    duration_days        INT          NOT NULL,
    weight_factor        INT,
    monsoon_sensitive    BOOLEAN      NOT NULL DEFAULT FALSE,
    is_payment_milestone BOOLEAN      NOT NULL DEFAULT FALSE,
    floor_loop           VARCHAR(16)  NOT NULL DEFAULT 'NONE',
    optional_cost        NUMERIC(14,2),
    -- BaseEntity audit columns (mirrors V53/V58/V112/V119 convention)
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by_user_id   BIGINT,
    updated_by_user_id   BIGINT,
    deleted_at           TIMESTAMP,
    deleted_by_user_id   BIGINT,
    version              BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT uk_crt_request_seq    UNIQUE (change_request_id, sequence),
    CONSTRAINT chk_crt_floor_loop    CHECK (floor_loop IN ('NONE','PER_FLOOR')),
    CONSTRAINT chk_crt_duration_days CHECK (duration_days >= 1)
);
CREATE INDEX IF NOT EXISTS idx_crt_change_request ON change_request_tasks(change_request_id);

CREATE TABLE IF NOT EXISTS change_request_task_predecessors (
    id                       BIGSERIAL PRIMARY KEY,
    successor_cr_task_id     BIGINT NOT NULL REFERENCES change_request_tasks(id) ON DELETE CASCADE,
    predecessor_cr_task_id   BIGINT NOT NULL REFERENCES change_request_tasks(id) ON DELETE CASCADE,
    lag_days                 INT    NOT NULL DEFAULT 0,
    dep_type                 VARCHAR(2) NOT NULL DEFAULT 'FS',
    -- BaseEntity audit columns
    created_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by_user_id       BIGINT,
    updated_by_user_id       BIGINT,
    deleted_at               TIMESTAMP,
    deleted_by_user_id       BIGINT,
    version                  BIGINT    NOT NULL DEFAULT 1,
    CONSTRAINT uk_crtp_pair UNIQUE (successor_cr_task_id, predecessor_cr_task_id),
    CONSTRAINT chk_crtp_self CHECK (successor_cr_task_id <> predecessor_cr_task_id)
);
CREATE INDEX IF NOT EXISTS idx_crtp_succ ON change_request_task_predecessors(successor_cr_task_id);
CREATE INDEX IF NOT EXISTS idx_crtp_pred ON change_request_task_predecessors(predecessor_cr_task_id);
