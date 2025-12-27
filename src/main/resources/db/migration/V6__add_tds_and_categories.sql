-- V6: Add TDS Tracking and Payment Categorization
-- Supports Indian Income Tax Act Section 194C compliance (optional, defaults to 0%)

-- Add TDS and categorization columns
ALTER TABLE payment_transactions 
ADD COLUMN IF NOT EXISTS tds_percentage NUMERIC(5,2) DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS tds_amount NUMERIC(15,2) DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS net_amount NUMERIC(15,2),
ADD COLUMN IF NOT EXISTS tds_deducted_by VARCHAR(50) DEFAULT 'CUSTOMER' NOT NULL,
ADD COLUMN IF NOT EXISTS payment_category VARCHAR(50) DEFAULT 'PROGRESS' NOT NULL;

-- Backfill net_amount for existing records (gross amount = net amount when no TDS)
UPDATE payment_transactions 
SET net_amount = amount 
WHERE net_amount IS NULL;

-- Make net_amount non-nullable after backfill
ALTER TABLE payment_transactions 
ALTER COLUMN net_amount SET NOT NULL;

-- Create indexes for reporting and filtering
CREATE INDEX IF NOT EXISTS idx_payment_transactions_tds 
ON payment_transactions(tds_deducted_by, payment_date);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_category 
ON payment_transactions(payment_category);

-- Add documentation comments
COMMENT ON COLUMN payment_transactions.tds_percentage IS 'TDS rate percentage (0-100). Default 0%. Common: 2% for Section 194C (construction)';
COMMENT ON COLUMN payment_transactions.tds_amount IS 'Calculated TDS amount deducted from gross payment';
COMMENT ON COLUMN payment_transactions.net_amount IS 'Net amount received after TDS deduction (amount - tds_amount)';
COMMENT ON COLUMN payment_transactions.tds_deducted_by IS 'Who deducted TDS: CUSTOMER (default), SELF, or NONE';
COMMENT ON COLUMN payment_transactions.payment_category IS 'Payment type: ADVANCE, PROGRESS, FINAL, RETENTION_RELEASE';
