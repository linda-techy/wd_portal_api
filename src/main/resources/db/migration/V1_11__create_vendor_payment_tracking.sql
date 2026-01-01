-- =============================================================================
-- PHASE 1: PROCUREMENT MODULE & VENDOR PAYMENT LIFECYCLE
-- Purpose: Create Procurement tables (PO, GRN, etc.) and Track vendor payments
-- =============================================================================

-- Drop existing if re-running (Clean slate for this failed migration)
DROP TABLE IF EXISTS vendor_payments CASCADE;
DROP TABLE IF EXISTS purchase_invoices CASCADE;
DROP TABLE IF EXISTS goods_received_notes CASCADE;
DROP TABLE IF EXISTS purchase_order_items CASCADE;
DROP TABLE IF EXISTS purchase_orders CASCADE;
DROP TABLE IF EXISTS inventory_stock CASCADE;
DROP TABLE IF EXISTS stock_adjustments CASCADE;
DROP TABLE IF EXISTS materials CASCADE;

DROP TRIGGER IF EXISTS trg_update_invoice_paid ON vendor_payments CASCADE;
DROP FUNCTION IF EXISTS update_invoice_paid_amount() CASCADE;

-- =============================================================================
-- STEP 0: Create Missing Procurement & Inventory Tables
-- =============================================================================

-- 1. Materials
CREATE TABLE IF NOT EXISTS materials (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    unit VARCHAR(50) NOT NULL,
    category VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. Purchase Orders
CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    po_number VARCHAR(50) NOT NULL UNIQUE,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id),
    project_id BIGINT NOT NULL REFERENCES customer_projects(id),
    po_date DATE NOT NULL,
    expected_delivery_date DATE,
    total_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    gst_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    net_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    created_by_id BIGINT REFERENCES portal_users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. Purchase Order Items
CREATE TABLE IF NOT EXISTS purchase_order_items (
    id BIGSERIAL PRIMARY KEY,
    po_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    material_id BIGINT REFERENCES materials(id),
    description VARCHAR(255) NOT NULL,
    quantity NUMERIC(15,2) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    rate NUMERIC(15,2) NOT NULL,
    gst_percentage NUMERIC(5,2) NOT NULL DEFAULT 18.00,
    amount NUMERIC(15,2) NOT NULL
);

-- 4. Goods Received Notes
CREATE TABLE IF NOT EXISTS goods_received_notes (
    id BIGSERIAL PRIMARY KEY,
    grn_number VARCHAR(50) NOT NULL UNIQUE,
    po_id BIGINT NOT NULL REFERENCES purchase_orders(id),
    received_date TIMESTAMP NOT NULL DEFAULT NOW(),
    received_by_id BIGINT NOT NULL,
    invoice_number VARCHAR(100),
    invoice_date DATE,
    challan_number VARCHAR(100),
    notes TEXT
);

-- 5. Inventory Stock
CREATE TABLE IF NOT EXISTS inventory_stock (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES customer_projects(id),
    material_id BIGINT NOT NULL REFERENCES materials(id),
    current_quantity NUMERIC(15,2) NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_project_material UNIQUE (project_id, material_id)
);

-- 6. Stock Adjustments
CREATE TABLE IF NOT EXISTS stock_adjustments (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES customer_projects(id),
    material_id BIGINT NOT NULL REFERENCES materials(id),
    adjustment_type VARCHAR(30) NOT NULL,
    quantity NUMERIC(15,2) NOT NULL,
    reason TEXT,
    adjusted_by_id BIGINT REFERENCES portal_users(id),
    adjusted_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- STEP 1: Create purchase_invoices table (Merged Definition)
-- =============================================================================

CREATE TABLE IF NOT EXISTS purchase_invoices (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id),
    project_id BIGINT NOT NULL REFERENCES customer_projects(id),
    po_id BIGINT REFERENCES purchase_orders(id),
    grn_id BIGINT REFERENCES goods_received_notes(id),
    vendor_invoice_number VARCHAR(100) NOT NULL,
    invoice_number VARCHAR(100), -- Internal
    invoice_date DATE NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- New columns from V1_11 logic
    invoice_amount NUMERIC(15,2),
    gst_amount NUMERIC(15,2) DEFAULT 0,
    paid_amount NUMERIC(15,2) DEFAULT 0,
    balance_due NUMERIC(15,2),
    due_date DATE,
    payment_status VARCHAR(20) DEFAULT 'UNPAID',
    payment_terms VARCHAR(100),
    
    CONSTRAINT chk_purchase_invoice_payment_status CHECK (payment_status IN ('UNPAID', 'PARTIAL', 'PAID'))
);

-- Data migration (Initialize new columns based on base amount if inserting old data, assuming empty here mostly)
-- But ensuring defaults are set
-- No UPDATE needed if we create fresh.

-- Add comments
COMMENT ON COLUMN purchase_invoices.invoice_amount IS 'Total invoice amount including taxes';
COMMENT ON COLUMN purchase_invoices.paid_amount IS 'Total amount paid so far';
COMMENT ON COLUMN purchase_invoices.balance_due IS 'Remaining amount to be paid';
COMMENT ON COLUMN purchase_invoices.payment_status IS 'UNPAID, PARTIAL, or PAID';

-- =============================================================================
-- STEP 2: Create vendor_payments table
-- =============================================================================

CREATE TABLE vendor_payments (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES purchase_invoices(id) ON DELETE CASCADE,
    payment_date DATE NOT NULL,
    
    -- Amount Breakdown
    amount_paid NUMERIC(15,2) NOT NULL,
    tds_deducted NUMERIC(15,2) DEFAULT 0,
    other_deductions NUMERIC(15,2) DEFAULT 0,
    net_paid NUMERIC(15,2) NOT NULL,
    
    -- Payment Details
    payment_mode VARCHAR(20) NOT NULL,
    transaction_reference VARCHAR(100),
    cheque_number VARCHAR(50),
    bank_name VARCHAR(255),
    
    -- Tracking
    paid_by_id BIGINT REFERENCES portal_users(id),
    approved_by_id BIGINT REFERENCES portal_users(id),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT chk_vendor_payment_net_paid 
        CHECK (net_paid = amount_paid - COALESCE(tds_deducted, 0) - COALESCE(other_deductions, 0)),
    CONSTRAINT chk_vendor_payment_mode 
        CHECK (payment_mode IN ('CASH', 'CHEQUE', 'NEFT', 'RTGS', 'UPI'))
);

-- Indexes for performance
CREATE INDEX idx_vendor_payments_invoice ON vendor_payments(invoice_id);
CREATE INDEX idx_vendor_payments_date ON vendor_payments(payment_date);
CREATE INDEX idx_vendor_payments_mode ON vendor_payments(payment_mode);

-- Comments
COMMENT ON TABLE vendor_payments IS 'Tracks all payments made to vendors against purchase invoices';
COMMENT ON COLUMN vendor_payments.tds_deducted IS 'TDS deducted as per applicable section (typically 194Q for materials)';
COMMENT ON COLUMN vendor_payments.net_paid IS 'Actual amount transferred to vendor (amount_paid - tds - deductions)';

-- =============================================================================
-- STEP 3: Create trigger to auto-update invoice paid_amount
-- =============================================================================

CREATE OR REPLACE FUNCTION update_invoice_paid_amount()
RETURNS TRIGGER AS $$
BEGIN
    -- Update the purchase_invoice with total paid and balance
    UPDATE purchase_invoices
    SET 
        paid_amount = (
            SELECT COALESCE(SUM(amount_paid), 0)
            FROM vendor_payments
            WHERE invoice_id = NEW.invoice_id
        ),
        balance_due = invoice_amount - (
            SELECT COALESCE(SUM(amount_paid), 0)
            FROM vendor_payments
            WHERE invoice_id = NEW.invoice_id
        ),
        payment_status = CASE
            WHEN (SELECT SUM(amount_paid) FROM vendor_payments WHERE invoice_id = NEW.invoice_id) >= invoice_amount
                THEN 'PAID'
            WHEN (SELECT SUM(amount_paid) FROM vendor_payments WHERE invoice_id = NEW.invoice_id) > 0
                THEN 'PARTIAL'
            ELSE 'UNPAID'
        END
    WHERE id = NEW.invoice_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_invoice_paid
    AFTER INSERT OR UPDATE ON vendor_payments
    FOR EACH ROW
    EXECUTE FUNCTION update_invoice_paid_amount();

COMMENT ON FUNCTION update_invoice_paid_amount() IS 'Auto-updates purchase_invoice paid_amount and status when vendor payment is recorded';

-- =============================================================================
-- STEP 4: Create views for accounts payable reporting
-- =============================================================================

-- View: Accounts Payable Aging Summary
CREATE OR REPLACE VIEW v_accounts_payable_aging AS
SELECT
    v.id AS vendor_id,
    v.name AS vendor_name,
    COUNT(pi.id) AS total_invoices,
    COALESCE(SUM(pi.balance_due), 0) AS total_outstanding,
    
    -- Aging buckets
    COALESCE(SUM(CASE 
        WHEN CURRENT_DATE - COALESCE(pi.due_date, pi.invoice_date) <= 30 
        THEN pi.balance_due ELSE 0 END), 0) AS due_0_30_days,
    
    COALESCE(SUM(CASE 
        WHEN CURRENT_DATE - COALESCE(pi.due_date, pi.invoice_date) BETWEEN 31 AND 60 
        THEN pi.balance_due ELSE 0 END), 0) AS due_31_60_days,
    
    COALESCE(SUM(CASE 
        WHEN CURRENT_DATE - COALESCE(pi.due_date, pi.invoice_date) > 60 
        THEN pi.balance_due ELSE 0 END), 0) AS overdue,
    
    -- Count of overdue invoices
    COUNT(CASE 
        WHEN CURRENT_DATE > COALESCE(pi.due_date, pi.invoice_date) 
            AND pi.payment_status != 'PAID'
        THEN 1 END) AS overdue_invoice_count

FROM vendors v
LEFT JOIN purchase_invoices pi ON pi.vendor_id = v.id
WHERE pi.payment_status != 'PAID' OR pi.payment_status IS NULL
GROUP BY v.id, v.name
HAVING COALESCE(SUM(pi.balance_due), 0) > 0
ORDER BY total_outstanding DESC;

COMMENT ON VIEW v_accounts_payable_aging IS 'Vendor-wise accounts payable aging (0-30, 31-60, >60 days)';

-- View: Pending Vendor Payments
CREATE OR REPLACE VIEW v_pending_vendor_payments AS
SELECT
    pi.id AS invoice_id,
    pi.invoice_number,
    pi.invoice_date,
    pi.due_date,
    v.id AS vendor_id,
    v.name AS vendor_name,
    po.project_id,
    p.name AS project_name,
    pi.invoice_amount,
    pi.paid_amount,
    pi.balance_due,
    pi.payment_status,
    CURRENT_DATE - COALESCE(pi.due_date, pi.invoice_date) AS days_overdue,
    CASE
        WHEN CURRENT_DATE > COALESCE(pi.due_date, pi.invoice_date) THEN 'OVERDUE'
        WHEN CURRENT_DATE >= COALESCE(pi.due_date, pi.invoice_date) - 7 THEN 'DUE_SOON'
        ELSE 'NOT_DUE'
    END AS urgency
FROM purchase_invoices pi
JOIN vendors v ON v.id = pi.vendor_id
LEFT JOIN purchase_orders po ON po.id = pi.po_id
LEFT JOIN customer_projects p ON p.id = po.project_id
WHERE pi.payment_status IN ('UNPAID', 'PARTIAL')
ORDER BY 
    CASE 
        WHEN CURRENT_DATE > COALESCE(pi.due_date, pi.invoice_date) THEN 1
        WHEN CURRENT_DATE >= COALESCE(pi.due_date, pi.invoice_date) - 7 THEN 2
        ELSE 3
    END,
    pi.due_date;

COMMENT ON VIEW v_pending_vendor_payments IS 'List of all pending vendor payments with urgency indicators';
