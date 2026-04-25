-- V57: Ensure email_verified column and email_verification_tokens table exist
-- for the customer API.
--
-- Context: wd_customer_api has its own migration V1010 that adds this column
-- and table, but in local dev that migration can end up recorded-as-applied
-- without the column actually existing (e.g. when the DB was restored from
-- a snapshot predating the column). That breaks every customer login because
-- CustomUserDetailsService.findByEmail() SELECTs email_verified.
--
-- Portal owns the customer_users schema, so mirroring the DDL here guarantees
-- the column is present regardless of customer Flyway state. Both statements
-- are IF NOT EXISTS, so if V1010 already applied cleanly this is a no-op.

ALTER TABLE customer_users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES customer_users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_email_verification_token
    ON email_verification_tokens(token);
