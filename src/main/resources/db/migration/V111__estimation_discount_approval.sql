-- V111__estimation_discount_approval.sql
-- O — Discount approval trail. Estimations with discount above the configured
-- threshold (default 5%) require explicit approval before they can transition
-- from DRAFT to SENT.

ALTER TABLE estimation
    ADD COLUMN discount_percent NUMERIC(5,4),
    ADD COLUMN discount_approval_status VARCHAR(20),
    ADD COLUMN discount_approved_by_user_id BIGINT,
    ADD COLUMN discount_approved_at TIMESTAMP,
    ADD COLUMN discount_approval_notes TEXT;

ALTER TABLE estimation
    ADD CONSTRAINT estimation_discount_approval_status_chk
    CHECK (
        discount_approval_status IS NULL
        OR discount_approval_status IN ('PENDING', 'APPROVED', 'REJECTED')
    );

-- Permission seed (idempotent — same pattern as V102).
INSERT INTO portal_permissions (name, description) VALUES
    ('ESTIMATION_DISCOUNT_APPROVE',
     'Approve or reject discount above the configured threshold on a lead estimation')
ON CONFLICT (name) DO NOTHING;

INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM portal_roles r, portal_permissions p
WHERE r.code = 'ADMIN'
  AND p.name = 'ESTIMATION_DISCOUNT_APPROVE'
ON CONFLICT DO NOTHING;
