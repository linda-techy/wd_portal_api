-- =============================================================================
-- PHASE 1: SUBCONTRACTOR WORK ORDER MANAGEMENT
-- Purpose: Enable tracking of piece-rate and lump-sum contractor work
-- Business Impact: Enables 60-70% of construction work tracking
-- =============================================================================

-- Drop existing tables if they exist (for clean migration)
DROP TABLE IF EXISTS subcontract_payments CASCADE;
DROP TABLE IF EXISTS subcontract_measurements CASCADE;
DROP TABLE IF EXISTS subcontract_work_orders CASCADE;
DROP SEQUENCE IF EXISTS subcontract_wo_seq CASCADE;

-- =============================================================================
-- PREREQUISITE: VENDORS TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS vendors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(255),
    vendor_type VARCHAR(50), -- 'MATERIAL', 'LABOUR', 'BOTH'
    address TEXT,
    gst_number VARCHAR(20),
    pan_number VARCHAR(20),
    bank_name VARCHAR(100),
    account_number VARCHAR(50),
    ifsc_code VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- =============================================================================
-- TABLE 1: subcontract_work_orders
-- =============================================================================
CREATE TABLE subcontract_work_orders (
    id BIGSERIAL PRIMARY KEY,
    work_order_number VARCHAR(50) NOT NULL UNIQUE, -- WAL/SC/YY/NNN
    
    -- Relationships
    project_id BIGINT NOT NULL REFERENCES customer_projects(id) ON DELETE CASCADE,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id),
    boq_item_id BIGINT REFERENCES boq_items(id),
    
    -- Scope and Terms
    scope_description TEXT NOT NULL,
    measurement_basis VARCHAR(20) NOT NULL DEFAULT 'UNIT_RATE', 
    -- 'LUMPSUM', 'UNIT_RATE'
    negotiated_amount NUMERIC(15,2) NOT NULL,
    unit VARCHAR(50), -- For unit-rate contracts (sqft, cum, etc.)
    rate NUMERIC(15,2), -- For unit-rate contracts
    
    -- Timeline
    start_date DATE,
    target_completion_date DATE,
    actual_completion_date DATE,
    
    -- Payment Terms
    payment_terms TEXT, -- e.g., "30% advance, 40% mid, 30% completion"
    advance_percentage NUMERIC(5,2) DEFAULT 0,
    advance_paid NUMERIC(15,2) DEFAULT 0,
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    -- 'DRAFT', 'ISSUED', 'IN_PROGRESS', 'COMPLETED', 'TERMINATED'
    
    -- Tracking
    created_by_id BIGINT REFERENCES portal_users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Notes
    notes TEXT,
    termination_reason TEXT,
    
    -- Constraints
    CONSTRAINT chk_measurement_basis CHECK (measurement_basis IN ('LUMPSUM', 'UNIT_RATE')),
    CONSTRAINT chk_status CHECK (status IN ('DRAFT', 'ISSUED', 'IN_PROGRESS', 'COMPLETED', 'TERMINATED')),
    CONSTRAINT chk_unit_rate_fields CHECK (
        (measurement_basis = 'LUMPSUM') OR 
        (measurement_basis = 'UNIT_RATE' AND unit IS NOT NULL AND rate IS NOT NULL)
    )
);

-- Indexes for performance
CREATE INDEX idx_subcontract_wo_project ON subcontract_work_orders(project_id);
CREATE INDEX idx_subcontract_wo_vendor ON subcontract_work_orders(vendor_id);
CREATE INDEX idx_subcontract_wo_status ON subcontract_work_orders(status) WHERE status NOT IN ('COMPLETED', 'TERMINATED');
CREATE INDEX idx_subcontract_wo_boq ON subcontract_work_orders(boq_item_id);

-- Sequence for work order numbers
CREATE SEQUENCE subcontract_wo_seq START 1;

-- Comments
COMMENT ON TABLE subcontract_work_orders IS 'Tracks subcontractor work orders for piece-rate and lump-sum contracts';
COMMENT ON COLUMN subcontract_work_orders.measurement_basis IS 'LUMPSUM: Fixed price for entire scope. UNIT_RATE: Price per unit of measurement';
COMMENT ON COLUMN subcontract_work_orders.payment_terms IS 'Human-readable payment milestone description';

-- =============================================================================
-- TABLE 2: subcontract_measurements
-- =============================================================================
CREATE TABLE subcontract_measurements (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES subcontract_work_orders(id) ON DELETE CASCADE,
    measurement_date DATE NOT NULL,
    
    -- Measurement Details
    description VARCHAR(255) NOT NULL,
    quantity NUMERIC(15,2) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    rate NUMERIC(15,2) NOT NULL,
    amount NUMERIC(15,2) NOT NULL, -- Auto-calculated: quantity * rate
    
    -- Running Bill Number
    bill_number VARCHAR(50),
    
    -- Approval Workflow
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', 
    -- 'PENDING', 'APPROVED', 'REJECTED'
    approved_by_id BIGINT REFERENCES portal_users(id),
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    
    -- Tracking
    measured_by_id BIGINT REFERENCES portal_users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT chk_measurement_amount CHECK (amount = quantity * rate),
    CONSTRAINT chk_measurement_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- Indexes
CREATE INDEX idx_subcontract_meas_wo ON subcontract_measurements(work_order_id);
CREATE INDEX idx_subcontract_meas_date ON subcontract_measurements(measurement_date);
CREATE INDEX idx_subcontract_meas_status ON subcontract_measurements(status) WHERE status = 'PENDING';

-- Comments
COMMENT ON TABLE subcontract_measurements IS 'Progress measurements for unit-rate subcontracts (like Measurement Book for subcontractors)';
COMMENT ON COLUMN subcontract_measurements.bill_number IS 'Running bill number (RA Bill 1, RA Bill 2, etc.)';

-- =============================================================================
-- TABLE 3: subcontract_payments
-- =============================================================================
CREATE TABLE subcontract_payments (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES subcontract_work_orders(id) ON DELETE CASCADE,
    payment_date DATE NOT NULL,
    
    -- Amount Breakdown
    gross_amount NUMERIC(15,2) NOT NULL,
    tds_percentage NUMERIC(5,2) NOT NULL DEFAULT 1.00, 
    -- Usually 1% for individuals, 2% for companies under section 194C
    tds_amount NUMERIC(15,2) NOT NULL,
    other_deductions NUMERIC(15,2) DEFAULT 0,
    net_amount NUMERIC(15,2) NOT NULL,
    
    -- Payment Details
    payment_mode VARCHAR(20) NOT NULL, 
    -- 'CASH', 'CHEQUE', 'NEFT', 'RTGS', 'UPI'
    transaction_reference VARCHAR(100),
    cheque_number VARCHAR(50),
    bank_name VARCHAR(255),
    
    -- Milestone (for lump-sum contracts)
    milestone_description VARCHAR(255),
    milestone_percentage NUMERIC(5,2),
    is_advance_payment BOOLEAN DEFAULT FALSE,
    
    -- Link to measurements (for unit-rate contracts)
    measurement_ids BIGINT[], -- Array of measurement IDs covered by this payment
    
    -- Tracking
    paid_by_id BIGINT REFERENCES portal_users(id),
    approved_by_id BIGINT REFERENCES portal_users(id),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT chk_payment_net_amount CHECK (net_amount = gross_amount - tds_amount - COALESCE(other_deductions, 0)),
    CONSTRAINT chk_payment_tds_amount CHECK (tds_amount = ROUND(gross_amount * tds_percentage / 100, 2)),
    CONSTRAINT chk_payment_mode CHECK (payment_mode IN ('CASH', 'CHEQUE', 'NEFT', 'RTGS', 'UPI'))
);

-- Indexes
CREATE INDEX idx_subcontract_pay_wo ON subcontract_payments(work_order_id);
CREATE INDEX idx_subcontract_pay_date ON subcontract_payments(payment_date);

-- Comments
COMMENT ON TABLE subcontract_payments IS 'Payment records for subcontractors with TDS calculation';
COMMENT ON COLUMN subcontract_payments.tds_percentage IS 'TDS under section 194C: 1% for individuals, 2% for companies';
COMMENT ON COLUMN subcontract_payments.measurement_ids IS 'Array of measurement IDs being paid in this transaction';

-- =============================================================================
-- TRIGGERS
-- =============================================================================

-- Trigger to update work order status based on completion
CREATE OR REPLACE FUNCTION update_work_order_status()
RETURNS TRIGGER AS $$
BEGIN
    -- Auto-update status to IN_PROGRESS when first measurement is added
    IF NEW.status = 'APPROVED' THEN
        UPDATE subcontract_work_orders
        SET status = 'IN_PROGRESS'
        WHERE id = NEW.work_order_id
        AND status = 'ISSUED';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_wo_status_on_measurement
    AFTER INSERT OR UPDATE ON subcontract_measurements
    FOR EACH ROW
    EXECUTE FUNCTION update_work_order_status();

-- =============================================================================
-- VIEWS FOR REPORTING
-- =============================================================================

-- View: Subcontract Work Order Summary
CREATE OR REPLACE VIEW v_subcontract_summary AS
SELECT 
    wo.id AS work_order_id,
    wo.work_order_number,
    wo.project_id,
    p.name AS project_name,
    wo.vendor_id,
    v.name AS vendor_name,
    wo.scope_description,
    wo.negotiated_amount AS total_contract_amount,
    wo.measurement_basis,
    wo.status,
    
    -- For UNIT_RATE contracts
    COALESCE(SUM(m.amount) FILTER (WHERE m.status = 'APPROVED'), 0) AS total_measured_amount,
    COALESCE(COUNT(m.id) FILTER (WHERE m.status = 'PENDING'), 0) AS pending_measurements,
    
    -- Payments
    COALESCE(SUM(pay.gross_amount), 0) AS total_paid,
    COALESCE(SUM(pay.tds_amount), 0) AS total_tds,
    
    -- Balance
    CASE 
        WHEN wo.measurement_basis = 'LUMPSUM' 
        THEN wo.negotiated_amount - COALESCE(SUM(pay.gross_amount), 0)
        ELSE COALESCE(SUM(m.amount) FILTER (WHERE m.status = 'APPROVED'), 0) - COALESCE(SUM(pay.gross_amount), 0)
    END AS balance_due,
    
    wo.start_date,
    wo.target_completion_date,
    wo.actual_completion_date
    
FROM subcontract_work_orders wo
JOIN customer_projects p ON p.id = wo.project_id
JOIN vendors v ON v.id = wo.vendor_id
LEFT JOIN subcontract_measurements m ON m.work_order_id = wo.id
LEFT JOIN subcontract_payments pay ON pay.work_order_id = wo.id
GROUP BY wo.id, p.name, v.name;

COMMENT ON VIEW v_subcontract_summary IS 'Summary view of all subcontract work orders with financials';

-- =============================================================================
-- SAMPLE DATA (for testing)
-- =============================================================================

-- Note: Uncomment below for development/testing

/*
-- Ensure we have a LABOUR type vendor
INSERT INTO vendors (name, phone, email, vendor_type, is_active)
VALUES 
    ('Ramesh Masonry Contractors', '9876543210', 'ramesh@masonry.com', 'LABOUR', true),
    ('Kumar Electrical Works', '9876543211', 'kumar@electrical.com', 'LABOUR', true),
    ('Venkat Tiling Services', '9876543212', 'venkat@tiles.com', 'LABOUR', true)
ON CONFLICT DO NOTHING;

-- Sample work order
INSERT INTO subcontract_work_orders (
    work_order_number, project_id, vendor_id, scope_description,
    measurement_basis, negotiated_amount, unit, rate, status
)
VALUES (
    'WAL/SC/25/001', 
    1, -- Assuming project ID 1 exists
    (SELECT id FROM vendors WHERE name = 'Ramesh Masonry Contractors' LIMIT 1),
    'Internal and external plastering for entire building',
    'UNIT_RATE',
    500000.00,
    'sqft',
    450.00,
    'ISSUED'
);
*/

-- =============================================================================
-- GRANTS (adjust as per your user setup)
-- =============================================================================

-- Grant permissions to application user
-- GRANT SELECT, INSERT, UPDATE, DELETE ON subcontract_work_orders TO your_app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON subcontract_measurements TO your_app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON subcontract_payments TO your_app_user;
-- GRANT USAGE ON SEQUENCE subcontract_wo_seq TO your_app_user;
-- GRANT SELECT ON v_subcontract_summary TO your_app_user;

-- =============================================================================
-- END OF MIGRATION
-- =============================================================================
