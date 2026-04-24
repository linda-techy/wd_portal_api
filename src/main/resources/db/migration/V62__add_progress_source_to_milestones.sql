-- V62__add_progress_source_to_milestones.sql
-- Distinguishes COMPUTED (rollup of child task progress) from MANUAL
-- (PM overrode the milestone progress directly). The customer Timeline
-- endpoint shows the milestone's effective progress regardless of source;
-- the auto-recompute on PATCH /tasks/{id}/progress only fires when source
-- is COMPUTED.

ALTER TABLE project_milestones
    ADD COLUMN IF NOT EXISTS progress_source VARCHAR(16) NOT NULL DEFAULT 'COMPUTED';

ALTER TABLE project_milestones
    ADD CONSTRAINT chk_progress_source CHECK (progress_source IN ('COMPUTED', 'MANUAL'));
