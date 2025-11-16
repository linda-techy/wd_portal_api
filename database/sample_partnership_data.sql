-- Sample Test Data for Partnership Users Table
-- Note: These passwords are BCrypt hashed for "Test@123"
-- Use this hash for testing: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

-- 1. Architectural Firm Partner (Active)
INSERT INTO partnership_users (
    phone, email, password_hash, full_name, designation,
    partnership_type, firm_name, gst_number, license_number,
    experience, specialization, portfolio_link,
    area_of_operation, status, created_at, updated_at
) VALUES (
    '9876543210',
    'architect@testfirm.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Rajesh Kumar',
    'Principal Architect',
    'architectural',
    'Kumar & Associates Architects',
    '32AABCU9603R1ZM',
    'AR/KL/2015/12345',
    15,
    'Residential, Commercial, Luxury Villas',
    'https://kumararchitects.com/portfolio',
    'Kochi, Trivandrum, Calicut',
    'active',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 2. Real Estate Agent (Approved - not logged in yet)
INSERT INTO partnership_users (
    phone, email, password_hash, full_name, designation,
    partnership_type, firm_name, gst_number, rera_number,
    experience, areas_covered, status, approved_at, created_at, updated_at
) VALUES (
    '9876543211',
    'realestate@properties.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Priya Menon',
    'Senior Real Estate Consultant',
    'realEstate',
    'Kerala Premium Properties',
    '32AACFP1234K1Z5',
    'RERA/KL/2020/00789',
    8,
    'Ernakulam, Kottayam, Alappuzha',
    'approved',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 3. Interior Designer (Active)
INSERT INTO partnership_users (
    phone, email, password_hash, full_name, designation,
    partnership_type, firm_name, gst_number, license_number,
    experience, specialization, portfolio_link, certifications,
    area_of_operation, status, created_at, updated_at
) VALUES (
    '9876543212',
    'interior@designs.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Anil Thomas',
    'Lead Interior Designer',
    'interiorDesigner',
    'Creative Interiors Studio',
    '32AAHCI5678P1Z2',
    'ID/KL/2018/45678',
    10,
    'Residential Interiors, Modular Kitchens, Home Automation',
    'https://creativeinteriors.in/gallery',
    'Certified Interior Designer (CID), LEED Accredited Professional',
    'Kochi, Thrissur, Palakkad',
    'active',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 4. Financial Institution Partner (Pending approval)
INSERT INTO partnership_users (
    phone, email, password_hash, full_name, designation,
    partnership_type, company_name, gst_number, cin_number, ifsc_code,
    business_size, location, message, status, created_at, updated_at
) VALUES (
    '9876543213',
    'loans@keralbank.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Suresh Pillai',
    'Branch Manager - Home Loans',
    'financial',
    'Kerala Bank Ltd.',
    '32AACCK1234L1Z9',
    'L65999KL1989PLC012345',
    'KERB0001234',
    'Enterprise',
    'Kochi Main Branch',
    'Interested in providing construction loans and home financing solutions',
    'pending',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 5. Material Supplier (Active)
INSERT INTO partnership_users (
    phone, email, password_hash, full_name, designation,
    partnership_type, company_name, gst_number, materials_supplied,
    experience, location, business_size, status, created_at, updated_at
) VALUES (
    '9876543214',
    'sales@buildmaterials.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Mohammed Ali',
    'Sales Director',
    'materialSupplier',
    'BuildPro Materials Pvt Ltd',
    '32AAECP5678M1Z7',
    'Cement, Steel, Tiles, Sanitary Ware, Electrical Fittings, Paints',
    12,
    'Kochi Industrial Area',
    'Large',
    'active',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 6. Vastu Consultant (Active)
INSERT INTO partnership_users (
    phone, email, password_hash, full_name, designation,
    partnership_type, business_name, years_of_practice, certifications,
    specialization, area_served, status, created_at, updated_at
) VALUES (
    '9876543215',
    'vastu@consultant.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Dr. Lakshmi Nair',
    'Chief Vastu Consultant',
    'vastu',
    'Vedic Vastu Consultancy',
    20,
    'PhD in Vastu Shastra, Certified Vastu Expert (CVE)',
    'Residential Vastu, Commercial Vastu, Vastu Corrections',
    'Kerala, Tamil Nadu, Karnataka',
    'active',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 7. Land Consultant (Approved)
INSERT INTO partnership_users (
    phone, email, password_hash, full_name, designation,
    partnership_type, firm_name, license_number, experience,
    land_types, areas_covered, specialization, status, approved_at, created_at, updated_at
) VALUES (
    '9876543216',
    'land@consultancy.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Vineeth Krishnan',
    'Senior Land Consultant',
    'landConsultant',
    'Kerala Land Experts',
    'LC/KL/2019/98765',
    7,
    'Residential Plots, Agricultural Land, Commercial Land',
    'Kochi, Trivandrum, Thrissur, Malappuram',
    'Land Acquisition, Legal Verification, Documentation',
    'approved',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 8. Corporate Partner (Pending)
INSERT INTO partnership_users (
    phone, email, password_hash, full_name, designation,
    partnership_type, company_name, gst_number, cin_number,
    industry, project_type, project_scale, timeline,
    location, message, status, created_at, updated_at
) VALUES (
    '9876543217',
    'projects@techcorp.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Amit Sharma',
    'Head of Administration',
    'corporate',
    'Tech Solutions India Pvt Ltd',
    '32AACCT9876N1Z3',
    'U72200KL2015PTC012345',
    'Information Technology',
    'Office Campus Development',
    'Large (50,000+ sq ft)',
    'Q1 2026',
    'InfoPark, Kochi',
    'Planning to build a new office campus with modern amenities',
    'pending',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 9. Another Architectural Firm (Suspended - for testing)
INSERT INTO partnership_users (
    phone, email, password_hash, full_name, designation,
    partnership_type, firm_name, gst_number, license_number,
    experience, specialization, area_of_operation, 
    status, created_at, updated_at
) VALUES (
    '9876543218',
    'suspended@architect.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Test Suspended User',
    'Architect',
    'architectural',
    'Suspended Architecture Firm',
    '32AABCS1111R1ZM',
    'AR/KL/2020/11111',
    5,
    'Residential',
    'Kochi',
    'suspended',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 10. Real Estate with additional contact (Active)
INSERT INTO partnership_users (
    phone, email, password_hash, full_name, designation,
    partnership_type, firm_name, gst_number, rera_number,
    experience, areas_covered, additional_contact,
    status, created_at, updated_at
) VALUES (
    '9876543219',
    'premium@realestate.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Sarah Joseph',
    'Managing Partner',
    'realEstate',
    'Premium Realty Group',
    '32AACPR7890K1Z8',
    'RERA/KL/2019/01234',
    12,
    'Ernakulam, Idukki, Wayanad',
    'Office: 0484-1234567, Assistant: 9876500001',
    'active',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- View all inserted data
-- SELECT id, full_name, phone, email, partnership_type, firm_name, company_name, status 
-- FROM partnership_users 
-- ORDER BY id;

-- Quick stats query
-- SELECT 
--     partnership_type,
--     status,
--     COUNT(*) as count
-- FROM partnership_users
-- GROUP BY partnership_type, status
-- ORDER BY partnership_type, status;

