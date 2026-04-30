-- ===========================================================================
-- V76 — Quotation 3-stage redesign (Phase 1: schema, additive only)
--
-- The legacy LeadQuotation row treats one artefact as "the quotation" — but
-- Walldot's actual sales motion has three distinct documents (Budgetary at
-- enquiry, Detailed Proposal after site visit, Contract BOQ after structural
-- plan). Showing a grand total at the budgetary stage damages trust because
-- area is unknown and material scope can still change. This migration is
-- step 1 of the redesign: it adds the discriminator + supporting columns
-- and the four child tables (inclusions, exclusions, assumptions, payment
-- milestones) without breaking any existing read path.
--
-- Scope of this migration (additive only — no drops, no NOT-NULL flips):
--   1. lead_quotations gains: quotation_type, tier, parent_quotation_id,
--      rate_per_sqft_min/max, estimated_area_min/max, duration_months_min/max,
--      valid_until (absolute date), show_grand_total, public_view_token (UUID).
--   2. New child tables: quotation_inclusions, quotation_exclusions,
--      quotation_assumptions, quotation_payment_milestones.
--   3. Existing rows are stamped quotation_type = 'DETAILED' so legacy
--      behaviour is preserved (their final_amount stays meaningful).
--
-- Out of scope here (deferred to later phases):
--   * Making final_amount nullable — done after service-layer rewrite so
--     existing read paths can't NPE on legacy DTOs.
--   * Floor estimates, add-ons, acceptance, revision log, rate card, scope
--     templates table — separate migrations once the core flow is wired.
--
-- Idempotent: every ALTER uses IF NOT EXISTS, every CREATE uses IF NOT
-- EXISTS, and every CHECK constraint is added through a guarded DO-block
-- so re-running the migration against a partially-applied schema is safe.
-- ===========================================================================

-- --- 1. Stage discriminator + parent link ---------------------------------

ALTER TABLE lead_quotations
    ADD COLUMN IF NOT EXISTS quotation_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS parent_quotation_id BIGINT;

UPDATE lead_quotations SET quotation_type = 'DETAILED' WHERE quotation_type IS NULL;

ALTER TABLE lead_quotations ALTER COLUMN quotation_type SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_lead_quotation_type'
    ) THEN
        ALTER TABLE lead_quotations
            ADD CONSTRAINT chk_lead_quotation_type
            CHECK (quotation_type IN ('BUDGETARY', 'DETAILED', 'CONTRACT_BOQ'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_lead_quotation_parent'
    ) THEN
        ALTER TABLE lead_quotations
            ADD CONSTRAINT fk_lead_quotation_parent
            FOREIGN KEY (parent_quotation_id) REFERENCES lead_quotations(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lead_quotations_type_status
    ON lead_quotations(quotation_type, status)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_lead_quotations_parent
    ON lead_quotations(parent_quotation_id)
    WHERE parent_quotation_id IS NOT NULL;

COMMENT ON COLUMN lead_quotations.quotation_type IS
    'Sales-stage discriminator. BUDGETARY = lead enquiry (tier ranges, no '
    'total). DETAILED = post-site-visit estimate (area × rate range, '
    'add-ons). CONTRACT_BOQ = signed-contract artefact (exact qty × rate, '
    'payment terms). Existing rows are migrated as DETAILED.';

COMMENT ON COLUMN lead_quotations.parent_quotation_id IS
    'When a BUDGETARY quote is promoted to DETAILED (or DETAILED to '
    'CONTRACT_BOQ), the new row points back to its predecessor. Powers the '
    '"this lead has been through 3 versions" timeline on the lead screen.';

-- --- 2. Pricing tier + tier-aware rate range -------------------------------

ALTER TABLE lead_quotations
    ADD COLUMN IF NOT EXISTS tier VARCHAR(20),
    ADD COLUMN IF NOT EXISTS rate_per_sqft_min NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS rate_per_sqft_max NUMERIC(12, 2);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_lead_quotation_tier'
    ) THEN
        ALTER TABLE lead_quotations
            ADD CONSTRAINT chk_lead_quotation_tier
            CHECK (tier IS NULL OR tier IN ('ECONOMY', 'STANDARD', 'PREMIUM'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_lead_quotation_rate_range'
    ) THEN
        ALTER TABLE lead_quotations
            ADD CONSTRAINT chk_lead_quotation_rate_range
            CHECK (
                rate_per_sqft_min IS NULL
                OR rate_per_sqft_max IS NULL
                OR rate_per_sqft_min <= rate_per_sqft_max
            );
    END IF;
END $$;

COMMENT ON COLUMN lead_quotations.tier IS
    'Finish tier — drives the 3-card customer choice (Economy / Standard / '
    'Premium) on the budgetary PDF. NULL for legacy DETAILED rows that '
    'predate tiering.';

COMMENT ON COLUMN lead_quotations.rate_per_sqft_min IS
    'Lower bound of the per-sqft rate range. Used by BUDGETARY/DETAILED '
    'PDFs to render "₹1,950–2,150/sqft" instead of a single number, which '
    'sets honest expectations before the BOQ is locked.';

COMMENT ON COLUMN lead_quotations.rate_per_sqft_max IS
    'Upper bound of the per-sqft rate range — see rate_per_sqft_min.';

-- --- 3. Estimated area + duration ranges ----------------------------------

ALTER TABLE lead_quotations
    ADD COLUMN IF NOT EXISTS estimated_area_min NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS estimated_area_max NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS duration_months_min INTEGER,
    ADD COLUMN IF NOT EXISTS duration_months_max INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_lead_quotation_area_range'
    ) THEN
        ALTER TABLE lead_quotations
            ADD CONSTRAINT chk_lead_quotation_area_range
            CHECK (
                estimated_area_min IS NULL
                OR estimated_area_max IS NULL
                OR estimated_area_min <= estimated_area_max
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_lead_quotation_duration_range'
    ) THEN
        ALTER TABLE lead_quotations
            ADD CONSTRAINT chk_lead_quotation_duration_range
            CHECK (
                duration_months_min IS NULL
                OR duration_months_max IS NULL
                OR duration_months_min <= duration_months_max
            );
    END IF;
END $$;

COMMENT ON COLUMN lead_quotations.estimated_area_min IS
    'Lower bound of estimated built-up area in sqft for DETAILED stage. '
    'Lets the customer-facing PDF say "1,800–2,000 sqft" before the plan '
    'is finalised.';

COMMENT ON COLUMN lead_quotations.duration_months_min IS
    'Lower bound of estimated construction duration in months. Surfaced on '
    'all three PDF stages with the standard Kerala monsoon-clause caveat.';

-- --- 4. Validity as an absolute date + show-total flag --------------------

ALTER TABLE lead_quotations
    ADD COLUMN IF NOT EXISTS valid_until DATE,
    ADD COLUMN IF NOT EXISTS show_grand_total BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill valid_until from the legacy validity_days + sent_at/created_at.
-- Existing rows: derive an absolute date so the new countdown banner has
-- something to render. validity_days stays around as a UI-default source
-- but is no longer the source of truth for the "valid till" line.
UPDATE lead_quotations
SET valid_until = (COALESCE(sent_at, created_at)::date
                   + COALESCE(validity_days, 30) * INTERVAL '1 day')::date
WHERE valid_until IS NULL;

-- Legacy DETAILED rows kept their grand-total semantics, so flip the flag
-- on. New BUDGETARY rows will default to FALSE (the safe, customer-friendly
-- default established by the redesign).
UPDATE lead_quotations SET show_grand_total = TRUE
WHERE quotation_type = 'DETAILED';

COMMENT ON COLUMN lead_quotations.valid_until IS
    'Absolute expiry date. Replaces validity_days as the source of truth — '
    'customers need to see "locked till 04 May 2026", not "30 days from '
    'when?". The validity_days column is preserved for now as a UI default '
    'feeder.';

COMMENT ON COLUMN lead_quotations.show_grand_total IS
    'When FALSE, the rendered PDF must suppress every grand-total figure. '
    'The new BUDGETARY stage defaults to FALSE because committing to a '
    'number before the area is fixed mis-sells the project. CONTRACT_BOQ '
    'always sets this TRUE.';

-- --- 5. Public view token (tracked link for WhatsApp share) ---------------

ALTER TABLE lead_quotations
    ADD COLUMN IF NOT EXISTS public_view_token UUID;

CREATE UNIQUE INDEX IF NOT EXISTS idx_lead_quotations_public_token
    ON lead_quotations(public_view_token)
    WHERE public_view_token IS NOT NULL;

COMMENT ON COLUMN lead_quotations.public_view_token IS
    'Random UUID for the customer-facing tracked link '
    '(/public/quotations/{token}). NULL until "Send" is clicked. Hits on '
    'the public endpoint are appended to a view-log table (later migration), '
    'so staff finally know whether the customer opened the PDF.';

-- --- 6. Inclusions ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS quotation_inclusions (
    id              BIGSERIAL PRIMARY KEY,
    quotation_id    BIGINT       NOT NULL,
    display_order   INTEGER      NOT NULL,
    category        VARCHAR(50),
    text            TEXT         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_quotation_inclusion_quotation
        FOREIGN KEY (quotation_id) REFERENCES lead_quotations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_quotation_inclusions_quotation
    ON quotation_inclusions(quotation_id, display_order);

COMMENT ON TABLE quotation_inclusions IS
    'Structured "what is included" rows. Replaces the free-text description '
    'paragraph that used to bury inclusion details — that ambiguity was the '
    'root of every Walldot scope dispute.';

-- --- 7. Exclusions ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS quotation_exclusions (
    id                    BIGSERIAL PRIMARY KEY,
    quotation_id          BIGINT       NOT NULL,
    display_order         INTEGER      NOT NULL,
    text                  TEXT         NOT NULL,
    cost_implication_note TEXT,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_quotation_exclusion_quotation
        FOREIGN KEY (quotation_id) REFERENCES lead_quotations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_quotation_exclusions_quotation
    ON quotation_exclusions(quotation_id, display_order);

COMMENT ON TABLE quotation_exclusions IS
    'Things explicitly NOT covered (compound wall, borewell, earth filling…) '
    'with optional cost-implication notes ("estimate ₹40k–60k extra"). '
    'Pre-empting these in writing has been shown to raise close rates — the '
    'opposite of what staff intuit.';

COMMENT ON COLUMN quotation_exclusions.cost_implication_note IS
    'Optional honest range for what the excluded item is likely to cost the '
    'customer separately. Builds trust by signalling we are not hiding it.';

-- --- 8. Assumptions --------------------------------------------------------

CREATE TABLE IF NOT EXISTS quotation_assumptions (
    id              BIGSERIAL PRIMARY KEY,
    quotation_id    BIGINT       NOT NULL,
    display_order   INTEGER      NOT NULL,
    text            TEXT         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_quotation_assumption_quotation
        FOREIGN KEY (quotation_id) REFERENCES lead_quotations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_quotation_assumptions_quotation
    ON quotation_assumptions(quotation_id, display_order);

COMMENT ON TABLE quotation_assumptions IS
    'Site / customer-side preconditions assumed by the quote (plot levelled, '
    'road access, single-phase electricity at site, customer supplies water…). '
    'Surfaced on every PDF and signed-off on the contract.';

-- --- 9. Payment milestones -------------------------------------------------

CREATE TABLE IF NOT EXISTS quotation_payment_milestones (
    id                BIGSERIAL PRIMARY KEY,
    quotation_id      BIGINT          NOT NULL,
    milestone_number  INTEGER         NOT NULL,
    trigger_event     VARCHAR(120)    NOT NULL,
    percentage        NUMERIC(5, 2)   NOT NULL,
    amount            NUMERIC(12, 2),
    notes             TEXT,
    created_at        TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_quotation_milestone_quotation
        FOREIGN KEY (quotation_id) REFERENCES lead_quotations(id) ON DELETE CASCADE,
    CONSTRAINT chk_quotation_milestone_pct CHECK (percentage > 0 AND percentage <= 100),
    CONSTRAINT uq_quotation_milestone_number UNIQUE (quotation_id, milestone_number)
);

CREATE INDEX IF NOT EXISTS idx_quotation_milestones_quotation
    ON quotation_payment_milestones(quotation_id, milestone_number);

COMMENT ON TABLE quotation_payment_milestones IS
    'Stage-linked payment schedule (Kerala default: 8 stages — Booking 10%, '
    'Foundation 15%, Plinth 15%, Walls 15%, Slab 15%, Plaster 10%, Flooring '
    '10%, Handover 10%). amount is NULL for BUDGETARY rows where the total '
    'is intentionally unknown; populated for DETAILED ranges and CONTRACT_BOQ '
    'fixed amounts.';

COMMENT ON COLUMN quotation_payment_milestones.percentage IS
    'Milestone share of contract value. Sum across all milestones for one '
    'quotation should equal 100, but this is enforced at the service layer '
    '(not as a CHECK) because intermediate edits would otherwise fail.';
