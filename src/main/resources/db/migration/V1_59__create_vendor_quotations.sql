-- Migration: Create vendor_quotations table
-- Version: V1_59__create_vendor_quotations.sql
-- Description: Vendor quotations received against material indent requests

CREATE TABLE IF NOT EXISTS vendor_quotations (
    id BIGSERIAL PRIMARY KEY,
    indent_id BIGINT NOT NULL REFERENCES material_indents(id),
    vendor_id BIGINT NOT NULL REFERENCES vendors(id),
    quoted_amount NUMERIC(15,2) NOT NULL,
    items_included VARCHAR(255),
    delivery_charges NUMERIC(15,2),
    tax_amount NUMERIC(15,2),
    expected_delivery_date DATE,
    valid_until DATE,
    document_url VARCHAR(255),
    notes TEXT,
    status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    selected_at TIMESTAMP WITHOUT TIME ZONE,
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
CREATE INDEX IF NOT EXISTS idx_vendor_quotations_indent ON vendor_quotations(indent_id);
CREATE INDEX IF NOT EXISTS idx_vendor_quotations_vendor ON vendor_quotations(vendor_id);
CREATE INDEX IF NOT EXISTS idx_vendor_quotations_status ON vendor_quotations(status);

COMMENT ON TABLE vendor_quotations IS 'Vendor quotations against material indent requests for comparative analysis';
