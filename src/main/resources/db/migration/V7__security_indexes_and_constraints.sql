-- ============================================================================
-- V7: Security fixes, performance indexes, and constraint improvements
-- ============================================================================
-- Every statement is wrapped in a DO $$ block that checks both TABLE and
-- COLUMN existence before executing. This makes V7 fully resilient to:
--   a) Table renames: refresh_tokens→portal_refresh_tokens,
--      project_invoice→project_invoices, delay_log→delay_logs,
--      approval_request→approval_requests
--   b) Column renames: assigned_to_id→assigned_to (tasks table)
--   c) Partial/incomplete schema bootstraps on shared test DBs
--
-- FlywayRepairConfig.repair() re-aligns checksums on every startup so this
-- file can be modified safely without breaking existing environments.
-- ============================================================================

-- ─── Helper macro: column exists? ────────────────────────────────────────────
-- Used below as: (SELECT FROM information_schema.columns WHERE ...)

-- ─── 1. Refresh Token Column: Shrink to 64 chars (SHA-256 hex hash) ──────────
-- Tokens are stored as SHA-256(rawJWT) — 64 hex chars, never the raw JWT.
-- If column is wider than 64 chars, existing raw-JWT rows are deleted first
-- (users re-login once — intentional). No-op if already VARCHAR(64).
DO $$
DECLARE
    v_col_len integer;
    v_tbl     text;
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables
               WHERE table_schema = 'public' AND table_name = 'portal_refresh_tokens') THEN
        v_tbl := 'portal_refresh_tokens';
    ELSIF EXISTS (SELECT FROM information_schema.tables
                  WHERE table_schema = 'public' AND table_name = 'refresh_tokens') THEN
        v_tbl := 'refresh_tokens';
    ELSE
        RETURN;
    END IF;

    SELECT character_maximum_length INTO v_col_len
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name   = v_tbl
      AND column_name  = 'token';

    IF v_col_len IS NULL OR v_col_len > 64 THEN
        EXECUTE 'DELETE FROM ' || v_tbl;
        EXECUTE 'ALTER TABLE ' || v_tbl || ' ALTER COLUMN token TYPE VARCHAR(64)';
    END IF;
END $$;

-- ─── 2. Performance Indexes ───────────────────────────────────────────────────

-- Tasks: project lookups with status filter
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='tasks' AND column_name='project_id')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='tasks' AND column_name='status')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='tasks' AND column_name='deleted_at') THEN
        CREATE INDEX IF NOT EXISTS idx_tasks_project_status
            ON tasks(project_id, status)
            WHERE deleted_at IS NULL;
    END IF;
END $$;

-- Tasks: due-date based queries (overdue tasks, due today)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='tasks' AND column_name='due_date')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='tasks' AND column_name='status')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='tasks' AND column_name='deleted_at') THEN
        CREATE INDEX IF NOT EXISTS idx_tasks_due_date_status
            ON tasks(due_date, status)
            WHERE deleted_at IS NULL;
    END IF;
END $$;

-- Tasks: assigned user lookups
-- NOTE: column is "assigned_to" (FK), NOT "assigned_to_id"
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='tasks' AND column_name='assigned_to')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='tasks' AND column_name='status')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='tasks' AND column_name='deleted_at') THEN
        CREATE INDEX IF NOT EXISTS idx_tasks_assigned_to
            ON tasks(assigned_to)
            WHERE deleted_at IS NULL AND status NOT IN ('COMPLETED', 'CANCELLED');
    END IF;
END $$;

-- Leads: status-based filtering
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='leads' AND column_name='lead_status')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='leads' AND column_name='deleted_at') THEN
        CREATE INDEX IF NOT EXISTS idx_leads_status
            ON leads(lead_status)
            WHERE deleted_at IS NULL;
    END IF;
END $$;

-- Leads: date of enquiry for monthly trend queries
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='leads' AND column_name='date_of_enquiry') THEN
        CREATE INDEX IF NOT EXISTS idx_leads_date_of_enquiry
            ON leads(date_of_enquiry);
    END IF;
END $$;

-- Payment schedule: status lookup
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='payment_schedule' AND column_name='status') THEN
        CREATE INDEX IF NOT EXISTS idx_payment_schedule_status
            ON payment_schedule(status);
    END IF;
END $$;

-- Payment schedule: paid_date for monthly revenue trend
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='payment_schedule' AND column_name='paid_date')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='payment_schedule' AND column_name='status') THEN
        CREATE INDEX IF NOT EXISTS idx_payment_schedule_paid_date
            ON payment_schedule(paid_date)
            WHERE status = 'PAID';
    END IF;
END $$;

-- Labour attendance: date + project lookups
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='labour_attendance' AND column_name='attendance_date')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='labour_attendance' AND column_name='project_id') THEN
        CREATE INDEX IF NOT EXISTS idx_labour_attendance_date_project
            ON labour_attendance(attendance_date, project_id);
    END IF;
END $$;

-- Project invoices: project + status
-- NOTE: entity table is "project_invoices" (was "project_invoice" in original V7)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='project_invoices' AND column_name='project_id')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='project_invoices' AND column_name='status') THEN
        CREATE INDEX IF NOT EXISTS idx_project_invoice_project_status
            ON project_invoices(project_id, status);
    ELSIF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='project_invoice' AND column_name='project_id')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='project_invoice' AND column_name='status') THEN
        CREATE INDEX IF NOT EXISTS idx_project_invoice_project_status
            ON project_invoice(project_id, status);
    END IF;
END $$;

-- Delay logs: active delays per project
-- NOTE: entity table is "delay_logs" (was "delay_log" in original V7)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='delay_logs' AND column_name='project_id')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='delay_logs' AND column_name='to_date') THEN
        CREATE INDEX IF NOT EXISTS idx_delay_log_project_active
            ON delay_logs(project_id)
            WHERE to_date IS NULL;
    ELSIF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='delay_log' AND column_name='project_id')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='delay_log' AND column_name='to_date') THEN
        CREATE INDEX IF NOT EXISTS idx_delay_log_project_active
            ON delay_log(project_id)
            WHERE to_date IS NULL;
    END IF;
END $$;

-- Approval requests: approver + status
-- NOTE: entity table is "approval_requests" (was "approval_request" in original V7)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='approval_requests' AND column_name='approver_id')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='approval_requests' AND column_name='status') THEN
        CREATE INDEX IF NOT EXISTS idx_approval_request_approver_status
            ON approval_requests(approver_id, status);
    ELSIF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='approval_request' AND column_name='approver_id')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='approval_request' AND column_name='status') THEN
        CREATE INDEX IF NOT EXISTS idx_approval_request_approver_status
            ON approval_request(approver_id, status);
    END IF;
END $$;

-- Approval requests: target entity lookup
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='approval_requests' AND column_name='target_type')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='approval_requests' AND column_name='target_id') THEN
        CREATE INDEX IF NOT EXISTS idx_approval_request_target
            ON approval_requests(target_type, target_id);
    ELSIF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='approval_request' AND column_name='target_type')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='approval_request' AND column_name='target_id') THEN
        CREATE INDEX IF NOT EXISTS idx_approval_request_target
            ON approval_request(target_type, target_id);
    END IF;
END $$;

-- Customer projects: status + soft-delete
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='customer_projects' AND column_name='project_status')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='customer_projects' AND column_name='deleted_at') THEN
        CREATE INDEX IF NOT EXISTS idx_customer_projects_status
            ON customer_projects(project_status)
            WHERE deleted_at IS NULL;
    END IF;
END $$;

-- BOQ items: project lookup with status filter
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='boq_items' AND column_name='project_id')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='boq_items' AND column_name='status')
    AND EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='boq_items' AND column_name='deleted_at') THEN
        CREATE INDEX IF NOT EXISTS idx_boq_items_project_status
            ON boq_items(project_id, status)
            WHERE deleted_at IS NULL;
    END IF;
END $$;

-- Site reports: date range queries
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='site_reports' AND column_name='report_date') THEN
        CREATE INDEX IF NOT EXISTS idx_site_reports_report_date
            ON site_reports(report_date);
    END IF;
END $$;

-- ─── 3. Observation status: no action needed (VARCHAR is flexible in PostgreSQL)

-- ─── 4. Refresh tokens: index on user_id for fast revoke-all-by-user ─────────
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='portal_refresh_tokens' AND column_name='user_id') THEN
        CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
            ON portal_refresh_tokens(user_id);
    ELSIF EXISTS (SELECT FROM information_schema.columns
               WHERE table_schema='public' AND table_name='refresh_tokens' AND column_name='user_id') THEN
        CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
            ON refresh_tokens(user_id);
    END IF;
END $$;
