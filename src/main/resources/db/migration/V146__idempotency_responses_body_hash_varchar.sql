-- ===========================================================================
-- V146 — Coerce idempotency_responses.request_body_hash to VARCHAR(64)
--
-- Production schema declares the column as CHAR(64) (bpchar). The
-- IdempotencyResponse entity declares it as VARCHAR(64). Hibernate
-- schema-validation aborts on the type mismatch.
--
-- CHAR(64) right-pads stored values with spaces — VARCHAR(64) does not.
-- The cast trims the padding, which is the desired behaviour given the
-- column holds SHA-256 hex digests (exactly 64 chars, no padding needed).
-- ===========================================================================

ALTER TABLE idempotency_responses
    ALTER COLUMN request_body_hash TYPE VARCHAR(64)
        USING TRIM(TRAILING ' ' FROM request_body_hash);
