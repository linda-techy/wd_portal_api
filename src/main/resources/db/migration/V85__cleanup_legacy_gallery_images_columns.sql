-- ===========================================================================
-- V85 — Defensive cleanup of legacy NOT NULL columns on gallery_images
--
-- Symptom: every site-report submission was silently failing the
-- post-save "auto-sync to gallery" step with:
--   null value in column "uploaded_by_type" of relation "gallery_images"
--   violates not-null constraint
-- The site report itself saved fine (V84.5 split tx boundaries so the
-- gallery failure no longer kills the parent), but the report's photos
-- never reached the gallery — the customer-side gallery view still showed
-- nothing.
--
-- Root cause: same pattern as site_visits.visitor_id (V80/V81) — an old
-- Hibernate auto-DDL generation left a {@code uploaded_by_type} column
-- on gallery_images with NOT NULL, and the current GalleryImage entity
-- doesn't map it. Likely candidates for legacy ghosts: uploaded_by_type,
-- uploaded_by_id wrong target, plus any other fields the entity dropped.
--
-- This migration takes the same defensive approach as V81 — drop NOT
-- NULL on every column that isn't on a whitelist matching the current
-- entity. Safer than whack-a-mole; future contributor adding a new
-- entity field will hit the keeper-set sync as a clear DDL signal.
-- ===========================================================================

DO $$
DECLARE
    col record;
    keeper_set text[] := ARRAY[
        'id',
        'project_id',
        'site_report_id',
        'image_url',
        'image_path',
        'thumbnail_path',
        'caption',
        'location_tag',
        'tags',
        'taken_date',
        'uploaded_at',
        'uploaded_by_id',
        'created_at'
    ];
BEGIN
    FOR col IN
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name   = 'gallery_images'
          AND is_nullable  = 'NO'
          AND NOT (column_name = ANY(keeper_set))
    LOOP
        EXECUTE format(
            'ALTER TABLE gallery_images ALTER COLUMN %I DROP NOT NULL',
            col.column_name
        );
        RAISE NOTICE 'Dropped NOT NULL on legacy column gallery_images.%', col.column_name;
    END LOOP;
END $$;
