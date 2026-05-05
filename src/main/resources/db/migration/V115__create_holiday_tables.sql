-- V115: holiday + project_holiday_override + project_schedule_config tables.
-- Designed for the four-tier composition: NATIONAL ∪ STATE ∪ DISTRICT ∪ PROJECT
-- minus per-project EXCLUDE overrides.

CREATE TABLE holiday (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(64),
    name                VARCHAR(128) NOT NULL,
    holiday_date        DATE         NOT NULL,
    scope               VARCHAR(16)  NOT NULL,
    scope_ref           VARCHAR(64),
    recurrence_type     VARCHAR(16)  NOT NULL,
    source              VARCHAR(32),
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by_user_id      BIGINT,
    updated_by_user_id      BIGINT,
    deleted_at              TIMESTAMP,
    deleted_by_user_id      BIGINT,
    version                 BIGINT NOT NULL DEFAULT 1,

    CONSTRAINT chk_holiday_scope CHECK (scope IN ('NATIONAL','STATE','DISTRICT','PROJECT')),
    CONSTRAINT chk_holiday_recurrence CHECK (recurrence_type IN ('FIXED_DATE','LUNAR','ONE_OFF'))
);

CREATE INDEX idx_holiday_scope_date ON holiday (scope, holiday_date);
CREATE INDEX idx_holiday_scope_ref ON holiday (scope_ref);
CREATE UNIQUE INDEX uq_holiday_dedupe
    ON holiday (code, holiday_date, scope, COALESCE(scope_ref, ''))
    WHERE deleted_at IS NULL AND code IS NOT NULL;

CREATE TABLE project_holiday_override (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT       NOT NULL REFERENCES customer_projects(id) ON DELETE CASCADE,
    holiday_id      BIGINT       REFERENCES holiday(id) ON DELETE CASCADE,
    override_date   DATE         NOT NULL,
    override_name   VARCHAR(128),
    action          VARCHAR(16)  NOT NULL,

    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by_user_id      BIGINT,
    updated_by_user_id      BIGINT,
    deleted_at              TIMESTAMP,
    deleted_by_user_id      BIGINT,
    version                 BIGINT NOT NULL DEFAULT 1,

    CONSTRAINT chk_override_action CHECK (action IN ('ADD','EXCLUDE')),
    CONSTRAINT chk_override_consistency CHECK (
        (action = 'EXCLUDE' AND holiday_id IS NOT NULL) OR
        (action = 'ADD' AND override_name IS NOT NULL)
    )
);

CREATE INDEX idx_pho_project ON project_holiday_override (project_id);

CREATE TABLE project_schedule_config (
    id                          BIGSERIAL PRIMARY KEY,
    project_id                  BIGINT       NOT NULL UNIQUE
                                              REFERENCES customer_projects(id) ON DELETE CASCADE,
    sunday_working              BOOLEAN      NOT NULL DEFAULT FALSE,
    monsoon_start_month_day     SMALLINT     NOT NULL DEFAULT 601,
    monsoon_end_month_day       SMALLINT     NOT NULL DEFAULT 930,
    district_code               VARCHAR(16),

    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by_user_id      BIGINT,
    updated_by_user_id      BIGINT,
    deleted_at              TIMESTAMP,
    deleted_by_user_id      BIGINT,
    version                 BIGINT NOT NULL DEFAULT 1,

    CONSTRAINT chk_psc_monsoon_start CHECK (monsoon_start_month_day BETWEEN 101 AND 1231),
    CONSTRAINT chk_psc_monsoon_end CHECK (monsoon_end_month_day BETWEEN 101 AND 1231)
);
