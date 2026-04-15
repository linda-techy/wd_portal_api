-- =============================================================================
-- V28: Demo seed data for VO / Stage / Deduction / Final Account modules
--
-- Creates (if no data already exists for the demo project):
--   1 demo CustomerProject  — "Demo Villa Project" (skipped if any project exists)
--   1 APPROVED BoqDocument  — required for PaymentStage FK
--   4 PaymentStages         — Foundation, Structure, Finishing, Handover
--   2 Addition ChangeOrders — one MATERIAL_HEAVY, one LABOUR_HEAVY (APPROVED)
--   1 Omission ChangeOrder  — SCOPE_REDUCTION (APPROVED, deduction raised)
--   1 Revision ChangeOrder  — DRAFT (revises the first addition CO)
--   1 DeductionRegister     — linked to the omission CO (PENDING)
--   co_payment_schedules    — auto-created for the 2 approved addition COs
-- =============================================================================

DO $$
DECLARE
    v_project_id        BIGINT;
    v_boq_doc_id        BIGINT;
    v_stage1_id         BIGINT;
    v_stage2_id         BIGINT;
    v_stage3_id         BIGINT;
    v_stage4_id         BIGINT;
    v_co_add1_id        BIGINT;
    v_co_add2_id        BIGINT;
    v_co_omit_id        BIGINT;
    v_co_rev_id         BIGINT;
    v_portal_user_id    BIGINT;
BEGIN
    -- Only seed if the demo project does not already exist
    IF EXISTS (SELECT 1 FROM customer_projects WHERE name = 'Demo Villa Project') THEN
        RAISE NOTICE 'Demo seed data already present — skipping V28';
        RETURN;
    END IF;

    -- ---- Resolve a portal user for FK references ----------------------------
    SELECT id INTO v_portal_user_id FROM portal_users LIMIT 1;
    IF v_portal_user_id IS NULL THEN
        RAISE NOTICE 'No portal user found — skipping V28 seed';
        RETURN;
    END IF;

    -- ---- Project ------------------------------------------------------------
    INSERT INTO customer_projects (
        project_uuid, name, location, state, district, start_date, end_date,
        project_phase, overall_progress, created_at, updated_at, version
    ) VALUES (
        gen_random_uuid(), 'Demo Villa Project', 'Mumbai, Maharashtra',
        'Maharashtra', 'Mumbai City',
        '2024-01-01', '2025-12-31',
        'CONSTRUCTION', 45.0,
        NOW(), NOW(), 1
    ) RETURNING id INTO v_project_id;

    -- ---- BOQ Document (APPROVED) needed as FK for PaymentStage -------------
    INSERT INTO boq_documents (
        project_id, status,
        total_value_ex_gst, gst_rate, total_gst_amount, total_value_incl_gst,
        revision_number, approved_at, created_at, updated_at, version
    ) VALUES (
        v_project_id, 'APPROVED',
        10000000.00, 0.18, 1800000.00, 11800000.00,
        1, NOW(), NOW(), NOW(), 1
    ) RETURNING id INTO v_boq_doc_id;

    -- ---- Payment Stages -----------------------------------------------------
    INSERT INTO payment_stages (
        project_id, boq_document_id, stage_number, stage_name,
        boq_value_snapshot, stage_percentage,
        stage_amount_ex_gst, gst_rate, gst_amount, stage_amount_incl_gst,
        applied_credit_amount, net_payable_amount, paid_amount,
        retention_pct, retention_held,
        status, due_date, created_at, updated_at, version
    ) VALUES
    (v_project_id, v_boq_doc_id, 1, 'Foundation & Plinth',
        10000000.00, 0.2500,
        2500000.00, 0.18, 450000.00, 2950000.00,
        0, 2950000.00, 2950000.00,
        0.05, 125000.00,
        'PAID', '2024-03-31', NOW(), NOW(), 1)
    RETURNING id INTO v_stage1_id;

    INSERT INTO payment_stages (
        project_id, boq_document_id, stage_number, stage_name,
        boq_value_snapshot, stage_percentage,
        stage_amount_ex_gst, gst_rate, gst_amount, stage_amount_incl_gst,
        applied_credit_amount, net_payable_amount, paid_amount,
        retention_pct, retention_held,
        status, due_date, certified_by, certified_at, created_at, updated_at, version
    ) VALUES
    (v_project_id, v_boq_doc_id, 2, 'Structural Frame',
        10000000.00, 0.2500,
        2500000.00, 0.18, 450000.00, 2950000.00,
        0, 2950000.00, 0,
        0.05, 125000.00,
        'INVOICED', '2024-07-31', 'Rajesh Kumar (PM)', NOW() - INTERVAL '5 days',
        NOW(), NOW(), 1)
    RETURNING id INTO v_stage2_id;

    INSERT INTO payment_stages (
        project_id, boq_document_id, stage_number, stage_name,
        boq_value_snapshot, stage_percentage,
        stage_amount_ex_gst, gst_rate, gst_amount, stage_amount_incl_gst,
        applied_credit_amount, net_payable_amount, paid_amount,
        retention_pct, retention_held,
        status, due_date, created_at, updated_at, version
    ) VALUES
    (v_project_id, v_boq_doc_id, 3, 'Finishing Works',
        10000000.00, 0.2500,
        2500000.00, 0.18, 450000.00, 2950000.00,
        0, 2950000.00, 0,
        0.05, 125000.00,
        'UPCOMING', '2025-01-31', NOW(), NOW(), 1)
    RETURNING id INTO v_stage3_id;

    INSERT INTO payment_stages (
        project_id, boq_document_id, stage_number, stage_name,
        boq_value_snapshot, stage_percentage,
        stage_amount_ex_gst, gst_rate, gst_amount, stage_amount_incl_gst,
        applied_credit_amount, net_payable_amount, paid_amount,
        retention_pct, retention_held,
        status, due_date, created_at, updated_at, version
    ) VALUES
    (v_project_id, v_boq_doc_id, 4, 'Handover & Completion',
        10000000.00, 0.2500,
        2500000.00, 0.18, 450000.00, 2950000.00,
        0, 2950000.00, 0,
        0.05, 125000.00,
        'UPCOMING', '2025-06-30', NOW(), NOW(), 1)
    RETURNING id INTO v_stage4_id;

    -- ---- Addition VO 1: Material-Heavy (APPROVED) ---------------------------
    INSERT INTO change_orders (
        project_id, boq_document_id, reference_number,
        co_type, status,
        net_amount_ex_gst, gst_rate, gst_amount, net_amount_incl_gst,
        title, description, justification,
        submitted_at, approved_at,
        vo_category, scope_notes,
        mapped_stage_ids, approved_cost,
        created_at, updated_at, version
    ) VALUES (
        v_project_id, v_boq_doc_id, 'CO-DEMO-001',
        'SCOPE_ADDITION', 'APPROVED',
        500000.00, 0.18, 90000.00, 590000.00,
        'Premium Marble Flooring Upgrade',
        'Replace standard ceramic tiles with imported Italian marble in all living areas.',
        'Client requested premium finish for living room, dining and master bedroom.',
        NOW() - INTERVAL '30 days', NOW() - INTERVAL '20 days',
        'MATERIAL_HEAVY',
        'Supply and fix 1200x600 Italian marble tiles with matching skirting. ' ||
        'Includes waterproofing membrane and premium adhesive.',
        jsonb_build_array(v_stage3_id),
        590000.00,
        NOW() - INTERVAL '30 days', NOW() - INTERVAL '20 days', 1
    ) RETURNING id INTO v_co_add1_id;

    -- Payment schedule for CO-DEMO-001 (MATERIAL_HEAVY: 40/40/20)
    INSERT INTO co_payment_schedule (
        co_id,
        advance_pct, advance_amount, advance_status, advance_due_date, advance_invoice_number,
        progress_pct, progress_amount, progress_status, progress_trigger_stage_id,
        completion_pct, completion_amount, completion_status,
        created_at, updated_at
    ) VALUES (
        v_co_add1_id,
        40, 236000.00, 'PAID', (NOW() - INTERVAL '18 days')::DATE, 'INV-CO-001-ADV',
        40, 236000.00, 'PENDING', v_stage3_id,
        20, 118000.00, 'PENDING',
        NOW(), NOW()
    );

    -- Approval history for CO-DEMO-001
    INSERT INTO co_approval_history (co_id, approver_name, approver_id, level, action, comment, action_at)
    VALUES
        (v_co_add1_id, 'Rajesh Kumar', v_portal_user_id, 'PM', 'APPROVED',
         'Scope confirmed with client. Within PM threshold.', NOW() - INTERVAL '20 days');

    -- ---- Addition VO 2: Labour-Heavy (APPROVED) -----------------------------
    INSERT INTO change_orders (
        project_id, boq_document_id, reference_number,
        co_type, status,
        net_amount_ex_gst, gst_rate, gst_amount, net_amount_incl_gst,
        title, description, justification,
        submitted_at, approved_at,
        vo_category, scope_notes,
        mapped_stage_ids, approved_cost,
        created_at, updated_at, version
    ) VALUES (
        v_project_id, v_boq_doc_id, 'CO-DEMO-002',
        'SCOPE_ADDITION', 'APPROVED',
        1200000.00, 0.18, 216000.00, 1416000.00,
        'Additional False Ceiling & Electrical Works',
        'Gypsum false ceiling with LED coves across all rooms. Additional electrical points.',
        'Client added false ceiling requirement post-BOQ. Requires additional MEP coordination.',
        NOW() - INTERVAL '25 days', NOW() - INTERVAL '10 days',
        'LABOUR_HEAVY',
        'Gypsum board false ceiling with 4-inch LED cove lighting. ' ||
        '24 additional electrical points. Modular switches throughout.',
        jsonb_build_array(v_stage3_id, v_stage4_id),
        1416000.00,
        NOW() - INTERVAL '25 days', NOW() - INTERVAL '10 days', 1
    ) RETURNING id INTO v_co_add2_id;

    -- Payment schedule for CO-DEMO-002 (LABOUR_HEAVY: 20/60/20)
    INSERT INTO co_payment_schedule (
        co_id,
        advance_pct, advance_amount, advance_status, advance_due_date,
        progress_pct, progress_amount, progress_status, progress_trigger_stage_id,
        completion_pct, completion_amount, completion_status,
        created_at, updated_at
    ) VALUES (
        v_co_add2_id,
        20, 283200.00, 'INVOICED', (NOW() - INTERVAL '8 days')::DATE,
        60, 849600.00, 'PENDING', v_stage3_id,
        20, 283200.00, 'PENDING',
        NOW(), NOW()
    );

    -- Approval history for CO-DEMO-002 (escalated to Commercial Manager)
    INSERT INTO co_approval_history (co_id, approver_name, approver_id, level, action, comment, action_at)
    VALUES
        (v_co_add2_id, 'Rajesh Kumar', v_portal_user_id, 'PM', 'ESCALATED',
         'Amount exceeds PM threshold. Escalating to Commercial Manager.',
         NOW() - INTERVAL '15 days'),
        (v_co_add2_id, 'Priya Sharma', v_portal_user_id, 'COMMERCIAL_MANAGER', 'APPROVED',
         'Approved. Scope is reasonable and within commercial guidelines.',
         NOW() - INTERVAL '10 days');

    -- ---- Omission VO: Scope Reduction (APPROVED) ----------------------------
    INSERT INTO change_orders (
        project_id, boq_document_id, reference_number,
        co_type, status,
        net_amount_ex_gst, gst_rate, gst_amount, net_amount_incl_gst,
        title, description, justification,
        submitted_at, approved_at,
        vo_category, scope_notes, approved_cost,
        created_at, updated_at, version
    ) VALUES (
        v_project_id, v_boq_doc_id, 'CO-DEMO-003',
        'SCOPE_REDUCTION', 'APPROVED',
        -300000.00, 0.18, -54000.00, -354000.00,
        'Remove Landscaping from Scope',
        'Client will handle external landscaping independently. Remove from project scope.',
        'Client has appointed a separate landscaping contractor.',
        NOW() - INTERVAL '15 days', NOW() - INTERVAL '7 days',
        'MIXED',
        'Remove all external landscaping works: soft landscaping, irrigation system, ' ||
        'pathway paving, and external lighting. Client to handle separately.',
        -354000.00,
        NOW() - INTERVAL '15 days', NOW() - INTERVAL '7 days', 1
    ) RETURNING id INTO v_co_omit_id;

    -- Deduction entry linked to the omission CO
    INSERT INTO deduction_register (
        project_id, co_id, item_description,
        requested_amount, accepted_amount,
        decision, escalation_status,
        settled_in_final_account, created_at, updated_at
    ) VALUES (
        v_project_id, v_co_omit_id,
        'Landscaping scope removal — external soft landscaping, irrigation, pathway paving',
        354000.00, NULL,
        'PENDING', 'NONE',
        FALSE, NOW(), NOW()
    );

    -- ---- Revision VO: DRAFT (revises CO-DEMO-001) ---------------------------
    INSERT INTO change_orders (
        project_id, boq_document_id, reference_number,
        co_type, status,
        net_amount_ex_gst, gst_rate, gst_amount, net_amount_incl_gst,
        title, description,
        vo_category, scope_notes, revises_co_id,
        created_at, updated_at, version
    ) VALUES (
        v_project_id, v_boq_doc_id, 'CO-DEMO-004',
        'SCOPE_ADDITION', 'DRAFT',
        650000.00, 0.18, 117000.00, 767000.00,
        'Premium Marble Flooring Upgrade — Revised (incl. Bathrooms)',
        'Revised scope to include all bathroom floors in Italian marble (was living areas only).',
        'MATERIAL_HEAVY',
        'Extended marble scope to include master bath, 3 additional bathrooms. ' ||
        'Same material specification as CO-DEMO-001.',
        v_co_add1_id,
        NOW(), NOW(), 1
    ) RETURNING id INTO v_co_rev_id;

    RAISE NOTICE 'V28 seed data created — project_id=%, stages=[%, %, %, %], COs=[%, %, %, %]',
        v_project_id, v_stage1_id, v_stage2_id, v_stage3_id, v_stage4_id,
        v_co_add1_id, v_co_add2_id, v_co_omit_id, v_co_rev_id;
END $$;
