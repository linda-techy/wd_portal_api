-- ===========================================================================
-- V145 — Backfill BaseEntity audit columns on every table whose entity
--        extends BaseEntity
--
-- BaseEntity gained `deleted_at` + `deleted_by_user_id` (soft-delete) and
-- the rest of its audit set was tightened up across releases. Several tables
-- were created before those fields existed (e.g. credit_notes from V22), so
-- portal-api with ddl-auto=validate fails at "missing column [deleted_at]
-- in table [credit_notes]" and would fail on similar tables after a fix.
--
-- This migration applies ADD COLUMN IF NOT EXISTS for the entire BaseEntity
-- column set to every table whose entity extends BaseEntity. Idempotent —
-- columns already present are no-ops.
-- ===========================================================================

DO $$
DECLARE
    t text;
    base_tables text[] := ARRAY[
        'boq_categories', 'boq_documents', 'boq_invoices', 'boq_items',
        'change_orders', 'change_request_task_predecessors', 'change_request_tasks',
        'credit_notes', 'customer_projects', 'dpc_customization_catalog',
        'dpc_customization_line', 'dpc_document', 'dpc_document_scope',
        'dpc_scope_option', 'dpc_scope_template', 'goods_received_notes',
        'holiday', 'inventory_stock', 'labour', 'labour_advances',
        'labour_attendance', 'labour_payments', 'leads', 'material_budgets',
        'material_indent_items', 'material_indents', 'materials',
        'measurement_book', 'portal_users', 'project_baseline',
        'project_documents', 'project_holiday_override', 'project_schedule_config',
        'project_variations', 'project_warranties', 'purchase_order_items',
        'purchase_orders', 'refund_notices', 'retention_releases',
        'site_reports', 'stock_adjustments', 'subcontract_work_orders',
        'task_baseline', 'task_predecessor', 'tasks', 'vendor_payments',
        'vendor_quotations', 'vendors', 'wage_sheet_entries', 'wage_sheets'
    ];
BEGIN
    FOREACH t IN ARRAY base_tables
    LOOP
        -- Skip silently if the table itself doesn't exist on this DB.
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = t
        ) THEN
            RAISE NOTICE 'V145: table % not present, skipping', t;
            CONTINUE;
        END IF;

        EXECUTE format('ALTER TABLE %I '
                       'ADD COLUMN IF NOT EXISTS created_at         TIMESTAMP, '
                       'ADD COLUMN IF NOT EXISTS updated_at         TIMESTAMP, '
                       'ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT, '
                       'ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT, '
                       'ADD COLUMN IF NOT EXISTS deleted_at         TIMESTAMP, '
                       'ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT, '
                       'ADD COLUMN IF NOT EXISTS version            BIGINT DEFAULT 1',
                       t);
    END LOOP;
END $$;
