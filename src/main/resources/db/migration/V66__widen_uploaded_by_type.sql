-- V66__widen_uploaded_by_type.sql
-- Existing dev DB had project_documents.uploaded_by_type pre-created
-- as VARCHAR(10) (added outside Flyway tracking). The Document entity
-- declares VARCHAR(32). Widen the column to match.
-- Idempotent: ALTER COLUMN TYPE to a wider VARCHAR is a no-op when the
-- column is already at or above the target width.

ALTER TABLE project_documents
    ALTER COLUMN uploaded_by_type TYPE VARCHAR(32);
