-- ===========================================================================
-- V73 - Demo Villa Project: BoQ items + sqfeet
--
-- V28 created the Demo Villa Project, an APPROVED BoqDocument (₹1 cr ex-GST),
-- and 4 PaymentStages — but never seeded any boq_categories or boq_items, and
-- never set sqfeet on the project record. The DPC builder runs against this
-- demo project but the cost rollup queries boq_items, finding none, so the
-- generated PDF showed "INR 0" everywhere (visible in
-- storage/dpc/47/dpc-rev01-20260426-135449.pdf).
--
-- This migration backfills:
--   - sqfeet = 2400 on the project (typical 4 BHK villa)
--   - 9 BoqCategories whose names match the DPC scope-template patterns
--     (e.g. "Foundation Works" matches the FOUNDATION pattern "foundation")
--   - 14 BoqItems totaling exactly 1,00,00,000 ex-GST so the BoQ snapshot
--     reconciles to the existing PaymentStage `boq_value_snapshot`.
--
-- Idempotent: skips entirely if Demo Villa already has any boq_items.
-- ===========================================================================

DO $$
DECLARE
    v_project_id    BIGINT;
    v_boq_doc_id    BIGINT;
    v_cat_found     BIGINT;
    v_cat_super     BIGINT;
    v_cat_plaster   BIGINT;
    v_cat_elec      BIGINT;
    v_cat_plumb     BIGINT;
    v_cat_floor     BIGINT;
    v_cat_joinery   BIGINT;
    v_cat_wallfin   BIGINT;
    v_cat_waterprf  BIGINT;
BEGIN
    -- Demo Villa Project may not exist (e.g. fresh prod DB) — V28 skips itself
    -- on prod when no portal user is present, so we mirror that behaviour.
    SELECT id INTO v_project_id
    FROM customer_projects
    WHERE name = 'Demo Villa Project'
    LIMIT 1;

    IF v_project_id IS NULL THEN
        RAISE NOTICE 'Demo Villa Project not present — skipping V73 (V28 also skipped)';
        RETURN;
    END IF;

    IF EXISTS (SELECT 1 FROM boq_items WHERE project_id = v_project_id) THEN
        RAISE NOTICE 'Demo Villa already has BoQ items — skipping V73';
        RETURN;
    END IF;

    -- Find the APPROVED BoQ doc V28 created — items link to this doc so the
    -- DPC rollup picks them up via DpcCostRollupService's `linked == :boq` check.
    SELECT id INTO v_boq_doc_id
    FROM boq_documents
    WHERE project_id = v_project_id AND status = 'APPROVED'
    ORDER BY id DESC
    LIMIT 1;

    IF v_boq_doc_id IS NULL THEN
        RAISE NOTICE 'No APPROVED BoQ on Demo Villa — skipping V73';
        RETURN;
    END IF;

    -- ---- Built-up area --------------------------------------------------
    UPDATE customer_projects SET sqfeet = 2400.00 WHERE id = v_project_id;

    -- ---- Categories — names chosen so case-insensitive substring match on
    --      the DPC scope-template `boq_category_patterns` finds them. ------
    INSERT INTO boq_categories (project_id, name, description, display_order, is_active,
        created_at, updated_at, version)
    VALUES
        (v_project_id, 'Foundation Works',     'Earthwork, footing, plinth.',  10, TRUE, NOW(), NOW(), 1)
    RETURNING id INTO v_cat_found;

    INSERT INTO boq_categories (project_id, name, description, display_order, is_active,
        created_at, updated_at, version)
    VALUES
        (v_project_id, 'Superstructure RCC',   'Slab, beam, column, walls.',   20, TRUE, NOW(), NOW(), 1)
    RETURNING id INTO v_cat_super;

    INSERT INTO boq_categories (project_id, name, description, display_order, is_active,
        created_at, updated_at, version)
    VALUES
        (v_project_id, 'Plastering',           'Internal + external plaster.', 30, TRUE, NOW(), NOW(), 1)
    RETURNING id INTO v_cat_plaster;

    INSERT INTO boq_categories (project_id, name, description, display_order, is_active,
        created_at, updated_at, version)
    VALUES
        (v_project_id, 'Electrical',           'Wiring, DB, switches.',        40, TRUE, NOW(), NOW(), 1)
    RETURNING id INTO v_cat_elec;

    INSERT INTO boq_categories (project_id, name, description, display_order, is_active,
        created_at, updated_at, version)
    VALUES
        (v_project_id, 'Plumbing',             'cPVC + sanitaryware.',         50, TRUE, NOW(), NOW(), 1)
    RETURNING id INTO v_cat_plumb;

    INSERT INTO boq_categories (project_id, name, description, display_order, is_active,
        created_at, updated_at, version)
    VALUES
        (v_project_id, 'Flooring',             'Vitrified tiles + skirting.',  60, TRUE, NOW(), NOW(), 1)
    RETURNING id INTO v_cat_floor;

    INSERT INTO boq_categories (project_id, name, description, display_order, is_active,
        created_at, updated_at, version)
    VALUES
        (v_project_id, 'Joinery',              'Doors, windows, frames.',      70, TRUE, NOW(), NOW(), 1)
    RETURNING id INTO v_cat_joinery;

    INSERT INTO boq_categories (project_id, name, description, display_order, is_active,
        created_at, updated_at, version)
    VALUES
        (v_project_id, 'Wall Finishes',        'Putty + emulsion paint.',      80, TRUE, NOW(), NOW(), 1)
    RETURNING id INTO v_cat_wallfin;

    INSERT INTO boq_categories (project_id, name, description, display_order, is_active,
        created_at, updated_at, version)
    VALUES
        (v_project_id, 'Waterproofing',        'Membrane + admixture.',        90, TRUE, NOW(), NOW(), 1)
    RETURNING id INTO v_cat_waterprf;

    -- ---- BASE items — total exactly 1,00,00,000.00 to reconcile with the
    --      V28 boq_documents.total_value_ex_gst snapshot. -------------------
    INSERT INTO boq_items (project_id, category_id, item_code, description, unit,
        quantity, unit_rate, total_amount,
        executed_quantity, billed_quantity, status, is_active, item_kind,
        boq_document_id, created_at, updated_at, version) VALUES

    -- Foundation (₹15,00,000)
    (v_project_id, v_cat_found,    'FND-01', 'Earthwork excavation',        'cum', 120,    1500,    180000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1),
    (v_project_id, v_cat_found,    'FND-02', 'PCC + footing concrete',      'cum', 32,     32500,  1040000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1),
    (v_project_id, v_cat_found,    'FND-03', 'Plinth filling + DPC',        'cum', 56,     5000,    280000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1),

    -- Superstructure (₹32,00,000)
    (v_project_id, v_cat_super,    'STR-01', 'RCC slab + beam concrete',    'cum', 78,     32500,  2535000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1),
    (v_project_id, v_cat_super,    'STR-02', 'Block masonry walls',         'cum', 95,     7000,    665000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1),

    -- Plastering (₹8,00,000)
    (v_project_id, v_cat_plaster,  'PLS-01', 'Internal plaster 12mm + 6mm', 'sqm', 480,    1250,    600000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1),
    (v_project_id, v_cat_plaster,  'PLS-02', 'External plaster + admix',    'sqm', 200,    1000,    200000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1),

    -- Electrical (₹10,00,000)
    (v_project_id, v_cat_elec,     'ELE-01', 'Wiring + DB + switches',      'pts', 250,    4000,   1000000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1),

    -- Plumbing (₹8,00,000)
    (v_project_id, v_cat_plumb,    'PLB-01', 'cPVC concealed + sanitary',   'set', 4,      200000,  800000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1),

    -- Flooring (₹12,00,000)
    (v_project_id, v_cat_floor,    'FLR-01', 'Vitrified tile + adhesive',   'sqm', 200,    6000,   1200000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1),

    -- Joinery (₹9,00,000)
    (v_project_id, v_cat_joinery,  'JOI-01', 'Steel main + flush + uPVC',   'set', 1,      900000,  900000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1),

    -- Wall finishes (₹4,00,000)
    (v_project_id, v_cat_wallfin,  'WAL-01', 'Putty + 2-coat emulsion',     'sqm', 800,    500,     400000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1),

    -- Waterproofing (₹2,00,000)
    (v_project_id, v_cat_waterprf, 'WTR-01', 'Acrylic membrane + admix',    'sqm', 80,     2500,    200000.00,
        0, 0, 'APPROVED', TRUE, 'BASE', v_boq_doc_id, NOW(), NOW(), 1);

    -- Sanity check: 1,80,000 + 10,40,000 + 2,80,000 + 25,35,000 + 6,65,000 +
    -- 6,00,000 + 2,00,000 + 10,00,000 + 8,00,000 + 12,00,000 + 9,00,000 +
    -- 4,00,000 + 2,00,000 = 1,00,00,000 ✓ (matches V28 boq snapshot)

    RAISE NOTICE 'V73 seeded: project=% sqfeet=2400, boq_doc=%, 13 BASE items totalling 1,00,00,000',
        v_project_id, v_boq_doc_id;
END $$;
