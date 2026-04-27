-- ===========================================================================
-- V69 - Quotation Item Catalog
--
-- A master library of reusable quotation line items so leads-team can pick
-- items quickly when building quotations, and ad-hoc items used once can be
-- promoted into the catalog for future reuse.
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1. quotation_item_catalog - the master catalog
-- ---------------------------------------------------------------------------
CREATE TABLE quotation_item_catalog (
    id                       BIGSERIAL PRIMARY KEY,
    code                     VARCHAR(80)  NOT NULL UNIQUE,
    name                     VARCHAR(255) NOT NULL,
    description              TEXT,
    category                 VARCHAR(80),
    unit                     VARCHAR(40),
    default_unit_price       NUMERIC(12,2) NOT NULL DEFAULT 0,
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
CREATE INDEX idx_quotation_item_catalog_active ON quotation_item_catalog(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_quotation_item_catalog_category ON quotation_item_catalog(category) WHERE deleted_at IS NULL;
CREATE INDEX idx_quotation_item_catalog_code ON quotation_item_catalog(code) WHERE deleted_at IS NULL;
CREATE INDEX idx_quotation_item_catalog_name_trgm ON quotation_item_catalog(LOWER(name)) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 2. Add catalog_item_id FK on lead_quotation_items (NULLABLE - ad-hoc allowed)
-- ---------------------------------------------------------------------------
ALTER TABLE lead_quotation_items
    ADD COLUMN catalog_item_id BIGINT REFERENCES quotation_item_catalog(id) ON DELETE SET NULL;

CREATE INDEX idx_lqi_catalog_item ON lead_quotation_items(catalog_item_id) WHERE catalog_item_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. Permissions for catalog management.
-- ---------------------------------------------------------------------------
INSERT INTO portal_permissions (name, description) VALUES
    ('QUOTATION_CATALOG_VIEW',   'View the quotation item catalog'),
    ('QUOTATION_CATALOG_MANAGE', 'Create, edit and disable items in the quotation catalog')
ON CONFLICT (name) DO NOTHING;

-- Grant VIEW to anyone holding LEAD_VIEW (broad read access).
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT rp.role_id, p.id
FROM portal_role_permissions rp
JOIN portal_permissions src ON src.id = rp.permission_id AND src.name = 'LEAD_VIEW'
JOIN portal_permissions p   ON p.name = 'QUOTATION_CATALOG_VIEW'
WHERE NOT EXISTS (
    SELECT 1 FROM portal_role_permissions x
    WHERE x.role_id = rp.role_id AND x.permission_id = p.id
);

-- Grant MANAGE only to senior leads roles holding LEAD_EDIT (can also use the
-- "promote to catalog" action while editing a quotation).
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT rp.role_id, p.id
FROM portal_role_permissions rp
JOIN portal_permissions src ON src.id = rp.permission_id AND src.name = 'LEAD_EDIT'
JOIN portal_permissions p   ON p.name = 'QUOTATION_CATALOG_MANAGE'
WHERE NOT EXISTS (
    SELECT 1 FROM portal_role_permissions x
    WHERE x.role_id = rp.role_id AND x.permission_id = p.id
);

-- ===========================================================================
-- 4. Seed common quotation catalog items - typical Walldot pre-sale add-ons
--    that come up in most leads' quotations.  Admin can edit/disable later.
-- ===========================================================================
INSERT INTO quotation_item_catalog (code, name, description, category, unit, default_unit_price) VALUES
    -- Site preparation
    ('SITE-CLEAR',          'Site clearing and levelling',           'Removal of vegetation, rubble and levelling of plot to engineering grade.', 'Site Prep', 'sqft', 25.00),
    ('SOIL-TEST',           'Soil testing (geotechnical report)',    'Geotechnical investigation with bore holes and lab analysis.',              'Site Prep', 'lot',  18000.00),
    ('TEMP-FENCE',          'Temporary site fencing',                'GI sheet site fencing during construction, removed at handover.',           'Site Prep', 'rft',  220.00),
    -- Foundation upgrades
    ('FND-UPGRADE-COL',     'Foundation upgrade to column footing',  'Upgrade from RR to isolated column footing where soil bearing requires.',   'Foundation', 'lot',  450000.00),
    ('FND-PILE-UPGRADE',    'Pile foundation upgrade',               'Upgrade to driven pile foundation where bearing strata is deep.',          'Foundation', 'lot',  650000.00),
    -- Civil add-ons
    ('COMPOUND-WALL',       'Compound wall (boundary)',              'RCC compound wall up to 6 ft, plastered both sides, two coats paint.',     'Civil',     'rft',  1850.00),
    ('GATE-MAIN',           'Main gate (MS fabrication)',            'MS sliding/swing main gate with powder-coat finish.',                       'Civil',     'lot',  85000.00),
    ('DRIVEWAY-PAVING',     'Driveway paving (interlock)',           'Cement interlock pavers laid on graded sand bed, 60mm thick.',              'Civil',     'sqft', 95.00),
    -- Elevation extras
    ('ELEV-TEXTURE-PAINT',  'Texture paint - elevation walls',       'Elevation texture paint per moodboard, two coats.',                        'Elevation', 'sqft', 65.00),
    ('ELEV-MS-RAILING',     'MS railings (balcony)',                 'MS fabrication railings with primer + powder coat.',                       'Elevation', 'rft',  1250.00),
    ('ELEV-POLY-ROOF-CAR',  'Polycarbonate roof - car porch',        'Polycarbonate sheet roofing over the car porch area.',                     'Elevation', 'sqft', 180.00),
    -- Utilities
    ('BOREWELL',            'Borewell + casing',                     '300 ft borewell with PVC casing, motor and submersible cable.',            'Utilities', 'lot',  95000.00),
    ('SUMP-TANK',           'Sump tank (underground 5000 L)',        'RCC underground sump tank, water-proofed, with manhole cover.',            'Utilities', 'lot',  120000.00),
    ('RAINWATER-HARVEST',   'Rainwater harvesting pit',              'Recharge pit with filter media, connected to terrace downpipes.',          'Utilities', 'lot',  35000.00),
    ('SOLAR-GRID-3KW',      'On-grid solar setup (3 kW)',            '3 kW on-grid solar with inverter, panels and net-metering.',               'Utilities', 'lot',  185000.00),
    -- Interiors / finishes
    ('MODULAR-KITCHEN',     'Modular kitchen (basic)',               'L-shaped modular kitchen with 8 ft cabinets, hardware and counter.',        'Finishes',  'lot',  225000.00),
    ('WARDROBE-MASTER',     'Wardrobe (master bedroom)',             'Floor-to-ceiling wardrobe in master, internal lights and locks.',          'Finishes',  'lot',  85000.00),
    ('FALSE-CEILING',       'Gypsum false ceiling with cove lights', 'Gypsum board false ceiling with cove LED strip lighting.',                 'Finishes',  'sqft', 110.00);
