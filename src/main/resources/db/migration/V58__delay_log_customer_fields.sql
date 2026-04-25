-- V58: Curated customer-facing delay log fields.
--
-- The raw delay_logs row contains free-text (reason_text, responsible_party,
-- impact_description) that can leak vendor names and internal blame. The
-- customer UI now shows a curated projection — driven by these three columns.

ALTER TABLE delay_logs
    ADD COLUMN customer_visible   BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN customer_summary   TEXT,
    ADD COLUMN impact_on_handover VARCHAR(20);

-- Allowed values for impact_on_handover: NONE | MINOR | MATERIAL
-- (nullable — legacy rows and internal-only delays stay null).
ALTER TABLE delay_logs
    ADD CONSTRAINT delay_logs_impact_on_handover_check
        CHECK (impact_on_handover IS NULL
               OR impact_on_handover IN ('NONE', 'MINOR', 'MATERIAL'));

-- Speeds up the customer-visible list query.
CREATE INDEX IF NOT EXISTS idx_delay_logs_project_customer_visible
    ON delay_logs (project_id, customer_visible, from_date DESC);
