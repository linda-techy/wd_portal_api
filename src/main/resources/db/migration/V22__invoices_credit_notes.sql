-- =============================================================================
-- V22: BOQ Invoices, Credit Notes, and Refund Notices
-- =============================================================================
-- Two distinct invoice lanes (Method 2):
--
--   STAGE_INVOICE  — one per payment stage; contains ONLY stage amount + GST +
--                    any applied credit note lines. Never contains CO amounts.
--
--   CO_INVOICE     — one per CO billing event (advance, midpoint, balance);
--                    completely separate from stage invoices.
--
-- Uses table name "boq_invoices" to avoid collision with existing project_invoices.
--
-- Invoice status flow:
--   DRAFT → SENT → VIEWED → PAID
--                          → DISPUTED  → RESOLVED → PAID
--                          → OVERDUE   → PAID
--
-- Credit Note:
--   Auto-generated on reduction CO approval.
--   Applied sequentially to the next DUE/UPCOMING stage invoice(s).
--   If credit > all remaining stage invoices → RefundNotice.
--
-- Refund Notice:
--   Issued when total credit notes exceed all remaining stage invoices.
-- =============================================================================

CREATE TABLE boq_invoices (
    id                      BIGSERIAL       PRIMARY KEY,
    project_id              BIGINT          NOT NULL REFERENCES customer_projects(id),

    -- Invoice type discriminator
    invoice_type            VARCHAR(20)     NOT NULL,
    CONSTRAINT chk_boq_invoice_type CHECK (
        invoice_type IN ('STAGE_INVOICE','CO_INVOICE')
    ),

    -- Human-readable invoice number (e.g. INV-2024-001)
    invoice_number          VARCHAR(50)     NOT NULL,
    CONSTRAINT uq_boq_invoice_number UNIQUE (project_id, invoice_number),

    -- Reference to source entity
    payment_stage_id        BIGINT          REFERENCES payment_stages(id),   -- for STAGE_INVOICE
    change_order_id         BIGINT          REFERENCES change_orders(id),    -- for CO_INVOICE

    -- CO billing sub-type (only for CO_INVOICE)
    co_billing_event        VARCHAR(20),
    CONSTRAINT chk_co_billing_event CHECK (
        co_billing_event IS NULL OR
        co_billing_event IN ('ADVANCE','MIDPOINT','BALANCE')
    ),

    -- Financial amounts
    subtotal_ex_gst         NUMERIC(18,6)   NOT NULL DEFAULT 0,
    gst_rate                NUMERIC(5,4)    NOT NULL DEFAULT 0.18,
    gst_amount              NUMERIC(18,6)   NOT NULL DEFAULT 0,
    total_incl_gst          NUMERIC(18,6)   NOT NULL DEFAULT 0,

    -- Credit note deductions applied to this invoice
    total_credit_applied    NUMERIC(18,6)   NOT NULL DEFAULT 0,

    -- Net amount due after credit application
    net_amount_due          NUMERIC(18,6)   NOT NULL DEFAULT 0,

    -- Status
    status                  VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    CONSTRAINT chk_boq_invoice_status CHECK (
        status IN ('DRAFT','SENT','VIEWED','PAID','DISPUTED','OVERDUE','VOID')
    ),

    -- Dates
    issue_date              DATE,
    due_date                DATE,
    sent_at                 TIMESTAMP,
    viewed_at               TIMESTAMP,
    paid_at                 TIMESTAMP,
    disputed_at             TIMESTAMP,
    dispute_reason          TEXT,
    resolved_at             TIMESTAMP,

    -- Payment details
    payment_reference       VARCHAR(100),
    payment_method          VARCHAR(50),

    notes                   TEXT,

    -- Audit
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id      BIGINT          REFERENCES portal_users(id),
    updated_by_user_id      BIGINT          REFERENCES portal_users(id),
    deleted_at              TIMESTAMP,
    deleted_by_user_id      BIGINT,
    version                 BIGINT          NOT NULL DEFAULT 1
);

CREATE INDEX idx_boq_invoices_project        ON boq_invoices(project_id);
CREATE INDEX idx_boq_invoices_type           ON boq_invoices(invoice_type);
CREATE INDEX idx_boq_invoices_status         ON boq_invoices(status);
CREATE INDEX idx_boq_invoices_stage          ON boq_invoices(payment_stage_id) WHERE payment_stage_id IS NOT NULL;
CREATE INDEX idx_boq_invoices_co             ON boq_invoices(change_order_id) WHERE change_order_id IS NOT NULL;
CREATE INDEX idx_boq_invoices_due            ON boq_invoices(due_date) WHERE due_date IS NOT NULL;

-- =============================================================================
-- BOQ Invoice Line Items
-- =============================================================================

CREATE TABLE boq_invoice_line_items (
    id                  BIGSERIAL       PRIMARY KEY,
    invoice_id          BIGINT          NOT NULL REFERENCES boq_invoices(id),
    line_type           VARCHAR(30)     NOT NULL,
    -- LINE_TYPES: STAGE_AMOUNT, CO_AMOUNT, GST, CREDIT_NOTE_DEDUCTION, RETENTION
    description         VARCHAR(255)    NOT NULL,
    quantity            NUMERIC(18,6)   NOT NULL DEFAULT 1,
    unit_price          NUMERIC(18,6)   NOT NULL DEFAULT 0,
    amount              NUMERIC(18,6)   NOT NULL DEFAULT 0,
    sort_order          INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_boq_invoice_line_items ON boq_invoice_line_items(invoice_id);

-- Back-fill the FK from payment_stages to boq_invoices now that the table exists
ALTER TABLE payment_stages
    ADD COLUMN IF NOT EXISTS boq_invoice_id BIGINT REFERENCES boq_invoices(id);

-- Drop the placeholder column added in V20 (invoice_id without FK)
ALTER TABLE payment_stages
    DROP COLUMN IF EXISTS invoice_id;

CREATE INDEX idx_payment_stages_boq_invoice
    ON payment_stages(boq_invoice_id)
    WHERE boq_invoice_id IS NOT NULL;

-- =============================================================================
-- Credit Notes
-- =============================================================================

CREATE TABLE credit_notes (
    id                      BIGSERIAL       PRIMARY KEY,
    project_id              BIGINT          NOT NULL REFERENCES customer_projects(id),
    change_order_id         BIGINT          NOT NULL REFERENCES change_orders(id),

    credit_note_number      VARCHAR(50)     NOT NULL,
    CONSTRAINT uq_credit_note_number UNIQUE (project_id, credit_note_number),

    credit_amount_ex_gst    NUMERIC(18,6)   NOT NULL,
    gst_rate                NUMERIC(5,4)    NOT NULL DEFAULT 0.18,
    gst_amount              NUMERIC(18,6)   NOT NULL,
    total_credit_incl_gst   NUMERIC(18,6)   NOT NULL,

    remaining_balance       NUMERIC(18,6)   NOT NULL,

    status                  VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT chk_cn_status CHECK (
        status IN ('AVAILABLE','PARTIALLY_APPLIED','FULLY_APPLIED','REFUNDED','VOID')
    ),

    issued_at               TIMESTAMP       NOT NULL DEFAULT NOW(),
    fully_applied_at        TIMESTAMP,
    notes                   TEXT,

    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id      BIGINT          REFERENCES portal_users(id),
    updated_by_user_id      BIGINT          REFERENCES portal_users(id),
    version                 BIGINT          NOT NULL DEFAULT 1
);

CREATE INDEX idx_credit_notes_project ON credit_notes(project_id);
CREATE INDEX idx_credit_notes_co      ON credit_notes(change_order_id);
CREATE INDEX idx_credit_notes_status  ON credit_notes(status);

-- =============================================================================
-- Credit Note Applications
-- =============================================================================

CREATE TABLE credit_note_applications (
    id                  BIGSERIAL       PRIMARY KEY,
    credit_note_id      BIGINT          NOT NULL REFERENCES credit_notes(id),
    invoice_id          BIGINT          NOT NULL REFERENCES boq_invoices(id),
    applied_amount      NUMERIC(18,6)   NOT NULL,
    applied_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    applied_by          BIGINT          REFERENCES portal_users(id)
);

CREATE INDEX idx_cna_credit_note ON credit_note_applications(credit_note_id);
CREATE INDEX idx_cna_invoice     ON credit_note_applications(invoice_id);

-- =============================================================================
-- Refund Notices
-- =============================================================================

CREATE TABLE refund_notices (
    id                      BIGSERIAL       PRIMARY KEY,
    project_id              BIGINT          NOT NULL REFERENCES customer_projects(id),
    credit_note_id          BIGINT          NOT NULL REFERENCES credit_notes(id),

    reference_number        VARCHAR(50)     NOT NULL,
    CONSTRAINT uq_refund_reference UNIQUE (project_id, reference_number),

    refund_amount           NUMERIC(18,6)   NOT NULL,
    reason                  TEXT,

    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    CONSTRAINT chk_refund_status CHECK (
        status IN ('PENDING','ACKNOWLEDGED','PROCESSING','COMPLETED','VOID')
    ),

    issued_at               TIMESTAMP       NOT NULL DEFAULT NOW(),
    acknowledged_at         TIMESTAMP,
    completed_at            TIMESTAMP,
    payment_reference       VARCHAR(100),
    notes                   TEXT,

    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id      BIGINT          REFERENCES portal_users(id),
    updated_by_user_id      BIGINT          REFERENCES portal_users(id),
    version                 BIGINT          NOT NULL DEFAULT 1
);

CREATE INDEX idx_refund_notices_project ON refund_notices(project_id);
CREATE INDEX idx_refund_notices_status  ON refund_notices(status);
