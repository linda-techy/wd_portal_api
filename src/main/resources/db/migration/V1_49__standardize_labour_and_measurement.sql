-- Standardize Labour and Measurement Modules
-- Migration: V1_49__standardize_labour_and_measurement.sql

-- 1. Update labour table
ALTER TABLE labour 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;

-- Add enum constraints for labour
ALTER TABLE labour
ADD CONSTRAINT chk_labour_trade_type CHECK (trade_type IN ('CARPENTER', 'PLUMBER', 'ELECTRICIAN', 'MASON', 'HELPER', 'PAINTER', 'TILER', 'WELDER', 'OTHER')),
ADD CONSTRAINT chk_labour_id_proof_type CHECK (id_proof_type IN ('AADHAAR', 'PAN', 'VOTER_ID', 'DRIVING_LICENSE', 'OTHER'));

-- 2. Update labour_attendance table
ALTER TABLE labour_attendance 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;

-- Add enum constraint for attendance status
ALTER TABLE labour_attendance
ADD CONSTRAINT chk_attendance_status CHECK (status IN ('PRESENT', 'ABSENT', 'HALF_DAY', 'LEAVE'));

-- 3. Update labour_payments table
ALTER TABLE labour_payments 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;

-- Add enum constraint for payment method
ALTER TABLE labour_payments
ADD CONSTRAINT chk_labour_payment_method CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'UPI', 'CHEQUE'));

-- 4. Update measurement_book table
ALTER TABLE measurement_book 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS deleted_by_user_id BIGINT,
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 1;
