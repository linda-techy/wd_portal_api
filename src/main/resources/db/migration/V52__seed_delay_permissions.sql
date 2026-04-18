-- V52: Seed DELAY module permissions
INSERT INTO permissions (name, description, module, created_at)
VALUES
    ('DELAY_CREATE', 'Create delay log entries', 'DELAYS', NOW()),
    ('DELAY_VIEW', 'View delay logs', 'DELAYS', NOW())
ON CONFLICT (name) DO NOTHING;
