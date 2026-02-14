-- ============================================================================
-- V1_69: Create BOQ Categories Table (Hierarchy Support)
-- ============================================================================
-- Enables nested category/subcategory structure for BOQ organization.
-- Categories are project-specific and support parent-child relationships.
-- ============================================================================

CREATE TABLE IF NOT EXISTS boq_categories (
    id BIGSERIAL PRIMARY KEY,
    
    -- Relationships
    project_id BIGINT NOT NULL REFERENCES customer_projects(id) ON DELETE CASCADE,
    parent_id BIGINT REFERENCES boq_categories(id) ON DELETE RESTRICT,
    
    -- Core fields
    name VARCHAR(255) NOT NULL,
    description TEXT,
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Audit trail (BaseEntity pattern)
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by_user_id BIGINT REFERENCES portal_users(id),
    updated_by_user_id BIGINT REFERENCES portal_users(id),
    deleted_at TIMESTAMP,
    deleted_by_user_id BIGINT REFERENCES portal_users(id),
    version BIGINT NOT NULL DEFAULT 1
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_boq_categories_project ON boq_categories(project_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_boq_categories_parent ON boq_categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_boq_categories_active ON boq_categories(is_active) WHERE is_active = TRUE;

-- Add foreign key from boq_items to boq_categories
ALTER TABLE boq_items 
ADD CONSTRAINT fk_boq_items_category 
FOREIGN KEY (category_id) REFERENCES boq_categories(id) ON DELETE SET NULL;

COMMENT ON TABLE boq_categories IS 'Hierarchical categories/subcategories for organizing BOQ items within a project.';
COMMENT ON COLUMN boq_categories.parent_id IS 'Self-referencing FK. NULL = top-level category, non-NULL = subcategory.';
