-- V35: Link leads to customer accounts + index for customer lead lookup

-- Nullable FK: anonymous website leads won't have a customer user ID;
-- authenticated customer enquiries will.
ALTER TABLE leads ADD COLUMN IF NOT EXISTS customer_user_id BIGINT
    REFERENCES customer_users(id);

CREATE INDEX IF NOT EXISTS idx_leads_customer_user_id
    ON leads(customer_user_id) WHERE customer_user_id IS NOT NULL;

-- Index for customer API's "my leads" lookup by email
CREATE INDEX IF NOT EXISTS idx_leads_email
    ON leads(email) WHERE deleted_at IS NULL;
