-- ─────────────────────────────────────────────────────────────────────────────
-- V8: Link PaymentTransaction → ProjectInvoice for payment reconciliation
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS project_invoice_id BIGINT
        REFERENCES project_invoices(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_payment_tx_invoice
    ON payment_transactions(project_invoice_id)
    WHERE project_invoice_id IS NOT NULL;
