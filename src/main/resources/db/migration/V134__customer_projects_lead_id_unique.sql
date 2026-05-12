-- G-05: Prevent duplicate project creation from concurrent lead-conversion calls.
--
-- LeadService.convertLead performs a check-then-create against
-- customer_projects.lead_id without isolation guarantees, so two simultaneous
-- requests could each pass the existence check and both insert a project for
-- the same lead. A partial unique index makes the database the source of
-- truth: only one non-deleted project may reference a given lead.
--
-- Partial predicate scope:
--   * lead_id IS NOT NULL — leads converted from the system always carry this.
--     Manually-created projects (no lead) remain unconstrained.
--   * deleted_at IS NULL — preserves history; a project that was soft-deleted
--     does not block re-conversion of the same lead.

CREATE UNIQUE INDEX IF NOT EXISTS ux_customer_projects_lead_id_active
    ON customer_projects (lead_id)
    WHERE lead_id IS NOT NULL AND deleted_at IS NULL;
