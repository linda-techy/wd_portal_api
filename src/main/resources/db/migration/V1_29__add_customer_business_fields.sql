-- V1_29__add_customer_business_fields.sql
-- Add construction-specific business fields to customer_users table

ALTER TABLE customer_users
ADD COLUMN phone VARCHAR(20),
ADD COLUMN whatsapp_number VARCHAR(20),
ADD COLUMN address TEXT,
ADD COLUMN company_name VARCHAR(100),
ADD COLUMN gst_number VARCHAR(20),
ADD COLUMN lead_source VARCHAR(50),
ADD COLUMN notes TEXT;

COMMENT ON COLUMN customer_users.phone IS 'Primary contact number';
COMMENT ON COLUMN customer_users.whatsapp_number IS 'WhatsApp number for project updates';
COMMENT ON COLUMN customer_users.address IS 'Billing or correspondence address';
COMMENT ON COLUMN customer_users.company_name IS 'Company name if B2B customer';
COMMENT ON COLUMN customer_users.gst_number IS 'GSTIN for invoicing';
COMMENT ON COLUMN customer_users.lead_source IS 'Source of the customer (e.g. Website, Referral)';
