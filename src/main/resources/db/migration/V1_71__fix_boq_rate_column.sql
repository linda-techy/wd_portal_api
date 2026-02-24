-- ============================================================================
-- V1_71: Fix BOQ rate column issues
-- ============================================================================
-- Handles potential rate vs unit_rate column mismatch
-- ============================================================================

-- Check if 'rate' column exists and remove its NOT NULL constraint or rename it
DO $$
BEGIN
    -- If 'rate' column exists and 'unit_rate' also exists, drop 'rate'
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'boq_items' AND column_name = 'rate') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'boq_items' AND column_name = 'unit_rate') THEN
        -- Both columns exist, drop the old 'rate' column
        ALTER TABLE boq_items DROP COLUMN rate;
        RAISE NOTICE 'Dropped redundant rate column';
    
    -- If only 'rate' exists, rename it to 'unit_rate'
    ELSIF EXISTS (SELECT 1 FROM information_schema.columns 
                  WHERE table_name = 'boq_items' AND column_name = 'rate')
          AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                          WHERE table_name = 'boq_items' AND column_name = 'unit_rate') THEN
        ALTER TABLE boq_items RENAME COLUMN rate TO unit_rate;
        RAISE NOTICE 'Renamed rate column to unit_rate';
    
    -- If only 'unit_rate' exists, all good
    ELSE
        RAISE NOTICE 'Column unit_rate already exists correctly';
    END IF;
    
    -- Ensure unit_rate allows null (entity default handles it)
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'boq_items' 
               AND column_name = 'unit_rate' 
               AND is_nullable = 'NO') THEN
        ALTER TABLE boq_items ALTER COLUMN unit_rate DROP NOT NULL;
        RAISE NOTICE 'Removed NOT NULL constraint from unit_rate';
    END IF;
END
$$;
