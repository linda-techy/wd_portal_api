-- V7: Add Retention Money Tracking
-- Supports 5-10% retention held until defect liability period ends

-- Add retention fields to design_package_payments
ALTER TABLE design_package_payments
ADD COLUMN IF NOT EXISTS retention_percentage NUMERIC(5,2) DEFAULT 10.00 NOT NULL,
ADD COLUMN IF NOT EXISTS retention_amount NUMERIC(15,2) DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS retention_released_amount NUMERIC(15,2) DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS defect_liability_end_date DATE,
ADD COLUMN IF NOT EXISTS retention_status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL;

-- Create retention_releases table
CREATE TABLE IF NOT EXISTS retention_releases (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES design_package_payments(id) ON DELETE CASCADE,
    release_amount NUMERIC(15,2) NOT NULL,
    release_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    release_reason VARCHAR(255),
    approved_by_id BIGINT REFERENCES portal_users(id),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_retention_releases_payment 
ON retention_releases(payment_id);

CREATE INDEX IF NOT EXISTS idx_retention_releases_date 
ON retention_releases(release_date);

CREATE INDEX IF NOT EXISTS idx_design_payments_retention_status 
ON design_package_payments(retention_status);

-- Calculate retention for existing payments
UPDATE design_package_payments
SET 
    retention_amount = total_amount * (retention_percentage / 100),
    retention_status = CASE 
        WHEN status = 'PAID' THEN 'RELEASED'
        ELSE 'ACTIVE'
    END
WHERE retention_amount = 0;

-- Add documentation
COMMENT ON COLUMN design_package_payments.retention_percentage IS 'Percentage held as retention (typically 5-10% for construction)';
COMMENT ON COLUMN design_package_payments.retention_amount IS 'Total amount held as retention until defect liability ends';
COMMENT ON COLUMN design_package_payments.retention_released_amount IS 'Total amount already released from retention';
COMMENT ON COLUMN design_package_payments.defect_liability_end_date IS 'Date when retention can be released (typically 6-12 months post-completion)';
COMMENT ON COLUMN design_package_payments.retention_status IS 'ACTIVE (holding), PARTIALLY_RELEASED, or RELEASED';
COMMENT ON TABLE retention_releases IS 'Individual retention money releases with approval tracking';
