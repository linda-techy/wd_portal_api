-- V50: Seed support ticket permissions for portal staff.

INSERT INTO permissions (name, description, module, created_at)
VALUES
    ('TICKET_VIEW', 'View support tickets', 'SUPPORT', NOW()),
    ('TICKET_ASSIGN', 'Assign support tickets to staff', 'SUPPORT', NOW()),
    ('TICKET_REPLY', 'Reply to support tickets', 'SUPPORT', NOW()),
    ('TICKET_MANAGE', 'Manage support ticket status', 'SUPPORT', NOW())
ON CONFLICT (name) DO NOTHING;
