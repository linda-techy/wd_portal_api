-- V1_28__add_lead_conversion_tracking.sql
-- Add fields to track lead conversion to customer projects

ALTER TABLE customer_projects 
ADD COLUMN converted_from_lead_id BIGINT;

ALTER TABLE customer_projects 
ADD CONSTRAINT fk_converted_from_lead
FOREIGN KEY (converted_from_lead_id) 
REFERENCES leads(lead_id);

COMMENT ON COLUMN customer_projects.converted_from_lead_id IS 'ID of the lead this project was converted from';
