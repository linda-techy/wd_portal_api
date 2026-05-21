-- Realign gallery_images date fields for rows backfilled from site reports.
--
-- Background: the V148 fix unblocked GalleryService.createImagesFromSiteReport,
-- and the admin backfill endpoint then created 18 gallery rows from
-- previously-orphaned site report photos. Those rows had created_at /
-- uploaded_at / taken_date set to "now" (the @PrePersist defaults), so the
-- portal gallery — which sorts/groups by created_at — showed every backfilled
-- photo as "today" regardless of when the underlying site report was filed.
--
-- This migration anchors every date field to the source site report:
--   1. reportDate if set (the field operator's intended capture date)
--   2. otherwise the site report's createdAt (when staff submitted it)
--   3. otherwise leave as-is (no source signal available)
--
-- Idempotent — running again sets the same values. Limited to rows linked
-- to a site report; manually-uploaded gallery rows are untouched.

UPDATE gallery_images gi
SET
    created_at  = COALESCE(sr.report_date, sr.created_at, gi.created_at),
    uploaded_at = COALESCE(sr.report_date, sr.created_at, gi.uploaded_at),
    taken_date  = COALESCE(DATE(sr.report_date), DATE(sr.created_at), gi.taken_date)
FROM site_reports sr
WHERE gi.site_report_id = sr.id;
