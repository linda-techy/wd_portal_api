-- Phase 3: Business logic workflow fixes

-- WageSheet approval tracking
ALTER TABLE wage_sheets ADD COLUMN IF NOT EXISTS approved_by BIGINT;
ALTER TABLE wage_sheets ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
ALTER TABLE wage_sheets ADD COLUMN IF NOT EXISTS approval_notes TEXT;

-- LabourPayment → WageSheet link
ALTER TABLE labour_payments ADD COLUMN IF NOT EXISTS wage_sheet_id BIGINT;
ALTER TABLE labour_payments ADD CONSTRAINT fk_labour_payment_wage_sheet
    FOREIGN KEY (wage_sheet_id) REFERENCES wage_sheets(id);

CREATE INDEX IF NOT EXISTS idx_labour_payments_wage_sheet ON labour_payments(wage_sheet_id);
