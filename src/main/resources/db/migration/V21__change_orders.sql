-- =============================================================================
-- V21: Change Orders — scope changes after BOQ approval
-- =============================================================================
-- Rule R-003 (Method 2): Every scope change after BOQ approval is a Change Order.
-- Change Orders have their own billing lane (separate from stage invoices).
--
-- CO types:
--   SCOPE_ADDITION        — new items not in the original BOQ
--   SCOPE_REDUCTION       — removal or reduction of originally scoped items
--   UNFORESEEN_VARIATION  — site conditions that change required work
--   QUANTITY_VARIATION_INC / QUANTITY_VARIATION_DEC — qty change on existing item
--   RATE_VARIATION_INC / RATE_VARIATION_DEC          — rate change on existing item
--
-- CO status flow:
--   DRAFT → SUBMITTED → CUSTOMER_REVIEW → APPROVED / REJECTED
--   APPROVED → IN_PROGRESS → COMPLETED → CLOSED
--
-- On SCOPE_REDUCTION / QUANTITY_VARIATION_DEC / RATE_VARIATION_DEC approval,
-- a credit_note is auto-generated (see V22).
-- =============================================================================

CREATE TABLE change_orders (
    id                      BIGSERIAL       PRIMARY KEY,
    boq_document_id         BIGINT          NOT NULL REFERENCES boq_documents(id),
    project_id              BIGINT          NOT NULL REFERENCES customer_projects(id),

    -- Human-readable reference (e.g. CO-2024-001)
    reference_number        VARCHAR(50)     NOT NULL,
    CONSTRAINT uq_co_reference UNIQUE (project_id, reference_number),

    co_type                 VARCHAR(40)     NOT NULL,
    CONSTRAINT chk_co_type CHECK (
        co_type IN (
            'SCOPE_ADDITION',
            'SCOPE_REDUCTION',
            'UNFORESEEN_VARIATION',
            'QUANTITY_VARIATION_INC',
            'QUANTITY_VARIATION_DEC',
            'RATE_VARIATION_INC',
            'RATE_VARIATION_DEC'
        )
    ),

    -- Status machine
    status                  VARCHAR(25)     NOT NULL DEFAULT 'DRAFT',
    CONSTRAINT chk_co_status CHECK (
        status IN (
            'DRAFT','SUBMITTED','CUSTOMER_REVIEW',
            'APPROVED','REJECTED',
            'IN_PROGRESS','COMPLETED','CLOSED'
        )
    ),

    -- Financial summary (aggregated from line items)
    -- Positive = addition, negative = reduction
    net_amount_ex_gst       NUMERIC(18,6)   NOT NULL DEFAULT 0,
    gst_rate                NUMERIC(5,4)    NOT NULL DEFAULT 0.18,
    gst_amount              NUMERIC(18,6)   NOT NULL DEFAULT 0,
    net_amount_incl_gst     NUMERIC(18,6)   NOT NULL DEFAULT 0,

    -- Description / justification
    title                   VARCHAR(255)    NOT NULL,
    description             TEXT,
    justification           TEXT,

    -- Workflow timestamps
    submitted_at            TIMESTAMP,
    submitted_by            BIGINT          REFERENCES portal_users(id),
    customer_reviewed_at    TIMESTAMP,
    approved_at             TIMESTAMP,
    approved_by             BIGINT          REFERENCES customer_users(id),
    rejected_at             TIMESTAMP,
    rejected_by             BIGINT          REFERENCES customer_users(id),
    rejection_reason        TEXT,
    completed_at            TIMESTAMP,
    closed_at               TIMESTAMP,

    -- Customer-facing due date for review
    review_deadline         DATE,

    -- Audit
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id      BIGINT          REFERENCES portal_users(id),
    updated_by_user_id      BIGINT          REFERENCES portal_users(id),
    deleted_at              TIMESTAMP,
    deleted_by_user_id      BIGINT,
    version                 BIGINT          NOT NULL DEFAULT 1
);

CREATE INDEX idx_co_boq_document   ON change_orders(boq_document_id);
CREATE INDEX idx_co_project        ON change_orders(project_id);
CREATE INDEX idx_co_status         ON change_orders(status);
CREATE INDEX idx_co_type           ON change_orders(co_type);
CREATE INDEX idx_co_reference      ON change_orders(reference_number);

-- =============================================================================
-- Change Order Line Items — individual work items within a CO
-- =============================================================================

CREATE TABLE change_order_line_items (
    id                      BIGSERIAL       PRIMARY KEY,
    change_order_id         BIGINT          NOT NULL REFERENCES change_orders(id),

    -- Optional back-reference to the original BOQ item being changed
    boq_item_id             BIGINT          REFERENCES boq_items(id),

    description             VARCHAR(255)    NOT NULL,
    unit                    VARCHAR(50),

    -- For QUANTITY_VARIATION: original_qty, new_qty, delta_qty
    original_quantity       NUMERIC(18,6)   DEFAULT 0,
    new_quantity            NUMERIC(18,6)   DEFAULT 0,
    delta_quantity          NUMERIC(18,6)   DEFAULT 0,  -- new - original (negative = reduction)

    -- For RATE_VARIATION: original_rate, new_rate
    original_rate           NUMERIC(18,6)   DEFAULT 0,
    new_rate                NUMERIC(18,6)   DEFAULT 0,

    -- The unit rate applicable to this line item
    unit_rate               NUMERIC(18,6)   NOT NULL DEFAULT 0,

    -- Line amount = delta_quantity * unit_rate (or equivalent for rate variation)
    -- Positive = addition, negative = reduction
    line_amount_ex_gst      NUMERIC(18,6)   NOT NULL DEFAULT 0,

    specifications          TEXT,
    notes                   TEXT,

    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_co_line_items_co   ON change_order_line_items(change_order_id);
CREATE INDEX idx_co_line_items_boq  ON change_order_line_items(boq_item_id)
    WHERE boq_item_id IS NOT NULL;

-- =============================================================================
-- CO Milestones — execution checkpoints for an approved CO
-- (only relevant once CO status = IN_PROGRESS)
-- =============================================================================

CREATE TABLE change_order_milestones (
    id                  BIGSERIAL       PRIMARY KEY,
    change_order_id     BIGINT          NOT NULL REFERENCES change_orders(id),
    milestone_number    INT             NOT NULL,
    description         VARCHAR(255)    NOT NULL,
    percentage          NUMERIC(6,4)    NOT NULL,  -- percentage of CO value due at this milestone
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    CONSTRAINT chk_co_milestone_status CHECK (
        status IN ('PENDING','IN_PROGRESS','COMPLETED')
    ),
    due_date            DATE,
    completed_at        TIMESTAMP,
    completed_by        BIGINT          REFERENCES portal_users(id),

    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_co_milestone UNIQUE (change_order_id, milestone_number)
);

CREATE INDEX idx_co_milestones_co ON change_order_milestones(change_order_id);
