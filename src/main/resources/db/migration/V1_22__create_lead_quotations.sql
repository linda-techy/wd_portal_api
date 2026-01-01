-- Lead Quotations/Proposals Table
-- Migration: V1_22
-- Tracks quotations and proposals sent to leads
-- Supports multiple versions, status tracking, and audit trail

CREATE TABLE lead_quotations (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(lead_id) ON DELETE CASCADE,
    quotation_number VARCHAR(50) UNIQUE NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Financial details
    total_amount NUMERIC(12,2) NOT NULL,
    tax_amount NUMERIC(12,2),
    discount_amount NUMERIC(12,2),
    final_amount NUMERIC(12,2) NOT NULL,
    
    -- Validity and status
    validity_days INTEGER DEFAULT 30,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    
    -- Tracking timestamps
    sent_at TIMESTAMP,
    viewed_at TIMESTAMP,
    responded_at TIMESTAMP,
    
    -- Audit trail
    created_by_id BIGINT REFERENCES portal_users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    notes TEXT,
    
    -- Constraints
    CONSTRAINT chk_quotation_status CHECK (
        status IN ('DRAFT', 'SENT', 'VIEWED', 'ACCEPTED', 'REJECTED', 'EXPIRED')
    ),
    CONSTRAINT chk_quotation_amounts CHECK (
        total_amount >= 0 AND 
        final_amount >= 0 AND
        (tax_amount IS NULL OR tax_amount >= 0) AND
        (discount_amount IS NULL OR discount_amount >= 0)
    )
);

-- Indexes for performance
CREATE INDEX idx_lead_quotations_lead ON lead_quotations(lead_id);
CREATE INDEX idx_lead_quotations_status ON lead_quotations(status);
CREATE INDEX idx_lead_quotations_number ON lead_quotations(quotation_number);
CREATE INDEX idx_lead_quotations_created_at ON lead_quotations(created_at DESC);

-- Trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_lead_quotation_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_lead_quotation_updated
    BEFORE UPDATE ON lead_quotations
    FOR EACH ROW
    EXECUTE FUNCTION update_lead_quotation_timestamp();

-- Quotation items table (line items for each quotation)
CREATE TABLE lead_quotation_items (
    id BIGSERIAL PRIMARY KEY,
    quotation_id BIGINT NOT NULL REFERENCES lead_quotations(id) ON DELETE CASCADE,
    item_number INTEGER NOT NULL,
    description TEXT NOT NULL,
    quantity NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit_price NUMERIC(12,2) NOT NULL,
    total_price NUMERIC(12,2) NOT NULL,
    notes TEXT,
    
    CONSTRAINT chk_quotation_item_amounts CHECK (
        quantity > 0 AND 
        unit_price >= 0 AND 
        total_price >= 0
    ),
    CONSTRAINT uq_quotation_item_number UNIQUE (quotation_id, item_number)
);

CREATE INDEX idx_lead_quotation_items_quotation ON lead_quotation_items(quotation_id);
