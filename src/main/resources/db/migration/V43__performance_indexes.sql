-- Add performance indexes on frequently queried columns.

CREATE INDEX IF NOT EXISTS idx_labour_payments_project ON labour_payments(project_id);
CREATE INDEX IF NOT EXISTS idx_labour_payments_labour ON labour_payments(labour_id);
CREATE INDEX IF NOT EXISTS idx_vendor_payments_date ON vendor_payments(payment_date);
CREATE INDEX IF NOT EXISTS idx_wage_sheets_project_date ON wage_sheets(project_id, period_start, period_end);
