-- V67__drop_wrong_fk_on_project_documents_created_by.sql
-- project_documents.created_by_user_id has a Hibernate-generated FK
-- (fkr1yhrxuag0vpktryqfdg99wpi) pointing to customer_users(id), but
-- the column is supposed to be polymorphic — populated from BaseEntity
-- with the authenticated user's id, which can be a portal_users.id
-- (staff upload) or customer_users.id (future). The 'uploaded_by_type'
-- column disambiguates which table the id refers to.
--
-- Drop the wrong FK so portal uploads stop violating the constraint.

ALTER TABLE project_documents
    DROP CONSTRAINT IF EXISTS fkr1yhrxuag0vpktryqfdg99wpi;
