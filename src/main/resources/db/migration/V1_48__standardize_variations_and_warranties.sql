-- Standardize Project Variations and Warranties tables with BaseEntity and Enum constraints

-- 1. Create Tables if they don't exist
CREATE TABLE IF NOT EXISTS project_variations (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES customer_projects(id),
    description TEXT NOT NULL,
    estimated_amount NUMERIC(15,2) NOT NULL,
    client_approved BOOLEAN DEFAULT FALSE,
    approved_by_id BIGINT REFERENCES portal_users(id),
    approved_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by_user_id BIGINT REFERENCES portal_users(id),
    updated_by_user_id BIGINT REFERENCES portal_users(id),
    deleted_at TIMESTAMP,
    deleted_by_user_id BIGINT REFERENCES portal_users(id),
    version BIGINT DEFAULT 1
);

CREATE TABLE IF NOT EXISTS project_warranties (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES customer_projects(id),
    component_name VARCHAR(255) NOT NULL,
    description TEXT,
    provider_name VARCHAR(255),
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    coverage_details TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by_user_id BIGINT REFERENCES portal_users(id),
    updated_by_user_id BIGINT REFERENCES portal_users(id),
    deleted_at TIMESTAMP,
    deleted_by_user_id BIGINT REFERENCES portal_users(id),
    version BIGINT DEFAULT 1
);

-- 2. Standardize project_variations (for existing tables)
DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='project_variations' AND column_name='created_by_id') THEN
        ALTER TABLE project_variations RENAME COLUMN created_by_id TO created_by_user_id;
    END IF;
END $$;

ALTER TABLE project_variations 
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;

-- Add check constraint for status
ALTER TABLE project_variations DROP CONSTRAINT IF EXISTS project_variations_status_check;
ALTER TABLE project_variations ADD CONSTRAINT project_variations_status_check 
CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED'));

-- 3. Standardize project_warranties (for existing tables)
ALTER TABLE project_warranties 
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;

-- Add check constraint for status
ALTER TABLE project_warranties DROP CONSTRAINT IF EXISTS project_warranties_status_check;
ALTER TABLE project_warranties ADD CONSTRAINT project_warranties_status_check 
CHECK (status IN ('ACTIVE', 'EXPIRED', 'VOID'));

-- Update existing null versions
UPDATE project_variations SET version = 1 WHERE version IS NULL;
UPDATE project_warranties SET version = 1 WHERE version IS NULL;
