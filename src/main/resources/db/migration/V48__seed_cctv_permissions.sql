-- V48: Seed CCTV camera permissions
INSERT INTO permissions (name, description, module, created_at)
VALUES
    ('CCTV_VIEW', 'View CCTV camera configurations', 'CCTV', NOW()),
    ('CCTV_CREATE', 'Add new CCTV cameras to projects', 'CCTV', NOW()),
    ('CCTV_EDIT', 'Edit CCTV camera settings', 'CCTV', NOW()),
    ('CCTV_DELETE', 'Remove CCTV cameras', 'CCTV', NOW())
ON CONFLICT (name) DO NOTHING;
