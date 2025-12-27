-- V3__create_payment_tables.sql
-- Payment system tables for design package payments

-- Design Package Payments - Master record for project payment agreement
CREATE TABLE IF NOT EXISTS design_package_payments (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL UNIQUE,
    package_name VARCHAR(50) NOT NULL,
    rate_per_sqft NUMERIC(10,2) NOT NULL,
    total_sqft NUMERIC(10,2) NOT NULL,
    base_amount NUMERIC(15,2) NOT NULL,
    gst_percentage NUMERIC(5,2) NOT NULL DEFAULT 18.00,
    gst_amount NUMERIC(15,2) NOT NULL,
    discount_percentage NUMERIC(5,2) DEFAULT 0,
    discount_amount NUMERIC(15,2) DEFAULT 0,
    total_amount NUMERIC(15,2) NOT NULL,
    payment_type VARCHAR(20) NOT NULL CHECK (payment_type IN ('FULL', 'INSTALLMENT')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PARTIAL', 'PAID')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id BIGINT,
    CONSTRAINT fk_design_payment_project FOREIGN KEY (project_id) REFERENCES customer_projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_design_payment_created_by FOREIGN KEY (created_by_id) REFERENCES portal_users(id)
);

-- Payment Schedule - Individual installments for a design package payment
CREATE TABLE IF NOT EXISTS payment_schedule (
    id BIGSERIAL PRIMARY KEY,
    design_payment_id BIGINT NOT NULL,
    installment_number INTEGER NOT NULL,
    description VARCHAR(100) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'OVERDUE')),
    paid_amount NUMERIC(15,2) DEFAULT 0,
    paid_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedule_design_payment FOREIGN KEY (design_payment_id) REFERENCES design_package_payments(id) ON DELETE CASCADE
);

-- Payment Transactions - Actual payments recorded
CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    payment_method VARCHAR(50),
    reference_number VARCHAR(100),
    payment_date TIMESTAMP NOT NULL,
    notes TEXT,
    recorded_by_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_schedule FOREIGN KEY (schedule_id) REFERENCES payment_schedule(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_recorded_by FOREIGN KEY (recorded_by_id) REFERENCES portal_users(id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_design_payment_project ON design_package_payments(project_id);
CREATE INDEX IF NOT EXISTS idx_payment_schedule_design_payment ON payment_schedule(design_payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_schedule_status ON payment_schedule(status);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_schedule ON payment_transactions(schedule_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_date ON payment_transactions(payment_date);
