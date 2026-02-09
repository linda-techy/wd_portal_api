-- Migration: Create receipts table
-- Version: V1_56__create_receipts.sql
-- Description: Payment receipts issued against project invoices

CREATE TABLE IF NOT EXISTS receipts (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES customer_projects(id),
    invoice_id BIGINT REFERENCES project_invoices(id),
    receipt_number VARCHAR(255) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_method VARCHAR(50),
    transaction_reference VARCHAR(100),
    notes VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

-- Unique constraint on receipt number
ALTER TABLE receipts ADD CONSTRAINT uk_receipts_receipt_number UNIQUE (receipt_number);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_receipts_project ON receipts(project_id);
CREATE INDEX IF NOT EXISTS idx_receipts_invoice ON receipts(invoice_id);
CREATE INDEX IF NOT EXISTS idx_receipts_payment_date ON receipts(payment_date);

COMMENT ON TABLE receipts IS 'Payment receipts for construction project invoices';
