-- Add construction specific fields to leads table
ALTER TABLE leads
ADD COLUMN plot_area NUMERIC(10,2),
ADD COLUMN floors INTEGER;

-- Add construction specific fields to customer_projects table
ALTER TABLE customer_projects
ADD COLUMN plot_area NUMERIC(10,2),
ADD COLUMN floors INTEGER,
ADD COLUMN facing VARCHAR(20),
ADD COLUMN permit_status VARCHAR(50),
ADD COLUMN project_description TEXT;
