INSERT INTO permissions (name, description, module, created_at)
VALUES
    ('SUBCONTRACT_VIEW', 'View subcontract work orders, measurements, and payments', 'SUBCONTRACT', NOW()),
    ('SUBCONTRACT_CREATE', 'Create work orders and record measurements/payments', 'SUBCONTRACT', NOW()),
    ('SUBCONTRACT_EDIT', 'Update work order details', 'SUBCONTRACT', NOW()),
    ('SUBCONTRACT_DELETE', 'Delete draft work orders', 'SUBCONTRACT', NOW()),
    ('SUBCONTRACT_APPROVE', 'Approve or reject measurements', 'SUBCONTRACT', NOW())
ON CONFLICT (name) DO NOTHING;
