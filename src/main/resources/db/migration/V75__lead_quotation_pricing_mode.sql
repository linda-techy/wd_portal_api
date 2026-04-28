-- ===========================================================================
-- V75 - LeadQuotation pricing-mode + rate-per-sqft columns
--
-- Walldot's actual customer quotations (e.g. "Work Quotation for Mr Clinton
-- Amballur") use a per-sqft pricing model — the document states a single
-- "Rs. X/- per square feet" headline rate and treats line items as scope
-- specifications (description + brand + max-cost ceiling), not as
-- qty×unitPrice rows. This migration adds the structural support so the
-- system can produce that document shape.
--
-- Two new columns:
--   pricing_mode    — LINE_ITEM (existing flow, preserved for back-compat)
--                     or SQFT_RATE (the Walldot-style document)
--   rate_per_sqft   — used by SQFT_RATE; subtotal = lead.sqft × rate
--
-- Existing rows are stamped LINE_ITEM so their numbers don't change. New
-- quotations default to SQFT_RATE at the application layer (entity field
-- initializer) since that's what the customer-facing template now expects.
-- ===========================================================================

-- Idempotent: Hibernate auto-DDL may have already added these columns on
-- a prior app boot. IF NOT EXISTS lets the migration succeed against
-- either state; the backfill + NOT NULL + CHECK below are safe to run
-- repeatedly because they're guarded by data state, not column presence.
ALTER TABLE lead_quotations
    ADD COLUMN IF NOT EXISTS pricing_mode VARCHAR(20),
    ADD COLUMN IF NOT EXISTS rate_per_sqft NUMERIC(12, 2);

-- Backfill: every existing quotation predates this column — they have line
-- items with qty + unitPrice and stay in LINE_ITEM mode.
UPDATE lead_quotations SET pricing_mode = 'LINE_ITEM' WHERE pricing_mode IS NULL;

ALTER TABLE lead_quotations ALTER COLUMN pricing_mode SET NOT NULL;

-- Conditional constraint add — Hibernate auto-DDL doesn't add CHECK
-- constraints, but a prior run of this migration might have. Skip if
-- already present.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_lead_quotation_pricing_mode'
    ) THEN
        ALTER TABLE lead_quotations
            ADD CONSTRAINT chk_lead_quotation_pricing_mode
            CHECK (pricing_mode IN ('LINE_ITEM', 'SQFT_RATE'));
    END IF;
END $$;

-- Adjacent-literal continuation (no `||`; expressions aren't valid in COMMENT IS).
COMMENT ON COLUMN lead_quotations.pricing_mode IS
    'LINE_ITEM = qty×unitPrice line items sum to subtotal (legacy + still '
    'supported). SQFT_RATE = subtotal computed as lead.sqfeet × '
    'rate_per_sqft; items become scope specs (description only).';

COMMENT ON COLUMN lead_quotations.rate_per_sqft IS
    'Per-sqft rate for SQFT_RATE mode (e.g. ₹2050.00/sqft). NULL for '
    'LINE_ITEM rows. The Walldot customer-facing PDF surfaces this as '
    'the headline rate.';
