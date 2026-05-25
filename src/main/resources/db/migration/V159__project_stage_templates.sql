-- V159: Per-project payment-stage template (audit P2-4).
--
-- Problem:
--   Payment stage configuration (name + percentage per stage) was passed ad-hoc
--   at BOQ approval time, with no agreed plan stored at project setup. Sales and
--   finance had no visibility into the stage split until the moment of approval.
--
-- Change:
--   1. Add project_stage_templates table: one row per stage per project, ordered
--      by stage_number. Percentages (stored as fractions, e.g. 0.10 = 10%) MUST
--      sum to 1.0 within a project — enforced by the application layer.
--   2. Seed the default Kerala 6-stage template for every existing project so
--      existing projects get a working template immediately.
--   3. recordCustomerApproval now falls back to the stored template when no
--      explicit stages are passed — the explicit-override path still works.

CREATE TABLE project_stage_templates (
    id               BIGSERIAL PRIMARY KEY,
    project_id       BIGINT NOT NULL REFERENCES customer_projects(id) ON DELETE CASCADE,
    stage_number     INT    NOT NULL CHECK (stage_number >= 1),
    name             VARCHAR(100) NOT NULL,
    percentage       NUMERIC(6,4) NOT NULL CHECK (percentage > 0 AND percentage <= 1),
    milestone_description TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (project_id, stage_number)
);

CREATE INDEX idx_project_stage_templates_project ON project_stage_templates(project_id);

-- Seed the Kerala 6-stage default for all existing (non-deleted) projects.
INSERT INTO project_stage_templates
    (project_id, stage_number, name, percentage, milestone_description)
SELECT
    id,
    stage_number,
    stage_name,
    stage_pct,
    stage_desc
FROM customer_projects
CROSS JOIN (VALUES
    (1, 'Mobilisation / Advance', 0.1000, 'Advance payment on contract signing'),
    (2, 'Foundation',             0.2000, 'On completion of foundation work'),
    (3, 'Structure / Slab',       0.2500, 'On completion of structural slab'),
    (4, 'Brickwork / Roofing',    0.2000, 'On completion of brickwork and roofing'),
    (5, 'Finishing / Interiors',  0.1500, 'On completion of finishing and interiors'),
    (6, 'Handover',               0.1000, 'Final payment on project handover')
) AS t(stage_number, stage_name, stage_pct, stage_desc)
WHERE deleted_at IS NULL;
