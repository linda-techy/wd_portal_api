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

ALTER TABLE lead_quotations
    ADD COLUMN pricing_mode VARCHAR(20),
    ADD COLUMN rate_per_sqft NUMERIC(12, 2);

-- Backfill: every existing quotation predates this column — they have line
-- items with qty + unitPrice and stay in LINE_ITEM mode.
UPDATE lead_quotations SET pricing_mode = 'LINE_ITEM' WHERE pricing_mode IS NULL;

ALTER TABLE lead_quotations
    ALTER COLUMN pricing_mode SET NOT NULL,
    ADD CONSTRAINT chk_lead_quotation_pricing_mode
        CHECK (pricing_mode IN ('LINE_ITEM', 'SQFT_RATE'));

COMMENT ON COLUMN lead_quotations.pricing_mode IS
    'LINE_ITEM = qty×unitPrice line items sum to subtotal (legacy + still '
    || 'supported). SQFT_RATE = subtotal computed as lead.sqfeet × '
    || 'rate_per_sqft; items become scope specs (description only).';

COMMENT ON COLUMN lead_quotations.rate_per_sqft IS
    'Per-sqft rate for SQFT_RATE mode (e.g. ₹2050.00/sqft). NULL for '
    || 'LINE_ITEM rows. The Walldot customer-facing PDF surfaces this as '
    || 'the headline rate.';
