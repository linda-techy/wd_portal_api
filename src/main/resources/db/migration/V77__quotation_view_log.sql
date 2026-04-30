-- ===========================================================================
-- V77 — Customer-facing quotation view log (P2 of 3-stage redesign)
--
-- V76 added the public_view_token UUID on lead_quotations. This migration
-- creates the table that the public token-gated endpoint will append to on
-- every customer hit, so staff can finally see "Mr Clinton opened the PDF
-- 4 times in the last 24 hours" — a strong signal he's about to call.
--
-- Append-only by convention; no soft-delete column. View rows live for the
-- lifetime of the parent quotation (ON DELETE CASCADE).
-- ===========================================================================

CREATE TABLE IF NOT EXISTS quotation_view_log (
    id           BIGSERIAL PRIMARY KEY,
    quotation_id BIGINT       NOT NULL,
    viewed_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    -- IPv4 = max 15 chars; IPv6 = max 45 chars; we hold the raw remote
    -- address as observed at the controller (no GeoIP join here).
    ip_address   VARCHAR(45),
    user_agent   TEXT,
    -- WHATSAPP_LINK / EMAIL_LINK / DIRECT / IN_APP — populated from a query
    -- param on the public endpoint when present; null when the customer
    -- hits the link out-of-band.
    source       VARCHAR(20),
    CONSTRAINT fk_quotation_view_log_quotation
        FOREIGN KEY (quotation_id) REFERENCES lead_quotations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_quotation_view_log_quotation_at
    ON quotation_view_log(quotation_id, viewed_at DESC);

COMMENT ON TABLE quotation_view_log IS
    'One row per customer-side hit on the public token-gated quotation '
    'endpoint. Powers the "viewed N times" badge on the lead screen and '
    'the first-view timestamp that the legacy lead_quotations.viewed_at '
    'column should be backfilled from going forward.';

COMMENT ON COLUMN quotation_view_log.source IS
    'Optional channel hint from the share path — WHATSAPP_LINK, EMAIL_LINK, '
    'DIRECT, IN_APP. Captured from the ?source=... query param on the '
    'public endpoint. NULL when the customer hits the link without one.';
