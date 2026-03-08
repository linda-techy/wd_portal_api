-- ============================================================================
-- Migration: Add Performance Indexes for Auth Token Tables
-- Purpose: Prevent full table scans on every authenticated API request.
--          refresh_tokens.token is queried on EVERY token validation,
--          and portal_users.email is queried on every login.
-- Date: 2026-03-09
-- ============================================================================

-- refresh_tokens.token — queried on every API request via findByToken()
-- Without this, every authenticated request triggers a full sequential scan.
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token
ON refresh_tokens(token);

-- refresh_tokens.expiry_date — used by the nightly cleanup @Scheduled job
-- Also used in token validation checks (expiryDate < NOW()).
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry
ON refresh_tokens(expiry_date);

-- refresh_tokens.revoked — used in combination with token lookup
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_revoked
ON refresh_tokens(revoked) WHERE revoked = false;

-- portal_users.email — the hottest query in the system (findByEmail on every login + JWT validate)
CREATE INDEX IF NOT EXISTS idx_portal_users_email
ON portal_users(email);

-- portal_users.username — used in UserDetailsService.loadUserByUsername
CREATE INDEX IF NOT EXISTS idx_portal_users_username
ON portal_users(username);

-- leads.email — used in email-based lead deduplication and lookups
CREATE INDEX IF NOT EXISTS idx_leads_email
ON leads(email);

-- leads.phone — used in phone-based lead deduplication
CREATE INDEX IF NOT EXISTS idx_leads_phone
ON leads(phone);

-- leads.created_at — used for analytics and reporting date range queries
CREATE INDEX IF NOT EXISTS idx_leads_created_at
ON leads(created_at DESC);

-- customer_projects.end_date — used by findOverdueProjects()
-- Powers the overdue project detection query on every stats page load
CREATE INDEX IF NOT EXISTS idx_customer_projects_end_date_status
ON customer_projects(end_date, project_phase);
