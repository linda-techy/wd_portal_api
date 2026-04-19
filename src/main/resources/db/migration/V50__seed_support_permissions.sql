-- V50: Seed support ticket permissions for portal staff.
-- Target table is portal_permissions (matches Permission entity + V18/V27/V32/V39).
INSERT INTO portal_permissions (name, description) VALUES
    ('TICKET_VIEW', 'View support tickets'),
    ('TICKET_ASSIGN', 'Assign support tickets to staff'),
    ('TICKET_REPLY', 'Reply to support tickets'),
    ('TICKET_MANAGE', 'Manage support ticket status')
ON CONFLICT (name) DO NOTHING;
