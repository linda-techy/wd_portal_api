-- ============================================================================
-- V161: Drop legacy NOT NULL on boq_items.work_type_id
-- ============================================================================
-- Work type is OPTIONAL on a BOQ line: CreateBoqItemRequest.workTypeId is
-- nullable, BoqItem.workType maps @JoinColumn("work_type_id") with no
-- nullable=false, and BoqService.createBoqItem only links a work type when one
-- is supplied. A legacy NOT NULL constraint on boq_items.work_type_id (predating
-- the optional-work-type model, and not covered by V15's created_by_id fix)
-- makes every API-created BOQ item without a work type fail with:
--   ERROR: null value in column "work_type_id" of relation "boq_items"
--          violates not-null constraint
-- The Demo Villa BOQ was seeded via SQL (V73), so this API insert path went
-- unexercised until now.
--
-- PostgreSQL 14 has no ALTER COLUMN IF EXISTS, so use a DO block that only
-- drops NOT NULL when the column exists and is currently NOT NULL (idempotent;
-- mirrors V15's approach).
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'boq_items'
          AND column_name = 'work_type_id'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE boq_items ALTER COLUMN work_type_id DROP NOT NULL;
    END IF;
END $$;
