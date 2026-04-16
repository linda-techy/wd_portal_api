-- V34: Add internal approval gate for change orders

-- New columns on change_orders table
ALTER TABLE change_orders ADD COLUMN IF NOT EXISTS internally_approved_at TIMESTAMP;
ALTER TABLE change_orders ADD COLUMN IF NOT EXISTS internally_approved_by BIGINT;

-- Index for querying COs pending internal approval
CREATE INDEX IF NOT EXISTS idx_co_internal_approval
    ON change_orders(status) WHERE status = 'SUBMITTED';

-- New activity types for the internal approval step
INSERT INTO activity_types (name, description)
SELECT 'CO_INTERNALLY_APPROVED', 'Change order approved internally by PM'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'CO_INTERNALLY_APPROVED');

INSERT INTO activity_types (name, description)
SELECT 'CO_INTERNALLY_REJECTED', 'Change order rejected internally'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'CO_INTERNALLY_REJECTED');
