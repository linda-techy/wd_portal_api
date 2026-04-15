-- =============================================================================
-- V23: Seed BOQ Work Types
-- =============================================================================
-- Populates boq_work_types with standard construction work categories.
-- Uses INSERT ... WHERE NOT EXISTS so re-running is safe (idempotent).
-- =============================================================================

INSERT INTO boq_work_types (name, description, display_order)
SELECT name, description, display_order
FROM (VALUES
    (1,  'Civil Work',               'Foundation, structure, RCC, brickwork, and allied civil works'),
    (2,  'Masonry',                  'Brick / block masonry, partition walls, compound walls'),
    (3,  'Plastering',               'Internal and external plastering, putty, texture finishes'),
    (4,  'Flooring',                 'Tile, marble, granite, wooden, epoxy, and other floor finishes'),
    (5,  'Waterproofing',            'Terrace, basement, bathroom, and wet-area waterproofing'),
    (6,  'Electrical Work',          'Wiring, conduit, DB boards, fixtures, and power points'),
    (7,  'Plumbing & Drainage',      'Water supply, drainage, sanitary fixtures, and pipework'),
    (8,  'HVAC',                     'Air conditioning, ventilation, and HVAC ducting'),
    (9,  'Carpentry & Joinery',      'Doors, windows, frames, shutters, and custom joinery'),
    (10, 'False Ceiling',            'Gypsum, grid, POP, and other false ceiling systems'),
    (11, 'Painting',                 'Interior and exterior painting, primer, waterproof coatings'),
    (12, 'Structural Steel',         'Steel columns, beams, trusses, and fabricated structures'),
    (13, 'Roofing',                  'Roofing sheets, tiles, waterproof membrane, and skylights'),
    (14, 'Glass & Glazing',          'Aluminium windows, curtain walls, glass partitions, ACP cladding'),
    (15, 'Landscaping',              'Garden, paving, irrigation, outdoor lighting, and site work'),
    (16, 'Interior Design Works',    'Modular furniture, wardrobes, kitchen units, and decorative items'),
    (17, 'Fire Fighting & Safety',   'Fire suppression, sprinklers, extinguishers, and alarm systems'),
    (18, 'Lifts & Escalators',       'Elevator installation, escalators, and related infrastructure'),
    (19, 'Solar & Renewable Energy', 'Solar panels, inverters, batteries, and related installations'),
    (20, 'Site Preparation',         'Excavation, backfilling, compaction, dewatering, and debris removal'),
    (21, 'Miscellaneous',            'Any work not covered by other categories')
) AS v(display_order, name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM boq_work_types wt WHERE wt.name = v.name
);
