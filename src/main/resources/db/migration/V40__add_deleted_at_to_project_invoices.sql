-- Add soft-delete support to project_invoices table.
-- ProjectInvoice does not extend BaseEntity, so deleted_at column must be added explicitly.

ALTER TABLE project_invoices ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_project_invoices_deleted_at ON project_invoices(deleted_at);
