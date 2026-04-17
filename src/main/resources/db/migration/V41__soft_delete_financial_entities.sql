-- Add soft-delete support to financial entities missing it.

ALTER TABLE wage_sheets ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE labour_payments ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE vendor_payments ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_wage_sheets_deleted_at ON wage_sheets(deleted_at);
CREATE INDEX IF NOT EXISTS idx_labour_payments_deleted_at ON labour_payments(deleted_at);
CREATE INDEX IF NOT EXISTS idx_vendor_payments_deleted_at ON vendor_payments(deleted_at);
