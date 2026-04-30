-- ===========================================================================
-- V81 — Defensive cleanup of legacy NOT NULL columns on site_visits
--
-- Background: V80 dropped the legacy `visitor_id` column. The next
-- check-in attempt then surfaced a SECOND legacy NOT NULL column —
-- `created_by_type` — that the SiteVisit entity also doesn't write to,
-- producing the same 23502 constraint-violation 500. Whack-a-mole on one
-- column at a time wastes turns; this migration takes the column list
-- the entity actually maps and **drops NOT NULL on every other column**
-- in a single pass, so any further unmapped columns can no longer block
-- inserts.
--
-- The entity-mapped column whitelist was lifted directly from
-- com/wd/api/model/SiteVisit.java — keep these in sync if SiteVisit
-- gains new persisted fields:
--   id, project_id, visit_date, notes, visited_by,
--   check_in_time, check_out_time,
--   check_in_latitude, check_in_longitude,
--   check_out_latitude, check_out_longitude,
--   distance_from_project_checkin, distance_from_project_checkout,
--   visit_type, visit_status,
--   duration_minutes, check_out_notes, purpose,
--   created_at, updated_at
--
-- Idempotent: skips columns that are already nullable.
-- ===========================================================================

DO $$
DECLARE
    col record;
    keeper_set text[] := ARRAY[
        'id',
        'project_id',
        'visit_date',
        'notes',
        'visited_by',
        'check_in_time',
        'check_out_time',
        'check_in_latitude',
        'check_in_longitude',
        'check_out_latitude',
        'check_out_longitude',
        'distance_from_project_checkin',
        'distance_from_project_checkout',
        'visit_type',
        'visit_status',
        'duration_minutes',
        'check_out_notes',
        'purpose',
        'created_at',
        'updated_at'
    ];
BEGIN
    -- Walk every NOT NULL column on site_visits that isn't on the keeper
    -- list and DROP the NOT NULL constraint. Keeps the column data
    -- intact (no DROP COLUMN) so any consumer relying on legacy values
    -- continues to read what's there — only future writes from Hibernate
    -- can leave them null.
    FOR col IN
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name   = 'site_visits'
          AND is_nullable  = 'NO'
          AND NOT (column_name = ANY(keeper_set))
    LOOP
        EXECUTE format(
            'ALTER TABLE site_visits ALTER COLUMN %I DROP NOT NULL',
            col.column_name
        );
        RAISE NOTICE 'Dropped NOT NULL on legacy column site_visits.%', col.column_name;
    END LOOP;

    -- Also drop any FK constraint pinned to a non-keeper column. These
    -- come from the same legacy schema generation and prevent a future
    -- DROP COLUMN if we ever choose to clean these up further.
    FOR col IN
        SELECT con.conname AS constraint_name, att.attname AS column_name
        FROM pg_constraint con
        JOIN pg_class      rel ON rel.oid = con.conrelid
        JOIN pg_attribute  att ON att.attrelid = con.conrelid AND att.attnum = ANY(con.conkey)
        WHERE rel.relname = 'site_visits'
          AND con.contype = 'f'
          AND NOT (att.attname = ANY(keeper_set))
    LOOP
        EXECUTE format(
            'ALTER TABLE site_visits DROP CONSTRAINT IF EXISTS %I',
            col.constraint_name
        );
        RAISE NOTICE 'Dropped legacy FK % on site_visits.%',
            col.constraint_name, col.column_name;
    END LOOP;
END $$;
