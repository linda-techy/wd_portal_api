-- ===========================================================================
-- V68 - Detailed Project Costing (DPC) Builder
--
-- Creates five new tables and seeds the company-level scope-template library.
-- DPC is a render layer over existing BoQ + PaymentSchedule data, so cost
-- numbers are NOT stored here - only choices, contact overrides, and the
-- descriptive content library.
-- ===========================================================================

-- ---------------------------------------------------------------------------
-- 1. dpc_scope_template - the content library (admin-managed, ~10 rows)
-- ---------------------------------------------------------------------------
CREATE TABLE dpc_scope_template (
    id                       BIGSERIAL PRIMARY KEY,
    code                     VARCHAR(50)  NOT NULL UNIQUE,
    display_order            INT          NOT NULL,
    title                    VARCHAR(255) NOT NULL,
    subtitle                 TEXT,
    intro_paragraph          TEXT,
    what_you_get             JSONB        NOT NULL DEFAULT '[]'::jsonb,
    quality_procedures       JSONB        NOT NULL DEFAULT '[]'::jsonb,
    documents_you_get        JSONB        NOT NULL DEFAULT '[]'::jsonb,
    boq_category_patterns    JSONB        NOT NULL DEFAULT '[]'::jsonb,
    default_brands           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE,
    -- BaseEntity audit columns
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by_user_id       BIGINT,
    updated_by_user_id       BIGINT,
    deleted_at               TIMESTAMP,
    deleted_by_user_id       BIGINT,
    version                  BIGINT       NOT NULL DEFAULT 1
);
CREATE INDEX idx_dpc_scope_template_code ON dpc_scope_template(code) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 2. dpc_scope_option - "options considered" cards per scope
-- ---------------------------------------------------------------------------
CREATE TABLE dpc_scope_option (
    id                       BIGSERIAL PRIMARY KEY,
    scope_template_id        BIGINT       NOT NULL REFERENCES dpc_scope_template(id) ON DELETE CASCADE,
    code                     VARCHAR(50)  NOT NULL,
    display_name             VARCHAR(100) NOT NULL,
    image_path               VARCHAR(500),
    display_order            INT          NOT NULL DEFAULT 0,
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by_user_id       BIGINT,
    updated_by_user_id       BIGINT,
    deleted_at               TIMESTAMP,
    deleted_by_user_id       BIGINT,
    version                  BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT uq_dpc_scope_option_template_code UNIQUE (scope_template_id, code)
);
CREATE INDEX idx_dpc_scope_option_template ON dpc_scope_option(scope_template_id) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 3. dpc_document - the per-project DPC instance
-- ---------------------------------------------------------------------------
CREATE TABLE dpc_document (
    id                          BIGSERIAL PRIMARY KEY,
    project_id                  BIGINT       NOT NULL REFERENCES customer_projects(id),
    boq_document_id             BIGINT       NOT NULL REFERENCES boq_documents(id),
    revision_number             INT          NOT NULL DEFAULT 1,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    title_override              VARCHAR(255),
    subtitle_override           TEXT,
    client_signatory_name       VARCHAR(255),
    walldot_signatory_name      VARCHAR(255),
    project_engineer_user_id    BIGINT,
    branch_manager_name         VARCHAR(255),
    branch_manager_phone        VARCHAR(30),
    crm_team_name               VARCHAR(255),
    crm_team_phone              VARCHAR(30),
    issued_at                   TIMESTAMP,
    issued_by_user_id           BIGINT,
    issued_pdf_document_id      BIGINT,
    -- BaseEntity
    created_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by_user_id          BIGINT,
    updated_by_user_id          BIGINT,
    deleted_at                  TIMESTAMP,
    deleted_by_user_id          BIGINT,
    version                     BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT chk_dpc_status CHECK (status IN ('DRAFT', 'ISSUED'))
);
CREATE INDEX idx_dpc_document_project ON dpc_document(project_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_dpc_document_boq ON dpc_document(boq_document_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_dpc_document_revision ON dpc_document(project_id, revision_number) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 4. dpc_document_scope - one row per scope per DPC instance
-- ---------------------------------------------------------------------------
CREATE TABLE dpc_document_scope (
    id                          BIGSERIAL PRIMARY KEY,
    dpc_document_id             BIGINT       NOT NULL REFERENCES dpc_document(id) ON DELETE CASCADE,
    scope_template_id           BIGINT       NOT NULL REFERENCES dpc_scope_template(id),
    selected_option_id          BIGINT REFERENCES dpc_scope_option(id),
    selected_option_rationale   TEXT,
    brands_override             JSONB,
    what_you_get_override       JSONB,
    included_in_pdf             BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order               INT          NOT NULL,
    -- BaseEntity
    created_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by_user_id          BIGINT,
    updated_by_user_id          BIGINT,
    deleted_at                  TIMESTAMP,
    deleted_by_user_id          BIGINT,
    version                     BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT uq_dpc_document_scope UNIQUE (dpc_document_id, scope_template_id)
);
CREATE INDEX idx_dpc_document_scope_doc ON dpc_document_scope(dpc_document_id) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 5. dpc_customization_line - itemized variance rows for the customizations page
-- ---------------------------------------------------------------------------
CREATE TABLE dpc_customization_line (
    id                          BIGSERIAL PRIMARY KEY,
    dpc_document_id             BIGINT       NOT NULL REFERENCES dpc_document(id) ON DELETE CASCADE,
    display_order               INT          NOT NULL DEFAULT 0,
    title                       VARCHAR(255) NOT NULL,
    description                 TEXT,
    amount                      NUMERIC(18,6) NOT NULL DEFAULT 0,
    source                      VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',
    boq_item_id                 BIGINT,
    -- BaseEntity
    created_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by_user_id          BIGINT,
    updated_by_user_id          BIGINT,
    deleted_at                  TIMESTAMP,
    deleted_by_user_id          BIGINT,
    version                     BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT chk_dpc_customization_source CHECK (source IN ('AUTO_FROM_BOQ_ADDON', 'MANUAL'))
);
CREATE INDEX idx_dpc_customization_line_doc ON dpc_customization_line(dpc_document_id) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- 6. Permissions - DPC capabilities mirror the BoQ permission shape.
--    Tables in this codebase are portal_permissions / portal_role_permissions
--    (not bare permissions / role_permissions).
-- ---------------------------------------------------------------------------
INSERT INTO portal_permissions (name, description) VALUES
    ('DPC_VIEW',            'View Detailed Project Costing documents'),
    ('DPC_CREATE',          'Create DPC documents from approved BoQ'),
    ('DPC_EDIT',            'Edit DRAFT DPC documents'),
    ('DPC_ISSUE',           'Issue (lock and persist PDF) a DPC document'),
    ('DPC_TEMPLATE_MANAGE', 'Manage the DPC scope-template content library')
ON CONFLICT (name) DO NOTHING;

-- Grant DPC_VIEW + DPC_CREATE + DPC_EDIT + DPC_ISSUE + DPC_TEMPLATE_MANAGE to roles
-- that already hold BOQ_APPROVE (portal staff senior enough to approve BoQ
-- should also be able to issue DPC).
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT rp.role_id, p.id
FROM portal_role_permissions rp
JOIN portal_permissions src ON src.id = rp.permission_id AND src.name = 'BOQ_APPROVE'
JOIN portal_permissions p   ON p.name IN ('DPC_VIEW', 'DPC_CREATE', 'DPC_EDIT', 'DPC_ISSUE', 'DPC_TEMPLATE_MANAGE')
WHERE NOT EXISTS (
    SELECT 1 FROM portal_role_permissions x
    WHERE x.role_id = rp.role_id AND x.permission_id = p.id
);

-- Grant DPC_VIEW to roles with BOQ_VIEW (broader read).
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT rp.role_id, p.id
FROM portal_role_permissions rp
JOIN portal_permissions src ON src.id = rp.permission_id AND src.name = 'BOQ_VIEW'
JOIN portal_permissions p   ON p.name = 'DPC_VIEW'
WHERE NOT EXISTS (
    SELECT 1 FROM portal_role_permissions x
    WHERE x.role_id = rp.role_id AND x.permission_id = p.id
);

-- ===========================================================================
-- 7. Seed the 10 standard scope templates.
--    Content lifted from the reference DPC document (Walldot handover format).
-- ===========================================================================

INSERT INTO dpc_scope_template (code, display_order, title, subtitle, intro_paragraph,
    what_you_get, quality_procedures, documents_you_get, boq_category_patterns, default_brands)
VALUES
('FOUNDATION', 1,
 'Foundation.',
 'The base your home stands on for the next century.',
 'The base your home stands on for the next century. Engineered for your soil, treated for water and termites, signed off at every cure.',
 '["Engineered for your specific soil report and bearing capacity.","Damp-proofing and termite treatment integrated at slab level.","Adheres to IS 456 for plain and reinforced concrete.","Cube tests at 7-day and 28-day intervals on every pour.","Anti-corrosion bar bending and cover blocks per drawings."]'::jsonb,
 '["Verify cover blocks, rebar spacing and binding before every pour.","Damp-proof course laid at plinth before backfilling.","Plinth filling consolidated in 200mm layers with compaction."]'::jsonb,
 '["Soil test report","Material test certificates","Structural drawings","Cube test results","Quality checklist signoff"]'::jsonb,
 '["foundation","footing","pcc","plinth","earthwork","excavation"]'::jsonb,
 '{"Cement":"Ambuja / ACC","Steel":"Vizag / Kaliyathu"}'::jsonb),

('SUPERSTRUCTURE', 2,
 'Superstructure.',
 'Walls, slabs, beams - built to take load and last decades.',
 'Walls, slabs, beams. Built to take load, resist water, and last decades - with admixtures in every mix and cube tests on every pour.',
 '["Structural system optimised between load-bearing and framed.","Waterproofing admixture in slab, beam and external plaster.","Block compressive strength tested per batch at site.","Daily progress tracking via the Walldot site app - visible to you.","Curing for 14 days on slabs with continuous water ponding."]'::jsonb,
 '["Cube test per concreting day, retained for 28-day verification.","Levels and verticality checked every course - tolerance plus or minus 3mm/m.","Block joints staggered, no continuous vertical seam."]'::jsonb,
 '["Concrete cube test reports","Block compressive strength reports","TMT bar test report","Pour register","Curing log"]'::jsonb,
 '["superstructure","masonry","block","brick","concrete","slab","beam","column","rcc","wall"]'::jsonb,
 '{"Cement":"Ambuja / ACC","Steel":"Vizag / Kaliyathu","Admix":"Dr. Fixit"}'::jsonb),

('PLASTERING', 3,
 'Plastering.',
 'The skin under your paint.',
 'The skin under your paint. Mixed by ratio, cured for strength, finished smooth - with chicken mesh at every junction to stop hairline cracks.',
 '["Waterproofing admixture in external and terrace plaster.","Chicken mesh at concrete-masonry junctions to prevent cracks.","Watercutting groove on every sunshade underside.","Mortar mixed strictly to 1:5 in mechanical mixer, never by hand.","Continuous wet curing for 7 days post-application."]'::jsonb,
 '["Surface dampened thoroughly before application - no dry-on-dry.","Two-coat system - base 12mm rough, finish 6mm smooth.","Plumb and level checked every 1m squared with straight edge."]'::jsonb,
 '["Mix ratio register","Curing log","Quality checklist signoff","Material delivery notes"]'::jsonb,
 '["plaster","plastering","stucco","render"]'::jsonb,
 '{"Cement":"Ambuja / ACC","Waterproofing":"Dr. Fixit"}'::jsonb),

('ELECTRICAL', 4,
 'Electrical.',
 'Wires, switches, and circuits planned for the way you live.',
 'Wires, switches, and circuits planned for the way you live - with car charging and on-grid solar provisions baked in from day one.',
 '["Branded copper wiring with colour-coded phase, neutral, earth.","Car charging point and on-grid solar provision pre-wired.","Master switches in every bedroom for one-touch shutdown.","DB sized from actual load calculations, not thumb rules.","Modular switch plates with self-closing 16A sockets."]'::jsonb,
 '["Conduits run only vertically and horizontally - no diagonals.","Earthing pit installed and continuity tested before commissioning.","Megger insulation test at 500V on every circuit before energising."]'::jsonb,
 '["MEP layout drawings","DB layout & load schedule","Earthing test certificate","Megger test report","Brand warranties"]'::jsonb,
 '["electrical","wiring","switch","light","mep electrical"]'::jsonb,
 '{"Wires":"V-Guard / Polycab","Switches":"L&T Engem"}'::jsonb),

('PLUMBING', 5,
 'Plumbing.',
 'Concealed cPVC, branded sanitaryware, pressure-tested.',
 'Concealed cPVC for hot and cold lines, branded sanitaryware, and pressure-tested before any wall closes up.',
 '["All concealed lines in cPVC - heat-resistant, no rust, 50-year life.","Geyser-ready provision in every toilet, dishwasher line in kitchen.","Pressure test at 1.5x working pressure for 24 hours before walls close.","Branded sanitaryware as per spec sheet.","Sloped wet-area floors verified with water-flow test before tiling."]'::jsonb,
 '["Pipes enter walls only at right angles - no diagonal embedment.","Pressure test logged and signed before plastering of toilet walls.","Each fixture flow-rate verified against spec sheet at handover."]'::jsonb,
 '["Plumbing layout drawing","Sanitary fixture schedule","Pressure test certificate","Brand warranties"]'::jsonb,
 '["plumbing","sanitary","pipe","drain","mep plumbing"]'::jsonb,
 '{"Pipes":"Ashirvad / Astral","Fixtures":"Cera / Jaquar"}'::jsonb),

('FLOORING', 6,
 'Flooring.',
 'Vitrified tiles, ceramic in toilets, all laid with epoxy fillers.',
 'Vitrified tiles across living and bedroom areas, ceramic in toilets, all laid with epoxy fillers and adhesive - no dry mortar shortcuts.',
 '["Vitrified tiles in living, dining, kitchen, bedrooms - anti-skid in toilets.","Tile material allowance covers epoxy filler and adhesive.","Wedges and clip spacers used to prevent lippage between tiles.","Hollow-tap test on every tile after curing - any sound triggers re-lay.","Skirting tile course matched to floor lot for colour consistency."]'::jsonb,
 '["Substrate level checked with spirit level before any tile laid.","Mortar mixed in measured proportions, never eyeballed.","Protection sheet covers all laid tiles until final handover."]'::jsonb,
 '["Flooring layout drawing","Tile lot certificates","Hollow-tap test register","Brand warranties"]'::jsonb,
 '["flooring","tile","tiles","floor","skirting"]'::jsonb,
 '{"Tiles":"Somany / Kajaria","Adhesive":"Roff / Laticrete"}'::jsonb),

('JOINERY', 7,
 'Joinery.',
 'Steel main door, FRP for wet areas, flush internal doors.',
 'Steel main door for security, FRP for wet areas, flush internal doors throughout - and uPVC windows for energy efficiency without the maintenance.',
 '["Steel main door with multi-point locking, fire-rated, termite-proof.","FRP doors in toilets - fully water-resistant, no warping or rot.","Flush doors in bedrooms with hardwood lipping for screw retention.","uPVC sliding window included as design customisation.","Plastering completed before window fixing - no cement stains."]'::jsonb,
 '["Plumb, level and diagonal checked on every frame before fixing.","Hinge positions and lock alignment verified at handover.","Frame-to-wall gaps closed with PU foam, never with mortar."]'::jsonb,
 '["Joinery specification drawings","Frame schedule","Brand warranties","Hardware fitting register"]'::jsonb,
 '["joinery","door","window","frame","upvc","wood"]'::jsonb,
 '{"Steel Door":"Cuirass / TATA Pravesh","uPVC":"A1 / Veka"}'::jsonb),

('WALL_FINISHES', 8,
 'Wall finishes.',
 'Putty, paint, weather-shield exterior.',
 'Two coats interior putty across all walls and ceilings. Two coats Asian Royale Emulsion in your selected palette. Asian Ace Emulsion exterior, weather-shield grade.',
 '["Two coats interior putty across all walls and ceilings.","Two coats Asian Royale Emulsion in your selected palette.","Asian Ace Emulsion exterior, weather-shield grade.","Final coat applied after all electrical and sanitary fixtures fit.","Texture or wall cladding available as design upgrade."]'::jsonb,
 '["Two coats putty fully cured before paint application.","Cutting-in done by brush, fields done by roller for even finish.","Touch-up after handover snag list closes."]'::jsonb,
 '["Paint shade card","Brand warranties","Coverage register"]'::jsonb,
 '["wall finish","paint","painting","emulsion","putty"]'::jsonb,
 '{"Paint":"Asian Paints","Putty":"JK Wall Putty"}'::jsonb),

('WATERPROOFING', 9,
 'Waterproofing.',
 'Acrylic membrane in toilets, admixture in slab and beam.',
 'Acrylic membrane on all toilet floors and walls up to 2 feet height. Admixture in slab, beam, and external plaster. Acrylic chosen for flexibility - moves with thermal expansion.',
 '["Acrylic membrane on all toilet floors and walls up to 2 feet height.","Admixture (Dr. Fixit) in slab, beam, and external plaster.","Terrace optionally upgradable to insulated waterproof membrane.","Acrylic chosen for flexibility - moves with thermal expansion.","Brand warranty passed through to you on handover."]'::jsonb,
 '["Surface cleaned and primed before membrane application.","Two coats applied in alternate directions for full coverage.","Pond test on toilet floors before tiling."]'::jsonb,
 '["Waterproofing application register","Brand warranty card","Pond test signoff"]'::jsonb,
 '["waterproof","waterproofing","membrane","damp"]'::jsonb,
 '{"Waterproofing":"Dr. Fixit (Pidilite)"}'::jsonb),

('ELEVATION', 10,
 'Elevation.',
 'Front elevation civil work and finishes.',
 'Extended slabs, sill slabs, MS fabrication, and texture paint per the approved elevation moodboard. These items sit outside the standard package and reconcile to the customizations page.',
 '["Extended slabs and sill slabs for elevation form.","MS fabrication: railings and balcony work.","Polycarbonate sheet roofing where specified.","Texture paint on selected elevation walls per moodboard.","All elevation civil signed off against architects drawings."]'::jsonb,
 '["Levels and elevation profile checked against architects drawing.","MS fabrication welds inspected and rust-treated before paint.","Texture sample approved by client before full application."]'::jsonb,
 '["Elevation drawings","Texture sample approval","MS fabrication shop drawings"]'::jsonb,
 '["elevation","facade","fabrication","ms work","texture"]'::jsonb,
 '{}'::jsonb);

-- ===========================================================================
-- 8. Seed scope options - "options considered" cards.
-- ===========================================================================

INSERT INTO dpc_scope_option (scope_template_id, code, display_name, image_path, display_order)
SELECT t.id, v.code, v.display_name, v.image_path, v.display_order
FROM dpc_scope_template t
JOIN (VALUES
    ('FOUNDATION',     'RANDOM_RUBBLE',   'Random Rubble',     '/dpc-assets/options/foundation-rr.png',         1),
    ('FOUNDATION',     'RAFT',            'Raft',              '/dpc-assets/options/foundation-raft.png',       2),
    ('FOUNDATION',     'COLUMN_FOOTING',  'Column Footing',    '/dpc-assets/options/foundation-column.png',     3),
    ('FOUNDATION',     'PILE',            'Pile',              '/dpc-assets/options/foundation-pile.png',       4),
    ('SUPERSTRUCTURE', 'SOLID_BLOCK',     'Solid Block',       '/dpc-assets/options/super-solid.png',           1),
    ('SUPERSTRUCTURE', 'AAC_BLOCK',       'AAC Block',         '/dpc-assets/options/super-aac.png',             2),
    ('SUPERSTRUCTURE', 'RED_BRICK',       'Red Brick',         '/dpc-assets/options/super-brick.png',           3),
    ('SUPERSTRUCTURE', 'POROTHERM',       'Porotherm',         '/dpc-assets/options/super-porotherm.png',       4),
    ('PLASTERING',     'CEMENT_PLASTER',  'Cement Plaster',    '/dpc-assets/options/plaster-cement.png',        1),
    ('PLASTERING',     'GYPSUM_PLASTER',  'Gypsum Plaster',    '/dpc-assets/options/plaster-gypsum.png',        2),
    ('PLASTERING',     'MUD_PLASTER',     'Mud Plaster',       '/dpc-assets/options/plaster-mud.png',           3),
    ('PLASTERING',     'STONE_CLADDING',  'Stone Cladding',    '/dpc-assets/options/plaster-stone.png',         4),
    ('ELECTRICAL',     'STANDARD_WHITE',  'Standard White',    '/dpc-assets/options/elec-white.png',            1),
    ('ELECTRICAL',     'MODULAR_GRID',    'Modular Grid',      '/dpc-assets/options/elec-grid.png',             2),
    ('ELECTRICAL',     'PREMIUM_MATTE',   'Premium Matte',     '/dpc-assets/options/elec-matte.png',            3),
    ('ELECTRICAL',     'TOUCH_SMART',     'Touch Smart',       '/dpc-assets/options/elec-smart.png',            4),
    ('PLUMBING',       'WC_CISTERN',      'WC + Cistern',      '/dpc-assets/options/plumb-wc.png',              1),
    ('PLUMBING',       'PEDESTAL_BASIN',  'Pedestal Basin',    '/dpc-assets/options/plumb-basin.png',           2),
    ('PLUMBING',       'CHROME_FAUCET',   'Chrome Faucet',     '/dpc-assets/options/plumb-faucet.png',          3),
    ('PLUMBING',       'CPVC_CONCEALED',  'cPVC Concealed',    '/dpc-assets/options/plumb-cpvc.png',            4),
    ('FLOORING',       'VITRIFIED_TILE',  'Vitrified Tile',    '/dpc-assets/options/floor-vitrified.png',       1),
    ('FLOORING',       'MARBLE',          'Marble',            '/dpc-assets/options/floor-marble.png',          2),
    ('FLOORING',       'WOODEN',          'Wooden',            '/dpc-assets/options/floor-wood.png',            3),
    ('FLOORING',       'RED_OXIDE',       'Red Oxide',         '/dpc-assets/options/floor-redox.png',           4),
    ('JOINERY',        'STEEL_MAIN',      'Steel Main',        '/dpc-assets/options/joinery-steel.png',         1),
    ('JOINERY',        'FRP_TOILET',      'FRP Toilet',        '/dpc-assets/options/joinery-frp.png',           2),
    ('JOINERY',        'FLUSH_INTERNAL',  'Flush Internal',    '/dpc-assets/options/joinery-flush.png',         3),
    ('JOINERY',        'WOODEN_PANEL',    'Wooden Panel',      '/dpc-assets/options/joinery-wood.png',          4),
    ('WALL_FINISHES',  'EMULSION',        'Emulsion',          '/dpc-assets/options/wall-emulsion.png',         1),
    ('WALL_FINISHES',  'TEXTURE',         'Texture',           '/dpc-assets/options/wall-texture.png',          2),
    ('WALL_FINISHES',  'CLADDING',        'Cladding',          '/dpc-assets/options/wall-cladding.png',         3),
    ('WATERPROOFING',  'ACRYLIC_WP',      'Acrylic WP',        '/dpc-assets/options/wp-acrylic.png',            1),
    ('WATERPROOFING',  'CEMENTITIOUS',    'Cementitious',      '/dpc-assets/options/wp-cement.png',             2),
    ('WATERPROOFING',  'BITUMINOUS',      'Bituminous',        '/dpc-assets/options/wp-bitumen.png',            3)
) AS v(scope_code, code, display_name, image_path, display_order)
ON v.scope_code = t.code;
