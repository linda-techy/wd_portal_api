-- V65__formalize_uploaded_by_type_on_project_documents.sql
-- The 'uploaded_by_type' column exists in the shared dev DB but was added
-- outside of Flyway tracking (schema drift). This migration formalizes the
-- column so fresh environments get it consistently.
--
-- Idempotent: ADD COLUMN IF NOT EXISTS leaves existing column alone.

ALTER TABLE project_documents
    ADD COLUMN IF NOT EXISTS uploaded_by_type VARCHAR(32) NOT NULL DEFAULT 'PORTAL';
