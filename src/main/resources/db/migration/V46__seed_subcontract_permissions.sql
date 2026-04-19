-- V46: Seed subcontract permissions.
-- Uses the correct table name `portal_permissions` (matches the
-- Permission entity's @Table mapping and all earlier migrations).
-- The table has (name, description) — no module / created_at columns.
INSERT INTO portal_permissions (name, description) VALUES
    ('SUBCONTRACT_VIEW', 'View subcontract work orders, measurements, and payments'),
    ('SUBCONTRACT_CREATE', 'Create work orders and record measurements/payments'),
    ('SUBCONTRACT_EDIT', 'Update work order details'),
    ('SUBCONTRACT_DELETE', 'Delete draft work orders'),
    ('SUBCONTRACT_APPROVE', 'Approve or reject measurements')
ON CONFLICT (name) DO NOTHING;
