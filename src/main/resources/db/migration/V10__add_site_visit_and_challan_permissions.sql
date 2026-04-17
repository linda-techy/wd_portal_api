-- Add missing permissions for site visit and challan modules.

INSERT INTO portal_permissions (name, description) VALUES
    ('SITE_VISIT_VIEW', 'View site visit records')
ON CONFLICT (name) DO NOTHING;

INSERT INTO portal_permissions (name, description) VALUES
    ('SITE_VISIT_CREATE', 'Create site visit check-in/check-out')
ON CONFLICT (name) DO NOTHING;

INSERT INTO portal_permissions (name, description) VALUES
    ('CHALLAN_VIEW', 'View payment challans')
ON CONFLICT (name) DO NOTHING;

INSERT INTO portal_permissions (name, description) VALUES
    ('CHALLAN_CREATE', 'Generate payment challans')
ON CONFLICT (name) DO NOTHING;

INSERT INTO portal_permissions (name, description) VALUES
    ('CHALLAN_DOWNLOAD', 'Download challan PDFs')
ON CONFLICT (name) DO NOTHING;
