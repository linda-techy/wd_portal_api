-- Task quality gates (ITP pattern).
--
-- For every schedule task the SAME assigned site engineer runs 3 sequential
-- quality-gate checks against the same task:
--
--   1. PRELIMINARY  — before work starts (setting out, layout, material check)
--   2. IN_PROGRESS  — during execution  (rebar before pour, formwork, alignment)
--   3. FINAL        — after completion  (dimensional verify, finish quality)
--
-- This is the construction Inspection-Test Plan (ITP) — hold-point, witness-
-- point, final-inspection. The same engineer signs each; gates are sequential;
-- the next can't be entered until the previous passes. A task can NOT be
-- marked COMPLETED until its FINAL gate is PASSED.

CREATE TABLE task_quality_gates (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    gate_type       VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    signed_by_user_id BIGINT REFERENCES portal_users(id),
    signed_at         TIMESTAMP,
    notes             TEXT,
    failure_reason    TEXT,

    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT task_qc_gate_type_check
        CHECK (gate_type IN ('PRELIMINARY','IN_PROGRESS','FINAL')),
    CONSTRAINT task_qc_status_check
        CHECK (status IN ('PENDING','PASSED','FAILED','NA')),
    CONSTRAINT task_qc_unique_gate
        UNIQUE (task_id, gate_type)
);

CREATE INDEX idx_task_qc_gates_task    ON task_quality_gates (task_id);
CREATE INDEX idx_task_qc_gates_status  ON task_quality_gates (status);

COMMENT ON TABLE  task_quality_gates IS
    'Per-task ITP quality gates (PRELIMINARY / IN_PROGRESS / FINAL). Exactly 3 rows per task.';
COMMENT ON COLUMN task_quality_gates.status IS
    'PENDING (not yet signed), PASSED (gate cleared), FAILED (rework required), NA (skipped by site eng with justification).';

-- ── Backfill: 3 PENDING gates for every existing non-deleted task ──────────
-- ON CONFLICT keeps this idempotent against a partially-seeded DB.

INSERT INTO task_quality_gates (task_id, gate_type, status)
SELECT t.id, 'PRELIMINARY', 'PENDING'
FROM tasks t WHERE t.deleted_at IS NULL
ON CONFLICT (task_id, gate_type) DO NOTHING;

INSERT INTO task_quality_gates (task_id, gate_type, status)
SELECT t.id, 'IN_PROGRESS', 'PENDING'
FROM tasks t WHERE t.deleted_at IS NULL
ON CONFLICT (task_id, gate_type) DO NOTHING;

INSERT INTO task_quality_gates (task_id, gate_type, status)
SELECT t.id, 'FINAL', 'PENDING'
FROM tasks t WHERE t.deleted_at IS NULL
ON CONFLICT (task_id, gate_type) DO NOTHING;

-- Tasks that were already COMPLETED before this migration are auto-passed so
-- the new FINAL-gate completion constraint doesn't retroactively un-complete them.
UPDATE task_quality_gates g
SET status = 'PASSED',
    notes  = 'Auto-passed by V152: task was already COMPLETED when quality gates were introduced.',
    updated_at = NOW()
FROM tasks t
WHERE g.task_id = t.id
  AND t.status  = 'COMPLETED'
  AND g.status  = 'PENDING';

-- ── New permission for signing off gates ───────────────────────────────────
INSERT INTO portal_permissions (name, description) VALUES
    ('TASK_QC_SIGNOFF',
     'Sign off (pass/fail) the preliminary, in-progress, and final quality gates on a task')
ON CONFLICT (name) DO NOTHING;

WITH role_permission_mapping(role_code, perm_name) AS (
    VALUES
    -- Field-level engineers + foremen actually run the inspections
    ('SITE_ENGINEER',        'TASK_QC_SIGNOFF'),
    ('SITE_SUPERVISOR',      'TASK_QC_SIGNOFF'),
    ('MEP_SUPERVISOR',       'TASK_QC_SIGNOFF'),
    ('FOREMAN',              'TASK_QC_SIGNOFF'),
    -- QA / safety has the final word on quality
    ('QUALITY_SAFETY',       'TASK_QC_SIGNOFF'),
    -- Admin + PM can override / unblock
    ('ADMIN',                'TASK_QC_SIGNOFF'),
    ('PROJECT_MANAGER',      'TASK_QC_SIGNOFF')
)
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role_permission_mapping rpm
JOIN portal_roles r        ON r.code = rpm.role_code
JOIN portal_permissions p  ON p.name = rpm.perm_name
WHERE NOT EXISTS (
    SELECT 1 FROM portal_role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
