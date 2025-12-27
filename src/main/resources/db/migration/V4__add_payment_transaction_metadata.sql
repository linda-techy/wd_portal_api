-- V4__add_payment_transaction_metadata.sql
-- Add receipt numbering and status tracking to payment transactions

ALTER TABLE payment_transactions 
ADD COLUMN IF NOT EXISTS receipt_number VARCHAR(50),
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'COMPLETED';

-- Ensure existing records have a status
UPDATE payment_transactions SET status = 'COMPLETED' WHERE status IS NULL;

-- Add unique constraint to receipt_number
-- Note: existing records will have null receipt numbers initially
ALTER TABLE payment_transactions 
ADD CONSTRAINT uk_payment_receipt_number UNIQUE (receipt_number);

-- Add index for status searches
CREATE INDEX IF NOT EXISTS idx_payment_transactions_status ON payment_transactions(status);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_receipt ON payment_transactions(receipt_number);
