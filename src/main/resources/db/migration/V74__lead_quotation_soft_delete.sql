-- ===========================================================================
-- V74 - Lead Quotation soft-delete column
--
-- Earlier audit flagged "hard-delete only" as a UX risk: a misclick deletes
-- a DRAFT and there is no recovery path. Switching to a soft-delete column
-- enables a 5-second Undo snackbar in the Flutter app and keeps the audit
-- trail of "why this draft existed" for compliance.
--
-- Hibernate side wires up @SQLDelete + @Where on the entity so the rest of
-- the codebase keeps working without query changes — every existing
-- `findAll` / `findById` query auto-filters deleted_at IS NULL.
-- ===========================================================================

-- Idempotent: Hibernate auto-DDL may have already added this column on
-- a prior app boot once the entity field was declared. IF NOT EXISTS
-- lets the migration re-run cleanly against either state.
ALTER TABLE lead_quotations
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Partial index — only non-deleted rows participate in normal lookups.
-- Cheap and a no-op while no rows are deleted.
CREATE INDEX IF NOT EXISTS idx_lead_quotations_active
    ON lead_quotations(id)
    WHERE deleted_at IS NULL;

-- Adjacent-literal continuation (no `||`; expressions aren't valid in COMMENT IS).
COMMENT ON COLUMN lead_quotations.deleted_at IS
    'Soft-delete tombstone. NULL = live row. Set by @SQLDelete on '
    'LeadQuotation; reset to NULL by LeadQuotationService.restoreQuotation '
    'within the Undo window (Flutter snackbar, 5s).';
