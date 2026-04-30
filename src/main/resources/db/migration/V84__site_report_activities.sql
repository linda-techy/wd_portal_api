-- ===========================================================================
-- V84 — Site report activities (per-activity manpower) + GalleryImage
--        cascade hardening
--
-- Why now: the existing site_reports.manpower_deployed is a single int
-- — fine for "8 men on site today," useless when three activities run
-- simultaneously (RCC pour 8, plastering 4, electrical 2). This
-- migration adds a child table so each report can carry N activities,
-- each with its own manpower count, optional equipment, and notes. The
-- legacy flat manpower_deployed column stays for back-compat; new
-- reports populate the activities table and the customer app sums it
-- up for a single-number display.
--
-- Also closes a data-integrity gap: gallery_images.site_report_id has
-- no ON DELETE rule, so deleting a SiteReport (or letting a soft-delete
-- propagate via cascade) leaves orphan FK pointers. Switching to
-- ON DELETE SET NULL keeps the gallery row visible (the photo is still
-- valuable) without dangling references.
--
-- Idempotent: every step uses IF NOT EXISTS / guarded DO-block.
-- ===========================================================================

CREATE TABLE IF NOT EXISTS site_report_activities (
    id              BIGSERIAL PRIMARY KEY,
    report_id       BIGINT       NOT NULL,
    name            VARCHAR(150) NOT NULL,
    manpower        INTEGER,
    equipment       TEXT,
    notes           TEXT,
    display_order   INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_site_report_activities_report
        FOREIGN KEY (report_id) REFERENCES site_reports(id) ON DELETE CASCADE,
    CONSTRAINT chk_site_report_activities_manpower CHECK (manpower IS NULL OR manpower >= 0)
);

CREATE INDEX IF NOT EXISTS idx_site_report_activities_report_order
    ON site_report_activities(report_id, display_order);

COMMENT ON TABLE site_report_activities IS
    'Per-activity manpower breakdown for a SiteReport (V84). Multiple '
    'rows per report — RCC pour, plastering, MEP, etc. The legacy '
    'site_reports.manpower_deployed flat column is kept for back-compat '
    'but new reports populate this table.';

-- --- gallery_images.site_report_id: ON DELETE SET NULL --------------------

DO $$
DECLARE
    fk_name text;
BEGIN
    -- Drop any pre-existing FK on the column (auto-named by Hibernate or
    -- a previous migration) so we can replace it with the SET NULL variant.
    FOR fk_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class      rel ON rel.oid = con.conrelid
        JOIN pg_attribute  att ON att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey)
        WHERE rel.relname = 'gallery_images'
          AND att.attname = 'site_report_id'
          AND con.contype = 'f'
    LOOP
        EXECUTE format('ALTER TABLE gallery_images DROP CONSTRAINT %I', fk_name);
    END LOOP;

    -- Re-add with ON DELETE SET NULL so the gallery row survives the
    -- deletion of the parent site report (the underlying file is still
    -- valuable to customers; only the back-pointer is invalidated).
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_gallery_images_site_report_set_null'
    ) THEN
        ALTER TABLE gallery_images
            ADD CONSTRAINT fk_gallery_images_site_report_set_null
            FOREIGN KEY (site_report_id) REFERENCES site_reports(id) ON DELETE SET NULL;
    END IF;
END $$;
