-- ─────────────────────────────────────────────────────────────────────────────
-- V8: Link PaymentTransaction → ProjectInvoice for payment reconciliation
-- ─────────────────────────────────────────────────────────────────────────────
-- Wrapped in a DO $$ block to be resilient to partial/incomplete schema
-- bootstraps where payment_transactions or project_invoices may not exist yet.
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables
               WHERE table_schema = 'public' AND table_name = 'payment_transactions')
    AND EXISTS (SELECT FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = 'project_invoices') THEN

        ALTER TABLE payment_transactions
            ADD COLUMN IF NOT EXISTS project_invoice_id BIGINT
                REFERENCES project_invoices(id) ON DELETE SET NULL;

        CREATE INDEX IF NOT EXISTS idx_payment_tx_invoice
            ON payment_transactions(project_invoice_id)
            WHERE project_invoice_id IS NOT NULL;
    END IF;
END $$;
