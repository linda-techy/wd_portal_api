-- Migration: Create material_indent_items table
-- Version: V1_58__create_material_indent_items.sql
-- Description: Line items for material indent requests

CREATE TABLE IF NOT EXISTS material_indent_items (
    id BIGSERIAL PRIMARY KEY,
    indent_id BIGINT NOT NULL REFERENCES material_indents(id) ON DELETE CASCADE,
    material_id BIGINT REFERENCES materials(id),
    item_name VARCHAR(255) NOT NULL,
    description TEXT,
    unit VARCHAR(255) NOT NULL,
    quantity_requested NUMERIC(15,2) NOT NULL,
    quantity_approved NUMERIC(15,2),
    po_quantity NUMERIC(15,2) DEFAULT 0.00,
    estimated_rate NUMERIC(15,2),
    estimated_amount NUMERIC(15,2),
    -- BaseEntity audit fields
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
    created_by_user_id BIGINT REFERENCES portal_users(id),
    updated_by_user_id BIGINT REFERENCES portal_users(id),
    deleted_at TIMESTAMP WITHOUT TIME ZONE,
    deleted_by_user_id BIGINT REFERENCES portal_users(id),
    version BIGINT NOT NULL DEFAULT 1
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_material_indent_items_indent ON material_indent_items(indent_id);
CREATE INDEX IF NOT EXISTS idx_material_indent_items_material ON material_indent_items(material_id);

COMMENT ON TABLE material_indent_items IS 'Individual line items within a material indent request';
