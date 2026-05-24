-- V157: Make BOQ payment-stage retention opt-in (default 0%).
--
-- Background:
--   V25 created payment_stages.retention_pct with NOT NULL DEFAULT 0.0500 (5%),
--   and the entity skims retention off net_payable_amount on persist. The result
--   was that EVERY stage silently withheld 5% from the moment it was created,
--   even when no retention was intended. The customer's payment schedule then
--   showed a net payable 5% short of the stage's gross.
--
-- Change:
--   1. Flip the column default to 0% so new stages hold nothing by default.
--      Retention stays available — it can still be set explicitly per stage at
--      certification (StagePaymentCertificationService) when a project needs it.
--   2. Reset retention on existing stages that are NOT yet settled, recomputing
--      net_payable = stage_amount_incl_gst - applied_credit (retention 0).
--      Stages already INVOICED or PAID are left untouched, because their amounts
--      may already be reflected in issued invoices / recorded receipts.

ALTER TABLE payment_stages
    ALTER COLUMN retention_pct SET DEFAULT 0.0000;

UPDATE payment_stages
SET retention_pct      = 0.0000,
    retention_held     = 0.000000,
    net_payable_amount = GREATEST(
        stage_amount_incl_gst - COALESCE(applied_credit_amount, 0),
        0
    ),
    updated_at         = NOW()
WHERE status NOT IN ('INVOICED', 'PAID');
