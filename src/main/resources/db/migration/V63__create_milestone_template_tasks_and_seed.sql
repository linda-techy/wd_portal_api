-- V63__create_milestone_template_tasks_and_seed.sql
-- Adds milestone_template_tasks table (default tasks per milestone in
-- each project-type template) and seeds the four canonical templates:
-- SINGLE_FLOOR, G_PLUS_N, COMMERCIAL, INTERIOR.

-- (1) Add code column on project_type_templates if not present
ALTER TABLE project_type_templates
    ADD COLUMN IF NOT EXISTS code VARCHAR(32);

CREATE UNIQUE INDEX IF NOT EXISTS idx_project_type_templates_code
    ON project_type_templates(code) WHERE code IS NOT NULL;

-- (2) New table for default tasks per milestone-template
CREATE TABLE IF NOT EXISTS milestone_template_tasks (
    id              BIGSERIAL PRIMARY KEY,
    milestone_template_id BIGINT NOT NULL
        REFERENCES milestone_templates(id) ON DELETE CASCADE,
    task_name       VARCHAR(255) NOT NULL,
    task_order      INTEGER NOT NULL,
    estimated_days  INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (milestone_template_id, task_order)
);

CREATE INDEX IF NOT EXISTS idx_milestone_template_tasks_template
    ON milestone_template_tasks(milestone_template_id);

-- (3) Seed the four templates (idempotent via ON CONFLICT)
-- Each template's milestones default_percentage MUST sum to 100.

-- ============================================================
-- SINGLE_FLOOR (12 milestones; weights sum 100)
-- ============================================================
INSERT INTO project_type_templates (project_type, code, description, created_at)
VALUES ('Single Floor House', 'SINGLE_FLOOR', 'Independent ground-floor home', NOW())
ON CONFLICT (code) WHERE code IS NOT NULL DO NOTHING;

WITH t AS (
  SELECT id FROM project_type_templates WHERE code = 'SINGLE_FLOOR'
)
INSERT INTO milestone_templates (template_id, milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days, created_at)
SELECT t.id, m.name, m.ord, m.pct, m.descr, m.phase, m.dur, NOW()
FROM t, (VALUES
    ('Site Preparation',      1,  5.0::numeric, 'Site clearing, layout, marking',                    'PLANNING',   7),
    ('Foundation',            2, 12.0::numeric, 'Excavation, footing, RCC base',                     'EXECUTION', 21),
    ('Plinth',                3,  6.0::numeric, 'Plinth beam, plinth filling, DPC',                  'EXECUTION', 10),
    ('Walls (Brick Masonry)', 4, 10.0::numeric, 'Brick masonry up to lintel and roof level',         'EXECUTION', 25),
    ('Roof Slab',             5, 12.0::numeric, 'Roof RCC slab casting + curing',                    'EXECUTION', 18),
    ('Electrical Rough-In',   6,  6.0::numeric, 'Conduits, junction boxes, main wiring',             'EXECUTION', 10),
    ('Plumbing Rough-In',     7,  6.0::numeric, 'Drainage, water lines, sanitary stub-outs',         'EXECUTION', 10),
    ('Plastering',            8,  9.0::numeric, 'Internal & external wall plaster, ceiling plaster', 'EXECUTION', 14),
    ('Flooring',              9,  9.0::numeric, 'Tile/marble laying, skirting',                      'EXECUTION', 14),
    ('Painting',             10,  8.0::numeric, 'Putty, primer, two coats',                          'COMPLETION',12),
    ('Fixtures & Fittings',  11,  9.0::numeric, 'Sanitary fixtures, switches, doors, hardware',      'COMPLETION',12),
    ('Handover',             12,  8.0::numeric, 'Snag list, cleanup, final walkthrough',             'HANDOVER',   5)
) AS m(name, ord, pct, descr, phase, dur)
ON CONFLICT (template_id, milestone_order) DO NOTHING;

INSERT INTO milestone_template_tasks (milestone_template_id, task_name, task_order, estimated_days, created_at)
SELECT mt.id, t.name, t.ord, t.dur, NOW()
FROM milestone_templates mt
JOIN project_type_templates ptt ON mt.template_id = ptt.id AND ptt.code = 'SINGLE_FLOOR'
JOIN (VALUES
    ('Site Preparation',      'Site clearing and grading',           1,  3),
    ('Site Preparation',      'Layout marking and benchmark',        2,  2),
    ('Site Preparation',      'Boundary fence + temp utilities',     3,  2),
    ('Foundation',            'Excavation for footings',             1,  5),
    ('Foundation',            'PCC bed and reinforcement',           2,  4),
    ('Foundation',            'Concrete pour and curing',            3,  7),
    ('Foundation',            'Backfilling',                         4,  3),
    ('Plinth',                'Plinth beam reinforcement',           1,  3),
    ('Plinth',                'Plinth concrete pour',                2,  2),
    ('Plinth',                'DPC and plinth filling',              3,  3),
    ('Walls (Brick Masonry)', 'Brick masonry up to lintel level',    1, 10),
    ('Walls (Brick Masonry)', 'Lintel band casting',                 2,  3),
    ('Walls (Brick Masonry)', 'Brick masonry up to roof',            3, 10),
    ('Roof Slab',             'Centering and shuttering',            1,  4),
    ('Roof Slab',             'Reinforcement laying',                2,  4),
    ('Roof Slab',             'RCC pour',                            3,  2),
    ('Roof Slab',             'Curing (min 14 days)',                4, 14),
    ('Electrical Rough-In',   'Wall chase cutting',                  1,  2),
    ('Electrical Rough-In',   'Conduit and junction boxes',          2,  4),
    ('Electrical Rough-In',   'Main panel + earthing',               3,  3),
    ('Plumbing Rough-In',     'Drainage line laying',                1,  3),
    ('Plumbing Rough-In',     'Water supply lines',                  2,  3),
    ('Plumbing Rough-In',     'Sanitary stub-outs',                  3,  2),
    ('Plastering',            'Internal wall plastering',            1,  6),
    ('Plastering',            'Ceiling plastering',                  2,  4),
    ('Plastering',            'External wall plastering',            3,  4),
    ('Flooring',              'Sub-floor preparation',               1,  3),
    ('Flooring',              'Tile/marble laying',                  2,  8),
    ('Flooring',              'Skirting and grouting',               3,  3),
    ('Painting',              'Putty work',                          1,  3),
    ('Painting',              'Primer coat',                         2,  2),
    ('Painting',              'Top coat (2 coats)',                  3,  5),
    ('Fixtures & Fittings',   'Door and window installation',        1,  4),
    ('Fixtures & Fittings',   'Sanitary fixtures',                   2,  3),
    ('Fixtures & Fittings',   'Electrical switches and fittings',    3,  3),
    ('Handover',              'Snag list walkthrough',               1,  2),
    ('Handover',              'Defect rectification',                2,  2),
    ('Handover',              'Final cleanup and key handover',      3,  1)
) AS t(milestone_name, name, ord, dur)
  ON mt.milestone_name = t.milestone_name
ON CONFLICT (milestone_template_id, task_order) DO NOTHING;

-- ============================================================
-- G_PLUS_N (per-floor pattern; weights sum 100)
-- ============================================================
INSERT INTO project_type_templates (project_type, code, description, created_at)
VALUES ('Multi-Floor House (G+N)', 'G_PLUS_N', 'Ground + N floors; per-floor milestones cloned at apply-time', NOW())
ON CONFLICT (code) WHERE code IS NOT NULL DO NOTHING;

WITH t AS (
  SELECT id FROM project_type_templates WHERE code = 'G_PLUS_N'
)
INSERT INTO milestone_templates (template_id, milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days, created_at)
SELECT t.id, m.name, m.ord, m.pct, m.descr, m.phase, m.dur, NOW()
FROM t, (VALUES
    ('Site Preparation',     1,  4.0::numeric, 'Site clearing, layout',                       'PLANNING',   7),
    ('Foundation',           2, 12.0::numeric, 'Foundation for full footprint',               'EXECUTION', 25),
    ('Plinth',               3,  5.0::numeric, 'Plinth beam, plinth filling',                 'EXECUTION', 10),
    ('Floor Walls',          4, 12.0::numeric, 'Brick masonry per floor (cloned at apply)',   'EXECUTION', 20),
    ('Floor Slab',           5, 14.0::numeric, 'RCC slab per floor (cloned at apply)',        'EXECUTION', 18),
    ('Floor MEP Rough-In',   6,  8.0::numeric, 'Electrical + plumbing rough per floor',       'EXECUTION', 12),
    ('Floor Plastering',     7,  7.0::numeric, 'Plastering per floor',                        'EXECUTION', 12),
    ('Floor Flooring',       8,  7.0::numeric, 'Flooring per floor',                          'EXECUTION', 12),
    ('Roof Treatment',       9,  7.0::numeric, 'Waterproofing, insulation',                   'EXECUTION', 10),
    ('Painting',            10,  8.0::numeric, 'Primer + two coats whole building',           'COMPLETION',15),
    ('Fixtures & Fittings', 11, 10.0::numeric, 'Doors, sanitary, electrical fittings',        'COMPLETION',15),
    ('Handover',            12,  6.0::numeric, 'Snag list, cleanup, walkthrough',             'HANDOVER',   5)
) AS m(name, ord, pct, descr, phase, dur)
ON CONFLICT (template_id, milestone_order) DO NOTHING;

INSERT INTO milestone_template_tasks (milestone_template_id, task_name, task_order, estimated_days, created_at)
SELECT mt.id, t.name, t.ord, t.dur, NOW()
FROM milestone_templates mt
JOIN project_type_templates ptt ON mt.template_id = ptt.id AND ptt.code = 'G_PLUS_N'
JOIN (VALUES
    ('Site Preparation',   'Site clearing and grading',            1, 3),
    ('Site Preparation',   'Layout marking',                       2, 2),
    ('Foundation',         'Excavation',                           1, 7),
    ('Foundation',         'Reinforcement and pour',               2,10),
    ('Foundation',         'Curing and backfill',                  3, 8),
    ('Plinth',             'Plinth beam',                          1, 5),
    ('Plinth',             'Plinth filling and DPC',               2, 5),
    ('Floor Walls',        'Brick masonry (per-floor)',            1,15),
    ('Floor Walls',        'Lintel band',                          2, 5),
    ('Floor Slab',         'Shuttering and reinforcement',         1, 6),
    ('Floor Slab',         'RCC pour and curing',                  2,12),
    ('Floor MEP Rough-In', 'Electrical conduits and panel',        1, 6),
    ('Floor MEP Rough-In', 'Plumbing lines and stub-outs',         2, 6),
    ('Floor Plastering',   'Internal and ceiling plaster',         1, 8),
    ('Floor Plastering',   'External plaster',                     2, 4),
    ('Floor Flooring',     'Sub-floor and tile laying',            1, 8),
    ('Floor Flooring',     'Skirting and grouting',                2, 4),
    ('Roof Treatment',     'Waterproofing membrane',               1, 4),
    ('Roof Treatment',     'Brick coba and tiling',                2, 6),
    ('Painting',           'Putty + primer',                       1, 5),
    ('Painting',           'Top coat (2 coats)',                   2,10),
    ('Fixtures & Fittings','Doors and windows',                    1, 5),
    ('Fixtures & Fittings','Sanitary fixtures',                    2, 5),
    ('Fixtures & Fittings','Electrical fittings',                  3, 5),
    ('Handover',           'Snag walkthrough',                     1, 2),
    ('Handover',           'Defect rectification',                 2, 2),
    ('Handover',           'Final cleanup and handover',           3, 1)
) AS t(milestone_name, name, ord, dur)
  ON mt.milestone_name = t.milestone_name
ON CONFLICT (milestone_template_id, task_order) DO NOTHING;

-- ============================================================
-- COMMERCIAL (10 milestones; weights sum 100)
-- ============================================================
INSERT INTO project_type_templates (project_type, code, description, created_at)
VALUES ('Commercial Building', 'COMMERCIAL', 'Mid-rise commercial with structural frame + facade + heavy MEP', NOW())
ON CONFLICT (code) WHERE code IS NOT NULL DO NOTHING;

WITH t AS (
  SELECT id FROM project_type_templates WHERE code = 'COMMERCIAL'
)
INSERT INTO milestone_templates (template_id, milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days, created_at)
SELECT t.id, m.name, m.ord, m.pct, m.descr, m.phase, m.dur, NOW()
FROM t, (VALUES
    ('Site Preparation', 1,  3.0::numeric, 'Site clearing, layout',                    'PLANNING',  10),
    ('Foundation',       2, 14.0::numeric, 'Pile/raft foundation',                     'EXECUTION', 35),
    ('Structural Frame', 3, 18.0::numeric, 'Columns, beams, slabs RCC frame',          'EXECUTION', 60),
    ('Facade',           4, 12.0::numeric, 'Curtain wall / cladding',                  'EXECUTION', 30),
    ('Core MEP',         5, 12.0::numeric, 'Electrical, plumbing, fire mains',         'EXECUTION', 30),
    ('HVAC',             6, 10.0::numeric, 'Chillers, ducting, AHUs',                  'EXECUTION', 25),
    ('Fire Safety',      7,  6.0::numeric, 'Sprinklers, hydrants, alarms',             'EXECUTION', 15),
    ('Interior Fitout',  8, 10.0::numeric, 'Partitions, ceilings, flooring, paint',    'COMPLETION',30),
    ('Final Compliance', 9,  8.0::numeric, 'OC, fire NOC, CMRI, lift inspection',      'COMPLETION',15),
    ('Handover',        10,  7.0::numeric, 'Snag list, cleanup, handover',             'HANDOVER',   7)
) AS m(name, ord, pct, descr, phase, dur)
ON CONFLICT (template_id, milestone_order) DO NOTHING;

INSERT INTO milestone_template_tasks (milestone_template_id, task_name, task_order, estimated_days, created_at)
SELECT mt.id, t.name, t.ord, t.dur, NOW()
FROM milestone_templates mt
JOIN project_type_templates ptt ON mt.template_id = ptt.id AND ptt.code = 'COMMERCIAL'
JOIN (VALUES
    ('Site Preparation', 'Site clearing and grading',           1, 5),
    ('Site Preparation', 'Layout and benchmark',                2, 5),
    ('Foundation',       'Pile installation',                   1,15),
    ('Foundation',       'Pile cap and raft',                   2,15),
    ('Foundation',       'Curing and backfill',                 3, 5),
    ('Structural Frame', 'Column casting per floor',            1,20),
    ('Structural Frame', 'Beam and slab per floor',             2,30),
    ('Structural Frame', 'Curing',                              3,10),
    ('Facade',           'Curtain wall installation',           1,15),
    ('Facade',           'Cladding panels',                     2,10),
    ('Facade',           'Glazing and sealing',                 3, 5),
    ('Core MEP',         'Electrical risers and main panel',    1,10),
    ('Core MEP',         'Plumbing risers and PHE',             2,10),
    ('Core MEP',         'Fire mains and standpipes',           3,10),
    ('HVAC',             'Chiller installation',                1,10),
    ('HVAC',             'Ducting routing',                     2,10),
    ('HVAC',             'AHU and VAV setup',                   3, 5),
    ('Fire Safety',      'Sprinkler heads and piping',          1, 7),
    ('Fire Safety',      'Hydrant and pump',                    2, 5),
    ('Fire Safety',      'Alarms and detection panel',          3, 3),
    ('Interior Fitout',  'Partition walls',                     1,10),
    ('Interior Fitout',  'False ceilings',                      2, 8),
    ('Interior Fitout',  'Flooring',                            3, 8),
    ('Interior Fitout',  'Painting',                            4, 4),
    ('Final Compliance', 'OC application',                      1, 5),
    ('Final Compliance', 'Fire NOC',                            2, 5),
    ('Final Compliance', 'Lift inspection',                     3, 5),
    ('Handover',         'Snag walkthrough',                    1, 3),
    ('Handover',         'Defect rectification',                2, 3),
    ('Handover',         'Final cleanup and handover',          3, 1)
) AS t(milestone_name, name, ord, dur)
  ON mt.milestone_name = t.milestone_name
ON CONFLICT (milestone_template_id, task_order) DO NOTHING;

-- ============================================================
-- INTERIOR (11 milestones; weights sum 100)
-- ============================================================
INSERT INTO project_type_templates (project_type, code, description, created_at)
VALUES ('Interior Only', 'INTERIOR', 'Finishing-only project; no structural', NOW())
ON CONFLICT (code) WHERE code IS NOT NULL DO NOTHING;

WITH t AS (
  SELECT id FROM project_type_templates WHERE code = 'INTERIOR'
)
INSERT INTO milestone_templates (template_id, milestone_name, milestone_order, default_percentage, description, phase, estimated_duration_days, created_at)
SELECT t.id, m.name, m.ord, m.pct, m.descr, m.phase, m.dur, NOW()
FROM t, (VALUES
    ('Site Survey',     1,  4.0::numeric, 'Measurement, condition assessment',           'PLANNING',  3),
    ('Demolition',      2,  6.0::numeric, 'Existing partitions/finishes removal',        'EXECUTION', 5),
    ('Carpentry',       3, 18.0::numeric, 'Custom cabinetry, partitions, doors',         'EXECUTION',20),
    ('Electrical',      4, 10.0::numeric, 'Wiring, points, panel work',                  'EXECUTION',10),
    ('False Ceiling',   5,  9.0::numeric, 'Gypsum/POP ceiling work',                     'EXECUTION', 8),
    ('Painting',        6,  9.0::numeric, 'Primer, putty, two coats',                    'EXECUTION', 8),
    ('Flooring',        7, 12.0::numeric, 'Tile/wood/vinyl laying',                      'EXECUTION',10),
    ('Furniture',       8, 14.0::numeric, 'Bed, sofa, modular kitchen install',          'COMPLETION',10),
    ('Lighting',        9,  8.0::numeric, 'Fixtures, lamps, controls',                   'COMPLETION', 5),
    ('Final Touch-Up', 10,  5.0::numeric, 'Polish, cleaning, decor',                     'COMPLETION', 3),
    ('Handover',       11,  5.0::numeric, 'Walkthrough, key handover',                   'HANDOVER',   2)
) AS m(name, ord, pct, descr, phase, dur)
ON CONFLICT (template_id, milestone_order) DO NOTHING;

INSERT INTO milestone_template_tasks (milestone_template_id, task_name, task_order, estimated_days, created_at)
SELECT mt.id, t.name, t.ord, t.dur, NOW()
FROM milestone_templates mt
JOIN project_type_templates ptt ON mt.template_id = ptt.id AND ptt.code = 'INTERIOR'
JOIN (VALUES
    ('Site Survey',    'Measurement and photos',            1, 1),
    ('Site Survey',    'Existing condition report',         2, 2),
    ('Demolition',     'Partition removal',                 1, 3),
    ('Demolition',     'Old finish stripping',              2, 2),
    ('Carpentry',      'Modular kitchen cabinetry',         1, 8),
    ('Carpentry',      'Wardrobe and storage',              2, 8),
    ('Carpentry',      'Doors and frames',                  3, 4),
    ('Electrical',     'Wiring and points',                 1, 6),
    ('Electrical',     'Panel and earthing',                2, 4),
    ('False Ceiling',  'Framework installation',            1, 4),
    ('False Ceiling',  'Gypsum board and finish',           2, 4),
    ('Painting',       'Putty and primer',                  1, 3),
    ('Painting',       'Top coat (2 coats)',                2, 5),
    ('Flooring',       'Sub-floor preparation',             1, 3),
    ('Flooring',       'Tile/wood/vinyl laying',            2, 7),
    ('Furniture',      'Furniture delivery and assembly',   1, 5),
    ('Furniture',      'Modular kitchen install',           2, 5),
    ('Lighting',       'Fixture installation',              1, 3),
    ('Lighting',       'Controls and dimmers',              2, 2),
    ('Final Touch-Up', 'Polish and cleaning',               1, 2),
    ('Final Touch-Up', 'Decor placement',                   2, 1),
    ('Handover',       'Walkthrough and snag list',         1, 1),
    ('Handover',       'Final handover',                    2, 1)
) AS t(milestone_name, name, ord, dur)
  ON mt.milestone_name = t.milestone_name
ON CONFLICT (milestone_template_id, task_order) DO NOTHING;
