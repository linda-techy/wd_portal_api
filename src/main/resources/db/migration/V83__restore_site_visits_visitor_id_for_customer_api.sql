-- ===========================================================================
-- V83 — Restore site_visits.visitor_id (dropped in V80) for Customer API
--
-- V80 was a mis-fire. We dropped `visitor_id` from `site_visits` because
-- the Portal API's `SiteVisit` entity doesn't reference it — its visitor
-- pointer is `visited_by` → PortalUser. But the **Customer API**
-- (wd_customer_api) has its own separate `SiteVisit` entity over the SAME
-- shared table that DOES reference `visitor_id` → CustomerUser:
--
--     @ManyToOne @JoinColumn(name = "visitor_id", nullable = false)
--     private CustomerUser visitor;
--
-- After V80, every Customer-API GET on /api/projects/{id}/site-visits
-- failed with:
--   ERROR: column sv1_0.visitor_id does not exist
-- (visible to the user as the "Failed to load site visits ... 500"
-- error in the customer Flutter app's Site Visits screen).
--
-- This migration restores the column. It is intentionally:
--   * NULLable — Portal-API check-ins won't populate it; only the
--     Customer-API path does. Making it NOT NULL would re-break Portal.
--   * FK'd to customer_users — that's what the Customer-API entity expects.
--   * Indexed for the customer-side "ongoing visits for project" query.
--
-- The Portal-API SiteVisit entity simply does not declare this column
-- and so will continue to ignore it on both reads and writes.
--
-- Idempotent: every step uses IF NOT EXISTS / guarded DO-block.
-- ===========================================================================

ALTER TABLE site_visits
    ADD COLUMN IF NOT EXISTS visitor_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_site_visits_visitor_id_customer_users'
    ) THEN
        ALTER TABLE site_visits
            ADD CONSTRAINT fk_site_visits_visitor_id_customer_users
            FOREIGN KEY (visitor_id) REFERENCES customer_users(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_site_visits_visitor_id
    ON site_visits(visitor_id)
    WHERE visitor_id IS NOT NULL;

COMMENT ON COLUMN site_visits.visitor_id IS
    'FK to customer_users. Used ONLY by the Customer API''s SiteVisit '
    'entity (customer-side check-ins). NULL for rows created by the '
    'Portal API, which uses visited_by → portal_users instead.';
