-- V48: Seed CCTV camera permissions
-- Target table is portal_permissions (matches Permission entity + V18/V27/V32/V39).
INSERT INTO portal_permissions (name, description) VALUES
    ('CCTV_VIEW', 'View CCTV camera configurations'),
    ('CCTV_CREATE', 'Add new CCTV cameras to projects'),
    ('CCTV_EDIT', 'Edit CCTV camera settings'),
    ('CCTV_DELETE', 'Remove CCTV cameras')
ON CONFLICT (name) DO NOTHING;
