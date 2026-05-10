-- V132: Drop NOT NULL on project_variations.estimated_amount.
--
-- Why: V127 (CR-v2 state machine) introduced cost_impact as the v2 field for
-- the variation's monetary impact. The legacy estimated_amount column was
-- left NOT NULL alongside it. The v2 controller path
-- (/api/projects/{id}/variations) accepts cost_impact and does NOT populate
-- estimated_amount, so any insert through the modern API path fails with
--   ConstraintViolationException: null value in column "estimated_amount"
-- The bug surfaced via the portal-features Playwright suite (Tier 6) and
-- previously forced the lifecycle suite to send a redundant `estimatedAmount`
-- field that the controller doesn't actually need.
--
-- Decision: drop NOT NULL. estimated_amount stays as a nullable column so
-- legacy reads keep working; cost_impact is the single source of truth for
-- new writes. A future migration can drop the column entirely once we've
-- confirmed no reader path depends on it.

ALTER TABLE project_variations
  ALTER COLUMN estimated_amount DROP NOT NULL;
