-- V51: Enhance delay_logs with structured fields for category, responsibility, duration
ALTER TABLE delay_logs ADD COLUMN IF NOT EXISTS reason_category VARCHAR(50);
ALTER TABLE delay_logs ADD COLUMN IF NOT EXISTS responsible_party VARCHAR(255);
ALTER TABLE delay_logs ADD COLUMN IF NOT EXISTS duration_days INTEGER;
ALTER TABLE delay_logs ADD COLUMN IF NOT EXISTS impact_description TEXT;
ALTER TABLE delay_logs ADD COLUMN IF NOT EXISTS reported_by BIGINT;
