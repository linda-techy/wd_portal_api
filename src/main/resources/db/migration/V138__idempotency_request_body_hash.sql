-- G-57: store a SHA-256 of the request body on idempotency cache rows so the
-- filter can return 409 when the same key is replayed with a different payload,
-- instead of silently returning the original cached response.
--
-- Nullable for backward compatibility with pre-existing rows; the filter
-- treats NULL as "legacy row, skip hash check" (rows expire within 24h).

ALTER TABLE idempotency_responses
    ADD COLUMN request_body_hash CHAR(64) NULL;
