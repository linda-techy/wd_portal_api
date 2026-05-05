-- S2 PR1: CPM denormalized columns on tasks.
-- Populated by CpmService.recompute(projectId); first-run trigger
-- (CpmInitialPopulator) backfills existing projects on app boot.

ALTER TABLE tasks
    ADD COLUMN actual_start_date DATE,
    ADD COLUMN es_date           DATE,
    ADD COLUMN ef_date           DATE,
    ADD COLUMN ls_date           DATE,
    ADD COLUMN lf_date           DATE,
    ADD COLUMN total_float_days  INT,
    ADD COLUMN is_critical       BOOLEAN NOT NULL DEFAULT FALSE;
