-- Partnership Users Table for Partner Login and Dashboard

CREATE TABLE partnership_users (
    id BIGSERIAL PRIMARY KEY,
    
    -- Login Credentials
    phone VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    
    -- Personal Information
    full_name VARCHAR(255) NOT NULL,
    designation VARCHAR(100),
    
    -- Partnership Details
    partnership_type VARCHAR(50) NOT NULL CHECK (partnership_type IN 
        ('architectural', 'realEstate', 'interiorDesigner', 'financial', 
         'materialSupplier', 'vastu', 'landConsultant', 'corporate')),
    
    -- Business Information
    firm_name VARCHAR(255),
    company_name VARCHAR(255),
    
    -- Verification Documents
    gst_number VARCHAR(20),
    license_number VARCHAR(100),
    rera_number VARCHAR(100),
    cin_number VARCHAR(50),
    ifsc_code VARCHAR(20),
    employee_id VARCHAR(100),
    
    -- Professional Details
    experience INTEGER,
    specialization VARCHAR(100),
    portfolio_link VARCHAR(500),
    certifications TEXT,
    
    -- Operational Details
    area_of_operation VARCHAR(255),
    areas_covered VARCHAR(255),
    land_types VARCHAR(255),
    materials_supplied VARCHAR(500),
    business_size VARCHAR(50),
    location VARCHAR(255),
    industry VARCHAR(100),
    project_type VARCHAR(100),
    project_scale VARCHAR(50),
    timeline VARCHAR(100),
    years_of_practice INTEGER,
    area_served VARCHAR(255),
    business_name VARCHAR(255),
    
    -- Additional Information
    additional_contact VARCHAR(255),
    message TEXT,
    
    -- Account Status
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN 
        ('pending', 'approved', 'active', 'suspended', 'rejected')),
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    last_login TIMESTAMP,
    
    -- Metadata
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Create indexes for better query performance
CREATE INDEX idx_partnership_users_phone ON partnership_users(phone);
CREATE INDEX idx_partnership_users_email ON partnership_users(email);
CREATE INDEX idx_partnership_users_partnership_type ON partnership_users(partnership_type);
CREATE INDEX idx_partnership_users_status ON partnership_users(status);
CREATE INDEX idx_partnership_users_created_at ON partnership_users(created_at);

-- Create updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_partnership_users_updated_at 
    BEFORE UPDATE ON partnership_users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE partnership_users IS 'Stores partner information for login and dashboard access';
COMMENT ON COLUMN partnership_users.phone IS 'Phone number used for login authentication';
COMMENT ON COLUMN partnership_users.email IS 'Email address used as login ID';
COMMENT ON COLUMN partnership_users.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN partnership_users.status IS 'Account status: pending (awaiting approval), approved (approved but not logged in), active (logged in at least once), suspended (temporarily disabled), rejected (application denied)';

