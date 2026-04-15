-- =============================================================================
-- V20: Payment Stages — immutable milestone-based payment schedule
-- =============================================================================
-- Rule R-002 (Method 2): Stage amounts are computed ONCE at BOQ approval time
-- and frozen.  They must NEVER be recomputed from live BOQ data after that.
--
-- A PaymentStage represents one milestone in the agreed payment schedule.
-- Each stage produces exactly one StageInvoice when it becomes due.
--
-- Stage status flow:
--   UPCOMING → DUE → INVOICED → PAID
--                             → OVERDUE    (if due_date passed and not paid)
--                             → ON_HOLD    (credit note applied, partial credit)
-- =============================================================================

CREATE TABLE payment_stages (
    id                      BIGSERIAL       PRIMARY KEY,
    boq_document_id         BIGINT          NOT NULL REFERENCES boq_documents(id),
    project_id              BIGINT          NOT NULL REFERENCES customer_projects(id),

    -- Stage identity
    stage_number            INT             NOT NULL,
    stage_name              VARCHAR(100)    NOT NULL,

    -- Immutable snapshot amounts (frozen at BOQ approval — never updated again)
    -- boq_value_snapshot = total_value_ex_gst from boq_documents at approval time
    boq_value_snapshot      NUMERIC(18,6)   NOT NULL,
    stage_percentage        NUMERIC(6,4)    NOT NULL,   -- e.g. 0.1500 = 15 %
    stage_amount_ex_gst     NUMERIC(18,6)   NOT NULL,   -- boq_value_snapshot * stage_percentage
    gst_rate                NUMERIC(5,4)    NOT NULL,   -- snapshot of GST rate at approval
    gst_amount              NUMERIC(18,6)   NOT NULL,   -- stage_amount_ex_gst * gst_rate
    stage_amount_incl_gst   NUMERIC(18,6)   NOT NULL,   -- stage_amount_ex_gst + gst_amount

    -- Status
    status                  VARCHAR(20)     NOT NULL DEFAULT 'UPCOMING',
    CONSTRAINT chk_stage_status CHECK (
        status IN ('UPCOMING','DUE','INVOICED','PAID','OVERDUE','ON_HOLD')
    ),

    -- Scheduling
    due_date                DATE,
    milestone_description   TEXT,

    -- Credit note tracking (applied against this stage)
    applied_credit_amount   NUMERIC(18,6)   NOT NULL DEFAULT 0,
    -- net_payable = stage_amount_incl_gst - applied_credit_amount
    -- Computed in application layer; stored for quick queries
    net_payable_amount      NUMERIC(18,6)   NOT NULL DEFAULT 0,

    -- Payment tracking
    paid_amount             NUMERIC(18,6)   NOT NULL DEFAULT 0,
    paid_at                 TIMESTAMP,
    invoice_id              BIGINT,         -- FK added after invoices table created (V22)

    -- Audit
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id      BIGINT          REFERENCES portal_users(id),
    updated_by_user_id      BIGINT          REFERENCES portal_users(id),
    version                 BIGINT          NOT NULL DEFAULT 1,

    CONSTRAINT uq_payment_stage UNIQUE (boq_document_id, stage_number)
);

CREATE INDEX idx_payment_stages_boq     ON payment_stages(boq_document_id);
CREATE INDEX idx_payment_stages_project ON payment_stages(project_id);
CREATE INDEX idx_payment_stages_status  ON payment_stages(status);
CREATE INDEX idx_payment_stages_due     ON payment_stages(due_date) WHERE due_date IS NOT NULL;
