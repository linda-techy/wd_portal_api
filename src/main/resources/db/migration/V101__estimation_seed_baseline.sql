-- ===========================================================================
-- V101 — Estimation seed baseline
--
-- Seeds the minimum data the preview endpoint needs to compute a quote:
--   3 packages × 3 rate versions = 9 rate combinations covering NEW_BUILD
--   1 active market_index_snapshot with composite_index = 1.0000 (no fluctuation)
--   7 customisation categories with 2-4 options each (~20 options)
--   21 package_default_customisation rows (one per package × category)
--   5 addons, 3 site fees, 4 govt fees
--
-- All UUIDs hard-coded so tests can reference seed rows by literal.
-- All inserts idempotent via ON CONFLICT DO NOTHING (using natural unique cols)
-- or INSERT … WHERE NOT EXISTS for tables without natural keys.
-- Per spec §4.4.
-- ===========================================================================

-- ---------- Packages ------------------------------------------------------
INSERT INTO estimation_package (id, internal_name, marketing_name, tagline, description, display_order, is_active)
VALUES
    ('11111111-1111-1111-1111-111111111101', 'BASIC',    'Foundation Series', 'Entry-level finish', 'Builder-grade materials, structurally sound, minimal aesthetics.', 10, TRUE),
    ('11111111-1111-1111-1111-111111111102', 'STANDARD', 'Signature',         'Mid-segment branded materials', 'Asian Paints, Jaquar mid, vitrified tiles. Most popular tier.', 20, TRUE),
    ('11111111-1111-1111-1111-111111111103', 'PREMIUM',  'Luxe',              'Architect-led premium build', 'Italian marble option, modular kitchen, smart home ready, false ceiling, designer lighting.', 30, TRUE)
ON CONFLICT (internal_name) DO NOTHING;

-- ---------- Package rate versions (NEW_BUILD only in PR-1; COMMERCIAL added later) -
INSERT INTO estimation_package_rate_version (id, package_id, project_type, material_rate, labour_rate, overhead_rate, effective_from)
VALUES
    ('22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111101', 'NEW_BUILD', 1150.00, 450.00, 200.00, '2026-04-01'),
    ('22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111102', 'NEW_BUILD', 1500.00, 550.00, 300.00, '2026-04-01'),
    ('22222222-2222-2222-2222-222222222203', '11111111-1111-1111-1111-111111111103', 'NEW_BUILD', 2100.00, 700.00, 500.00, '2026-04-01')
ON CONFLICT (id) DO NOTHING;

-- ---------- Market index snapshot ----------------------------------------
INSERT INTO estimation_market_index_snapshot
    (id, snapshot_date, steel_rate, cement_rate, sand_rate, aggregate_rate, tiles_rate, electrical_rate, paints_rate,
     weights_json, composite_index, is_active)
VALUES (
    '33333333-3333-3333-3333-333333333301',
    '2026-04-30',
    62.50, 410.00, 5800.00, 1850.00, 38.00, 92.00, 285.00,
    '{"steel":"0.30","cement":"0.20","sand":"0.12","aggregate":"0.08","tiles":"0.12","electrical":"0.10","paints":"0.08"}'::jsonb,
    1.0000,
    TRUE)
ON CONFLICT (id) DO NOTHING;

-- ---------- Customisation categories -------------------------------------
INSERT INTO estimation_customisation_category (id, name, pricing_mode, display_order)
VALUES
    ('44444444-4444-4444-4444-444444444401', 'Flooring',          'PER_SQFT', 10),
    ('44444444-4444-4444-4444-444444444402', 'Wall Finish',       'PER_SQFT', 20),
    ('44444444-4444-4444-4444-444444444403', 'Doors and Windows', 'PER_UNIT', 30),
    ('44444444-4444-4444-4444-444444444404', 'Kitchen',           'PER_RFT',  40),
    ('44444444-4444-4444-4444-444444444405', 'Bathroom Fittings', 'PER_UNIT', 50),
    ('44444444-4444-4444-4444-444444444406', 'Electrical',        'PER_SQFT', 60),
    ('44444444-4444-4444-4444-444444444407', 'False Ceiling',     'PER_SQFT', 70)
ON CONFLICT (name) DO NOTHING;

-- ---------- Customisation options ----------------------------------------
INSERT INTO estimation_customisation_option (id, category_id, name, rate, display_order)
VALUES
    -- Flooring
    ('55555555-5555-5555-5555-555555555501', '44444444-4444-4444-4444-444444444401', 'Vitrified Tiles',  180.00, 10),
    ('55555555-5555-5555-5555-555555555502', '44444444-4444-4444-4444-444444444401', 'Granite',           420.00, 20),
    ('55555555-5555-5555-5555-555555555503', '44444444-4444-4444-4444-444444444401', 'Italian Marble',    950.00, 30),
    ('55555555-5555-5555-5555-555555555504', '44444444-4444-4444-4444-444444444401', 'Wooden',            720.00, 40),
    -- Wall Finish
    ('55555555-5555-5555-5555-555555555511', '44444444-4444-4444-4444-444444444402', 'Putty + Emulsion',   55.00, 10),
    ('55555555-5555-5555-5555-555555555512', '44444444-4444-4444-4444-444444444402', 'Texture',           120.00, 20),
    ('55555555-5555-5555-5555-555555555513', '44444444-4444-4444-4444-444444444402', 'Wallpaper',         220.00, 30),
    -- Doors and Windows
    ('55555555-5555-5555-5555-555555555521', '44444444-4444-4444-4444-444444444403', 'Flush Door',       6500.00, 10),
    ('55555555-5555-5555-5555-555555555522', '44444444-4444-4444-4444-444444444403', 'Teak Door',       18500.00, 20),
    ('55555555-5555-5555-5555-555555555523', '44444444-4444-4444-4444-444444444403', 'UPVC Window',      9800.00, 30),
    -- Kitchen
    ('55555555-5555-5555-5555-555555555531', '44444444-4444-4444-4444-444444444404', 'Civil Kitchen',    8500.00, 10),
    ('55555555-5555-5555-5555-555555555532', '44444444-4444-4444-4444-444444444404', 'Modular MDF',     14500.00, 20),
    ('55555555-5555-5555-5555-555555555533', '44444444-4444-4444-4444-444444444404', 'Modular Plywood', 22000.00, 30),
    ('55555555-5555-5555-5555-555555555534', '44444444-4444-4444-4444-444444444404', 'Acrylic',         32000.00, 40),
    -- Bathroom Fittings
    ('55555555-5555-5555-5555-555555555541', '44444444-4444-4444-4444-444444444405', 'Standard',        25000.00, 10),
    ('55555555-5555-5555-5555-555555555542', '44444444-4444-4444-4444-444444444405', 'Jaquar Continental', 45000.00, 20),
    ('55555555-5555-5555-5555-555555555543', '44444444-4444-4444-4444-444444444405', 'Kohler',          85000.00, 30),
    -- Electrical
    ('55555555-5555-5555-5555-555555555551', '44444444-4444-4444-4444-444444444406', 'Concealed Standard', 145.00, 10),
    ('55555555-5555-5555-5555-555555555552', '44444444-4444-4444-4444-444444444406', 'Concealed Premium',  225.00, 20),
    ('55555555-5555-5555-5555-555555555553', '44444444-4444-4444-4444-444444444406', 'Smart Home',         420.00, 30),
    -- False Ceiling
    ('55555555-5555-5555-5555-555555555561', '44444444-4444-4444-4444-444444444407', 'Gypsum Plain',       95.00, 10),
    ('55555555-5555-5555-5555-555555555562', '44444444-4444-4444-4444-444444444407', 'Gypsum Designer',   180.00, 20),
    ('55555555-5555-5555-5555-555555555563', '44444444-4444-4444-4444-444444444407', 'POP',                70.00, 30)
ON CONFLICT (category_id, name) DO NOTHING;

-- ---------- Package default customisations (one per package × category) --
-- BASIC defaults
INSERT INTO estimation_package_default_customisation (id, package_id, category_id, option_id) VALUES
    ('66666666-6666-6666-6666-666666666601', '11111111-1111-1111-1111-111111111101', '44444444-4444-4444-4444-444444444401', '55555555-5555-5555-5555-555555555501'), -- Flooring → Vitrified
    ('66666666-6666-6666-6666-666666666602', '11111111-1111-1111-1111-111111111101', '44444444-4444-4444-4444-444444444402', '55555555-5555-5555-5555-555555555511'), -- Wall → Putty
    ('66666666-6666-6666-6666-666666666603', '11111111-1111-1111-1111-111111111101', '44444444-4444-4444-4444-444444444403', '55555555-5555-5555-5555-555555555521'), -- Doors → Flush
    ('66666666-6666-6666-6666-666666666604', '11111111-1111-1111-1111-111111111101', '44444444-4444-4444-4444-444444444404', '55555555-5555-5555-5555-555555555531'), -- Kitchen → Civil
    ('66666666-6666-6666-6666-666666666605', '11111111-1111-1111-1111-111111111101', '44444444-4444-4444-4444-444444444405', '55555555-5555-5555-5555-555555555541'), -- Bath → Standard
    ('66666666-6666-6666-6666-666666666606', '11111111-1111-1111-1111-111111111101', '44444444-4444-4444-4444-444444444406', '55555555-5555-5555-5555-555555555551'), -- Elec → Standard
    ('66666666-6666-6666-6666-666666666607', '11111111-1111-1111-1111-111111111101', '44444444-4444-4444-4444-444444444407', '55555555-5555-5555-5555-555555555563')  -- Ceiling → POP
ON CONFLICT (package_id, category_id) DO NOTHING;
-- STANDARD defaults
INSERT INTO estimation_package_default_customisation (id, package_id, category_id, option_id) VALUES
    ('66666666-6666-6666-6666-666666666611', '11111111-1111-1111-1111-111111111102', '44444444-4444-4444-4444-444444444401', '55555555-5555-5555-5555-555555555502'), -- Flooring → Granite
    ('66666666-6666-6666-6666-666666666612', '11111111-1111-1111-1111-111111111102', '44444444-4444-4444-4444-444444444402', '55555555-5555-5555-5555-555555555512'), -- Wall → Texture
    ('66666666-6666-6666-6666-666666666613', '11111111-1111-1111-1111-111111111102', '44444444-4444-4444-4444-444444444403', '55555555-5555-5555-5555-555555555522'), -- Doors → Teak
    ('66666666-6666-6666-6666-666666666614', '11111111-1111-1111-1111-111111111102', '44444444-4444-4444-4444-444444444404', '55555555-5555-5555-5555-555555555532'), -- Kitchen → Modular MDF
    ('66666666-6666-6666-6666-666666666615', '11111111-1111-1111-1111-111111111102', '44444444-4444-4444-4444-444444444405', '55555555-5555-5555-5555-555555555542'), -- Bath → Jaquar
    ('66666666-6666-6666-6666-666666666616', '11111111-1111-1111-1111-111111111102', '44444444-4444-4444-4444-444444444406', '55555555-5555-5555-5555-555555555552'), -- Elec → Premium
    ('66666666-6666-6666-6666-666666666617', '11111111-1111-1111-1111-111111111102', '44444444-4444-4444-4444-444444444407', '55555555-5555-5555-5555-555555555561')  -- Ceiling → Gypsum Plain
ON CONFLICT (package_id, category_id) DO NOTHING;
-- PREMIUM defaults
INSERT INTO estimation_package_default_customisation (id, package_id, category_id, option_id) VALUES
    ('66666666-6666-6666-6666-666666666621', '11111111-1111-1111-1111-111111111103', '44444444-4444-4444-4444-444444444401', '55555555-5555-5555-5555-555555555503'), -- Flooring → Italian
    ('66666666-6666-6666-6666-666666666622', '11111111-1111-1111-1111-111111111103', '44444444-4444-4444-4444-444444444402', '55555555-5555-5555-5555-555555555513'), -- Wall → Wallpaper
    ('66666666-6666-6666-6666-666666666623', '11111111-1111-1111-1111-111111111103', '44444444-4444-4444-4444-444444444403', '55555555-5555-5555-5555-555555555523'), -- Doors → UPVC
    ('66666666-6666-6666-6666-666666666624', '11111111-1111-1111-1111-111111111103', '44444444-4444-4444-4444-444444444404', '55555555-5555-5555-5555-555555555534'), -- Kitchen → Acrylic
    ('66666666-6666-6666-6666-666666666625', '11111111-1111-1111-1111-111111111103', '44444444-4444-4444-4444-444444444405', '55555555-5555-5555-5555-555555555543'), -- Bath → Kohler
    ('66666666-6666-6666-6666-666666666626', '11111111-1111-1111-1111-111111111103', '44444444-4444-4444-4444-444444444406', '55555555-5555-5555-5555-555555555553'), -- Elec → Smart Home
    ('66666666-6666-6666-6666-666666666627', '11111111-1111-1111-1111-111111111103', '44444444-4444-4444-4444-444444444407', '55555555-5555-5555-5555-555555555562')  -- Ceiling → Designer
ON CONFLICT (package_id, category_id) DO NOTHING;

-- ---------- Add-ons ------------------------------------------------------
INSERT INTO estimation_addon (id, name, description, lump_amount, is_active) VALUES
    ('77777777-7777-7777-7777-777777777701', 'Lift (3-stop)',     'Hydraulic 3-stop residential lift', 850000.00, TRUE),
    ('77777777-7777-7777-7777-777777777702', 'Solar 3kW',         'Rooftop solar, 3kW with inverter',  180000.00, TRUE),
    ('77777777-7777-7777-7777-777777777703', 'Smart Home Basic',  'Lighting + AC zoning + voice control',150000.00, TRUE),
    ('77777777-7777-7777-7777-777777777704', 'Landscaping',       'Front yard + driveway hardscape',   320000.00, TRUE),
    ('77777777-7777-7777-7777-777777777705', 'Swimming Pool',     '4×8m fibreglass pool with filtration',1250000.00, TRUE)
ON CONFLICT (name) DO NOTHING;

-- ---------- Site fees ----------------------------------------------------
INSERT INTO estimation_site_fee (id, name, mode, lump_amount, per_sqft_rate, is_active) VALUES
    ('88888888-8888-8888-8888-888888888801', 'Difficult-soil surcharge', 'LUMP',     75000.00, NULL,  TRUE),
    ('88888888-8888-8888-8888-888888888802', 'Narrow road access',       'LUMP',     45000.00, NULL,  TRUE),
    ('88888888-8888-8888-8888-888888888803', 'Excavation',               'PER_SQFT', NULL,     12.00, TRUE)
ON CONFLICT (id) DO NOTHING;

-- ---------- Government fees ----------------------------------------------
INSERT INTO estimation_govt_fee (id, name, lump_amount, is_active) VALUES
    ('99999999-9999-9999-9999-999999999901', 'Building permit',       25000.00, TRUE),
    ('99999999-9999-9999-9999-999999999902', 'Electricity connection',15000.00, TRUE),
    ('99999999-9999-9999-9999-999999999903', 'Water connection',       8000.00, TRUE),
    ('99999999-9999-9999-9999-999999999904', 'Plinth + occupancy certificates', 12000.00, TRUE)
ON CONFLICT (name) DO NOTHING;

COMMENT ON TABLE estimation_package IS
    'Three-tier package definitions seeded by V101 — BASIC/STANDARD/PREMIUM with hard-coded UUIDs for test reference.';
