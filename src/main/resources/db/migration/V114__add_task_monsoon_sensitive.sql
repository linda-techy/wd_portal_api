-- V114: add monsoon_sensitive boolean to tasks. Used by S1 monsoon warnings
-- (warningsFor() implementation lands in PR2). Default false so existing
-- rows are unchanged.
ALTER TABLE tasks
    ADD COLUMN monsoon_sensitive BOOLEAN NOT NULL DEFAULT FALSE;
