-- ============================================================================
-- V1_72: Seed BOQ Work Types and Materials
-- ============================================================================
-- Populates common construction work types and materials for BOQ system
-- ============================================================================

-- ============================================================================
-- PART 1: Seed BOQ Work Types
-- ============================================================================

INSERT INTO boq_work_types (name, description, display_order) VALUES
    ('Civil Work', 'Civil and structural construction work', 10),
    ('Electrical Work', 'Electrical installations and wiring', 20),
    ('Plumbing Work', 'Plumbing and sanitary installations', 30),
    ('HVAC Work', 'Heating, ventilation, and air conditioning', 40),
    ('Carpentry Work', 'Woodwork and carpentry', 50),
    ('Painting Work', 'Painting and finishing work', 60),
    ('Flooring Work', 'Flooring installations', 70),
    ('Roofing Work', 'Roofing and waterproofing', 80),
    ('Masonry Work', 'Brick and block masonry', 90),
    ('Steel Work', 'Structural steel and metalwork', 100),
    ('Glass & Aluminum Work', 'Windows, doors, and glazing', 110),
    ('Plastering Work', 'Plastering and rendering', 120),
    ('Tiling Work', 'Wall and floor tiling', 130),
    ('False Ceiling Work', 'Suspended ceiling installations', 140),
    ('Landscaping Work', 'External landscaping and gardening', 150),
    ('Demolition Work', 'Demolition and site clearance', 160),
    ('Excavation Work', 'Earthwork and excavation', 170),
    ('Concrete Work', 'RCC and concrete work', 180),
    ('Interior Fit-out', 'Interior finishing and fit-out', 190),
    ('MEP Work', 'Mechanical, electrical, and plumbing combined', 200)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- PART 2: Seed Materials
-- ============================================================================

-- Insert materials only if they don't already exist
-- Categories: 'CEMENT', 'STEEL', 'AGGREGATE', 'SAND', 'BRICKS_BLOCKS', 'TILES_MARBLE', 
--             'ELECTRICAL', 'PLUMBING', 'PAINTING', 'WOOD_CARPENTRY', 'HARDWARE', 'OTHERS'
-- Units: 'BAG', 'KG', 'MT', 'CFT', 'SQFT', 'NOS', 'CUM', 'LTR', 'PKT', 'BUNDLE', 'TRUCK', 'LOAD'

INSERT INTO materials (name, unit, category, is_active, created_at, updated_at, version)
SELECT * FROM (VALUES
    -- Cement & Binding Materials
    ('OPC Cement 53 Grade', 'BAG', 'CEMENT', true, NOW(), NOW(), 1),
    ('OPC Cement 43 Grade', 'BAG', 'CEMENT', true, NOW(), NOW(), 1),
    ('PPC Cement', 'BAG', 'CEMENT', true, NOW(), NOW(), 1),
    ('White Cement', 'BAG', 'CEMENT', true, NOW(), NOW(), 1),
    ('Lime', 'KG', 'CEMENT', true, NOW(), NOW(), 1),
    
    -- Sand
    ('M Sand (Manufactured Sand)', 'CFT', 'SAND', true, NOW(), NOW(), 1),
    ('River Sand', 'CFT', 'SAND', true, NOW(), NOW(), 1),
    ('P Sand (Plastering Sand)', 'CFT', 'SAND', true, NOW(), NOW(), 1),
    
    -- Aggregates
    ('20mm Jelly (Aggregate)', 'CFT', 'AGGREGATE', true, NOW(), NOW(), 1),
    ('10mm Jelly (Aggregate)', 'CFT', 'AGGREGATE', true, NOW(), NOW(), 1),
    ('Stone Chips', 'CFT', 'AGGREGATE', true, NOW(), NOW(), 1),
    
    -- Steel & Reinforcement
    ('TMT Steel Bar 8mm', 'KG', 'STEEL', true, NOW(), NOW(), 1),
    ('TMT Steel Bar 10mm', 'KG', 'STEEL', true, NOW(), NOW(), 1),
    ('TMT Steel Bar 12mm', 'KG', 'STEEL', true, NOW(), NOW(), 1),
    ('TMT Steel Bar 16mm', 'KG', 'STEEL', true, NOW(), NOW(), 1),
    ('TMT Steel Bar 20mm', 'KG', 'STEEL', true, NOW(), NOW(), 1),
    ('TMT Steel Bar 25mm', 'KG', 'STEEL', true, NOW(), NOW(), 1),
    ('Binding Wire', 'KG', 'STEEL', true, NOW(), NOW(), 1),
    ('Mild Steel Angle', 'KG', 'STEEL', true, NOW(), NOW(), 1),
    ('Mild Steel Channel', 'KG', 'STEEL', true, NOW(), NOW(), 1),
    
    -- Bricks & Blocks
    ('Red Brick (1st Class)', 'NOS', 'BRICKS_BLOCKS', true, NOW(), NOW(), 1),
    ('Wire Cut Brick', 'NOS', 'BRICKS_BLOCKS', true, NOW(), NOW(), 1),
    ('Fly Ash Brick', 'NOS', 'BRICKS_BLOCKS', true, NOW(), NOW(), 1),
    ('AAC Block 4 inch', 'NOS', 'BRICKS_BLOCKS', true, NOW(), NOW(), 1),
    ('AAC Block 6 inch', 'NOS', 'BRICKS_BLOCKS', true, NOW(), NOW(), 1),
    ('AAC Block 8 inch', 'NOS', 'BRICKS_BLOCKS', true, NOW(), NOW(), 1),
    ('Solid Concrete Block 4 inch', 'NOS', 'BRICKS_BLOCKS', true, NOW(), NOW(), 1),
    ('Solid Concrete Block 6 inch', 'NOS', 'BRICKS_BLOCKS', true, NOW(), NOW(), 1),
    
    -- Tiles & Marble
    ('Vitrified Tiles 2x2', 'SQFT', 'TILES_MARBLE', true, NOW(), NOW(), 1),
    ('Ceramic Tiles 2x2', 'SQFT', 'TILES_MARBLE', true, NOW(), NOW(), 1),
    ('Granite Flooring', 'SQFT', 'TILES_MARBLE', true, NOW(), NOW(), 1),
    ('Marble Flooring', 'SQFT', 'TILES_MARBLE', true, NOW(), NOW(), 1),
    ('Bathroom Tiles', 'SQFT', 'TILES_MARBLE', true, NOW(), NOW(), 1),
    ('Kitchen Tiles', 'SQFT', 'TILES_MARBLE', true, NOW(), NOW(), 1),
    
    -- Painting Materials
    ('Acrylic Emulsion Paint', 'LTR', 'PAINTING', true, NOW(), NOW(), 1),
    ('Synthetic Enamel Paint', 'LTR', 'PAINTING', true, NOW(), NOW(), 1),
    ('Wood Primer', 'LTR', 'PAINTING', true, NOW(), NOW(), 1),
    ('Wall Putty', 'KG', 'PAINTING', true, NOW(), NOW(), 1),
    ('Asian Paints Primer', 'LTR', 'PAINTING', true, NOW(), NOW(), 1),
    
    -- Plumbing Materials
    ('CPVC Pipe 1/2 inch', 'MT', 'PLUMBING', true, NOW(), NOW(), 1),
    ('CPVC Pipe 3/4 inch', 'MT', 'PLUMBING', true, NOW(), NOW(), 1),
    ('PVC Pipe 110mm', 'MT', 'PLUMBING', true, NOW(), NOW(), 1),
    ('PVC Pipe 160mm', 'MT', 'PLUMBING', true, NOW(), NOW(), 1),
    ('Sanitary Ware Set', 'NOS', 'PLUMBING', true, NOW(), NOW(), 1),
    ('CP Fittings', 'NOS', 'PLUMBING', true, NOW(), NOW(), 1),
    
    -- Electrical Materials
    ('PVC Conduit Pipe 25mm', 'MT', 'ELECTRICAL', true, NOW(), NOW(), 1),
    ('Electrical Wire 2.5 sqmm', 'MT', 'ELECTRICAL', true, NOW(), NOW(), 1),
    ('Electrical Wire 4 sqmm', 'MT', 'ELECTRICAL', true, NOW(), NOW(), 1),
    ('Modular Switch', 'NOS', 'ELECTRICAL', true, NOW(), NOW(), 1),
    ('Socket 5A', 'NOS', 'ELECTRICAL', true, NOW(), NOW(), 1),
    ('Socket 15A', 'NOS', 'ELECTRICAL', true, NOW(), NOW(), 1),
    ('LED Light', 'NOS', 'ELECTRICAL', true, NOW(), NOW(), 1),
    ('MCB 32A', 'NOS', 'ELECTRICAL', true, NOW(), NOW(), 1),
    ('Distribution Board', 'NOS', 'ELECTRICAL', true, NOW(), NOW(), 1),
    
    -- Wood & Carpentry
    ('Wooden Door Frame', 'MT', 'WOOD_CARPENTRY', true, NOW(), NOW(), 1),
    ('Flush Door', 'SQFT', 'WOOD_CARPENTRY', true, NOW(), NOW(), 1),
    ('Teak Wood', 'CFT', 'WOOD_CARPENTRY', true, NOW(), NOW(), 1),
    ('Plywood 18mm', 'NOS', 'WOOD_CARPENTRY', true, NOW(), NOW(), 1),
    
    -- Hardware
    ('UPVC Window', 'SQFT', 'HARDWARE', true, NOW(), NOW(), 1),
    ('Aluminum Window', 'SQFT', 'HARDWARE', true, NOW(), NOW(), 1),
    ('MS Grill', 'SQFT', 'HARDWARE', true, NOW(), NOW(), 1),
    ('Door Hinges', 'NOS', 'HARDWARE', true, NOW(), NOW(), 1),
    ('Door Lock', 'NOS', 'HARDWARE', true, NOW(), NOW(), 1),
    
    -- Others
    ('Gypsum Board 12mm', 'SQFT', 'OTHERS', true, NOW(), NOW(), 1),
    ('MS Channel for Ceiling', 'MT', 'OTHERS', true, NOW(), NOW(), 1),
    ('Pop Ceiling', 'SQFT', 'OTHERS', true, NOW(), NOW(), 1),
    ('Waterproofing Membrane', 'SQFT', 'OTHERS', true, NOW(), NOW(), 1),
    ('Dr Fixit', 'KG', 'OTHERS', true, NOW(), NOW(), 1),
    ('Bitumen', 'KG', 'OTHERS', true, NOW(), NOW(), 1),
    ('RMC M20 Grade', 'CUM', 'OTHERS', true, NOW(), NOW(), 1),
    ('RMC M25 Grade', 'CUM', 'OTHERS', true, NOW(), NOW(), 1),
    ('Labour Charges', 'NOS', 'OTHERS', true, NOW(), NOW(), 1),
    ('Mason Charges', 'NOS', 'OTHERS', true, NOW(), NOW(), 1),
    ('Carpenter Charges', 'NOS', 'OTHERS', true, NOW(), NOW(), 1),
    ('Electrician Charges', 'NOS', 'OTHERS', true, NOW(), NOW(), 1),
    ('Plumber Charges', 'NOS', 'OTHERS', true, NOW(), NOW(), 1),
    ('Helper Charges', 'NOS', 'OTHERS', true, NOW(), NOW(), 1)
) AS t(name, unit, category, is_active, created_at, updated_at, version)
WHERE NOT EXISTS (
    SELECT 1 FROM materials WHERE materials.name = t.name
);

-- Update display order for work types if needed
UPDATE boq_work_types SET display_order = id * 10 WHERE display_order IS NULL;

-- Add helpful comment
COMMENT ON TABLE boq_work_types IS 'Master list of construction work types for BOQ categorization';
COMMENT ON TABLE materials IS 'Master list of construction materials available for BOQ line items';
