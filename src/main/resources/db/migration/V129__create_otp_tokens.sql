CREATE TABLE otp_tokens (
    id                BIGSERIAL PRIMARY KEY,
    code_hash         VARCHAR(64) NOT NULL,
    target_type       VARCHAR(32) NOT NULL,
    target_id         BIGINT NOT NULL,
    customer_user_id  BIGINT NOT NULL,
    expires_at        TIMESTAMP NOT NULL,
    used_at           TIMESTAMP NULL,
    attempts          INT NOT NULL DEFAULT 0,
    max_attempts      INT NOT NULL DEFAULT 3,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_tokens_target_unused
    ON otp_tokens (target_type, target_id, customer_user_id)
    WHERE used_at IS NULL;

CREATE INDEX idx_otp_tokens_customer_created_at
    ON otp_tokens (customer_user_id, target_type, target_id, created_at);
