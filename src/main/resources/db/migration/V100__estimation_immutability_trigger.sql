-- ===========================================================================
-- V100 — Immutability trigger on estimation table
--
-- Once an estimation reaches status='ACCEPTED', the following fields are
-- frozen by a database-level trigger:
--   lead_id, package_id, estimation_no, rate_version_id, market_index_id,
--   dimensions_json, grand_total
--
-- This protects audit history even from direct SQL by an admin. Status
-- changes (e.g., REJECTED for refund) are still allowed.
-- Per spec §1.4.
--
-- Idempotent: CREATE OR REPLACE FUNCTION + DROP TRIGGER IF EXISTS + CREATE TRIGGER.
-- ===========================================================================

CREATE OR REPLACE FUNCTION enforce_estimation_accepted_immutability()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = 'ACCEPTED' AND (
        NEW.lead_id          IS DISTINCT FROM OLD.lead_id          OR
        NEW.package_id       IS DISTINCT FROM OLD.package_id       OR
        NEW.estimation_no    IS DISTINCT FROM OLD.estimation_no    OR
        NEW.rate_version_id  IS DISTINCT FROM OLD.rate_version_id  OR
        NEW.market_index_id  IS DISTINCT FROM OLD.market_index_id  OR
        NEW.dimensions_json  IS DISTINCT FROM OLD.dimensions_json  OR
        NEW.grand_total      IS DISTINCT FROM OLD.grand_total
    ) THEN
        RAISE EXCEPTION 'Accepted estimations are immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_estimation_immutable ON estimation;

CREATE TRIGGER trg_estimation_immutable
    BEFORE UPDATE ON estimation
    FOR EACH ROW EXECUTE FUNCTION enforce_estimation_accepted_immutability();
