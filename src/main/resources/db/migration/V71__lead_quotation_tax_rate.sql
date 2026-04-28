-- ===========================================================================
-- V71 - Lead Quotation tax-rate-percent column
--
-- Adds an optional tax-rate column so LeadQuotationService.calculateTotals()
-- can auto-compute the tax against the discounted base — matching Indian-GST
-- invoicing convention (tax applies to subtotal − discount, not subtotal).
--
-- Existing rows are left as NULL on purpose: the previous behaviour was a
-- staff-entered manual `tax_amount`, and a NULL rate signals "use the manual
-- amount as-is" so legacy quotations don't have their finalAmount silently
-- recomputed by a later edit.
--
-- New rows default to 18.00 (Indian standard residential GST) — the entity
-- field initializer applies for objects built via `new LeadQuotation()`, but
-- the DB-level default is included for direct inserts (tests, ad-hoc SQL).
-- ===========================================================================

ALTER TABLE lead_quotations
    ADD COLUMN tax_rate_percent NUMERIC(5, 2) DEFAULT 18.00;

-- Drop the default after the ADD so the column reads NULL for any future
-- staff-controlled "manual mode" inserts (the entity's @Column(default)
-- equivalent is the field initializer; the DB default was only useful for
-- the row currently being added by the migration, of which there are none).
ALTER TABLE lead_quotations
    ALTER COLUMN tax_rate_percent DROP DEFAULT;

-- Backfill: existing rows keep NULL (= "manual tax_amount honored unchanged").
-- No UPDATE statement here — that's intentional.

-- Postgres COMMENT IS expects a string *constant*, not an expression —
-- `||` is not allowed here. Use adjacent-literal concatenation across
-- newlines (the SQL standard "string-literal continuation" syntax)
-- which the parser treats as a single concatenated string.
COMMENT ON COLUMN lead_quotations.tax_rate_percent IS
    'Tax rate as percent (e.g. 18.00 = 18% GST). NULL = legacy manual mode: '
    'tax_amount is used as-is. When set, service auto-computes '
    'tax_amount = (total_amount − discount_amount) × tax_rate_percent / 100.';
