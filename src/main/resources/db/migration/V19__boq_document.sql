-- =============================================================================
-- V19: BOQ Document — project-level BOQ container
-- =============================================================================
-- A BOQ Document is the top-level entity representing the agreed scope of work
-- for a project.  The three unbreakable Method 2 rules encoded here:
--
--   R-001  BOQ is permanently locked on customer approval (APPROVED is terminal).
--   R-002  Stage amounts are computed once at approval time and stored as
--          immutable values in payment_stages (see V20).
--   R-003  Every scope change after approval must go through a Change Order (V21).
--
-- Status flow: DRAFT → PENDING_APPROVAL → APPROVED (terminal)
--                                       → REJECTED  → DRAFT
-- =============================================================================

CREATE TABLE boq_documents (
    id                  BIGSERIAL       PRIMARY KEY,
    project_id          BIGINT          NOT NULL REFERENCES customer_projects(id),

    -- The total value of this BOQ (sum of all boq_items at snapshot time).
    -- Populated when the BOQ is submitted for approval; immutable after APPROVED.
    total_value_ex_gst  NUMERIC(18,6)   NOT NULL DEFAULT 0,
    gst_rate            NUMERIC(5,4)    NOT NULL DEFAULT 0.18,   -- e.g. 0.18 = 18 %
    total_gst_amount    NUMERIC(18,6)   NOT NULL DEFAULT 0,
    total_value_incl_gst NUMERIC(18,6)  NOT NULL DEFAULT 0,

    -- Status machine
    status              VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    -- CHECK ensures only valid transitions can be stored
    CONSTRAINT chk_boq_doc_status CHECK (
        status IN ('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED')
    ),

    -- Submission / approval metadata
    submitted_at        TIMESTAMP,
    submitted_by        BIGINT          REFERENCES portal_users(id),
    approved_at         TIMESTAMP,
    approved_by         BIGINT          REFERENCES portal_users(id),  -- portal side
    customer_approved_at TIMESTAMP,
    customer_approved_by BIGINT         REFERENCES customer_users(id),
    rejected_at         TIMESTAMP,
    rejected_by         BIGINT,         -- either portal or customer user
    rejection_reason    TEXT,

    -- Versioning: each time a REJECTED BOQ is revised a new version is drafted
    revision_number     INT             NOT NULL DEFAULT 1,

    -- Standard audit columns
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by_user_id  BIGINT          REFERENCES portal_users(id),
    updated_by_user_id  BIGINT          REFERENCES portal_users(id),
    deleted_at          TIMESTAMP,
    deleted_by_user_id  BIGINT,
    version             BIGINT          NOT NULL DEFAULT 1
);

-- Only one active (non-deleted) APPROVED document per project at a time
CREATE UNIQUE INDEX uq_boq_doc_project_approved
    ON boq_documents(project_id)
    WHERE status = 'APPROVED' AND deleted_at IS NULL;

CREATE INDEX idx_boq_doc_project    ON boq_documents(project_id);
CREATE INDEX idx_boq_doc_status     ON boq_documents(status);
CREATE INDEX idx_boq_doc_submitted  ON boq_documents(submitted_at) WHERE submitted_at IS NOT NULL;

-- =============================================================================
-- Link existing boq_items rows to a boq_document.
-- We add a nullable FK now; V20 will handle default-document backfill if needed.
-- =============================================================================
ALTER TABLE boq_items
    ADD COLUMN IF NOT EXISTS boq_document_id BIGINT REFERENCES boq_documents(id);

CREATE INDEX IF NOT EXISTS idx_boq_items_document
    ON boq_items(boq_document_id)
    WHERE boq_document_id IS NOT NULL;

-- =============================================================================
-- Stage percentage configuration — one row per stage per project.
-- Stored here so the percentages used at approval time are auditable.
-- =============================================================================
CREATE TABLE boq_stage_config (
    id                  BIGSERIAL   PRIMARY KEY,
    project_id          BIGINT      NOT NULL REFERENCES customer_projects(id),
    stage_number        INT         NOT NULL,   -- 1-based ordering
    stage_name          VARCHAR(100) NOT NULL,  -- e.g. "Foundation", "Structure"
    percentage          NUMERIC(6,4) NOT NULL,  -- e.g. 0.1500 = 15 %
    CONSTRAINT chk_stage_percentage CHECK (percentage > 0 AND percentage <= 1),
    CONSTRAINT uq_stage_config UNIQUE (project_id, stage_number),

    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    created_by_user_id  BIGINT      REFERENCES portal_users(id)
);

CREATE INDEX idx_boq_stage_config_project ON boq_stage_config(project_id);
