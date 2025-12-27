-- V5: Create Receipt Number Sequence (Fixes Race Condition)
-- This ensures atomic, thread-safe receipt number generation

-- Create sequence for sequential numbering
CREATE SEQUENCE IF NOT EXISTS receipt_number_seq START WITH 1;

-- Create function for atomic receipt generation
CREATE OR REPLACE FUNCTION generate_receipt_number()
RETURNS VARCHAR(50) AS $$
DECLARE
    next_num BIGINT;
    year_part TEXT;
    receipt TEXT;
BEGIN
    -- Get next sequence number atomically
    next_num := nextval('receipt_number_seq');
    
    -- Extract current year
    year_part := EXTRACT(YEAR FROM CURRENT_DATE)::TEXT;
    
    -- Format: WAL/PAY/YYYY/NNNN (e.g., WAL/PAY/2025/0001)
    receipt := 'WAL/PAY/' || year_part || '/' || LPAD(next_num::TEXT, 4, '0');
    
    RETURN receipt;
END;
$$ LANGUAGE plpgsql;

-- Add comment for documentation
COMMENT ON FUNCTION generate_receipt_number() IS 'Generates unique receipt numbers in format WAL/PAY/YYYY/NNNN using atomic sequence';
COMMENT ON SEQUENCE receipt_number_seq IS 'Sequential numbering for payment receipts';
