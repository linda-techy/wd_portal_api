-- ============================================================================
-- V1_70: Create BOQ Audit Log Table
-- ============================================================================
-- Full audit trail for all BOQ operations. No silent updates allowed.
-- Records are immutable - never updated or deleted.
-- ============================================================================

CREATE TABLE IF NOT EXISTS boq_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    project_id BIGINT REFERENCES customer_projects(id),
    action_type VARCHAR(20) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    changed_by_id BIGINT REFERENCES portal_users(id),
    changed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Constraint for valid action types
ALTER TABLE boq_audit_logs ADD CONSTRAINT chk_boq_audit_action_type
    CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'LOCK', 'COMPLETE', 'ARCHIVE', 'EXECUTE', 'BILL'));

-- Indexes for querying
CREATE INDEX IF NOT EXISTS idx_boq_audit_entity ON boq_audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_boq_audit_project ON boq_audit_logs(project_id);
CREATE INDEX IF NOT EXISTS idx_boq_audit_changed_at ON boq_audit_logs(changed_at);
CREATE INDEX IF NOT EXISTS idx_boq_audit_action_type ON boq_audit_logs(action_type);

COMMENT ON TABLE boq_audit_logs IS 'Immutable audit trail for all BOQ operations. Records are never updated or deleted for compliance.';
COMMENT ON COLUMN boq_audit_logs.old_value IS 'JSON snapshot of entity state before change.';
COMMENT ON COLUMN boq_audit_logs.new_value IS 'JSON snapshot of entity state after change.';
