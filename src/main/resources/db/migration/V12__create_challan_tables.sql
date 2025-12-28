-- Migration: Create Challan Master and Sequence tables
-- Description: Supports FY-based sequential numbering and persistent challan records.

-- Sequence table for tracking numbers per Financial Year
CREATE TABLE challan_sequences (
    id BIGSERIAL PRIMARY KEY,
    fy VARCHAR(10) NOT NULL UNIQUE,
    last_sequence INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Main Challan table
CREATE TABLE payment_challans (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL UNIQUE REFERENCES payment_transactions(id) ON DELETE RESTRICT,
    challan_number VARCHAR(50) NOT NULL UNIQUE,
    fy VARCHAR(10) NOT NULL,
    sequence_number INTEGER NOT NULL,
    transaction_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    generated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_by_id BIGINT NOT NULL REFERENCES portal_users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    
    CONSTRAINT chk_challan_status CHECK (status IN ('ISSUED', 'CANCELLED'))
);

-- Performance and Integrity Indexes
CREATE INDEX idx_payment_challans_fy ON payment_challans(fy);
CREATE INDEX idx_payment_challans_transaction_date ON payment_challans(transaction_date);
CREATE INDEX idx_payment_challans_generated_at ON payment_challans(generated_at);

-- Add comment to explain format WAL/CH/FY/NNN
COMMENT ON COLUMN payment_challans.challan_number IS 'Unique challan identifier formatted as WAL/CH/FY/NNN';
