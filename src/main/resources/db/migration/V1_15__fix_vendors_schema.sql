ALTER TABLE vendors 
    ADD COLUMN IF NOT EXISTS contact_person VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS gstin VARCHAR(15),
    ADD COLUMN IF NOT EXISTS address TEXT,
    ADD COLUMN IF NOT EXISTS vendor_type VARCHAR(50) DEFAULT 'MATERIAL',
    ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS account_number VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ifsc_code VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();

-- Add constraints if not exist (optional but good practice, though standard SQL doesn't support IF NOT EXISTS for constraints easily without PL/PGSQL block)
-- We skip adding constraints for now to avoid complexity in simple alteration script.
