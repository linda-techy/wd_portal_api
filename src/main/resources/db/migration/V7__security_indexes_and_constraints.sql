-- ============================================================================
-- V7: Security fixes, performance indexes, and constraint improvements
-- ============================================================================
-- Covers:
--   1. Shrink refresh_tokens.token column (now stores 64-char SHA-256 hash)
--   2. Performance indexes on frequently-queried status/date columns
--   3. Ensure status columns can accommodate enum string values
-- ============================================================================

-- ─── 1. Refresh Token Column: Shrink to 64 chars (SHA-256 hex hash) ──────────
-- Tokens are now stored as SHA-256(rawJWT) — 64 hex chars, never the raw JWT.
-- Existing tokens will be invalidated on next deploy (users must re-login once).
ALTER TABLE refresh_tokens
    ALTER COLUMN token TYPE VARCHAR(64);

-- ─── 2. Performance Indexes ───────────────────────────────────────────────────

-- Tasks: project lookups with status filter (dashboard, project detail)
CREATE INDEX IF NOT EXISTS idx_tasks_project_status
    ON tasks(project_id, status)
    WHERE deleted_at IS NULL;

-- Tasks: due-date based queries (overdue tasks, due today)
CREATE INDEX IF NOT EXISTS idx_tasks_due_date_status
    ON tasks(due_date, status)
    WHERE deleted_at IS NULL;

-- Tasks: assigned user lookups
CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to
    ON tasks(assigned_to_id)
    WHERE deleted_at IS NULL AND status NOT IN ('COMPLETED', 'CANCELLED');

-- Leads: status-based filtering (open leads, by source)
CREATE INDEX IF NOT EXISTS idx_leads_status
    ON leads(lead_status)
    WHERE deleted_at IS NULL;

-- Leads: date of enquiry for monthly trend queries
CREATE INDEX IF NOT EXISTS idx_leads_date_of_enquiry
    ON leads(date_of_enquiry);

-- Payment schedule: status lookup (revenue collected, pending payments)
CREATE INDEX IF NOT EXISTS idx_payment_schedule_status
    ON payment_schedule(status);

-- Payment schedule: paid_date for monthly revenue trend
CREATE INDEX IF NOT EXISTS idx_payment_schedule_paid_date
    ON payment_schedule(paid_date)
    WHERE status = 'PAID';

-- Labour attendance: date + project lookups (on-site count, wage sheets)
CREATE INDEX IF NOT EXISTS idx_labour_attendance_date_project
    ON labour_attendance(attendance_date, project_id);

-- Project invoice: project + status (invoice listing, revenue totals)
CREATE INDEX IF NOT EXISTS idx_project_invoice_project_status
    ON project_invoice(project_id, status);

-- Delay log: active delays per project (delays with no end date)
CREATE INDEX IF NOT EXISTS idx_delay_log_project_active
    ON delay_log(project_id)
    WHERE to_date IS NULL;

-- Approval request: approver + status (pending approvals dashboard)
CREATE INDEX IF NOT EXISTS idx_approval_request_approver_status
    ON approval_request(approver_id, status);

-- Approval request: requested entity lookup
CREATE INDEX IF NOT EXISTS idx_approval_request_target
    ON approval_request(target_type, target_id);

-- Customer projects: status + soft-delete (active project count)
CREATE INDEX IF NOT EXISTS idx_customer_projects_status
    ON customer_projects(project_status)
    WHERE deleted_at IS NULL;

-- BOQ items: project lookup with status filter
CREATE INDEX IF NOT EXISTS idx_boq_items_project_status
    ON boq_items(project_id, status)
    WHERE deleted_at IS NULL;

-- Site reports: date range queries (this week's reports)
CREATE INDEX IF NOT EXISTS idx_site_reports_report_date
    ON site_reports(report_date);

-- ─── 3. Observation status: ensure column is wide enough for enum values ─────
-- (ObservationStatus max value is 'IN_PROGRESS' = 11 chars — well within default)
-- No action needed; VARCHAR fields in PostgreSQL are flexible.

-- ─── 4. Refresh tokens: index on user_id for fast revoke-all-by-user ─────────
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);
