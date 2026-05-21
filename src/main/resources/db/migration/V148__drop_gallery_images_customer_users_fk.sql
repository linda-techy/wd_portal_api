-- Drop the spurious gallery_images.uploaded_by_id → customer_users FK.
--
-- Background: gallery_images.uploaded_by_id had TWO foreign-key constraints
-- pointing at different tables:
--   * fk1gsnl3r27gk7xkgvmxfj2wnwl → portal_users(id)   (correct for portal API)
--   * fkqqocbhr47a74k0o25ko3l8asf → customer_users(id) (left over from a
--     historical customer-api boot when its GalleryImage entity ran with
--     spring.jpa.hibernate.ddl-auto=update)
--
-- The two constraints contradict: a portal user id satisfies the first but
-- fails the second, and vice-versa, so EVERY insert into gallery_images
-- failed with a FK violation. Symptom: site-report photos never synced to
-- the gallery (portal SiteReportService.createReport caught the exception
-- and logged GALLERY_SYNC_FAILED, then both portal + customer galleries
-- showed an empty state).
--
-- Both APIs now run with ddl-auto=validate, so dropping the constraint
-- here is durable — neither app will recreate it on startup.
--
-- We deliberately keep the portal_users FK. The portal API is the primary
-- writer (auto-sync from site reports + staff uploads via GalleryService).
-- If/when the customer API gains a direct upload path, it should write
-- through the portal API or this column will need to become a soft
-- reference (drop the remaining FK + lean on uploaded_by_type discriminator).

ALTER TABLE gallery_images
    DROP CONSTRAINT IF EXISTS fkqqocbhr47a74k0o25ko3l8asf;
