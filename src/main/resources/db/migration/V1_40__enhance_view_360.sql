-- Enhance view_360 table with thumbnail and uploader info
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='view_360' AND column_name='thumbnail_url') THEN
        ALTER TABLE view_360 ADD COLUMN thumbnail_url VARCHAR(500);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='view_360' AND column_name='capture_date') THEN
        ALTER TABLE view_360 ADD COLUMN capture_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='view_360' AND column_name='uploaded_by') THEN
        ALTER TABLE view_360 ADD COLUMN uploaded_by BIGINT;
        ALTER TABLE view_360 ADD CONSTRAINT fk_view360_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES portal_users(id);
    END IF;
END $$;
