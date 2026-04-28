-- ===========================================================================
-- V72 - Race-safe quotation-number sequence
--
-- Replaces the previous `count(*) + 1` numbering strategy in
-- LeadQuotationService.generateQuotationNumber().  The old approach was racy
-- (two concurrent creates compute the same count) and reused numbers after a
-- hard delete.  A Postgres SEQUENCE is atomic and never reuses a value.
--
-- The number format ("QUO-{yyyyMMdd}-{NNNN}") stays the same; only the
-- counter source changes.
-- ===========================================================================

CREATE SEQUENCE IF NOT EXISTS lead_quotation_number_seq
    INCREMENT BY 1
    MINVALUE 1
    NO CYCLE;

-- Initialise the sequence past the highest existing id so newly issued
-- numbers can never collide with already-issued ones (the previous strategy
-- used a global counter equivalent to the row count, so id is a safe proxy).
DO $$
DECLARE
    next_start BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) + 1 INTO next_start FROM lead_quotations;
    -- false = "the next nextval() returns this exact value", not value + 1.
    PERFORM setval('lead_quotation_number_seq', next_start, false);
END $$;

-- Adjacent-literal continuation (no `||` — that's an expression, not a constant).
COMMENT ON SEQUENCE lead_quotation_number_seq IS
    'Atomic counter for the suffix of LeadQuotation.quotationNumber. '
    'Replaces the racy count(*)+1 strategy.';
