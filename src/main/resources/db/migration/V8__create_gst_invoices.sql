-- V8: Create GST Invoices Table
-- Supports GST-compliant invoice generation with CGST/SGST/IGST breakdown

-- Create invoice number sequence
CREATE SEQUENCE IF NOT EXISTS invoice_number_seq START WITH 1;

-- Create GST invoices table
CREATE TABLE IF NOT EXISTS tax_invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    payment_id BIGINT NOT NULL REFERENCES design_package_payments(id) ON DELETE CASCADE,
    
    -- GST Registration Details
    company_gstin VARCHAR(15) NOT NULL DEFAULT '29AABCU9603R1ZX',  -- Placeholder
    customer_gstin VARCHAR(15),
    place_of_supply VARCHAR(100) NOT NULL,
    is_interstate BOOLEAN DEFAULT FALSE NOT NULL,
    
    -- Financial Breakdown
    taxable_value NUMERIC(15,2) NOT NULL,  -- Amount before tax
    
    -- Intra-state (CGST + SGST)
    cgst_rate NUMERIC(5,2),
    cgst_amount NUMERIC(15,2),
    sgst_rate NUMERIC(5,2),
    sgst_amount NUMERIC(15,2),
    
    -- Inter-state (IGST)
    igst_rate NUMERIC(5,2),
    igst_amount NUMERIC(15,2),
    
    -- Total
    total_tax_amount NUMERIC(15,2) NOT NULL,
    invoice_total NUMERIC(15,2) NOT NULL,  -- taxable_value + total_tax_amount
    
    -- Metadata
    invoice_date DATE NOT NULL DEFAULT CURRENT_DATE,
    financial_year VARCHAR(10) NOT NULL,  -- e.g., '2024-25'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by_id BIGINT REFERENCES portal_users(id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_tax_invoices_payment ON tax_invoices(payment_id);
CREATE INDEX IF NOT EXISTS idx_tax_invoices_date ON tax_invoices(invoice_date);
CREATE INDEX IF NOT EXISTS idx_tax_invoices_fy ON tax_invoices(financial_year);
CREATE INDEX IF NOT EXISTS idx_tax_invoices_gstin ON tax_invoices(company_gstin, customer_gstin);

-- Function to generate invoice number: WAL/INV/FY/NNNN
CREATE OR REPLACE FUNCTION generate_invoice_number()
RETURNS VARCHAR(50) AS $$
DECLARE
    next_num BIGINT;
    fy_part TEXT;
    invoice TEXT;
    current_month INT;
    current_year INT;
BEGIN
    next_num := nextval('invoice_number_seq');
    
    -- Calculate Financial Year (April-March for India)
    current_month := EXTRACT(MONTH FROM CURRENT_DATE);
    current_year := EXTRACT(YEAR FROM CURRENT_DATE);
    
    IF current_month >= 4 THEN
        -- April-December: FY is current year to next year
        fy_part := current_year::TEXT || '-' || LPAD((current_year + 1 - 2000)::TEXT, 2, '0');
    ELSE
        -- January-March: FY is previous year to current year
        fy_part := (current_year - 1)::TEXT || '-' || LPAD((current_year - 2000)::TEXT, 2, '0');
    END IF;
    
    -- Format: WAL/INV/2024-25/0001
    invoice := 'WAL/INV/' || fy_part || '/' || LPAD(next_num::TEXT, 4, '0');
    RETURN invoice;
END;
$$ LANGUAGE plpgsql;

-- Add documentation
COMMENT ON SEQUENCE invoice_number_seq IS 'Sequential numbering for GST invoices';
COMMENT ON FUNCTION generate_invoice_number() IS 'Generates invoice numbers in format WAL/INV/FY/NNNN (e.g., WAL/INV/2024-25/0001)';
COMMENT ON TABLE tax_invoices IS 'GST-compliant tax invoices for financial reporting and GSTR-1 filing';
COMMENT ON COLUMN tax_invoices.is_interstate IS 'TRUE for IGST (interstate), FALSE for CGST+SGST (intrastate)';
COMMENT ON COLUMN tax_invoices.financial_year IS 'Indian FY format YYYY-YY (April to March)';
