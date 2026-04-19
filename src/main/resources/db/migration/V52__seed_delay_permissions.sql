-- V52: Seed DELAY module permissions
-- Target table is portal_permissions (matches Permission entity + V18/V27/V32/V39).
INSERT INTO portal_permissions (name, description) VALUES
    ('DELAY_CREATE', 'Create delay log entries'),
    ('DELAY_VIEW', 'View delay logs')
ON CONFLICT (name) DO NOTHING;
