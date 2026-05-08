-- ===========================================================================
-- V127 — S4 PR1 CR v2 state machine + history + permissions
--
-- 1) Renames legacy PENDING_APPROVAL rows to CUSTOMER_APPROVAL_PENDING
--    (data fixup BEFORE the new CHECK so existing rows pass validation).
-- 2) Drops Hibernate's auto-generated CHECK on project_variations.status
--    (both possible names: _check from Hibernate, _chk from a prior run).
-- 3) Adds named CHECK admitting all 9 CR v2 states.
-- 4) Adds 9 new columns on project_variations for CR v2 audit trail
--    (cost/time impact, rejection reason, transition timestamps).
-- 5) Creates change_request_approval_history (NEW table — distinct from
--    co_approval_history from V26 which serves the legacy ChangeOrder
--    domain). FK to project_variations(id). Append-only at app layer.
-- 6) Seeds 7 new portal_permissions: CR_SUBMIT, CR_COST,
--    CR_SEND_TO_CUSTOMER, CR_SCHEDULE, CR_START, CR_COMPLETE, CR_REJECT.
--    (CR_APPROVE is customer-side; not seeded here.)
-- 7) Grants the matrix from the spec (13 pairs).
--
-- Idempotent: ON CONFLICT DO NOTHING + IF EXISTS / IF NOT EXISTS.
-- ===========================================================================

-- 0) Widen the status column BEFORE the data fixup. Hibernate's
--    @Column(length=20) is too short for CUSTOMER_APPROVAL_PENDING (25 chars).
--    Task 3 also bumps the entity to length=40; this ALTER aligns the DB.
ALTER TABLE project_variations
    ALTER COLUMN status TYPE VARCHAR(40);

-- 1) Data fixup BEFORE the new CHECK so existing rows still validate.
UPDATE project_variations
   SET status = 'CUSTOMER_APPROVAL_PENDING'
 WHERE status = 'PENDING_APPROVAL';

-- 2) Drop existing CHECK (Hibernate auto-generates *_check; a prior PR
--    might have used *_chk. Drop both for safety.).
ALTER TABLE project_variations
    DROP CONSTRAINT IF EXISTS project_variations_status_check;
ALTER TABLE project_variations
    DROP CONSTRAINT IF EXISTS project_variations_status_chk;

-- 3) Add named CHECK admitting all 9 CR v2 states.
ALTER TABLE project_variations
    ADD CONSTRAINT project_variations_status_chk
    CHECK (status IN (
        'DRAFT',
        'SUBMITTED',
        'COSTED',
        'CUSTOMER_APPROVAL_PENDING',
        'APPROVED',
        'SCHEDULED',
        'IN_PROGRESS',
        'COMPLETE',
        'REJECTED'
    ));

-- 4) New columns on project_variations for CR v2 metadata.
ALTER TABLE project_variations
    ADD COLUMN IF NOT EXISTS cost_impact              NUMERIC(15,2)  NULL,
    ADD COLUMN IF NOT EXISTS time_impact_working_days INTEGER        NULL,
    ADD COLUMN IF NOT EXISTS rejection_reason         VARCHAR(500)   NULL,
    ADD COLUMN IF NOT EXISTS submitted_at             TIMESTAMP      NULL,
    ADD COLUMN IF NOT EXISTS costed_at                TIMESTAMP      NULL,
    ADD COLUMN IF NOT EXISTS sent_to_customer_at      TIMESTAMP      NULL,
    ADD COLUMN IF NOT EXISTS scheduled_at             TIMESTAMP      NULL,
    ADD COLUMN IF NOT EXISTS started_at               TIMESTAMP      NULL,
    ADD COLUMN IF NOT EXISTS completed_at             TIMESTAMP      NULL,
    ADD COLUMN IF NOT EXISTS rejected_at              TIMESTAMP      NULL;
-- Note: approved_at already exists on project_variations (V1-era).

-- 5) New audit table for CR transitions. Distinct from co_approval_history
--    (V26) which is for the legacy ChangeOrder VO domain.
CREATE TABLE IF NOT EXISTS change_request_approval_history (
    id                  BIGSERIAL    PRIMARY KEY,
    change_request_id   BIGINT       NOT NULL REFERENCES project_variations(id) ON DELETE CASCADE,
    from_status         VARCHAR(40)  NULL,
    to_status           VARCHAR(40)  NOT NULL,
    otp_hash            VARCHAR(64)  NULL,
    customer_ip         VARCHAR(64)  NULL,
    actor_user_id       BIGINT       NULL,
    customer_user_id    BIGINT       NULL,
    reason              VARCHAR(500) NULL,
    action_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_crah_change_request_id ON change_request_approval_history(change_request_id);
CREATE INDEX IF NOT EXISTS idx_crah_action_at         ON change_request_approval_history(change_request_id, action_at DESC);

-- 6) Permission rows (idempotent).
INSERT INTO portal_permissions (name, description) VALUES
    ('CR_SUBMIT',           'Submit a draft CR for QS costing'),
    ('CR_COST',             'Add cost + time impact to a submitted CR (Quantity Surveyor)'),
    ('CR_SEND_TO_CUSTOMER', 'Forward a costed CR to customer for OTP approval'),
    ('CR_SCHEDULE',         'Merge an approved CR into the project WBS'),
    ('CR_START',            'Mark a scheduled CR as in-progress (Site Engineer)'),
    ('CR_COMPLETE',         'Mark an in-progress CR as complete with photo evidence'),
    ('CR_REJECT',           'Reject a CR at any stage with a reason')
ON CONFLICT (name) DO NOTHING;

-- 7) Grants. INNER-JOIN pattern mirrors V117/V120/V125. Roles that
--    aren't seeded yet are silently dropped by the join. Idempotent.
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM portal_roles r
JOIN portal_permissions p ON TRUE
JOIN (VALUES
    ('PROJECT_MANAGER',  'CR_SUBMIT'),
    ('PROJECT_MANAGER',  'CR_SEND_TO_CUSTOMER'),
    ('PROJECT_MANAGER',  'CR_REJECT'),
    ('SCHEDULER',        'CR_SUBMIT'),
    ('SCHEDULER',        'CR_SCHEDULE'),
    ('QUANTITY_SURVEYOR','CR_COST'),
    ('SITE_ENGINEER',    'CR_START'),
    ('SITE_ENGINEER',    'CR_COMPLETE'),
    ('ADMIN',            'CR_SUBMIT'),
    ('ADMIN',            'CR_COST'),
    ('ADMIN',            'CR_SEND_TO_CUSTOMER'),
    ('ADMIN',            'CR_SCHEDULE'),
    ('ADMIN',            'CR_REJECT')
) AS grants(role_code, permission_name)
     ON r.code = grants.role_code AND p.name = grants.permission_name
ON CONFLICT (role_id, permission_id) DO NOTHING;
