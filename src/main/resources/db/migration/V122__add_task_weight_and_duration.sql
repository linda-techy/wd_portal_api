-- S3 PR1 — Weighted Progress
-- Adds two nullable INT columns to tasks:
--   weight        — scheduler override; null = "use duration_days"
--   duration_days — planned working-day duration
-- Both nullable; existing rows stay null. TaskDurationBackfiller
-- populates duration_days for live tasks at boot.
ALTER TABLE tasks
  ADD COLUMN weight        INT NULL,
  ADD COLUMN duration_days INT NULL;
