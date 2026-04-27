-- ===========================================================================
-- V70 - DPC Customization Catalog
--
-- A master library of reusable DPC customization line items (lump-sum amounts)
-- so the DPC builder can pick common customizations quickly, and ad-hoc lines
-- typed once can be promoted into the catalog for future reuse.
--
-- Mirrors V69 (quotation_item_catalog) almost exactly; the only modeling
-- difference is that customization lines are LUMP-SUM amounts (no quantity),
-- so the catalog row stores `default_amount` rather than `default_unit_price`.
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1. dpc_customization_catalog - the master catalog
-- ---------------------------------------------------------------------------
CREATE TABLE dpc_customization_catalog (
    id                       BIGSERIAL PRIMARY KEY,
    code                     VARCHAR(80)  NOT NULL UNIQUE,
    name                     VARCHAR(255) NOT NULL,
    description              TEXT,
    category                 VARCHAR(80),
    unit                     VARCHAR(40),
    default_amount           NUMERIC(18,6) NOT NULL DEFAULT 0,
    times_used               INTEGER       NOT NULL DEFAULT 0,
    is_active                BOOLEAN       NOT NULL DEFAULT TRUE,
    -- BaseEntity audit columns
    created_at               TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by_user_id       BIGINT,
    updated_by_user_id       BIGINT,
    deleted_at               TIMESTAMP,
    deleted_by_user_id       BIGINT,
    version                  BIGINT        NOT NULL DEFAULT 1
);
CREATE INDEX idx_dpc_customization_catalog_active   ON dpc_customization_catalog(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_dpc_customization_catalog_category ON dpc_customization_catalog(category)  WHERE deleted_at IS NULL;
CREATE INDEX idx_dpc_customization_catalog_code     ON dpc_customization_catalog(code)      WHERE deleted_at IS NULL;
CREATE INDEX idx_dpc_customization_catalog_name_trgm ON dpc_customization_catalog(LOWER(name)) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 2. Add customization_catalog_id FK on dpc_customization_line (NULLABLE - ad-hoc allowed)
-- ---------------------------------------------------------------------------
ALTER TABLE dpc_customization_line
    ADD COLUMN customization_catalog_id BIGINT REFERENCES dpc_customization_catalog(id) ON DELETE SET NULL;

CREATE INDEX idx_dpc_cust_line_catalog ON dpc_customization_line(customization_catalog_id) WHERE customization_catalog_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. Permissions for catalog management.
-- ---------------------------------------------------------------------------
INSERT INTO portal_permissions (name, description) VALUES
    ('DPC_CUSTOMIZATION_CATALOG_VIEW',   'View the DPC customization catalog'),
    ('DPC_CUSTOMIZATION_CATALOG_MANAGE', 'Create, edit and disable items in the DPC customization catalog')
ON CONFLICT (name) DO NOTHING;

-- Grant VIEW to anyone holding DPC_VIEW (broad read access for the picker).
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT rp.role_id, p.id
FROM portal_role_permissions rp
JOIN portal_permissions src ON src.id = rp.permission_id AND src.name = 'DPC_VIEW'
JOIN portal_permissions p   ON p.name = 'DPC_CUSTOMIZATION_CATALOG_VIEW'
WHERE NOT EXISTS (
    SELECT 1 FROM portal_role_permissions x
    WHERE x.role_id = rp.role_id AND x.permission_id = p.id
);

-- Grant MANAGE only to roles holding DPC_TEMPLATE_MANAGE (senior team — also
-- the authority that runs "promote to catalog" alongside DPC_EDIT).
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT rp.role_id, p.id
FROM portal_role_permissions rp
JOIN portal_permissions src ON src.id = rp.permission_id AND src.name = 'DPC_TEMPLATE_MANAGE'
JOIN portal_permissions p   ON p.name = 'DPC_CUSTOMIZATION_CATALOG_MANAGE'
WHERE NOT EXISTS (
    SELECT 1 FROM portal_role_permissions x
    WHERE x.role_id = rp.role_id AND x.permission_id = p.id
);

-- ===========================================================================
-- 4. Seed common DPC customization rows derived from real Walldot scope.
--    Admin can edit/disable later via the catalog admin screen.
-- ===========================================================================
INSERT INTO dpc_customization_catalog (code, name, description, category, unit, default_amount) VALUES
    ('FOUNDATION-RR-TO-COL',  'Foundation upgrade — RR to column footing',     'Switch from random rubble to isolated column footing per soil report.', 'Foundation', 'lot',  429203),
    ('FRONT-ELEVATION-CIVIL', 'Front elevation civil work',                    'Extended slabs, sill slabs, additional steel for elevation form.',      'Elevation',  'lot',  300822),
    ('MS-BALCONY-RAILING',    'MS balcony railing fabrication',                'Steel railings + fabrication work at first-floor balcony.',             'Elevation',  'rft',  1250),
    ('CAR-PORCH-POLY',        'Polycarbonate car porch roof',                  'Polycarbonate sheet roofing over the car porch area.',                  'Elevation',  'sqft', 180),
    ('FF-BALCONY-POLY',       'Polycarbonate first-floor balcony roof',        'Polycarbonate sheet over first-floor balcony.',                         'Elevation',  'lot',  44435),
    ('UTILITY-POLY',          'Polycarbonate utility area roof',               'Polycarbonate sheet over kitchen utility yard.',                        'Elevation',  'lot',  44695),
    ('TEXTURE-PAINT-ELEV',    'Texture paint on elevation walls',              'Texture paint per moodboard on selected elevation walls.',              'Elevation',  'sqft', 65),
    ('UPVC-SLIDING-DOOR',     'uPVC sliding door (white)',                     'White finish uPVC sliding door for living-balcony opening.',            'Joinery',    'nos',  61687),
    ('POWDER-ROOM-EXTRA',     'Powder room sanitary (5th toilet)',             'Additional sanitaryware for the fifth toilet (powder room).',           'Plumbing',   'lot',  14786),
    ('MANGALORE-ROOF-ELEV',   'Mangalore tile sloped roof',                    'Sloped roof on first floor with Mangalore tiles + truss.',              'Elevation',  'sqft', 420);
