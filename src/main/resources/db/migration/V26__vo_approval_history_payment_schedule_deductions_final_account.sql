-- =============================================================================
-- V26: New tables for full VO lifecycle management
--   co_approval_history   — immutable approval audit trail per VO
--   co_payment_schedule   — advance / progress / completion split per VO
--   deduction_register    — omission deduction tracking per project
--   final_account         — final account reconciliation per project
-- =============================================================================

-- ---- co_approval_history -----------------------------------------------------
-- Immutable: INSERT only. No UPDATE or DELETE ever.
CREATE TABLE IF NOT EXISTS co_approval_history (
    id              BIGSERIAL       PRIMARY KEY,
    co_id           BIGINT          NOT NULL REFERENCES change_orders(id),

    approver_name   VARCHAR(100)    NOT NULL,
    approver_id     BIGINT          REFERENCES portal_users(id),
    level           VARCHAR(30)     NOT NULL
        CONSTRAINT chk_cah_level CHECK (level IN ('PM','COMMERCIAL_MANAGER','DIRECTOR')),
    action          VARCHAR(20)     NOT NULL
        CONSTRAINT chk_cah_action CHECK (action IN ('APPROVED','REJECTED','ESCALATED','RETURNED')),
    comment         TEXT,
    action_at       TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cah_co_id       ON co_approval_history(co_id);
CREATE INDEX IF NOT EXISTS idx_cah_action_at   ON co_approval_history(co_id, action_at DESC);

-- ---- co_payment_schedule -----------------------------------------------------
-- One row per approved VO. Percentages must sum to 100.
-- For MATERIAL_HEAVY: advance 40%, progress 40%, completion 20%
-- For LABOUR_HEAVY:   advance 20%, progress 60%, completion 20%
-- For MIXED:          advance 30%, progress 50%, completion 20%
-- For CUSTOM:         user-defined
CREATE TABLE IF NOT EXISTS co_payment_schedule (
    id                          BIGSERIAL       PRIMARY KEY,
    co_id                       BIGINT          NOT NULL UNIQUE REFERENCES change_orders(id),

    -- Advance
    advance_pct                 INTEGER         NOT NULL DEFAULT 30,
    advance_amount              NUMERIC(18,6)   NOT NULL DEFAULT 0,
    advance_status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
        CONSTRAINT chk_cps_adv_status CHECK (advance_status IN ('PENDING','INVOICED','PAID','OVERDUE')),
    advance_due_date            DATE,
    advance_paid_date           DATE,
    advance_invoice_number      VARCHAR(50),

    -- Progress
    progress_pct                INTEGER         NOT NULL DEFAULT 50,
    progress_amount             NUMERIC(18,6)   NOT NULL DEFAULT 0,
    progress_status             VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
        CONSTRAINT chk_cps_prg_status CHECK (progress_status IN ('PENDING','INVOICED','PAID','OVERDUE')),
    progress_trigger_stage_id   BIGINT          REFERENCES payment_stages(id),
    progress_paid_date          DATE,

    -- Completion
    completion_pct              INTEGER         NOT NULL DEFAULT 20,
    completion_amount           NUMERIC(18,6)   NOT NULL DEFAULT 0,
    completion_status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
        CONSTRAINT chk_cps_cmp_status CHECK (completion_status IN ('PENDING','INVOICED','PAID','OVERDUE')),
    completion_trigger          VARCHAR(50)     NOT NULL DEFAULT 'FINAL_ACCOUNT_APPROVED',
    completion_paid_date        DATE,

    CONSTRAINT chk_cps_pct_total CHECK (advance_pct + progress_pct + completion_pct = 100),

    created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cps_co_id
    ON co_payment_schedule(co_id);
CREATE INDEX IF NOT EXISTS idx_cps_trigger_stage
    ON co_payment_schedule(progress_trigger_stage_id) WHERE progress_trigger_stage_id IS NOT NULL;

-- ---- deduction_register -------------------------------------------------------
-- Tracks omission deductions raised per project.
-- Links to an OMISSION/SCOPE_REDUCTION ChangeOrder via co_id (optional).
CREATE TABLE IF NOT EXISTS deduction_register (
    id                      BIGSERIAL       PRIMARY KEY,
    project_id              BIGINT          NOT NULL REFERENCES customer_projects(id),
    co_id                   BIGINT          REFERENCES change_orders(id),

    item_description        TEXT            NOT NULL,
    requested_amount        NUMERIC(18,6)   NOT NULL,
    accepted_amount         NUMERIC(18,6),

    decision                VARCHAR(30)     NOT NULL DEFAULT 'PENDING'
        CONSTRAINT chk_dr_decision CHECK (
            decision IN ('PENDING','ACCEPTABLE','PARTIALLY_ACCEPTABLE','REJECTED')
        ),
    rejection_reason        TEXT,

    escalation_status       VARCHAR(20)     NOT NULL DEFAULT 'NONE'
        CONSTRAINT chk_dr_escalation CHECK (escalation_status IN ('NONE','ESCALATED','RESOLVED')),
    escalated_to            VARCHAR(100),

    settled_in_final_account BOOLEAN        NOT NULL DEFAULT FALSE,
    approved_by             VARCHAR(100),
    decision_date           DATE,

    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dr_project_id ON deduction_register(project_id);
CREATE INDEX IF NOT EXISTS idx_dr_co_id      ON deduction_register(co_id) WHERE co_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_dr_decision   ON deduction_register(project_id, decision);

-- ---- final_account ------------------------------------------------------------
-- One per project. Summarises contract value, VOs, deductions, payments, retention.
CREATE TABLE IF NOT EXISTS final_account (
    id                          BIGSERIAL       PRIMARY KEY,
    project_id                  BIGINT          NOT NULL UNIQUE REFERENCES customer_projects(id),

    base_contract_value         NUMERIC(18,6)   NOT NULL DEFAULT 0,
    total_additions             NUMERIC(18,6)   NOT NULL DEFAULT 0,
    total_accepted_deductions   NUMERIC(18,6)   NOT NULL DEFAULT 0,
    -- net_revised_contract_value = base + additions - deductions (computed on read)

    total_received_to_date      NUMERIC(18,6)   NOT NULL DEFAULT 0,
    total_retention_held        NUMERIC(18,6)   NOT NULL DEFAULT 0,
    -- balance_payable = net_revised_contract_value - total_received_to_date (computed)

    status                      VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_fa_status CHECK (
            status IN ('DRAFT','SUBMITTED','DISPUTED','AGREED','CLOSED')
        ),

    dlp_start_date              DATE,
    dlp_end_date                DATE,
    retention_released          BOOLEAN         NOT NULL DEFAULT FALSE,
    retention_release_date      DATE,

    prepared_by                 VARCHAR(100),
    agreed_by                   VARCHAR(100),

    created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fa_project_id ON final_account(project_id);
CREATE INDEX IF NOT EXISTS idx_fa_status     ON final_account(status);
